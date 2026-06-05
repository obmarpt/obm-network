const express = require('express');
const { query } = require('../db');

const router = express.Router();

/**
 * POST /money/add
 * Body: { uuid, amount } — rejeita campo "coins".
 */
router.post('/add', async (req, res) => {
  try {
    const { uuid, amount } = req.body || {};
    if (req.body?.coins !== undefined) {
      console.warn('[MONEY] Rejected body with "coins" — use "amount" only');
      return res.status(400).json({ error: 'use_amount_not_coins' });
    }

    const safeUuid = String(uuid || '').toLowerCase();
    const delta = parseInt(amount, 10);
    if (!safeUuid || safeUuid.length < 32 || !Number.isFinite(delta) || delta <= 0) {
      return res.status(400).json({ error: 'invalid_payload' });
    }

    await query(
      `INSERT INTO players (uuid, coins, updated_at)
       VALUES ($1, $2, NOW())
       ON CONFLICT (uuid) DO UPDATE SET
         coins = players.coins + EXCLUDED.coins,
         updated_at = NOW()`,
      [safeUuid, delta]
    );

    const row = await query('SELECT coins FROM players WHERE uuid = $1', [safeUuid]);
    const balance = Number(row.rows[0]?.coins) || 0;

    res.json({ ok: true, uuid: safeUuid, amount: delta, balance, coins: balance });
  } catch (err) {
    console.error('[MONEY] add', err.message);
    res.status(500).json({ error: 'internal_error' });
  }
});

module.exports = router;
