const express = require('express');
const { query } = require('../db');

const router = express.Router();

/**
 * POST /stats/sync
 * Body: { uuid, name, coins, emeralds, kills, playtime }
 * Upsert para leaderboards (PostgreSQL).
 */
router.post('/sync', async (req, res) => {
  try {
    const { uuid, name, coins, emeralds, kills, playtime } = req.body || {};

    if (!uuid || typeof uuid !== 'string' || uuid.length < 32) {
      return res.status(400).json({ error: 'invalid_uuid' });
    }
    const safeName = (name && String(name).trim().slice(0, 32)) || 'Unknown';
    const safeCoins = Math.max(0, parseInt(coins, 10) || 0);
    const safeEmeralds = Math.max(0, parseInt(emeralds, 10) || 0);
    const safeKills = Math.max(0, parseInt(kills, 10) || 0);
    const safePlaytime = Math.max(0, parseInt(playtime, 10) || 0);

    await query(
      `INSERT INTO players (uuid, name, coins, emeralds, kills, playtime, updated_at)
       VALUES ($1, $2, $3, $4, $5, $6, NOW())
       ON CONFLICT (uuid) DO UPDATE SET
         name = EXCLUDED.name,
         coins = EXCLUDED.coins,
         emeralds = EXCLUDED.emeralds,
         kills = EXCLUDED.kills,
         playtime = EXCLUDED.playtime,
         updated_at = NOW()`,
      [uuid.toLowerCase(), safeName, safeCoins, safeEmeralds, safeKills, safePlaytime]
    );

    res.json({ ok: true, uuid: uuid.toLowerCase() });
  } catch (err) {
    console.error('[STATS] sync', err.message);
    res.status(500).json({ error: 'internal_error' });
  }
});

module.exports = router;
