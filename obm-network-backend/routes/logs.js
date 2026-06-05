const { Router } = require('express');
const pool = require('../database');
const { pluginOrAuthMiddleware } = require('../auth');
const { notifyAdminLog } = require('../discord');
const { isNonEmptyString, badRequest, internalError } = require('./validation');

const router = Router();

router.post('/log', pluginOrAuthMiddleware, async (req, res) => {
  const { staff, action, target, value } = req.body || {};

  if (!isNonEmptyString(staff)) return badRequest(res, 'staff inválido');
  if (!isNonEmptyString(action)) return badRequest(res, 'action inválido');

  const safeTarget = target != null ? String(target) : '';
  const safeValue = value != null ? String(value) : '';

  try {
    await pool.query(
      'INSERT INTO logs (staff, action, target, value) VALUES ($1, $2, $3, $4)',
      [staff.trim(), action.trim(), safeTarget, safeValue]
    );

    console.log(`📋 LOG: ${staff} → ${action} | target=${safeTarget} | value=${safeValue}`);

    notifyAdminLog({
      staff: staff.trim(),
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
