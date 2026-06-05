const db = require('../database');

function normalizeUuid(uuid) {
  return String(uuid || '').trim().toLowerCase();
}

async function getActiveBan(uuid) {
  const id = normalizeUuid(uuid);
  const result = await db.query(
    `SELECT id, uuid, username, reason, banned_by, created_at, expires_at
     FROM bans
     WHERE uuid = $1 AND active = true
       AND (expires_at IS NULL OR expires_at > NOW())
     ORDER BY created_at DESC
     LIMIT 1`,
    [id]
  );
  return result.rows[0] || null;
}

async function listBans({ page = 1, limit = 30, activeOnly = false } = {}) {
  const lim = Math.min(100, Math.max(1, parseInt(limit, 10) || 30));
  const pg = Math.max(1, parseInt(page, 10) || 1);
  const offset = (pg - 1) * lim;
  const where = activeOnly ? 'WHERE active = true' : '';

  const [countRes, listRes] = await Promise.all([
    db.query(`SELECT COUNT(*)::int AS c FROM bans ${where}`),
    db.query(
      `SELECT * FROM bans ${where} ORDER BY created_at DESC LIMIT $1 OFFSET $2`,
      [lim, offset]
    ),
  ]);

  return {
    page: pg,
    limit: lim,
    total: countRes.rows[0]?.c || 0,
    bans: listRes.rows,
  };
}

async function listBanHistory(uuid) {
  const id = normalizeUuid(uuid);
  const result = await db.query(
    `SELECT * FROM bans WHERE uuid = $1 ORDER BY created_at DESC LIMIT 50`,
    [id]
  );
  return result.rows;
}

async function createBan({ uuid, username, reason, bannedBy, expiresAt }) {
  const id = normalizeUuid(uuid);
  await db.query(
    `UPDATE bans SET active = false, unbanned_at = NOW(), unbanned_by = $2
     WHERE uuid = $1 AND active = true`,
    [id, bannedBy || 'system']
  );

  const result = await db.query(
    `INSERT INTO bans (uuid, username, reason, banned_by, expires_at)
     VALUES ($1, $2, $3, $4, $5)
     RETURNING *`,
    [
      id,
      (username || 'Unknown').slice(0, 32),
      String(reason || 'Sem motivo').slice(0, 500),
      String(bannedBy || 'admin').slice(0, 64),
      expiresAt || null,
    ]
  );
  return result.rows[0];
}

async function unban(uuid, staff) {
  const id = normalizeUuid(uuid);
  const result = await db.query(
    `UPDATE bans SET active = false, unbanned_at = NOW(), unbanned_by = $2
     WHERE uuid = $1 AND active = true
     RETURNING *`,
    [id, String(staff || 'admin').slice(0, 64)]
  );
  return result.rows;
}

module.exports = {
  getActiveBan,
  listBans,
  listBanHistory,
  createBan,
  unban,
};
