const { Router } = require('express');
const { authMiddleware } = require('../auth');
const { requireMod, requireAdmin } = require('../middleware/permissions');
const storeRepo = require('../db/storeRepository');
const promoRepo = require('../db/promoRepository');
const subscriptionRepo = require('../db/subscriptionRepository');
const fulfillment = require('../services/storeFulfillmentService');
const { badRequest, internalError, parseInteger, sanitizeProductPatch } = require('./validation');

const router = Router();

router.use(authMiddleware);

router.get('/admin/store/products', requireMod, async (_req, res) => {
  try {
    const products = await storeRepo.listAllProducts();
    res.json({ products });
  } catch (err) {
    return internalError(res, err, 'admin/store/products');
  }
});

router.patch('/admin/store/products/:id', requireAdmin, async (req, res) => {
  const id = parseInteger(req.params.id);
  if (!id) return badRequest(res, 'id inválido');
  const patch = sanitizeProductPatch(req.body);
  if (!patch) return badRequest(res, 'payload inválido');
  try {
    const updated = await storeRepo.updateProduct(id, patch);
    if (!updated) return res.status(404).json({ error: 'not_found' });
    res.json({ product: updated });
  } catch (err) {
    return internalError(res, err, 'admin/store/product patch');
  }
});

router.get('/admin/store/orders', requireMod, async (req, res) => {
  try {
    const page = parseInteger(req.query.page) || 1;
    const status = req.query.status || null;
    const data = await storeRepo.listOrders({ page, limit: 30, status });
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'admin/store/orders');
  }
});

router.get('/admin/store/subscriptions', requireMod, async (req, res) => {
  try {
    const status = req.query.status || null;
    const subs = await subscriptionRepo.listAll({ status, page: 1, limit: 50 });
    res.json({ subscriptions: subs });
  } catch (err) {
    return internalError(res, err, 'admin/store/subscriptions');
  }
});

router.get('/admin/store/promos', requireAdmin, async (_req, res) => {
  try {
    const promos = await promoRepo.listAll();
    const redemptions = await promoRepo.listRedemptions({ limit: 30 });
    res.json({ promos, redemptions });
  } catch (err) {
    return internalError(res, err, 'admin/store/promos');
  }
});

router.post('/admin/store/promos', requireAdmin, async (req, res) => {
  const { code, type, value, max_uses, per_account_limit, expires_at } = req.body || {};
  if (!code || !type || value == null) return badRequest(res, 'dados incompletos');
  try {
    const promo = await promoRepo.createPromo({
      code,
      type,
      value,
      max_uses,
      per_account_limit,
      expires_at,
    });
    res.status(201).json({ promo });
  } catch (err) {
    return internalError(res, err, 'admin/store/promo create');
  }
});

router.post('/admin/store/orders/:id/fulfill', requireAdmin, async (req, res) => {
  const id = parseInteger(req.params.id);
  if (!id) return badRequest(res, 'id inválido');
  try {
    const order = await storeRepo.getOrderById(id);
    if (!order) return res.status(404).json({ error: 'not_found' });
    if (order.status === 'pending') {
      await storeRepo.markOrderPaid(id, 'manual_admin');
    }
    const result = await fulfillment.fulfillOrder(id);
    res.json(result);
  } catch (err) {
    return internalError(res, err, 'admin/store/fulfill');
  }
});

module.exports = router;
