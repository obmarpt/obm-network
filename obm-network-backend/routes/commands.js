const { Router } = require('express');
const pool = require('../database');
const { pluginOnlyAuth } = require('../auth');
const { isNonEmptyString, badRequest, internalError } = require('./validation');
const { sanitizeRemoteCommand } = require('../utils/commandWhitelist');

const router = Router();

router.post('/command', pluginOnlyAuth, async (req, res) => {
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

router.get('/commands', pluginOnlyAuth, async (req, res) => {
  try {
    const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 25));
    const result = await pool.query(
      `UPDATE commands
       SET executed = true
       WHERE id IN (
         SELECT id FROM commands
         WHERE executed = false
         ORDER BY id ASC
         LIMIT $1
         FOR UPDATE SKIP LOCKED
       )
       RETURNING id, command, created_at`,
      [limit]
    );

    console.log(`⚡ GET /commands → ${result.rows.length} comando(s) entregue(s)`);
    res.json(result.rows);
  } catch (err) {
    return internalError(res, err, 'commands');
  }
});

module.exports = router;
