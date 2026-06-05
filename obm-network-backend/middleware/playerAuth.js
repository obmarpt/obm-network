const jwt = require('jsonwebtoken');

const PLAYER_COOKIE = 'obm_player_token';

function getJwtSecret() {
  return process.env.JWT_SECRET;
}

function signPlayerToken(account) {
  const secret = getJwtSecret();
  if (!secret) throw new Error('JWT_SECRET not configured');
  return jwt.sign(
    {
      role: 'player',
      accountId: account.id,
      email: account.email,
      uuid: account.minecraft_uuid,
      username: account.minecraft_username,
    },
    secret,
    { expiresIn: '7d' }
  );
}

function extractPlayerToken(req) {
  const cookie = req.cookies?.[PLAYER_COOKIE];
  if (cookie) return cookie;
  const header = req.headers.authorization;
  if (header?.startsWith('Bearer ')) return header.slice(7);
  return null;
}

function verifyPlayerToken(token) {
  const secret = getJwtSecret();
  if (!secret || !token) return null;
  try {
    const payload = jwt.verify(token, secret);
    if (payload.role !== 'player') return null;
    return payload;
  } catch {
    return null;
  }
}

function playerAuthMiddleware(req, res, next) {
  const secret = getJwtSecret();
  if (!secret) return res.status(503).json({ error: 'auth_not_configured' });
  const token = extractPlayerToken(req);
  if (!token) return res.status(401).json({ error: 'unauthorized' });
  const user = verifyPlayerToken(token);
  if (!user) return res.status(403).json({ error: 'invalid_token' });
  req.player = user;
  next();
}

function optionalPlayerAuth(req, _res, next) {
  const token = extractPlayerToken(req);
  if (token) {
    const user = verifyPlayerToken(token);
    if (user) req.player = user;
  }
  next();
}

function setPlayerCookie(res, token) {
  const secure = process.env.NODE_ENV === 'production' || process.env.COOKIE_SECURE === 'true';
  res.cookie(PLAYER_COOKIE, token, {
    httpOnly: true,
    secure,
    sameSite: 'strict',
    maxAge: 7 * 24 * 60 * 60 * 1000,
    path: '/',
  });
}

function clearPlayerCookie(res) {
  res.clearCookie(PLAYER_COOKIE, { path: '/' });
}

module.exports = {
  PLAYER_COOKIE,
  signPlayerToken,
  playerAuthMiddleware,
  optionalPlayerAuth,
  setPlayerCookie,
  clearPlayerCookie,
  verifyPlayerToken,
};
