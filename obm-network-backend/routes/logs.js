const { Router } = require('express');
const pool = require('../database');
const { pluginOnlyAuth } = require('../auth');
const adminRepo = require('../db/adminRepository');
const { notifyAdminLog } = require('../discord');
const { isNonEmptyString, badRequest, internalError, normalizeUuid } = require('./validation');

const router = Router();

router.post('/events', pluginOnlyAuth, async (req, res) => {
  const body = req.body || {};
  const type = body.type || body.event_type;
  if (!isNonEmptyString(type)) return badRequest(res, 'type inválido');

  const actorUuid = normalizeUuid(body.actor_uuid || body.uuid);
  const targetUuid = normalizeUuid(body.target_uuid);

  try {
    await adminRepo.insertServerEvent({
      type: type.trim(),
      actorUuid,
      actorName: body.actor_name || body.name,
      targetUuid,
      targetName: body.target_name,
      detail: body.detail || body.value,
    });
    res.json({ ok: true });
  } catch (err) {
    return internalError(res, err, 'events');
  }
});

router.post('/log', pluginOnlyAuth, async (req, res) => {
  const { action, target, value } = req.body || {};

  if (!isNonEmptyString(action)) return badRequest(res, 'action inválido');

  const safeStaff = 'plugin';
  const safeTarget = target != null ? String(target).slice(0, 128) : '';
  const safeValue = value != null ? String(value).slice(0, 512) : '';

  try {
    await pool.query(
      'INSERT INTO logs (staff, action, target, value) VALUES ($1, $2, $3, $4)',
      [safeStaff, action.trim(), safeTarget, safeValue]
    );

    console.log(`📋 LOG: ${safeStaff} → ${action} | target=${safeTarget} | value=${safeValue}`);

    notifyAdminLog({
      staff: safeStaff,
      action: action.trim(),
      target: safeTarget,
      value: safeValue,
    });

    res.sendStatus(200);
  } catch (err) {
    return internalError(res, err, 'log');
  }
});

module.exports = router;
