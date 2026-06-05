const storeRepo = require('../db/storeRepository');
const stripeService = require('../services/stripeService');
const fulfillment = require('../services/storeFulfillmentService');
const subscriptionRepo = require('../db/subscriptionRepository');
const entitlementRepo = require('../db/entitlementRepository');
const { buildRevokeCommand } = require('../services/entitlementExpirationService');
const pool = require('../database');
const promoRepo = require('../db/promoRepository');
const { sanitizeRemoteCommand } = require('../utils/commandWhitelist');

async function queueRevokeForSubscription(sub) {
  const product = await storeRepo.getProductById(sub.product_id);
  if (!product) return;
  const payload = product.reward_payload || {};
  const ent = {
    minecraft_username: sub.minecraft_username,
    entitlement_type: product.reward_type === 'rank_sub' ? 'rank_sub' : 'rank_temp',
    payload,
  };
  const cmd = buildRevokeCommand(ent);
  if (cmd) {
    const safe = sanitizeRemoteCommand(cmd);
    if (safe) await pool.query('INSERT INTO commands (command) VALUES ($1)', [safe]);
  }
  await entitlementRepo.revokeBySubscription(sub.id);
}

async function recordProcessedEvent(eventId, eventType) {
  await pool.query(
    'INSERT INTO store_stripe_events (event_id, event_type) VALUES ($1, $2) ON CONFLICT (event_id) DO NOTHING',
    [eventId, eventType]
  );
}

async function handleCheckoutCompleted(session) {
  if (session.mode === 'payment' && session.payment_status !== 'paid') {
    return { skipped: 'not_paid' };
  }
  if (session.mode === 'subscription' && session.status !== 'complete') {
    return { skipped: 'not_complete' };
  }

  const order = await storeRepo.getOrderByStripeSession(session.id);
  if (!order) {
    console.warn('[Stripe] Order not found for session', session.id);
    return { warning: 'order_not_found' };
  }

  if (session.metadata?.order_id && String(session.metadata.order_id) !== String(order.id)) {
    console.warn('[Stripe] metadata.order_id mismatch', session.id, session.metadata.order_id, order.id);
    return { error: 'metadata_mismatch' };
  }

  if (session.amount_total != null && order.amount_cents != null
      && Number(session.amount_total) !== Number(order.amount_cents)) {
    console.warn('[Stripe] amount mismatch', session.id, session.amount_total, order.amount_cents);
    return { error: 'amount_mismatch' };
  }

  if (order.status === 'fulfilled') {
    return { duplicate: true };
  }

  const paid = await storeRepo.markOrderPaid(
    order.id,
    session.payment_intent || session.subscription || 'stripe'
  );
  if (!paid) {
    if (order.status === 'fulfilled') return { duplicate: true };
    return { skipped: 'already_processed' };
  }

  let subscriptionId = null;
  if (session.mode === 'subscription' && session.subscription) {
    const subRow = await subscriptionRepo.upsertSubscription({
      accountId: order.account_id,
      productId: order.product_id,
      uuid: order.minecraft_uuid,
      username: order.minecraft_username,
      stripeSubscriptionId: session.subscription,
      stripeCustomerId: session.customer,
      status: 'active',
      currentPeriodEnd: null,
      cancelAtPeriodEnd: false,
    });
    subscriptionId = subRow?.id;
    await storeRepo.attachSubscriptionToOrder(order.id, session.subscription);
  }

  if (paid.promo_code_id) {
    await promoRepo.tryClaimRedemption({
      promoId: paid.promo_code_id,
      accountId: paid.account_id,
      orderId: paid.id,
      uuid: paid.minecraft_uuid,
    });
  }

  await fulfillment.fulfillOrder(order.id, { subscriptionId });
  console.log(`[Stripe] Order #${order.id} fulfilled for ${order.minecraft_username}`);
  return { fulfilled: true };
}

async function handleStripeWebhook(req, res) {
  const signature = req.headers['stripe-signature'];
  if (!signature) {
    return res.status(400).send('missing signature');
  }

  let event;
  try {
    event = stripeService.constructWebhookEvent(req.body, signature);
  } catch (err) {
    console.error('[Stripe] Webhook signature failed:', err.message);
    return res.status(400).send(`Webhook Error: ${err.message}`);
  }

  const dup = await pool.query('SELECT 1 FROM store_stripe_events WHERE event_id = $1', [event.id]);
  if (dup.rows.length > 0) {
    return res.json({ received: true, duplicate: true });
  }

  try {
    let payload = { received: true };

    if (event.type === 'checkout.session.completed') {
      const result = await handleCheckoutCompleted(event.data.object);
      if (result.error) {
        console.warn('[Stripe] Checkout rejected:', result.error, event.id);
        return res.status(422).json({ received: true, rejected: result.error });
      }
      payload = { received: true, ...result };
    } else if (event.type === 'invoice.paid') {
      const invoice = event.data.object;
      if (invoice.billing_reason === 'subscription_create') {
        payload = { received: true, skipped: 'initial_invoice' };
      } else {
        const subId = invoice.subscription;
        if (subId) {
          const sub = await subscriptionRepo.findByStripeId(subId);
          if (sub) {
            const product = await storeRepo.getProductById(sub.product_id);
            if (product) {
              const periodEnd = invoice.lines?.data?.[0]?.period?.end
                ? new Date(invoice.lines.data[0].period.end * 1000)
                : null;
              await subscriptionRepo.updateStatus(subId, 'active', { currentPeriodEnd: periodEnd });

              const built = fulfillment.buildCommands(product, sub.minecraft_username);
              for (const cmd of built.commands) {
                const safe = sanitizeRemoteCommand(cmd);
                if (safe) await pool.query('INSERT INTO commands (command) VALUES ($1)', [safe]);
              }
              const days = product.reward_payload?.days || 30;
              await entitlementRepo.createEntitlement({
                uuid: sub.minecraft_uuid,
                username: sub.minecraft_username,
                type: 'rank_sub',
                payload: product.reward_payload,
                subscriptionId: sub.id,
                expiresAt: periodEnd || new Date(Date.now() + days * 86400000),
              });
              console.log(`[Stripe] Subscription renewed: ${sub.minecraft_username}`);
            }
          }
        }
      }
    } else if (event.type === 'customer.subscription.deleted'
        || event.type === 'customer.subscription.updated') {
      const stripeSub = event.data.object;
      const sub = await subscriptionRepo.findByStripeId(stripeSub.id);
      if (sub) {
        const status = stripeSub.status;
        await subscriptionRepo.updateStatus(stripeSub.id, status, {
          currentPeriodEnd: stripeSub.current_period_end
            ? new Date(stripeSub.current_period_end * 1000)
            : null,
          cancelAtPeriodEnd: stripeSub.cancel_at_period_end,
        });

        if (status === 'canceled' || status === 'unpaid') {
          await queueRevokeForSubscription(sub);
          console.log(`[Stripe] Subscription revoked perks: ${sub.minecraft_username}`);
        }
      }
    }

    await recordProcessedEvent(event.id, event.type);
    return res.json(payload);
  } catch (err) {
    console.error('[Stripe] Webhook handler error:', err);
    return res.status(500).json({ error: 'webhook_handler_failed' });
  }
}

module.exports = { handleStripeWebhook };
