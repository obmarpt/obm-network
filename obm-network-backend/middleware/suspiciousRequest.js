const securityRepo = require('../db/securityRepository');

const SUSPICIOUS_PATTERNS = [
  /(\bOR\b|\bUNION\b|\bSELECT\b).*\bFROM\b/i,
  /<script/i,
  /\.\.\//,
  /%00/,
];

function isSuspiciousBody(body) {
  if (!body || typeof body !== 'object') {
    return false;
  }
  const text = JSON.stringify(body);
  return SUSPICIOUS_PATTERNS.some((re) => re.test(text));
}

function suspiciousRequestLogger(req, res, next) {
  if (req.method === 'GET' || req.method === 'HEAD') {
    return next();
  }

  const suspicious = isSuspiciousBody(req.body)
    || (req.query && isSuspiciousBody(req.query));

  if (suspicious) {
    const detail = `${req.method} ${req.originalUrl || req.path}`;
    console.warn(`[SECURITY] Suspicious request from ${req.ip}: ${detail}`);
    securityRepo.insertBatch([{
      uuid: null,
      username: 'API',
      event_type: 'SUSPICIOUS_REQUEST',
      detail,
      world: null,
      ip_address: req.ip,
    }]).catch(() => {});
    return res.status(400).json({ error: 'invalid input' });
  }

  next();
}

module.exports = { suspiciousRequestLogger };
