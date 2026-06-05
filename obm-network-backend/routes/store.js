const { Router } = require('express');
const rateLimit = require('express-rate-limit');
const storeRepo = require('../db/storeRepository');
const promoRepo = require('../db/promoRepository');
const stripeService = require('../services/stripeService');
const {
  playerAuthMiddleware,
  optionalPlayerAuth,
} = require('../middleware/playerAuth');
const webAuthRepo = require('../db/webAuthRepository');
const fulfillment = require('../services/storeFulfillmentService');
const fs = require('fs');
const path = require('path');
const { badRequest, internalError } = require('./validation');

const router = Router();

const checkoutLimiter = rateLimit({
  windowMs: 60_000,
  max: 10,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'too_many_checkouts' },
});

router.get('/api/store/tiers', (_req, res) => {
  try {
    const raw = fs.readFileSync(path.join(__dirname, '..', 'data', 'vip-tiers.json'), 'utf8');
    const data = JSON.parse(raw);
    if (!data.matrix && Array.isArray(data.tiers) && data.tiers.length) {
      data.matrix = {
        highlightIndex: data.tiers.findIndex((t) => t.highlight) ?? data.tiers.length - 1,
        columns: data.tiers.map((t) => t.name),
        rows: [
          { label: 'Money Boost', values: data.tiers.map((t) => t.moneyBoost), bestIndex: data.tiers.length - 1 },
          { label: 'Homes extra', values: data.tiers.map((t) => t.homes), bestIndex: data.tiers.length - 1 },
          { label: 'Keys / dia', values: data.tiers.map((t) => String(t.keys || '—')), bestIndex: data.tiers.length - 1 },
        ],
      };
    }
    res.json(data);
  } catch {
    res.status(500).json({ error: 'tiers_unavailable' });
  }
});

router.get('/api/store/products', async (_req, res) => {
  try {
    const products = await storeRepo.listActiveProducts();
    res.json({ products });
  } catch (err) {
    return internalError(res, err, 'store/products');
  }
});

router.get('/api/store/products/:slug', async (req, res) => {
  try {
    const product = await storeRepo.getProductBySlug(req.params.slug);
    if (!product) return res.status(404).json({ error: 'not_found' });
    res.json({ product });
  } catch (err) {
    return internalError(res, err, 'store/product');
  }
});

router.post('/api/store/promo/redeem', playerAuthMiddleware, async (req, res) => {
  const { code } = req.body || {};
  if (!code) return badRequest(res, 'code obrigatório');
  try {
    const account = await webAuthRepo.findById(req.player.accountId);
    if (!account?.minecraft_username || !account.minecraft_uuid) {
      return badRequest(res, 'liga a conta Minecraft primeiro');
    }
    const result = await promoRepo.validateStarterRedeem({
      code,
      accountId: account.id,
    });
    if (!result.valid) return res.status(400).json({ error: result.error });

    const claim = await promoRepo.tryClaimRedemption({
      promoId: result.promo.id,
      accountId: account.id,
      orderId: null,
      uuid: account.minecraft_uuid,
    });
    if (!claim.claimed) return res.status(400).json({ error: claim.error || 'already_used' });

    const player = account.minecraft_username;
    const queued = [];
    for (const grant of result.grants) {
      const cmds = fulfillment.buildSingleReward(grant.type, grant, player);
      for (const cmd of cmds) {
        await fulfillment.queueCommand(cmd);
        queued.push(cmd);
      }
    }
    res.json({ ok: true, commands: queued });
  } catch (err) {
    return internalError(res, err, 'store/promo/redeem');
  }
});

