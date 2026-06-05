const { Router } = require('express');
const { pluginOnlyAuth, authMiddleware } = require('../auth');
const securityRepo = require('../db/securityRepository');
const { isNonEmptyString, badRequest, internalError } = require('./validation');
const { pluginWriteLimiter } = require('../middleware/rateLimit');

const router = Router();

router.post('/security/logs', pluginWriteLimiter, pluginOnlyAuth, async (req, res) => {
  const body = req.body || {};
  const logs = Array.isArray(body.logs) ? body.logs : [];
  if (logs.length === 0) {
    return badRequest(res, 'logs array obrigatório');
  }
  if (logs.length > 50) {
    return badRequest(res, 'max 50 logs por request');
  }

  for (const log of logs) {
    if (!isNonEmptyString(log.event_type)) {
      return badRequest(res, 'event_type inválido em logs');
    }
  }

  try {
    const inserted = await securityRepo.insertBatch(logs);
    res.json({ ok: true, inserted });
  } catch (err) {
    return internalError(res, err, 'security/logs');
  }
});

router.get('/admin/security/logs', authMiddleware, async (req, res) => {
  try {
    const limit = Math.min(200, Math.max(1, parseInt(req.query.limit, 10) || 100));
    const rows = await securityRepo.listRecent(limit);
    res.json({ logs: rows });
  } catch (err) {
    return internalError(res, err, 'admin/security/logs');
  }
});

module.exports = router;
