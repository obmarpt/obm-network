let stripeClient = null;

function getStripe() {
  if (stripeClient) return stripeClient;
  const key = process.env.STRIPE_SECRET_KEY;
  if (!key) return null;
  // eslint-disable-next-line global-require
  const Stripe = require('stripe');
  stripeClient = new Stripe(key, { apiVersion: '2024-11-20.acacia' });
  return stripeClient;
}

function isConfigured() {
  return Boolean(process.env.STRIPE_SECRET_KEY && process.env.STRIPE_WEBHOOK_SECRET);
}

function siteUrl() {
  return (process.env.SITE_URL || 'http://localhost:3000').replace(/\/$/, '');
}

async function createCheckoutSession({
  product,
  username,
  customerEmail,
  orderId,
  finalAmountCents,
  promoCode,
}) {
  const stripe = getStripe();
  if (!stripe) throw new Error('stripe_not_configured');

  const amount = finalAmountCents ?? product.price_cents;
  const isSub = Boolean(product.is_subscription);

  const lineItem = isSub
    ? {
        price_data: {
          currency: product.currency || 'eur',
          unit_amount: amount,
          recurring: { interval: product.subscription_interval || 'month' },
          product_data: {
            name: product.name,
            description: product.description?.slice(0, 200) || undefined,
            metadata: { slug: product.slug },
          },
        },
        quantity: 1,
      }
    : {
        price_data: {
          currency: product.currency || 'eur',
          unit_amount: amount,
          product_data: {
            name: product.name,
            description: product.description?.slice(0, 200) || undefined,
            metadata: { slug: product.slug },
          },
        },
        quantity: 1,
      };

  const session = await stripe.checkout.sessions.create({
    mode: isSub ? 'subscription' : 'payment',
    payment_method_types: ['card'],
    customer_email: customerEmail || undefined,
    line_items: [lineItem],
    metadata: {
      order_id: String(orderId || 0),
      product_slug: product.slug,
      minecraft_username: username,
      promo_code: promoCode || '',
    },
    subscription_data: isSub
      ? { metadata: { product_slug: product.slug, minecraft_username: username } }
      : undefined,
    success_url: `${siteUrl()}/store.html?success=1&session_id={CHECKOUT_SESSION_ID}`,
    cancel_url: `${siteUrl()}/store.html?cancelled=1`,
  });

  return session;
}

function constructWebhookEvent(rawBody, signature) {
  const stripe = getStripe();
  const secret = process.env.STRIPE_WEBHOOK_SECRET;
  if (!stripe || !secret) throw new Error('stripe_webhook_not_configured');
  return stripe.webhooks.constructEvent(rawBody, signature, secret);
}

async function recordStripeEvent(eventId, eventType) {
  const db = require('../database');
  try {
    await db.query(
      'INSERT INTO store_stripe_events (event_id, event_type) VALUES ($1, $2) ON CONFLICT DO NOTHING',
      [eventId, eventType]
    );
    const check = await db.query('SELECT event_id FROM store_stripe_events WHERE event_id = $1', [eventId]);
    return check.rows.length > 0;
  } catch {
    return true;
  }
}

module.exports = {
  getStripe,
  isConfigured,
  createCheckoutSession,
  constructWebhookEvent,
  siteUrl,
  recordStripeEvent,
};
