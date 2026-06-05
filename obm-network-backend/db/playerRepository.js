const db = require('../database');
const { query, getPool } = db;

const ALLOWED_TABLES = new Set(['players', 'smp_stats', 'hc_stats', 'tierspace_stats']);
const ALLOWED_COLUMNS = {
  players: new Set(['coins', 'emeralds']),
  smp_stats: new Set(['kills', 'deaths', 'playtime']),
  hc_stats: new Set(['hc_kills', 'hc_totems_used', 'hc_wins']),
  tierspace_stats: new Set(['rating', 'wins', 'winstreak']),
};

const LEADERBOARD_METRICS = {
  coins: { table: 'players', column: 'coins' },
  money: { table: 'players', column: 'coins' },
  emeralds: { table: 'players', column: 'emeralds' },
  smp_kills: { table: 'smp_stats', column: 'kills' },
  kills: { table: 'smp_stats', column: 'kills' },
  smp_deaths: { table: 'smp_stats', column: 'deaths' },
  smp_playtime: { table: 'smp_stats', column: 'playtime' },
  playtime: { table: 'smp_stats', column: 'playtime' },
  hc_kills: { table: 'hc_stats', column: 'hc_kills' },
  hc_totems_used: { table: 'hc_stats', column: 'hc_totems_used' },
  hc_wins: { table: 'hc_stats', column: 'hc_wins' },
  tierspace_rating: { table: 'tierspace_stats', column: 'rating' },
  rating: { table: 'tierspace_stats', column: 'rating' },
  tierspace_wins: { table: 'tierspace_stats', column: 'wins' },
  tierspace_winstreak: { table: 'tierspace_stats', column: 'winstreak' },
};

function safeInt(v, def = 0) {
  const n = parseInt(v, 10);
  return Number.isFinite(n) ? Math.max(0, n) : def;
}

function normalizeUuid(uuid) {
  return String(uuid || '').trim().toLowerCase();
}

async function ensurePlayer(uuid, username) {
  const id = normalizeUuid(uuid);
  await query(
    `INSERT INTO players (uuid, username, updated_at)
     VALUES ($1, $2, NOW())
     ON CONFLICT (uuid) DO UPDATE SET
       username = COALESCE(NULLIF(EXCLUDED.username, 'Unknown'), players.username),
       updated_at = NOW()`,
    [id, (username || 'Unknown').trim().slice(0, 32)]
  );
  await query(`INSERT INTO smp_stats (uuid) VALUES ($1) ON CONFLICT (uuid) DO NOTHING`, [id]);
  await query(`INSERT INTO hc_stats (uuid) VALUES ($1) ON CONFLICT (uuid) DO NOTHING`, [id]);
  await query(`INSERT INTO tierspace_stats (uuid) VALUES ($1) ON CONFLICT (uuid) DO NOTHING`, [id]);
  return id;
}

async function getPlayerFull(uuid) {
  const id = normalizeUuid(uuid);
  const result = await query(
    `SELECT
       p.uuid, p.username, p.coins, p.emeralds, p.rank, p.smp_level, p.hc_level, p.global_level,
       p.created_at, p.updated_at,
       s.kills AS smp_kills, s.deaths AS smp_deaths, s.playtime AS smp_playtime,
       h.hc_kills, h.hc_deaths, h.hc_playtime, h.hc_totems_used, h.hc_wins,
       h.hc_level AS hc_stat_level,
       t.rating, t.wins AS ts_wins, t.losses AS ts_losses, t.winstreak
     FROM players p
     LEFT JOIN smp_stats s ON s.uuid = p.uuid
     LEFT JOIN hc_stats h ON h.uuid = p.uuid
     LEFT JOIN tierspace_stats t ON t.uuid = p.uuid
     WHERE p.uuid = $1`,
    [id]
  );
  return result.rows[0] || null;
}

