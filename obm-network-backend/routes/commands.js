const { Router } = require('express');
const pool = require('../database');
const { pluginOrAuthMiddleware } = require('../auth');
const { isNonEmptyString, badRequest, internalError } = require('./validation');
const { sanitizeRemoteCommand } = require('../utils/commandWhitelist');

const router = Router();

router.post('/command', pluginOrAuthMiddleware, async (req, res) => {
  const { command } = req.body || {};

  if (!isNonEmptyString(command)) return badRequest(res, 'command inválido');

  const safe = sanitizeRemoteCommand(command);
  if (!safe) {
    console.warn(`❌ Comando rejeitado (whitelist): ${command}`);
    return res.status(400).json({ error: 'command not allowed' });
  }

  try {
    const result = await pool.query(
      'INSERT INTO commands (command) VALUES ($1) RETURNING id',
      [safe]
    );

    console.log(`⚡ Comando guardado [#${result.rows[0].id}]: ${safe}`);
    res.sendStatus(200);
  } catch (err) {
    return internalError(res, err, 'command');
  }
});

router.get('/commands', pluginOrAuthMiddleware, async (req, res) => {
  try {
    const result = await pool.query(
      `UPDATE commands
       SET executed = true
       WHERE executed = false
       RETURNING id, command, created_at`
    );

    console.log(`⚡ GET /commands → ${result.rows.length} comando(s) entregue(s)`);
    res.json(result.rows);
  } catch (err) {
    return internalError(res, err, 'commands');
  }
});

module.exports = router;
