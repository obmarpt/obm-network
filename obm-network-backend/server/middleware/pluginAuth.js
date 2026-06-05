const crypto = require('crypto');

/**
 * Valida header X-Plugin-Key em todas as rotas da API do plugin (exceto /health).
 */
function pluginAuth(req, res, next) {
  if (req.path === '/health') {
    return next();
  }

  const expected = process.env.PLUGIN_API_KEY || '';
  if (!expected) {
    console.error('[AUTH] PLUGIN_API_KEY não definida — API bloqueada');
    return res.status(503).json({ error: 'plugin_auth_not_configured' });
  }

  const provided = req.get('X-Plugin-Key') || req.get('x-plugin-key') || '';
  const ba = Buffer.from(provided);
  const bb = Buffer.from(expected);
  if (ba.length !== bb.length || !crypto.timingSafeEqual(ba, bb)) {
    return res.status(403).json({ error: 'invalid_plugin_key' });
  }
  return next();
}

module.exports = { pluginAuth };
