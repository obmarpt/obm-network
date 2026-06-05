function isNonEmptyString(value) {
  return typeof value === 'string' && value.trim().length > 0;
}

function parseInteger(value) {
  const n = Number(value);
  if (!Number.isFinite(n) || !Number.isInteger(n)) return null;
  return n;
}

function normalizeUuid(raw) {
  if (raw == null) return null;
  const uuid = String(raw).trim().toLowerCase();
  if (uuid.length < 32) return null;
  return uuid;
}

function validateUuid(res, raw) {
  const uuid = normalizeUuid(raw);
  if (!uuid) {
    badRequest(res, 'uuid inválido');
    return null;
  }
  return uuid;
}

function badRequest(res, message) {
  console.log(`❌ 400 → ${message}`);
  return res.status(400).json({ error: message });
}

function internalError(res, err, context) {
  console.error(`Erro${context ? ` (${context})` : ''}:`, err);
  return res.status(500).json({ error: 'internal error' });
}

module.exports = {
  isNonEmptyString,
  parseInteger,
  normalizeUuid,
  validateUuid,
  badRequest,
  internalError,
};
