const pool = require('../database');

async function insertBatch(logs) {
  if (!Array.isArray(logs) || logs.length === 0) {
    return 0;
  }
  const client = await pool.connect();
  try {
    let inserted = 0;
    for (const log of logs.slice(0, 50)) {
      await client.query(
        `INSERT INTO security_logs (uuid, username, event_type, detail, world, ip_address)
         VALUES ($1, $2, $3, $4, $5, $6)`,
        [
          log.uuid || null,
          (log.username || 'Unknown').slice(0, 64),
          (log.event_type || 'UNKNOWN').slice(0, 64),
          log.detail != null ? String(log.detail).slice(0, 2000) : null,
          log.world != null ? String(log.world).slice(0, 64) : null,
          log.ip_address != null ? String(log.ip_address).slice(0, 64) : null,
        ]
      );
      inserted += 1;
    }
    return inserted;
  } finally {
    client.release();
  }
}

async function listRecent(limit = 100) {
  const safeLimit = Math.min(200, Math.max(1, parseInt(limit, 10) || 100));
  const result = await pool.query(
    `SELECT id, uuid, username, event_type, detail, world, ip_address, created_at
     FROM security_logs
     ORDER BY created_at DESC
     LIMIT $1`,
    [safeLimit]
  );
  return result.rows;
}

module.exports = { insertBatch, listRecent };
