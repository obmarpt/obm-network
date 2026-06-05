const { Router } = require('express');
const pool = require('../database');
const { pluginOnlyAuth } = require('../auth');
const { broadcast } = require('../ws');
const { col } = require('../database');
const { parseInteger, validateUuid, badRequest, internalError } = require('./validation');

const router = Router();

router.post('/money/add', pluginOnlyAuth, async (req, res) => {
  const { amount, name } = req.body || {};
  const uuid = validateUuid(res, req.body?.uuid);
  if (!uuid) return;

  if (amount === undefined || amount === null) return badRequest(res, 'amount obrigatório');

  const parsedAmount = parseInteger(amount);
  if (parsedAmount === null) return badRequest(res, 'amount inválido (número inteiro obrigatório)');
  const MAX_DELTA = 10_000_000;
  if (Math.abs(parsedAmount) > MAX_DELTA) {
    return badRequest(res, `amount excede limite ±${MAX_DELTA}`);
  }

  const safeName = typeof name === 'string' && name.trim() ? name.trim().slice(0, 32) : 'Unknown';

  try {
    const playerRepo = require('../db/playerRepository');
    const balance = await playerRepo.addCoins(uuid, parsedAmount, safeName);
    console.log(`💰 ${parsedAmount >= 0 ? '+' : ''}${parsedAmount} coins → ${uuid} (total: ${balance})`);
    broadcast({ type: 'update' });
    res.json({ ok: true, uuid, coins: balance, balance });
  } catch (err) {
    return internalError(res, err, 'money/add');
  }
});

module.exports = router;