router.post('/api/store/promo/validate', optionalPlayerAuth, async (req, res) => {
  const { code, productId } = req.body || {};
  const pid = parseInt(productId, 10);
  if (!code || !pid) return badRequest(res, 'code e productId obrigatórios');
  try {
    const product = await storeRepo.getProductById(pid);
    if (!product) return res.status(404).json({ error: 'product_not_found' });
    const result = await promoRepo.validateForCheckout({
      code,
      accountId: req.player?.accountId,
      priceCents: product.price_cents,
    });
    if (!result.valid) return res.status(400).json({ error: result.error });
    res.json({
      valid: true,
      code: result.promo.code,
      discountCents: result.discountCents,
      finalCents: result.finalCents,
      finalDisplay: (result.finalCents / 100).toFixed(2),
    });
  } catch (err) {
    return internalError(res, err, 'store/promo');
  }
});

router.post('/api/store/checkout', checkoutLimiter, playerAuthMiddleware, async (req, res) => {
  const { productId, minecraftUsername, promoCode } = req.body || {};
  const pid = parseInt(productId, 10);
  if (!pid) return badRequest(res, 'productId inválido');
  if (!stripeService.isConfigured()) return res.status(503).json({ error: 'payments_unavailable' });

  try {
    const product = await storeRepo.getProductById(pid);
    if (!product || !product.active) return res.status(404).json({ error: 'product_not_found' });

    const account = await webAuthRepo.findById(req.player.accountId);
    if (!account) return res.status(401).json({ error: 'unauthorized' });
    if (process.env.REQUIRE_EMAIL_VERIFY === 'true' && !account.email_verified) {
      return res.status(403).json({ error: 'email_not_verified' });
    }

    if (!account.minecraft_uuid || !account.minecraft_username) {
      return res.status(403).json({ error: 'minecraft_not_linked' });
    }
    const username = account.minecraft_username;
    const uuid = account.minecraft_uuid;

    let finalAmountCents = product.price_cents;
    let promoCodeId = null;
    let discountCents = 0;

    if (promoCode) {
      const promoResult = await promoRepo.validateForCheckout({
        code: promoCode,
        accountId: account.id,
        priceCents: product.price_cents,
      });
      if (!promoResult.valid) return res.status(400).json({ error: promoResult.error });
      finalAmountCents = promoResult.finalCents;
      discountCents = promoResult.discountCents;
      promoCodeId = promoResult.promo.id;
    }

    const order = await storeRepo.createOrder({
      accountId: account.id,
      productId: product.id,
      minecraftUuid: uuid,
      minecraftUsername: username,
      stripeSessionId: null,
      amountCents: finalAmountCents,
      currency: product.currency,
      promoCodeId,
      discountCents,
      isSubscription: product.is_subscription,
    });

    const session = await stripeService.createCheckoutSession({
      orderId: order.id,
      product,
      username,
      customerEmail: account.email,
      finalAmountCents,
      promoCode: promoCode || '',
    });

    await require('../database').query(
      'UPDATE store_orders SET stripe_session_id = $2 WHERE id = $1',
      [order.id, session.id]
    );

    res.json({
      checkoutUrl: session.url,
      sessionId: session.id,
      orderId: order.id,
      finalAmount: (finalAmountCents / 100).toFixed(2),
    });
  } catch (err) {
    return internalError(res, err, 'store/checkout');
  }
});

router.get('/api/store/orders', playerAuthMiddleware, async (req, res) => {
  try {
    const orders = await storeRepo.listOrdersByAccount(req.player.accountId, { limit: 20 });
    res.json({ orders });
  } catch (err) {
    return internalError(res, err, 'store/orders');
  }
});

router.get('/api/store/order/status', optionalPlayerAuth, async (req, res) => {
  const sessionId = req.query.session_id;
  if (!sessionId) return badRequest(res, 'session_id obrigatório');
  try {
    const order = await storeRepo.getOrderByStripeSession(String(sessionId));
    if (!order) return res.status(404).json({ error: 'not_found' });
    if (req.player && order.account_id !== req.player.accountId) {
      return res.status(403).json({ error: 'forbidden' });
    }
    res.json({
      status: order.status,
      fulfilled: order.status === 'fulfilled',
      username: order.minecraft_username,
    });
  } catch (err) {
    return internalError(res, err, 'store/order/status');
  }
});

module.exports = router;
