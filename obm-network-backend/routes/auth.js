const { Router } = require('express');
const {
  signToken,
  resolveLoginRole,
  setAuthCookie,
  clearAuthCookie,
  JWT_EXPIRES,
} = require('../auth');
const { isNonEmptyString, badRequest } = require('./validation');
const { loginLimiter } = require('../middleware/rateLimit');

const router = Router();

router.post('/login', loginLimiter, (req, res) => {
  const { username, password } = req.body || {};

  if (!isNonEmptyString(username)) return badRequest(res, 'username inválido');
  if (!password) return badRequest(res, 'password inválido');

  if (!process.env.JWT_SECRET || !process.env.ADMIN_USER || !process.env.ADMIN_PASS) {
    console.error('❌ Login: variáveis ADMIN_USER, ADMIN_PASS ou JWT_SECRET em falta');
    return res.status(503).json({ error: 'Auth not configured' });
  }

  const role = resolveLoginRole(username.trim(), password);
  if (!role) {
    console.log(`❌ Login falhou: ${username}`);
    return res.status(401).json({ error: 'Invalid credentials' });
  }

  const token = signToken(username.trim(), role);
  setAuthCookie(res, token);
  console.log(`✅ Login: ${username.trim()} (${role})`);
  res.json({ ok: true, expiresIn: JWT_EXPIRES, role });
});

router.post('/logout', (req, res) => {
  clearAuthCookie(res);
  res.json({ ok: true });
});

module.exports = router;
