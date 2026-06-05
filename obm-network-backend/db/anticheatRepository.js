const db = require('../database');

function normalizeUuid(uuid) {
  return String(uuid || '').trim().toLowerCase();
}

function mapCheatType(checkName) {
  const c = String(checkName || '').toLowerCase();
  if (c.includes('speed') || c.includes('timer') || c.includes('phase')) return 'speed';
  if (c.includes('reach') || c.includes('hitbox')) return 'reach';
  if (c.includes('killaura') || c.includes('aim') || c.includes('aura')) return 'killaura';
  if (c.includes('fly') || c.includes('ground') || c.includes('nofall')) return 'movement';
  if (c.includes('scaffold') || c.includes('place')) return 'scaffold';
  return c.split(/[^a-z0-9]+/)[0] || 'unknown';
}

function mapSeverity(checkName, vl) {
  const c = String(checkName || '').toLowerCase();
  const v = Number(vl) || 0;
  if (c.includes('killaura') || c.includes('reach') || c.includes('hitbox') || v >= 50) return 'critical';
  if (c.includes('speed') || c.includes('timer') || c.includes('phase') || v >= 20) return 'high';
  if (v >= 5) return 'medium';
  return 'low';
}

async function insertViolationsBatch(rows) {
  if (!rows.length) return 0;
  const CHUNK = 50;
  let inserted = 0;
  for (let offset = 0; offset < rows.length; offset += CHUNK) {
    const slice = rows.slice(offset, offset + CHUNK);
    const values = [];
    const params = [];
    let i = 1;
    for (const r of slice) {
      const check = String(r.check_name || r.check || 'unknown').slice(0, 96);
      const cheatType = String(r.cheat_type || mapCheatType(check)).slice(0, 64);
      const severity = String(r.severity || mapSeverity(check, r.vl)).slice(0, 16);
      values.push(`($${i++},$${i++},$${i++},$${i++},$${i++},$${i++},$${i++})`);
      params.push(
        normalizeUuid(r.uuid),
        (r.username || 'Unknown').slice(0, 32),
        check,
        cheatType,
        severity,
        Number(r.vl) || 0,
        r.verbose != null ? String(r.verbose).slice(0, 2000) : null
      );
    }
    const sql = `INSERT INTO grim_violations (uuid, username, check_name, cheat_type, severity, vl, verbose)
                 VALUES ${values.join(', ')}`;
    await db.query(sql, params);
    inserted += slice.length;
  }
  return inserted;
}

async function createAlert({ uuid, username, flagCount, windowSeconds, reason, lastCheck }) {
  const id = normalizeUuid(uuid);
  const result = await db.query(
    `INSERT INTO anticheat_alerts (uuid, username, flag_count, window_seconds, reason)
     VALUES ($1, $2, $3, $4, $5) RETURNING *`,
    [
      id,
      (username || 'Unknown').slice(0, 32),
      Math.max(1, parseInt(flagCount, 10) || 1),
      Math.max(1, parseInt(windowSeconds, 10) || 10),
      String(reason || '').slice(0, 500),
    ]
  );
  await upsertSuspect(id, username, flagCount, lastCheck || reason);
  return result.rows[0];
}

async function upsertSuspect(uuid, username, flagCount, lastCheck) {
  await db.query(
    `INSERT INTO anticheat_suspects (uuid, username, active, total_flags, last_check, updated_at)
     VALUES ($1, $2, true, $3, $4, NOW())
     ON CONFLICT (uuid) DO UPDATE SET
       username = EXCLUDED.username,
       active = true,
       total_flags = anticheat_suspects.total_flags + EXCLUDED.total_flags,
       last_check = COALESCE(EXCLUDED.last_check, anticheat_suspects.last_check),
       updated_at = NOW()`,
    [uuid, (username || 'Unknown').slice(0, 32), Math.max(1, flagCount), lastCheck ? String(lastCheck).slice(0, 64) : null]
  );
}

