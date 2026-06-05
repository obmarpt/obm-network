const { Router } = require('express');
const { authMiddleware } = require('../auth');
const { requireMod } = require('../middleware/permissions');
const supportRepo = require('../db/supportRepository');
const { badRequest, internalError, parseInteger } = require('./validation');

const router = Router();

router.use(authMiddleware);

router.get('/admin/support/tickets', requireMod, async (req, res) => {
  try {
    const page = parseInteger(req.query.page) || 1;
    const status = req.query.status || null;
    const data = await supportRepo.listAllTickets({ page, limit: 30, status });
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'admin/support/list');
  }
});

router.get('/admin/support/tickets/:id', requireMod, async (req, res) => {
  const id = parseInteger(req.params.id);
  if (!id) return badRequest(res, 'id inválido');
  try {
    const data = await supportRepo.getTicket(id, null);
    if (!data) return res.status(404).json({ error: 'not_found' });
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'admin/support/get');
  }
});

router.post('/admin/support/tickets/:id/reply', requireMod, async (req, res) => {
  const id = parseInteger(req.params.id);
  const { message } = req.body || {};
  if (!id || !message) return badRequest(res, 'dados incompletos');
  try {
    const staffName = req.user?.username || 'staff';
    const ticket = await supportRepo.addStaffReply(id, staffName, String(message));
    if (!ticket) return res.status(404).json({ error: 'not_found' });
    res.json({ ticket });
  } catch (err) {
    return internalError(res, err, 'admin/support/reply');
  }
});

router.post('/admin/support/tickets/:id/close', requireMod, async (req, res) => {
  const id = parseInteger(req.params.id);
  if (!id) return badRequest(res, 'id inválido');
  try {
    const ticket = await supportRepo.closeTicket(id);
    if (!ticket) return res.status(404).json({ error: 'not_found' });
    res.json({ ticket });
  } catch (err) {
    return internalError(res, err, 'admin/support/close');
  }
});

module.exports = router;
