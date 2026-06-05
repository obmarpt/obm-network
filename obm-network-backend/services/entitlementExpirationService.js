const pool = require('../database');
const entitlementRepo = require('../db/entitlementRepository');
const { sanitizeRemoteCommand } = require('../utils/commandWhitelist');

function buildRevokeCommand(entitlement) {
  const payload = typeof entitlement.payload === 'object'
    ? entitlement.payload
    : JSON.parse(entitlement.payload || '{}');
  const player = entitlement.minecraft_username;
  const type = entitlement.entitlement_type;

  switch (type) {
    case 'rank_temp':
    case 'rank_sub': {
      const group = payload.group || payload.rank;
      if (!group) return null;
      return `lp user ${player} parent remove ${group}`;
    }
    case 'booster_money':
    case 'booster_bp':
      return `obmstore revoke booster ${player} ${type === 'booster_bp' ? 'bp' : 'money'}`;
    default:
      return null;
  }
}

async function processExpiredBatch() {
  const expired = await entitlementRepo.findExpired(40);
  let revoked = 0;
  for (const ent of expired) {
    const cmd = buildRevokeCommand(ent);
    if (cmd) {
      const safe = sanitizeRemoteCommand(cmd);
      if (safe) {
        await pool.query('INSERT INTO commands (command) VALUES ($1)', [safe]);
      }
    }
    await entitlementRepo.markRevoked(ent.id);
    revoked += 1;
  }
  return revoked;
}

function startExpirationScheduler(intervalMs = 60_000) {
  const tick = async () => {
    try {
      const n = await processExpiredBatch();
      if (n > 0) console.log(`[Store] Revoked ${n} expired entitlement(s)`);
    } catch (err) {
      console.warn('[Store] Expiration tick:', err.message);
    }
  };
  tick();
  return setInterval(tick, intervalMs);
}

module.exports = { processExpiredBatch, startExpirationScheduler, buildRevokeCommand };