async function listViolations({ page = 1, limit = 50, uuid, severity } = {}) {
  const lim = Math.min(200, Math.max(1, parseInt(limit, 10) || 50));
  const pg = Math.max(1, parseInt(page, 10) || 1);
  const offset = (pg - 1) * lim;
  const params = [];
  const clauses = [];
  if (uuid) {
    params.push(normalizeUuid(uuid));
    clauses.push(`uuid = $${params.length}`);
  }
  if (severity) {
    params.push(String(severity).slice(0, 16));
    clauses.push(`severity = $${params.length}`);
  }
  const where = clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
  const countRes = await db.query(`SELECT COUNT(*)::int AS c FROM grim_violations ${where}`, params);
  params.push(lim, offset);
  const listRes = await db.query(
    `SELECT id, uuid, username, check_name, cheat_type, severity, vl, verbose, created_at
     FROM grim_violations ${where}
     ORDER BY created_at DESC LIMIT $${params.length - 1} OFFSET $${params.length}`,
    params
  );
  return {
    page: pg,
    limit: lim,
    total: countRes.rows[0]?.c || 0,
    violations: listRes.rows,
  };
}

async function listAlerts({ page = 1, limit = 30, openOnly = true } = {}) {
  const lim = Math.min(100, Math.max(1, parseInt(limit, 10) || 30));
  const pg = Math.max(1, parseInt(page, 10) || 1);
  const offset = (pg - 1) * lim;
  const where = openOnly ? 'WHERE acknowledged = false' : '';
  const [countRes, listRes] = await Promise.all([
    db.query(`SELECT COUNT(*)::int AS c FROM anticheat_alerts ${where}`),
    db.query(
      `SELECT * FROM anticheat_alerts ${where} ORDER BY created_at DESC LIMIT $1 OFFSET $2`,
      [lim, offset]
    ),
  ]);
  return { page: pg, limit: lim, total: countRes.rows[0]?.c || 0, alerts: listRes.rows };
}

async function listSuspects(limit = 50) {
  const lim = Math.min(100, Math.max(1, parseInt(limit, 10) || 50));
  const result = await db.query(
    `SELECT * FROM anticheat_suspects WHERE active = true
     ORDER BY updated_at DESC LIMIT $1`,
    [lim]
  );
  return result.rows;
}

async function getDashboardStats() {
  const [violations24h, alertsOpen, suspects, bySeverity, topChecks] = await Promise.all([
    db.query(`SELECT COUNT(*)::int AS c FROM grim_violations WHERE created_at > NOW() - INTERVAL '24 hours'`),
    db.query(`SELECT COUNT(*)::int AS c FROM anticheat_alerts WHERE acknowledged = false`),
    db.query(`SELECT COUNT(*)::int AS c FROM anticheat_suspects WHERE active = true`),
    db.query(
      `SELECT severity, COUNT(*)::int AS c FROM grim_violations
       WHERE created_at > NOW() - INTERVAL '24 hours'
       GROUP BY severity`
    ),
    db.query(
      `SELECT cheat_type, COUNT(*)::int AS c FROM grim_violations
       WHERE created_at > NOW() - INTERVAL '24 hours'
       GROUP BY cheat_type ORDER BY c DESC LIMIT 10`
    ),
  ]);
  return {
    violations24h: violations24h.rows[0]?.c || 0,
    alertsOpen: alertsOpen.rows[0]?.c || 0,
    suspectsActive: suspects.rows[0]?.c || 0,
    bySeverity: bySeverity.rows,
    topChecks: topChecks.rows,
  };
}

async function getViolationsTimeline(hours = 24) {
  const h = Math.min(72, Math.max(1, parseInt(hours, 10) || 24));
  const result = await db.query(
    `SELECT date_trunc('hour', created_at) AS bucket, COUNT(*)::int AS count
     FROM grim_violations
     WHERE created_at > NOW() - ($1::text || ' hours')::interval
     GROUP BY bucket ORDER BY bucket ASC`,
    [String(h)]
  );
  return result.rows.map((r) => ({
    t: r.bucket,
    count: Number(r.count) || 0,
  }));
}

async function acknowledgeAlert(id) {
  const result = await db.query(
    `UPDATE anticheat_alerts SET acknowledged = true WHERE id = $1 RETURNING *`,
    [parseInt(id, 10)]
  );
  return result.rows[0];
}

