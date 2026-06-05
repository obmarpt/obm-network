const { Router } = require('express');
const pool = require('../database');
const { authMiddleware, pluginOnlyAuth } = require('../auth');
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

router.post('/stats/sync', pluginOnlyAuth, async (req, res) => {
  const body = req.body || {};
  const safeUuid = validateUuid(res, body.uuid);
  if (!safeUuid) return;

  const safeName = isNonEmptyString(body.name) ? body.name.trim().slice(0, 32) : 'Unknown';
  const safeCoins = Math.max(0, parseInteger(body.coins) ?? 0);
  const safeEmeralds = Math.max(0, parseInteger(body.emeralds) ?? 0);
  const safeKills = Math.max(0, parseInteger(body.kills) ?? 0);
  const safePlaytime = Math.max(0, parseInteger(body.playtime) ?? 0);
  const smp = body.smp || {};
  const hc = body.hc || {};
  const ts = body.tierspace || {};

  try {
    const playerRepo = require('../db/playerRepository');
    await playerRepo.savePlayerFull({
      uuid: safeUuid,
      username: safeName,
      name: safeName,
      coins: safeCoins,
      emeralds: safeEmeralds,
      smp_level: parseInteger(body.smp_level) ?? 1,
      hc_level: parseInteger(body.hc_level) ?? 1,
      global_level: parseInteger(body.global_level) ?? 1,
      rank: body.rank || 'bronze',
      smp: {
        kills: parseInteger(smp.kills) ?? safeKills,
        deaths: parseInteger(smp.deaths) ?? 0,
        playtime: parseInteger(smp.playtime) ?? safePlaytime,
        money_earned: parseInteger(smp.money_earned) ?? 0,
        blocks_broken: parseInteger(smp.blocks_broken) ?? 0,
      },
      hc: hc,
      tierspace: ts,
    });

    console.log(`✅ Stats sync: ${safeName} (${safeUuid})`);
    broadcast({ type: 'update' });
    res.json({ ok: true, uuid: safeUuid });
  } catch (err) {
    return internalError(res, err, 'stats/sync');
  }
});

router.get('/top/kills', authMiddleware, async (req, res) => {
  try {
    const playerRepo = require('../db/playerRepository');
    const data = await playerRepo.getLeaderboard('smp_kills', 10);
    res.json(data.entries.map((e) => ({ uuid: e.uuid, name: e.name, kills: e.value })));
  } catch (err) {
    return internalError(res, err, 'top/kills');
  }
});

router.get('/top/playtime', authMiddleware, async (req, res) => {
  try {
    const playerRepo = require('../db/playerRepository');
    const data = await playerRepo.getLeaderboard('smp_playtime', 10);
    res.json(data.entries.map((e) => ({ uuid: e.uuid, name: e.name, playtime: e.value })));
  } catch (err) {
    return internalError(res, err, 'top/playtime');
  }
});

router.get('/top/coins', authMiddleware, async (req, res) => {
  try {
    const playerRepo = require('../db/playerRepository');
    const data = await playerRepo.getLeaderboard('coins', 10);
    res.json(data.entries.map((e) => ({ uuid: e.uuid, name: e.name, coins: e.value })));
  } catch (err) {
    return internalError(res, err, 'top/coins');
  }
});

router.get('/top/emeralds', authMiddleware, async (req, res) => {
  try {
    const playerRepo = require('../db/playerRepository');
    const data = await playerRepo.getLeaderboard('emeralds', 10);
    res.json(data.entries.map((e) => ({ uuid: e.uuid, name: e.name, emeralds: e.value })));
  } catch (err) {
    return internalError(res, err, 'top/emeralds');
  }
});

router.get('/top/:metric', authMiddleware, async (req, res) => {
  try {
    const playerRepo = require('../db/playerRepository');
    const data = await playerRepo.getLeaderboard(req.params.metric, 10);
    if (data.error) {
      return res.status(400).json(data);
    }
    res.json(data.entries);
  } catch (err) {
    return internalError(res, err, 'top/:metric');
  }
});

module.exports = router;
