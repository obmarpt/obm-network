function isNonEmptyString(value) {
  return typeof value === 'string' && value.trim().length > 0;
}

function parseInteger(value) {
  const n = Number(value);
  if (!Number.isFinite(n) || !Number.isInteger(n)) return null;
  return n;
}

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

function normalizeUuid(raw) {
  if (raw == null) return null;
  const uuid = String(raw).trim().toLowerCase();
  if (!UUID_RE.test(uuid)) return null;
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

function sanitizeProfileMeta(raw) {
  if (!raw || typeof raw !== 'object') return null;
  const achievements = Array.isArray(raw.achievements)
    ? raw.achievements.filter((x) => typeof x === 'string').map((s) => s.trim().slice(0, 64)).slice(0, 100)
    : [];
  const cosmeticsUnlocked = Array.isArray(raw.cosmetics_unlocked)
    ? raw.cosmetics_unlocked.filter((x) => typeof x === 'string').map((s) => s.trim().slice(0, 64)).slice(0, 100)
    : [];
  const cosmeticsActive = raw.cosmetics_active && typeof raw.cosmetics_active === 'object'
    ? raw.cosmetics_active
    : {};
  return {
    bp_level: Math.max(0, parseInteger(raw.bp_level) ?? 0),
    bp_xp: Math.max(0, parseInteger(raw.bp_xp) ?? 0),
    achievements,
    cosmetics_unlocked: cosmeticsUnlocked,
    cosmetics_active: cosmeticsActive,
  };
}

function parseBoundedInt(value, min, max, fallback = null) {
  const n = parseInteger(value);
  if (n == null) return fallback;
  return Math.min(max, Math.max(min, n));
}

function sanitizeUsername(raw) {
  if (!isNonEmptyString(raw)) return null;
  const name = raw.trim().slice(0, 32);
  if (!/^[a-zA-Z0-9_]{3,16}$/.test(name)) return null;
  return name;
}

function sanitizeReason(raw, maxLen = 500) {
  if (!isNonEmptyString(raw)) return null;
  const reason = raw.trim().slice(0, maxLen);
  return reason.length >= 3 ? reason : null;
}

function sanitizeLinkCode(raw) {
  if (!isNonEmptyString(raw)) return null;
  const code = raw.trim().toUpperCase().replace(/[^A-Z0-9]/g, '');
  if (code.length < 4 || code.length > 12) return null;
  return code;
}

function sanitizeProductPatch(body) {
  if (!body || typeof body !== 'object') return null;
  const patch = {};
  if (body.name != null) {
    if (!isNonEmptyString(body.name)) return null;
    patch.name = body.name.trim().slice(0, 120);
  }
  if (body.description != null) {
    patch.description = String(body.description).slice(0, 500);
  }
  if (body.price_cents != null) {
    const cents = parseBoundedInt(body.price_cents, 0, 99999999, null);
    if (cents == null) return null;
    patch.price_cents = cents;
  }
  if (body.active != null) {
    patch.active = Boolean(body.active);
  }
  if (body.badge != null) {
    const allowed = new Set(['popular', 'best_value', 'limited', 'recommended', '']);
    const badge = String(body.badge).trim().toLowerCase();
    if (!allowed.has(badge)) return null;
    patch.badge = badge || null;
  }
  return Object.keys(patch).length ? patch : null;
}

function sanitizePlayerSaveBody(body, uuid) {
  const safeName = isNonEmptyString(body.name) ? body.name.trim().slice(0, 32)
    : (isNonEmptyString(body.username) ? body.username.trim().slice(0, 32) : 'Unknown');
  const smp = body.smp || {};
  const hc = body.hc || {};
  const ts = body.tierspace || {};

  return {
    uuid,
    username: safeName,
    name: safeName,
    coins: Math.max(0, parseInteger(body.coins) ?? 0),
    emeralds: Math.max(0, parseInteger(body.emeralds) ?? 0),
    smp_level: Math.max(1, parseInteger(body.smp_level) ?? 1),
    hc_level: Math.max(1, parseInteger(body.hc_level) ?? 1),
    global_level: Math.max(1, parseInteger(body.global_level) ?? 1),
    rank: typeof body.rank === 'string' ? body.rank.trim().slice(0, 32) : 'bronze',
    world: typeof body.world === 'string' ? body.world.trim().slice(0, 64) : '',
    smp: {
      kills: Math.max(0, parseInteger(smp.kills) ?? 0),
      deaths: Math.max(0, parseInteger(smp.deaths) ?? 0),
      playtime: Math.max(0, parseInteger(smp.playtime) ?? 0),
      money_earned: Math.max(0, parseInteger(smp.money_earned) ?? 0),
      blocks_broken: Math.max(0, parseInteger(smp.blocks_broken) ?? 0),
    },
    hc,
    tierspace: ts,
    profile_meta: sanitizeProfileMeta(body.profile_meta),
  };
}

module.exports = {
  isNonEmptyString,
  parseInteger,
  parseBoundedInt,
  normalizeUuid,
  validateUuid,
  badRequest,
  internalError,
  sanitizeUsername,
  sanitizeReason,
  sanitizeLinkCode,
  sanitizeProductPatch,
  sanitizePlayerSaveBody,
  sanitizeProfileMeta,
};
