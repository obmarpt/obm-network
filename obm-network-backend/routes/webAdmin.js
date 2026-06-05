const { Router } = require('express');
const { authMiddleware } = require('../auth');
const { requireMod, requireAdmin } = require('../middleware/permissions');
const db = require('../database');
const { internalError, parseInteger } = require('./validation');

const router = Router();

router.use(authMiddleware);

router.get('/admin/web/accounts', requireMod, async (req, res) => {
  try {
    const page = parseInteger(req.query.page) || 1;
    const lim = 30;
    const offset = (page - 1) * lim;
    const count = await db.query('SELECT COUNT(*)::int AS c FROM web_accounts');
    const res2 = await db.query(
      `SELECT id, email, minecraft_username, minecraft_uuid, email_verified, created_at
       FROM web_accounts ORDER BY created_at DESC LIMIT $1 OFFSET $2`,
      [lim, offset]
    );
    res.json({ total: count.rows[0]?.c || 0, page, accounts: res2.rows });
  } catch (err) {
    return internalError(res, err, 'admin/web/accounts');
  }
});

router.get('/admin/web/fulfillment-log', requireAdmin, async (req, res) => {
  try {
    const r = await db.query(
      `SELECT l.*, o.minecraft_username, o.status AS order_status
       FROM store_fulfillment_log l
       JOIN store_orders o ON o.id = l.order_id
       ORDER BY l.created_at DESC LIMIT 100`
    );
    res.json({ logs: r.rows });
  } catch (err) {
    return internalError(res, err, 'admin/web/fulfillment-log');
  }
});

module.exports = router;
