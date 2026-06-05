const { Router } = require('express');
const { pluginOrAuthMiddleware, pluginOnlyAuth } = require('../auth');
const playerRepo = require('../db/playerRepository');
const { validateUuid, badRequest, internalError, sanitizePlayerSaveBody } = require('./validation');

const router = Router();

router.get('/player/:uuid/full', pluginOrAuthMiddleware, async (req, res) => {
  const uuid = validateUuid(res, req.params.uuid);
  if (!uuid) return;

  try {
    let row = await playerRepo.getPlayerFull(uuid);
    if (!row) {
      await playerRepo.ensurePlayer(uuid, 'Unknown');
      row = await playerRepo.getPlayerFull(uuid);
    }
    if (!row) {
      return res.status(404).json({ error: 'not_found' });
    }

    const balance = Number(row.coins) || 0;
    res.json({
      uuid: row.uuid,
      username: row.username,
      name: row.username,
      coins: balance,
      balance,
      money: balance,
      emeralds: Number(row.emeralds) || 0,
      rank: row.rank || 'bronze',
      smp_level: Number(row.smp_level) || 1,
      hc_level: Number(row.hc_level) || 1,
      global_level: Number(row.global_level) || 1,
      world: row.world || '',
      smp: {
        kills: Number(row.smp_kills) || 0,
        deaths: Number(row.smp_deaths) || 0,
        playtime: Number(row.smp_playtime) || 0,
        money_earned: Number(row.money_earned) || 0,
        blocks_broken: Number(row.blocks_broken) || 0,
      },
      hc: {
        hc_kills: Number(row.hc_kills) || 0,
        hc_deaths: Number(row.hc_deaths) || 0,
        hc_playtime: Number(row.hc_playtime) || 0,
        hc_totems_used: Number(row.hc_totems_used) || 0,
        hc_wins: Number(row.hc_wins) || 0,
        hc_level: Number(row.hc_stat_level) || Number(row.hc_level) || 1,
      },
      tierspace: {
        rating: Number(row.rating) || 1000,
        wins: Number(row.ts_wins) || 0,
        losses: Number(row.ts_losses) || 0,
        winstreak: Number(row.winstreak) || 0,
        best_winstreak: Number(row.best_winstreak) || 0,
      },
    });
  } catch (err) {
    return internalError(res, err, 'player/full');
  }
});

router.post('/player/save', pluginOnlyAuth, async (req, res) => {
  const body = req.body || {};
  const uuid = validateUuid(res, body.uuid);
  if (!uuid) return;

  try {
    await playerRepo.savePlayerFull(sanitizePlayerSaveBody(body, uuid));
    res.json({ ok: true, uuid });
  } catch (err) {
    return internalError(res, err, 'player/save');
  }
});

router.get('/leaderboard/bulk', pluginOrAuthMiddleware, async (req, res) => {
  try {
    const raw = String(req.query.metrics || '');
    const metrics = raw.split(',')
      .map((m) => m.trim().toLowerCase())
      .filter(Boolean)
      .slice(0, 15);
    if (!metrics.length) {
      return badRequest(res, 'metrics obrigatório (csv)');
    }
    const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 10));
    const results = [];
    for (const metric of metrics) {
      const data = await playerRepo.getLeaderboard(metric, limit);
      if (!data.error) {
        results.push({ metric, entries: data.entries });
      }
    }
    res.json({ results });
  } catch (err) {
    return internalError(res, err, 'leaderboard/bulk');
  }
});

router.get('/leaderboard/:metric', pluginOrAuthMiddleware, async (req, res) => {
  try {
    const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 10));
    const data = await playerRepo.getLeaderboard(req.params.metric, limit);
    if (data.error) {
      return badRequest(res, data.error);
    }
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'leaderboard');
  }
});

module.exports = router;