async function clearSuspect(uuid) {
  await db.query(
    `UPDATE anticheat_suspects SET active = false, updated_at = NOW() WHERE uuid = $1`,
    [normalizeUuid(uuid)]
  );
}

async function insertMiningLogsBatch(rows) {
  if (!rows.length) return 0;
  const CHUNK = 50;
  let inserted = 0;
  for (let offset = 0; offset < rows.length; offset += CHUNK) {
    const slice = rows.slice(offset, offset + CHUNK);
    const values = [];
    const params = [];
    let i = 1;
    for (const r of slice) {
      values.push(`($${i++},$${i++},$${i++},$${i++},$${i++},$${i++},$${i++},$${i++})`);
      params.push(
        normalizeUuid(r.uuid),
        (r.username || 'Unknown').slice(0, 32),
        String(r.block_type || 'unknown').slice(0, 32),
        r.world != null ? String(r.world).slice(0, 64) : null,
        parseInt(r.x, 10) || 0,
        parseInt(r.y, 10) || 0,
        parseInt(r.z, 10) || 0,
        Number.isFinite(Number(r.blocks_before)) ? parseInt(r.blocks_before, 10) : -1
      );
    }
    await db.query(
      `INSERT INTO mining_logs (uuid, username, block_type, world, x, y, z, blocks_before)
       VALUES ${values.join(', ')}`,
      params
    );
    inserted += slice.length;
  }
  return inserted;
}

async function listMiningLogs({ uuid, limit = 50 } = {}) {
  const lim = Math.min(200, Math.max(1, parseInt(limit, 10) || 50));
  const params = [];
  let where = '';
  if (uuid) {
    params.push(normalizeUuid(uuid));
    where = `WHERE uuid = $1`;
  }
  params.push(lim);
  const result = await db.query(
    `SELECT id, uuid, username, block_type, world, x, y, z, blocks_before, created_at
     FROM mining_logs ${where}
     ORDER BY created_at DESC LIMIT $${params.length}`,
    params
  );
  return result.rows;
}

async function getMiningHeatmap({ uuid, world, hours = 24, limit = 500 } = {}) {
  const lim = Math.min(2000, Math.max(50, parseInt(limit, 10) || 500));
  const hrs = Math.min(168, Math.max(1, parseInt(hours, 10) || 24));
  const params = [lim];
  let where = `WHERE created_at > NOW() - INTERVAL '${hrs} hours'`;
  if (uuid) {
    params.unshift(normalizeUuid(uuid));
    where += ` AND uuid = $1`;
  }
  if (world) {
    params.push(String(world).slice(0, 64));
    where += ` AND world = $${params.length}`;
  }
  const result = await db.query(
    `SELECT
       world,
       (x >> 4)::int AS chunk_x,
       (z >> 4)::int AS chunk_z,
       block_type,
       COUNT(*)::int AS hits
     FROM mining_logs
     ${where}
     GROUP BY world, chunk_x, chunk_z, block_type
     ORDER BY hits DESC
     LIMIT $${params.length}`,
    params
  );
  return result.rows;
}

async function getMiningStats(uuid) {
  const id = normalizeUuid(uuid);
  const result = await db.query(
    `SELECT
       COUNT(*)::int AS total_ores,
       COUNT(*) FILTER (WHERE blocks_before >= 0 AND blocks_before < 12)::int AS fast_finds,
       AVG(NULLIF(blocks_before, -1))::float AS avg_blocks_before
     FROM mining_logs
     WHERE uuid = $1 AND created_at > NOW() - INTERVAL '7 days'`,
    [id]
  );
  return result.rows[0] || { total_ores: 0, fast_finds: 0, avg_blocks_before: null };
}

module.exports = {
  insertViolationsBatch,
  createAlert,
  listViolations,
  listAlerts,
  listSuspects,
  getDashboardStats,
  getViolationsTimeline,
  acknowledgeAlert,
  clearSuspect,
  insertMiningLogsBatch,
  listMiningLogs,
  getMiningHeatmap,
  getMiningStats,
  mapCheatType,
  mapSeverity,
};
