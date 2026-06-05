const db = require('../database');

function normalizeUuid(uuid) {
  return String(uuid || '').trim().toLowerCase();
}

async function createReport({ reporterUuid, reporterName, targetUuid, targetName, reason }) {
  const result = await db.query(
    `INSERT INTO reports (reporter_uuid, reporter_name, target_uuid, target_name, reason)
     VALUES ($1, $2, $3, $4, $5)
     RETURNING *`,
    [
      normalizeUuid(reporterUuid),
      (reporterName || 'Unknown').slice(0, 32),
      normalizeUuid(targetUuid),
      (targetName || 'Unknown').slice(0, 32),
      String(reason || 'Sem motivo').slice(0, 500),
    ]
  );
  return result.rows[0];
}

async function listReports({ status = 'open', page = 1, limit = 30 } = {}) {
  const lim = Math.min(100, Math.max(1, parseInt(limit, 10) || 30));
  const pg = Math.max(1, parseInt(page, 10) || 1);
  const offset = (pg - 1) * lim;
  const params = [];
  let where = '';

  if (status && status !== 'all') {
    params.push(status);
    where = 'WHERE status = $1';
  }

  const countSql = `SELECT COUNT(*)::int AS c FROM reports ${where}`;
  const countRes = await db.query(countSql, params);

  params.push(lim, offset);
  const listSql = `SELECT * FROM reports ${where} ORDER BY created_at DESC LIMIT $${params.length - 1} OFFSET $${params.length}`;
  const listRes = await db.query(listSql, params);

  return {
    page: pg,
    limit: lim,
    total: countRes.rows[0]?.c || 0,
    reports: listRes.rows,
  };
}

async function closeReport(id, staff) {
  const result = await db.query(
    `UPDATE reports SET status = 'closed', closed_by = $2, closed_at = NOW()
     WHERE id = $1 AND status = 'open'
     RETURNING *`,
    [parseInt(id, 10), String(staff || 'staff').slice(0, 64)]
  );
  return result.rows[0] || null;
}

module.exports = { createReport, listReports, closeReport };
