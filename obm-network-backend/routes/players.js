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
    await pool.query(
      `INSERT INTO players (uuid, name, world, updated_at)
       VALUES ($1, $2, $3, NOW())
       ON CONFLICT (uuid)
       DO UPDATE SET name = EXCLUDED.name, world = EXCLUDED.world, updated_at = NOW()`,
      [uuid, name.trim(), world.trim()]
    );

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
    const coinsCol = col('coins');
    const emeraldsCol = col('emeralds');
    const result = await pool.query(
      `SELECT uuid, name, ${coinsCol} AS coins, ${emeraldsCol} AS emeralds, rank
       FROM players WHERE uuid = $1`,
      [uuid]
    );

    if (result.rows.length === 0) {
      console.log(`❌ Player não encontrado: ${uuid}`);
      return res.sendStatus(404);
    }

    const player = result.rows[0];
    const balance = Number(player.coins) || 0;
    const rank = player.rank || 'bronze';

    console.log(`✅ GET /player/${uuid} → coins=${balance}, emeralds=${player.emeralds}, rank=${rank}`);

    res.json({
      uuid: player.uuid,
      name: player.name,
      coins: balance,
      balance,
      money: balance,
      emeralds: Number(player.emeralds) || 0,
      rank,
    });
  } catch (err) {
    return internalError(res, err, 'player/:uuid');
  }
});

module.exports = router;
