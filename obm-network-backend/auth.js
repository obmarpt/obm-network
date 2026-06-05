const crypto = require('crypto');
const jwt = require('jsonwebtoken');

const JWT_EXPIRES = '2h';
const COOKIE_NAME = 'obm_token';

function getJwtSecret() {
  return process.env.JWT_SECRET;
}

function signToken(username, role = 'admin') {
  const secret = getJwtSecret();
  if (!secret) throw new Error('JWT_SECRET not configured');
  const safeRole = role === 'mod' ? 'mod' : 'admin';
  return jwt.sign({ username, role: safeRole }, secret, { expiresIn: JWT_EXPIRES });
}

function extractToken(req) {
  const cookieToken = req.cookies?.[COOKIE_NAME];
  if (cookieToken) return cookieToken;

  const header = req.headers.authorization;
  if (header?.startsWith('Bearer ')) {
    return header.slice(7);
  }

  if (req.query?.token) {
    return String(req.query.token);
  }

  return null;
}

function verifyToken(token) {
  const secret = getJwtSecret();
  if (!secret || !token) return null;
  try {
    return jwt.verify(token, secret);
  } catch {
    return null;
  }
}

function authMiddleware(req, res, next) {
  const secret = getJwtSecret();
  if (!secret) {
    return res.status(503).json({ error: 'Auth not configured' });
  }

  const token = extractToken(req);
  if (!token) {
    return res.status(401).json({ error: 'Unauthorized' });
  }

  const user = verifyToken(token);
  if (!user) {
    return res.status(403).json({ error: 'Invalid or expired token' });
  }

  req.user = user;
  req.authSource = 'jwt';
  next();
}

function pluginAuth(req, res, next) {
  const expected = process.env.PLUGIN_API_KEY;
  if (!expected) {
    return res.status(503).json({ error: 'PLUGIN_API_KEY not configured' });
  }

  const pluginKey = req.headers['x-plugin-key'];
  if (!pluginKey || !timingSafeEqual(pluginKey, expected)) {
    console.warn(`⚠️ Plugin key inválida: ${req.method} ${req.originalUrl || req.path}`);
    return res.status(403).json({ error: 'Forbidden' });
  }

  req.authSource = 'plugin';
  next();
}

function pluginOrAuthMiddleware(req, res, next) {
  const expected = process.env.PLUGIN_API_KEY;
  const pluginKey = req.headers['x-plugin-key'];

  if (expected && pluginKey && timingSafeEqual(pluginKey, expected)) {
    req.authSource = 'plugin';
    return next();
  }

  return authMiddleware(req, res, next);
}

function timingSafeEqual(a, b) {
  const ba = Buffer.from(String(a));
  const bb = Buffer.from(String(b));
  if (ba.length !== bb.length) return false;
  return crypto.timingSafeEqual(ba, bb);
}

function resolveLoginRole(username, password) {
  if (!username || !password || !getJwtSecret()) return null;

  const adminUser = process.env.ADMIN_USER;
  const adminPass = process.env.ADMIN_PASS;
  if (adminUser && adminPass && timingSafeEqual(username, adminUser) && timingSafeEqual(password, adminPass)) {
    return 'admin';
  }

  const modUser = process.env.MOD_USER;
  const modPass = process.env.MOD_PASS;
  if (modUser && modPass && timingSafeEqual(username, modUser) && timingSafeEqual(password, modPass)) {
    return 'mod';
  }

  return null;
}

function verifyCredentials(username, password) {
  return resolveLoginRole(username, password) != null;
}

function setAuthCookie(res, token) {
  const secure = process.env.NODE_ENV === 'production' || process.env.COOKIE_SECURE === 'true';
  res.cookie(COOKIE_NAME, token, {
    httpOnly: true,
    secure,
    sameSite: 'strict',
    maxAge: 2 * 60 * 60 * 1000,
    path: '/',
  });
}

function clearAuthCookie(res) {
  res.clearCookie(COOKIE_NAME, { path: '/' });
}

/** Escrita de integração — apenas plugin key (sem fallback JWT). */
function pluginOnlyAuth(req, res, next) {
  return pluginAuth(req, res, next);
}

module.exports = {
  signToken,
  authMiddleware,
  pluginAuth,
  pluginOnlyAuth,
  pluginOrAuthMiddleware,
  verifyCredentials,
  resolveLoginRole,
  verifyToken,
  extractToken,
  setAuthCookie,
  clearAuthCookie,
  COOKIE_NAME,
  JWT_EXPIRES,
};
