const rateLimit = require('express-rate-limit');

const globalReadLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 800,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests' },
});

const writeLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 400,
  standardHeaders: true,
  legacyHeaders: false,
  skip: (req) => req.method === 'GET' || req.method === 'HEAD' || req.path === '/health',
  message: { error: 'Too many requests' },
});

const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 20,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many login attempts' },
});

const playerJoinLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 120,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests' },
});

const pluginWriteLimiter = rateLimit({
  windowMs: 1000,
  max: 30,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Plugin write rate limit exceeded' },
  keyGenerator: (req) => req.headers['x-plugin-key'] || req.ip,
});

const securityLogLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 200,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Security log rate limit exceeded' },
});

module.exports = {
  globalReadLimiter,
  writeLimiter,
  loginLimiter,
  playerJoinLimiter,
  pluginWriteLimiter,
  securityLogLimiter,
};
