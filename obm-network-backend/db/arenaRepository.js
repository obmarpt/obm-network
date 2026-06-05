const db = require('../database');

function rowToArena(row) {
  if (!row) return null;
  return {
    id: row.id,
    name: row.name,
    mode: row.mode,
    world: row.world,
    pos1: {
      x: Number(row.pos1_x),
      y: Number(row.pos1_y),
      z: Number(row.pos1_z),
      yaw: Number(row.pos1_yaw),
      pitch: Number(row.pos1_pitch),
    },
    pos2: {
      x: Number(row.pos2_x),
      y: Number(row.pos2_y),
      z: Number(row.pos2_z),
      yaw: Number(row.pos2_yaw),
      pitch: Number(row.pos2_pitch),
    },
    enabled: row.enabled !== false,
    auto_reset: row.auto_reset === true,
    created_at: row.created_at,
    updated_at: row.updated_at,
  };
}

async function listArenas({ mode, enabledOnly } = {}) {
  const clauses = [];
  const params = [];
  let idx = 1;

  if (mode) {
    clauses.push(`mode = $${idx++}`);
    params.push(String(mode).toLowerCase());
  }
  if (enabledOnly) {
    clauses.push('enabled = true');
  }

  const where = clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
  const result = await db.query(
    `SELECT * FROM tierspace_arenas ${where} ORDER BY name ASC`,
    params
  );
  return result.rows.map(rowToArena);
}

async function getArena(id) {
  const result = await db.query('SELECT * FROM tierspace_arenas WHERE id = $1', [id]);
  return rowToArena(result.rows[0]);
}

async function createArena(data) {
  const result = await db.query(
    `INSERT INTO tierspace_arenas (
      id, name, mode, world,
      pos1_x, pos1_y, pos1_z, pos1_yaw, pos1_pitch,
      pos2_x, pos2_y, pos2_z, pos2_yaw, pos2_pitch,
      enabled, auto_reset, updated_at
    ) VALUES (
      $1, $2, $3, $4,
      $5, $6, $7, $8, $9,
      $10, $11, $12, $13, $14,
      $15, $16, NOW()
    ) RETURNING *`,
    [
      data.id,
      data.name,
      data.mode,
      data.world,
      data.pos1.x, data.pos1.y, data.pos1.z, data.pos1.yaw ?? 0, data.pos1.pitch ?? 0,
      data.pos2.x, data.pos2.y, data.pos2.z, data.pos2.yaw ?? 180, data.pos2.pitch ?? 0,
      data.enabled !== false,
      data.auto_reset === true,
    ]
  );
  return rowToArena(result.rows[0]);
}

async function updateArena(id, data) {
  const existing = await getArena(id);
  if (!existing) return null;

  const merged = {
    name: data.name ?? existing.name,
    mode: data.mode ?? existing.mode,
    world: data.world ?? existing.world,
    pos1: { ...existing.pos1, ...(data.pos1 || {}) },
    pos2: { ...existing.pos2, ...(data.pos2 || {}) },
    enabled: data.enabled !== undefined ? !!data.enabled : existing.enabled,
    auto_reset: data.auto_reset !== undefined ? !!data.auto_reset : existing.auto_reset,
  };

  const result = await db.query(
    `UPDATE tierspace_arenas SET
      name = $2, mode = $3, world = $4,
      pos1_x = $5, pos1_y = $6, pos1_z = $7, pos1_yaw = $8, pos1_pitch = $9,
      pos2_x = $10, pos2_y = $11, pos2_z = $12, pos2_yaw = $13, pos2_pitch = $14,
      enabled = $15, auto_reset = $16, updated_at = NOW()
    WHERE id = $1 RETURNING *`,
    [
      id,
      merged.name,
      merged.mode,
      merged.world,
      merged.pos1.x, merged.pos1.y, merged.pos1.z, merged.pos1.yaw, merged.pos1.pitch,
      merged.pos2.x, merged.pos2.y, merged.pos2.z, merged.pos2.yaw, merged.pos2.pitch,
      merged.enabled,
      merged.auto_reset,
    ]
  );
  return rowToArena(result.rows[0]);
}

async function deleteArena(id) {
  const result = await db.query('DELETE FROM tierspace_arenas WHERE id = $1 RETURNING id', [id]);
  return result.rows[0]?.id || null;
}

module.exports = {
  listArenas,
  getArena,
  createArena,
  updateArena,
  deleteArena,
};
