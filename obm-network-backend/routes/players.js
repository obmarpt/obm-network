const { Router } = require('express');
const pool = require('../database');
const { authMiddleware, pluginOrAuthMiddleware } = require('../auth');
const { broadcast } = require('../ws');
const { sqlPlayerSelect, col } = require('../database');
const { isNonEmptyString, validateUuid, badRequest, internalError } = require('./validation');
const { playerJoinLimiter } = require('../middleware/rateLimit');

const router = Router();

router.post('/playerJoin', playerJoinLimiter, pluginOrAuthMiddleware, async (req, res) => {
  const { name, world } = req.body || {};
  const uuid = validateUuid(res, req.body?.uuid);
  if (!uuid) return;

  if (!isNonEmptyString(name)) return badRequest(res, 'name inválido');
  if (!isNonEmptyString(world)) return badRequest(res, 'world inválido');

  try {
    const bansRepo = require('../db/bansRepository');
    const activeBan = await bansRepo.getActiveBan(uuid);
    if (activeBan) {
      return res.status(403).json({
        banned: true,
        reason: activeBan.reason,
        expires_at: activeBan.expires_at,
        banned_by: activeBan.banned_by,
      });
    }

    const playerRepo = require('../db/playerRepository');
    await playerRepo.ensurePlayer(uuid, name.trim());
    await pool.query(
      'UPDATE players SET world = $2, updated_at = NOW() WHERE uuid = $1',
      [uuid, world.trim().slice(0, 64)]
    );

    const adminRepo = require('../db/adminRepository');
    const statsHistory = require('../services/statsHistoryService');
    await statsHistory.incrementDailyJoin();
    await adminRepo.insertServerEvent({
      type: 'join',
      actorUuid: uuid,
      actorName: name.trim(),
      detail: world.trim(),
    });

    console.log(`✅ Player sync: ${name} (${uuid}) → ${world}`);
    broadcast({ type: 'update' });
    res.sendStatus(200);
  } catch (err) {
    return internalError(res, err, 'playerJoin');
  }
});

router.get('/players', authMiddleware, async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT ${sqlPlayerSelect()} FROM players ORDER BY COALESCE(updated_at, created_at) DESC`
    );
    console.log(`📋 GET /players → ${result.rows.length} registos`);
    res.json(result.rows.map((row) => ({ ...row, rank: row.rank || 'bronze' })));
  } catch (err) {
    return internalError(res, err, 'players');
  }
});

router.get('/player/:uuid', pluginOrAuthMiddleware, async (req, res) => {
  const uuid = validateUuid(res, req.params.uuid);
  if (!uuid) return;

  try {
    const playerRepo = require('../db/playerRepository');
    let row = await playerRepo.getPlayerFull(uuid);
    if (!row) {
      await playerRepo.ensurePlayer(uuid, 'Unknown');
      row = await playerRepo.getPlayerFull(uuid);
    }
    if (!row) {
      console.log(`❌ Player não encontrado: ${uuid}`);
      return res.sendStatus(404);
    }

    const balance = Number(row.coins) || 0;
    const rank = row.rank || 'bronze';

    console.log(`✅ GET /player/${uuid} → coins=${balance}, emeralds=${row.emeralds}, rank=${rank}`);

    res.json({
      uuid: row.uuid,
      name: row.username,
      username: row.username,
      coins: balance,
      balance,
      money: balance,
      emeralds: Number(row.emeralds) || 0,
      rank,
      smp_level: Number(row.smp_level) || 1,
      hc_level: Number(row.hc_level) || 1,
      global_level: Number(row.global_level) || 1,
      kills: Number(row.smp_kills) || 0,
      playtime: Number(row.smp_playtime) || 0,
      smp: {
        kills: Number(row.smp_kills) || 0,
        deaths: Number(row.smp_deaths) || 0,
        playtime: Number(row.smp_playtime) || 0,
      },
      hc: {
        hc_kills: Number(row.hc_kills) || 0,
        hc_deaths: Number(row.hc_deaths) || 0,
        hc_playtime: Number(row.hc_playtime) || 0,
        hc_wins: Number(row.hc_wins) || 0,
        hc_lives: Number(row.hc_lives) ?? 1,
      },
      tierspace: {
        rating: Number(row.rating) || 1000,
        wins: Number(row.ts_wins) || 0,
        losses: Number(row.ts_losses) || 0,
      },
    });
  } catch (err) {
    return internalError(res, err, 'player/:uuid');
  }
});

module.exports = router;
