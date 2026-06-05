const { Router } = require('express');
const rateLimit = require('express-rate-limit');
const supportRepo = require('../db/supportRepository');
const { playerAuthMiddleware } = require('../middleware/playerAuth');
const webAuthRepo = require('../db/webAuthRepository');
const { badRequest, internalError } = require('./validation');

const router = Router();

const ticketLimiter = rateLimit({
  windowMs: 60_000,
  max: 5,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'too_many_tickets' },
});

router.post('/api/support/ticket', ticketLimiter, playerAuthMiddleware, async (req, res) => {
  const { subject, message } = req.body || {};
  if (!subject || !message) return badRequest(res, 'assunto e mensagem obrigatórios');
  if (String(message).length < 10) return badRequest(res, 'mensagem demasiado curta');

  try {
    const account = await webAuthRepo.findById(req.player.accountId);
    if (!account) return res.status(401).json({ error: 'unauthorized' });

    const ticket = await supportRepo.createTicket({
      accountId: account.id,
      email: account.email,
      subject: String(subject).trim(),
      message: String(message).trim(),
    });
    res.status(201).json({ ticket: { id: ticket.id, status: ticket.status } });
  } catch (err) {
    return internalError(res, err, 'support/create');
  }
});

router.get('/api/support/tickets', playerAuthMiddleware, async (req, res) => {
  try {
    const tickets = await supportRepo.listTicketsForAccount(req.player.accountId);
    res.json({ tickets });
  } catch (err) {
    return internalError(res, err, 'support/list');
  }
});

router.get('/api/support/tickets/:id', playerAuthMiddleware, async (req, res) => {
  const id = parseInt(req.params.id, 10);
  if (!id) return badRequest(res, 'id inválido');
  try {
    const data = await supportRepo.getTicket(id, req.player.accountId);
    if (!data) return res.status(404).json({ error: 'not_found' });
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'support/get');
  }
});

module.exports = router;
