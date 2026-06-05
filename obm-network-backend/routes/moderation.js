const { Router } = require('express');
const { pluginOnlyAuth, pluginOrAuthMiddleware } = require('../auth');
const bansRepo = require('../db/bansRepository');
const reportsRepo = require('../db/reportsRepository');
const statsHistory = require('../services/statsHistoryService');
const adminRepo = require('../db/adminRepository');
const {
  validateUuid, badRequest, internalError, sanitizeReason, sanitizeUsername, parseBoundedInt,
} = require('./validation');

const router = Router();

router.get('/bans/check/:uuid', pluginOrAuthMiddleware, async (req, res) => {
  const uuid = validateUuid(res, req.params.uuid);
  if (!uuid) return;
  try {
    const ban = await bansRepo.getActiveBan(uuid);
    if (!ban) {
      return res.json({ banned: false });
    }
    res.json({
      banned: true,
      reason: ban.reason,
      banned_by: ban.banned_by,
      expires_at: ban.expires_at,
      created_at: ban.created_at,
    });
  } catch (err) {
    return internalError(res, err, 'bans/check');
  }
});

router.get('/reports/open', pluginOnlyAuth, async (req, res) => {
  try {
    const limit = parseBoundedInt(req.query.limit, 1, 30, 15);
    const result = await reportsRepo.listReports({ status: 'open', page: 1, limit });
    res.json(result);
  } catch (err) {
    return internalError(res, err, 'reports/open');
  }
});

router.post('/reports', pluginOnlyAuth, async (req, res) => {
  const body = req.body || {};
  const reporterUuid = validateUuid(res, body.reporter_uuid || body.reporterUuid);
  if (!reporterUuid) return;
  const targetUuid = validateUuid(res, body.target_uuid || body.targetUuid);
  if (!targetUuid) return;
  const reason = sanitizeReason(body.reason);
  if (!reason) return badRequest(res, 'reason inválido (3–500 chars)');

  const reporterName = sanitizeUsername(body.reporter_name || body.reporterName) || 'Unknown';
  const targetName = sanitizeUsername(body.target_name || body.targetName) || 'Unknown';

  try {
    const row = await reportsRepo.createReport({
      reporterUuid,
      reporterName,
      targetUuid,
      targetName,
      reason,
    });
    await adminRepo.insertServerEvent({
      type: 'report',
      actorUuid: reporterUuid,
      actorName: body.reporter_name,
      targetUuid,
      targetName: body.target_name,
      detail: body.reason,
    });
    res.json({ ok: true, report: row });
  } catch (err) {
    return internalError(res, err, 'reports');
  }
});

router.post('/events/kill', pluginOnlyAuth, async (req, res) => {
  const body = req.body || {};
  try {
    await statsHistory.incrementDailyKill();
    await adminRepo.insertServerEvent({
      type: 'kill',
      actorUuid: body.killer_uuid,
      actorName: body.killer_name,
      targetUuid: body.victim_uuid,
      targetName: body.victim_name,
      detail: body.weapon || body.mode || '',
    });
    res.json({ ok: true });
  } catch (err) {
    return internalError(res, err, 'events/kill');
  }
});

module.exports = router;
