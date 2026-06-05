const cache = new Map();
const DEFAULT_TTL_MS = 12_000;

function get(key) {
  const entry = cache.get(key);
  if (!entry) return null;
  if (Date.now() > entry.expires) {
    cache.delete(key);
    return null;
  }
  return entry.value;
}

function set(key, value, ttlMs = DEFAULT_TTL_MS) {
  cache.set(key, { value, expires: Date.now() + ttlMs });
}

async function wrap(key, ttlMs, fn) {
  const hit = get(key);
  if (hit !== null) return hit;
  const value = await fn();
  set(key, value, ttlMs);
  return value;
}

function invalidate(prefix) {
  for (const key of cache.keys()) {
    if (key.startsWith(prefix)) cache.delete(key);
  }
}

module.exports = { get, set, wrap, invalidate, DEFAULT_TTL_MS };
