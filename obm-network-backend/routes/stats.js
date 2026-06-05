const { Router } = require('express');
const pool = require('../database');
const { authMiddleware, pluginOrAuthMiddleware } = require('../auth');
const { broadcast } = require('../ws');
const { col } = require('../database');
const {
  isNonEmptyString,
  parseInteger,
  validateUuid,
  badRequest,
  internalError,
} = require('./validation');

const router = Router();

router.get('/stats/global', authMiddleware, async (req, res) => {
  try {
    const coinsCol = col('coins');
    const result = await pool.query(`
      SELECT
        COUNT(*)::int AS total_players,
        COALESCE(SUM(${coinsCol}), 0)::bigint AS total_coins,
        COALESCE(AVG(${coinsCol}), 0)::float AS avg_coins
      FROM players
    `);

    const row = result.rows[0];
    res.json({
      totalPlayers: row.total_players,
      totalCoins: Number(row.total_coins),
      avgCoins: Math.round(Number(row.avg_coins) * 100) / 100,
    });
  } catch (err) {
    return internalError(res, err, 'stats/global');
  }
});

router.post('/stats/sync', pluginOrAuthMiddleware, async (req, res) => {
  const { uuid, name, coins, emeralds, kills, playtime } = req.body || {};
  const safeUuid = validateUuid(res, uuid);
  if (!safeUuid) return;

  const safeName = isNonEmptyString(name) ? name.trim().slice(0, 32) : 'Unknown';
  const safeCoins = Math.max(0, parseInteger(coins) ?? 0);
  const safeEmeralds = Math.max(0, parseInteger(emeralds) ?? 0);
  const safeKills = Math.max(0, parseInteger(kills) ?? 0);
  const safePlaytime = Math.max(0, parseInteger(playtime) ?? 0);

  try {
    const coinsCol = col('coins');
    const emeraldsCol = col('emeralds');

    await pool.query(
      `INSERT INTO players (uuid, name, ${coinsCol}, ${emeraldsCol}, kills, playtime, updated_at)
       VALUES ($1, $2, $3, $4, $5, $6, NOW())
       ON CONFLICT (uuid) DO UPDATE SET
         name = EXCLUDED.name,
         ${coinsCol} = EXCLUDED.${coinsCol},
         ${emeraldsCol} = EXCLUDED.${emeraldsCol},
         kills = EXCLUDED.kills,
         playtime = EXCLUDED.playtime,
         updated_at = NOW()`,
      [safeUuid, safeName, safeCoins, safeEmeralds, safeKills, safePlaytime]
    );

    console.log(`✅ Stats sync: ${safeName} (${safeUuid})`);
    broadcast({ type: 'update' });
    res.json({ ok: true, uuid: safeUuid });
  } catch (err) {
    return internalError(res, err, 'stats/sync');
  }
});

router.get('/top/coins', authMiddleware, async (req, res) => {
  try {
    const coinsCol = col('coins');
    const result = await pool.query(
      `SELECT name, uuid, ${coinsCol} AS coins FROM players ORDER BY ${coinsCol} DESC LIMIT 10`
    );
    res.json(result.rows);
  } catch (err) {
    return internalError(res, err, 'top/coins');
  }
});

router.get('/top/emeralds', authMiddleware, async (req, res) => {
  try {
    const emeraldsCol = col('emeralds');
    const result = await pool.query(
      `SELECT name, uuid, ${emeraldsCol} AS emeralds FROM players ORDER BY ${emeraldsCol} DESC LIMIT 10`
    );
    res.json(result.rows);
  } catch (err) {
    return internalError(res, err, 'top/emeralds');
  }
});

module.exports = router;
