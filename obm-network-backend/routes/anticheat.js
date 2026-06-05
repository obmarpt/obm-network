const { Router } = require('express');
const { pluginOnlyAuth } = require('../auth');
const anticheatRepo = require('../db/anticheatRepository');
const adminRepo = require('../db/adminRepository');
const bansRepo = require('../db/bansRepository');
const securityRepo = require('../db/securityRepository');
const { broadcast } = require('../ws');
const { badRequest, internalError, isNonEmptyString, validateUuid } = require('./validation');
const { pluginWriteLimiter } = require('../middleware/rateLimit');

const router = Router();

router.post('/anticheat/violations', pluginOnlyAuth, async (req, res) => {
  const body = req.body || {};
  const rows = Array.isArray(body.violations) ? body.violations : body.flags;
  if (!Array.isArray(rows) || rows.length === 0) {
    return badRequest(res, 'violations array obrigatório');
  }
  if (rows.length > 100) {
    return badRequest(res, 'max 100 violations por request');
  }
  try {
    const count = await anticheatRepo.insertViolationsBatch(rows);
    res.json({ ok: true, inserted: count });
  } catch (err) {
    return internalError(res, err, 'anticheat/violations');
  }
});

router.post('/anticheat/alerts', pluginOnlyAuth, async (req, res) => {
  const body = req.body || {};
  const uuid = validateUuid(res, body.uuid);
  if (!uuid) return;
  try {
    const alert = await anticheatRepo.createAlert({
      uuid,
      username: body.username || body.name,
      flagCount: body.flag_count || body.flagCount,
      windowSeconds: body.window_seconds || body.windowSeconds || 10,
      reason: body.reason,
      lastCheck: body.last_check || body.lastCheck,
    });
    await adminRepo.insertServerEvent({
      type: 'anticheat',
      actorUuid: uuid,
      actorName: body.username,
      detail: body.reason || `flags=${body.flag_count}`,
    });
    broadcast({ type: 'anticheat_alert', alert });
    res.json({ ok: true, alert });
  } catch (err) {
    return internalError(res, err, 'anticheat/alerts');
  }
});

router.post('/anticheat/mining', pluginWriteLimiter, pluginOnlyAuth, async (req, res) => {
  const logs = Array.isArray(req.body?.logs) ? req.body.logs : [];
  if (logs.length === 0) return badRequest(res, 'logs array obrigatório');
  if (logs.length > 50) return badRequest(res, 'max 50 logs por request');
  try {
    const inserted = await anticheatRepo.insertMiningLogsBatch(logs);
    res.json({ ok: true, inserted });
  } catch (err) {
    return internalError(res, err, 'anticheat/mining');
  }
});

router.post('/anticheat/auto-ban', pluginWriteLimiter, pluginOnlyAuth, async (req, res) => {
  const body = req.body || {};
  const uuid = validateUuid(res, body.uuid);
  if (!uuid) return;
  if (!isNonEmptyString(body.reason)) return badRequest(res, 'reason obrigatório');
  const hours = Math.min(720, Math.max(1, parseInt(body.duration_hours, 10) || 72));
  const expiresAt = new Date(Date.now() + hours * 3600 * 1000);
  try {
    const ban = await bansRepo.createBan({
      uuid,
      username: body.username || 'Unknown',
      reason: String(body.reason).slice(0, 500),
      bannedBy: 'OBM-AC',
      expiresAt,
    });
    await securityRepo.insertBatch([{
      uuid,
      username: body.username || 'Unknown',
      event_type: 'AUTO_BAN',
      detail: body.reason,
      world: null,
    }]);
    await adminRepo.insertServerEvent({
      type: 'anticheat_ban',
      actorUuid: uuid,
      actorName: body.username,
      detail: body.reason,
    });
    res.json({ ok: true, ban });
  } catch (err) {
    return internalError(res, err, 'anticheat/auto-ban');
  }
});

module.exports = router;
