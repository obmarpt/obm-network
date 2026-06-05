const { Router } = require('express');
const { pluginOrAuthMiddleware } = require('../auth');
const { requireAdmin, requireMod } = require('../middleware/permissions');
const arenaRepo = require('../db/arenaRepository');
const db = require('../database');
const { broadcast } = require('../ws');
const { badRequest, internalError, isNonEmptyString } = require('./validation');

const router = Router();

function requireDb(_req, res, next) {
  if (!db.isDatabaseAvailable()) {
    return res.status(503).json({ error: 'database_unavailable' });
  }
  next();
}

function requireAdminOrPlugin(req, res, next) {
  if (req.authSource === 'plugin') {
    return next();
  }
  return requireAdmin(req, res, next);
}

function requireModOrPlugin(req, res, next) {
  if (req.authSource === 'plugin') {
    return next();
  }
  return requireMod(req, res, next);
}

function parseArenaBody(body) {
  if (!body || typeof body !== 'object') {
    return { error: 'invalid_body' };
  }
  if (!isNonEmptyString(body.id)) {
    return { error: 'id_required' };
  }
  if (!isNonEmptyString(body.name)) {
    return { error: 'name_required' };
  }
  const mode = (body.mode || 'sword').toLowerCase();
  const world = body.world || 'TierSpace';
  const pos1 = body.pos1 || body.pos1_spawn;
  const pos2 = body.pos2 || body.pos2_spawn;
  if (!pos1 || !pos2) {
    return { error: 'positions_required' };
  }
  return {
    id: String(body.id).trim().slice(0, 64),
    name: String(body.name).trim().slice(0, 128),
    mode,
    world: String(world).trim().slice(0, 64),
    pos1: {
      x: Number(pos1.x),
      y: Number(pos1.y),
      z: Number(pos1.z),
      yaw: Number(pos1.yaw ?? 0),
      pitch: Number(pos1.pitch ?? 0),
    },
    pos2: {
      x: Number(pos2.x),
      y: Number(pos2.y),
      z: Number(pos2.z),
      yaw: Number(pos2.yaw ?? 180),
      pitch: Number(pos2.pitch ?? 0),
    },
    enabled: body.enabled !== false,
    auto_reset: body.auto_reset === true,
  };
}

router.use(pluginOrAuthMiddleware);

router.get('/admin/arenas', requireModOrPlugin, requireDb, async (req, res) => {
  try {
    const mode = req.query.mode ? String(req.query.mode).toLowerCase() : null;
    const enabledOnly = req.query.enabled === 'true';
    const arenas = await arenaRepo.listArenas({ mode, enabledOnly });
    res.json({ arenas });
  } catch (err) {
    return internalError(res, err, 'admin/arenas GET');
  }
});

router.get('/admin/arenas/:id', requireModOrPlugin, requireDb, async (req, res) => {
  try {
    const arena = await arenaRepo.getArena(req.params.id);
    if (!arena) return res.status(404).json({ error: 'not_found' });
    res.json(arena);
  } catch (err) {
    return internalError(res, err, 'admin/arenas/:id GET');
  }
});

router.post('/admin/arenas', requireAdmin, requireDb, async (req, res) => {
  const parsed = parseArenaBody(req.body);
  if (parsed.error) {
    return badRequest(res, parsed.error);
  }
  try {
    const existing = await arenaRepo.getArena(parsed.id);
    if (existing) {
      return res.status(409).json({ error: 'arena_exists' });
    }
    const arena = await arenaRepo.createArena(parsed);
    broadcast({ type: 'arenas_updated' });
    res.status(201).json(arena);
  } catch (err) {
    return internalError(res, err, 'admin/arenas POST');
  }
});

router.put('/admin/arenas/:id', requireAdmin, requireDb, async (req, res) => {
  try {
    const parsed = parseArenaBody({ ...req.body, id: req.params.id });
    if (parsed.error) {
      return badRequest(res, parsed.error);
    }
    const arena = await arenaRepo.updateArena(req.params.id, parsed);
    if (!arena) return res.status(404).json({ error: 'not_found' });
    broadcast({ type: 'arenas_updated' });
    res.json(arena);
  } catch (err) {
    return internalError(res, err, 'admin/arenas PUT');
  }
});

router.delete('/admin/arenas/:id', requireAdmin, requireDb, async (req, res) => {
  try {
    const deleted = await arenaRepo.deleteArena(req.params.id);
    if (!deleted) return res.status(404).json({ error: 'not_found' });
    broadcast({ type: 'arenas_updated' });
    res.json({ ok: true, id: deleted });
  } catch (err) {
    return internalError(res, err, 'admin/arenas DELETE');
  }
});

module.exports = router;
