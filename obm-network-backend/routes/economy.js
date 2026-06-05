const { Router } = require('express');
const pool = require('../database');
const { pluginOrAuthMiddleware } = require('../auth');
const { broadcast } = require('../ws');
const { col } = require('../database');
const { parseInteger, validateUuid, badRequest, internalError } = require('./validation');

const router = Router();

router.post('/money/add', pluginOrAuthMiddleware, async (req, res) => {
  const { amount, name } = req.body || {};
  const uuid = validateUuid(res, req.body?.uuid);
  if (!uuid) return;

  if (amount === undefined || amount === null) return badRequest(res, 'amount obrigatório');

  const parsedAmount = parseInteger(amount);
  if (parsedAmount === null) return badRequest(res, 'amount inválido (número inteiro obrigatório)');

  const safeName = typeof name === 'string' && name.trim() ? name.trim().slice(0, 32) : 'Unknown';

  try {
    const coinsCol = col('coins');
    const result = await pool.query(
      `INSERT INTO players (uuid, name, ${coinsCol}, updated_at)
       VALUES ($1, $2, GREATEST(0, $3), NOW())
       ON CONFLICT (uuid) DO UPDATE SET
         ${coinsCol} = GREATEST(0, players.${coinsCol} + $3),
         name = COALESCE(NULLIF(EXCLUDED.name, 'Unknown'), players.name),
         updated_at = NOW()
       RETURNING ${coinsCol} AS coins`,
      [uuid, safeName, parsedAmount]
    );

    const balance = Number(result.rows[0].coins) || 0;
    console.log(`💰 ${parsedAmount >= 0 ? '+' : ''}${parsedAmount} coins → ${uuid} (total: ${balance})`);
    broadcast({ type: 'update' });
    res.json({ ok: true, uuid, coins: balance, balance });
  } catch (err) {
    return internalError(res, err, 'money/add');
  }
});

module.exports = router;
