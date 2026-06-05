const db = require('../database');

async function createEntitlement({
  uuid,
  username,
  type,
  payload,
  orderId,
  subscriptionId,
  expiresAt,
}) {
  const res = await db.query(
    `INSERT INTO store_entitlements (minecraft_uuid, minecraft_username, entitlement_type, payload, order_id, subscription_id, expires_at)
     VALUES ($1,$2,$3,$4,$5,$6,$7) RETURNING *`,
    [
      uuid,
      username,
      type,
      JSON.stringify(payload || {}),
      orderId || null,
      subscriptionId || null,
      expiresAt || null,
    ]
  );
  return res.rows[0];
}

async function findExpired(limit = 50) {
  const res = await db.query(
    `SELECT * FROM store_entitlements
     WHERE revoked_at IS NULL AND expires_at IS NOT NULL AND expires_at <= NOW()
     ORDER BY expires_at ASC LIMIT $1`,
    [limit]
  );
  return res.rows;
}

async function markRevoked(id) {
  const res = await db.query(
    `UPDATE store_entitlements SET revoked_at = NOW() WHERE id = $1 AND revoked_at IS NULL RETURNING *`,
    [id]
  );
  return res.rows[0] || null;
}

async function revokeBySubscription(subscriptionId) {
  const res = await db.query(
    `UPDATE store_entitlements SET revoked_at = NOW()
     WHERE subscription_id = $1 AND revoked_at IS NULL RETURNING *`,
    [subscriptionId]
  );
  return res.rows;
}

async function listActiveForUuid(uuid) {
  const res = await db.query(
    `SELECT * FROM store_entitlements
     WHERE minecraft_uuid = $1 AND revoked_at IS NULL
     AND (expires_at IS NULL OR expires_at > NOW())
     ORDER BY created_at DESC`,
    [uuid]
  );
  return res.rows;
}

module.exports = {
  createEntitlement,
  findExpired,
  markRevoked,
  revokeBySubscription,
  listActiveForUuid,
};