async function savePlayerFull(payload) {
  const client = await getPool().connect();
  const uuid = normalizeUuid(payload.uuid);
  const p = payload;
  const smp = p.smp || {};
  const hc = p.hc || {};
  const ts = p.tierspace || {};

  try {
    await client.query('BEGIN');

    const rankVal = typeof p.rank === 'string' && p.rank.trim() ? p.rank.trim().slice(0, 32) : 'bronze';

    await client.query(
      `INSERT INTO players (uuid, username, coins, emeralds, rank, smp_level, hc_level, global_level, updated_at)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,NOW())
       ON CONFLICT (uuid) DO UPDATE SET
         username = COALESCE(NULLIF(EXCLUDED.username, 'Unknown'), players.username),
         coins = EXCLUDED.coins,
         emeralds = EXCLUDED.emeralds,
         rank = EXCLUDED.rank,
         smp_level = EXCLUDED.smp_level,
         hc_level = EXCLUDED.hc_level,
         global_level = EXCLUDED.global_level,
         updated_at = NOW()`,
      [
        uuid,
        (p.username || p.name || 'Unknown').trim().slice(0, 32),
        safeInt(p.coins),
        safeInt(p.emeralds),
        rankVal,
        safeInt(p.smp_level, 1),
        safeInt(p.hc_level, 1),
        safeInt(p.global_level, 1),
      ]
    );

    await client.query(
      `INSERT INTO smp_stats (uuid, kills, deaths, playtime)
       VALUES ($1,$2,$3,$4)
       ON CONFLICT (uuid) DO UPDATE SET
         kills = EXCLUDED.kills,
         deaths = EXCLUDED.deaths,
         playtime = EXCLUDED.playtime`,
      [
        uuid,
        safeInt(smp.kills ?? p.kills),
        safeInt(smp.deaths),
        safeInt(smp.playtime ?? p.playtime),
      ]
    );

    await client.query(
      `INSERT INTO hc_stats (uuid, hc_kills, hc_deaths, hc_playtime, hc_totems_used, hc_wins, hc_level)
       VALUES ($1,$2,$3,$4,$5,$6,$7)
       ON CONFLICT (uuid) DO UPDATE SET
         hc_kills = EXCLUDED.hc_kills,
         hc_deaths = EXCLUDED.hc_deaths,
         hc_playtime = EXCLUDED.hc_playtime,
         hc_totems_used = EXCLUDED.hc_totems_used,
         hc_wins = EXCLUDED.hc_wins,
         hc_level = EXCLUDED.hc_level`,
      [
        uuid,
        safeInt(hc.hc_kills ?? hc.kills),
        safeInt(hc.hc_deaths ?? hc.deaths),
        safeInt(hc.hc_playtime ?? hc.playtime),
        safeInt(hc.hc_totems_used),
        safeInt(hc.hc_wins ?? hc.wins),
        safeInt(hc.hc_level ?? p.hc_level, 1),
      ]
    );

    await client.query(
      `INSERT INTO tierspace_stats (uuid, rating, wins, losses, winstreak)
       VALUES ($1,$2,$3,$4,$5)
       ON CONFLICT (uuid) DO UPDATE SET
         rating = EXCLUDED.rating,
         wins = EXCLUDED.wins,
         losses = EXCLUDED.losses,
         winstreak = EXCLUDED.winstreak`,
      [
        uuid,
        safeInt(ts.rating, 1000),
        safeInt(ts.wins),
        safeInt(ts.losses),
        safeInt(ts.winstreak),
      ]
    );

    await client.query('COMMIT');

    const row = await getPlayerFull(uuid);
    const snapshotService = require('../services/playerSnapshotService');
    const profileMeta = p.profile_meta || null;
    await snapshotService.onPlayerSaved(uuid, row, profileMeta);

    return uuid;
  } catch (err) {
    await client.query('ROLLBACK').catch(() => {});
    throw err;
  } finally {
    client.release();
  }
}

async function addCoins(uuid, amount, username) {
  const id = await ensurePlayer(uuid, username);
  const result = await query(
    `UPDATE players SET
       coins = GREATEST(0, coins + $2),
       username = COALESCE(NULLIF($3, 'Unknown'), username),
       updated_at = NOW()
     WHERE uuid = $1
     RETURNING coins`,
    [id, parseInt(amount, 10) || 0, (username || 'Unknown').trim().slice(0, 32)]
  );
  return Number(result.rows[0]?.coins) || 0;
}

async function getLeaderboard(metric, limit = 10) {
  const key = String(metric || 'coins').toLowerCase();
  const spec = LEADERBOARD_METRICS[key];
  if (!spec) {
    return { error: 'unknown_metric', metric: key };
  }
  if (!ALLOWED_TABLES.has(spec.table) || !ALLOWED_COLUMNS[spec.table].has(spec.column)) {
    return { error: 'invalid_metric' };
  }
  const lim = Math.min(50, Math.max(1, parseInt(limit, 10) || 10));
  let sql;
  if (spec.table === 'players') {
    sql = `SELECT p.uuid, p.username AS name, p.${spec.column} AS value
           FROM players p
           ORDER BY p.${spec.column} DESC NULLS LAST
           LIMIT $1`;
  } else {
    sql = `SELECT p.uuid, p.username AS name, t.${spec.column} AS value
           FROM ${spec.table} t
           INNER JOIN players p ON p.uuid = t.uuid
           ORDER BY t.${spec.column} DESC NULLS LAST
           LIMIT $1`;
  }
  const result = await query(sql, [lim]);
  return {
    metric: key,
    entries: result.rows.map((row, i) => ({
      rank: i + 1,
      uuid: row.uuid,
      name: row.name || 'Unknown',
      value: Number(row.value) || 0,
    })),
  };
}

module.exports = {
  ensurePlayer,
  getPlayerFull,
  savePlayerFull,
  addCoins,
  getLeaderboard,
  LEADERBOARD_METRICS,
};
