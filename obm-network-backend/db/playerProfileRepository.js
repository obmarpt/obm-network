const db = require('../database');

function normalizeUuid(uuid) {
  return String(uuid || '').trim().toLowerCase();
}

function seasonPoints(coins, kills, rating) {
  return Math.max(0, (coins || 0) + (kills || 0) * 50 + (rating || 1000));
}

async function getProfileMeta(uuid) {
  const id = normalizeUuid(uuid);
  const r = await db.query(
    `SELECT bp_level, bp_xp, achievements, cosmetics_unlocked, cosmetics_active, updated_at
     FROM player_profile_meta WHERE uuid = $1`,
    [id]
  );
  return r.rows[0] || null;
}

async function upsertProfileMeta(uuid, meta = {}) {
  const id = normalizeUuid(uuid);
  const achievements = JSON.stringify(Array.isArray(meta.achievements) ? meta.achievements : []);
  const unlocked = JSON.stringify(Array.isArray(meta.cosmetics_unlocked) ? meta.cosmetics_unlocked : []);
  const active = JSON.stringify(meta.cosmetics_active && typeof meta.cosmetics_active === 'object' ? meta.cosmetics_active : {});

  await db.query(
    `INSERT INTO player_profile_meta (uuid, bp_level, bp_xp, achievements, cosmetics_unlocked, cosmetics_active, updated_at)
     VALUES ($1, $2, $3, $4::jsonb, $5::jsonb, $6::jsonb, NOW())
     ON CONFLICT (uuid) DO UPDATE SET
       bp_level = EXCLUDED.bp_level,
       bp_xp = EXCLUDED.bp_xp,
       achievements = EXCLUDED.achievements,
       cosmetics_unlocked = EXCLUDED.cosmetics_unlocked,
       cosmetics_active = EXCLUDED.cosmetics_active,
       updated_at = NOW()`,
    [
      id,
      Math.max(0, parseInt(meta.bp_level, 10) || 0),
      Math.max(0, parseInt(meta.bp_xp, 10) || 0),
      achievements,
      unlocked,
      active,
    ]
  );
}

async function upsertDailySnapshot(uuid, stats) {
  const id = normalizeUuid(uuid);
  const coins = Math.max(0, parseInt(stats.coins, 10) || 0);
  const kills = Math.max(0, parseInt(stats.kills, 10) || 0);
  const deaths = Math.max(0, parseInt(stats.deaths, 10) || 0);
  const rating = Math.max(0, parseInt(stats.rating, 10) || 1000);
  const bpLevel = Math.max(0, parseInt(stats.bp_level, 10) || 0);

  const ranks = await getRankPositions(id, { coins, kills, rating });

  await db.query(
    `INSERT INTO player_stats_snapshots
       (uuid, snapshot_date, coins, kills, deaths, rating, bp_level, rank_money, rank_kills, rank_rating, rank_overall)
     VALUES ($1, CURRENT_DATE, $2, $3, $4, $5, $6, $7, $8, $9, $10)
     ON CONFLICT (uuid, snapshot_date) DO UPDATE SET
       coins = EXCLUDED.coins,
       kills = EXCLUDED.kills,
       deaths = EXCLUDED.deaths,
       rating = EXCLUDED.rating,
       bp_level = EXCLUDED.bp_level,
       rank_money = EXCLUDED.rank_money,
       rank_kills = EXCLUDED.rank_kills,
       rank_rating = EXCLUDED.rank_rating,
       rank_overall = EXCLUDED.rank_overall`,
    [id, coins, kills, deaths, rating, bpLevel, ranks.money, ranks.kills, ranks.rating, ranks.overall]
  );
}

async function getSnapshots(uuid, days = 30) {
  const id = normalizeUuid(uuid);
  const lim = Math.min(90, Math.max(7, parseInt(days, 10) || 30));
  const r = await db.query(
    `SELECT snapshot_date, coins, kills, deaths, rating, bp_level,
            rank_money, rank_kills, rank_rating, rank_overall
     FROM player_stats_snapshots
     WHERE uuid = $1 AND snapshot_date >= CURRENT_DATE - $2::int
     ORDER BY snapshot_date ASC`,
    [id, lim]
  );
  return r.rows;
}

async function getActiveSeason() {
  const r = await db.query(
    `SELECT id, name, mode, starts_at, ends_at, is_active
     FROM seasons WHERE is_active = true
     ORDER BY starts_at DESC LIMIT 1`
  );
  return r.rows[0] || null;
}

async function getAllSeasons() {
  const r = await db.query(
    `SELECT id, name, mode, starts_at, ends_at, is_active
     FROM seasons ORDER BY starts_at DESC`
  );
  return r.rows;
}

async function ensureDefaultSeason() {
  const active = await getActiveSeason();
  if (active) return active;

  const countR = await db.query('SELECT COUNT(*)::int AS c FROM seasons');
  const num = (countR.rows[0]?.c || 0) + 1;
  const r = await db.query(
    `INSERT INTO seasons (name, mode, starts_at, ends_at, is_active)
     VALUES ($1, 'global', NOW(), NOW() + INTERVAL '30 days', true)
     RETURNING id, name, mode, starts_at, ends_at, is_active`,
    [`Season ${num}`]
  );
  return r.rows[0];
}

async function getPlayerSeasonStats(uuid, seasonId) {
  const id = normalizeUuid(uuid);
  const r = await db.query(
    `SELECT season_id, position, coins, kills, deaths, rating, bp_level, points, updated_at
     FROM player_season_stats WHERE uuid = $1 AND season_id = $2`,
    [id, seasonId]
  );
  return r.rows[0] || null;
}

async function upsertPlayerSeasonStats(uuid, seasonId, stats) {
  const id = normalizeUuid(uuid);
  const coins = Math.max(0, parseInt(stats.coins, 10) || 0);
  const kills = Math.max(0, parseInt(stats.kills, 10) || 0);
  const deaths = Math.max(0, parseInt(stats.deaths, 10) || 0);
  const rating = Math.max(0, parseInt(stats.rating, 10) || 1000);
  const bpLevel = Math.max(0, parseInt(stats.bp_level, 10) || 0);
  const pts = seasonPoints(coins, kills, rating);

  await db.query(
    `INSERT INTO player_season_stats (season_id, uuid, coins, kills, deaths, rating, bp_level, points, updated_at)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW())
     ON CONFLICT (season_id, uuid) DO UPDATE SET
       coins = EXCLUDED.coins,
       kills = EXCLUDED.kills,
       deaths = EXCLUDED.deaths,
       rating = EXCLUDED.rating,
       bp_level = EXCLUDED.bp_level,
       points = EXCLUDED.points,
       updated_at = NOW()`,
    [seasonId, id, coins, kills, deaths, rating, bpLevel, pts]
  );

  const posR = await db.query(
    `SELECT COUNT(*)::int + 1 AS pos
     FROM player_season_stats
     WHERE season_id = $1 AND points > $2`,
    [seasonId, pts]
  );
  const position = posR.rows[0]?.pos || 1;
  await db.query(
    `UPDATE player_season_stats SET position = $3 WHERE season_id = $1 AND uuid = $2`,
    [seasonId, id, position]
  );
  return { position, points: pts };
}

async function getPlayerSeasonHistory(uuid) {
  const id = normalizeUuid(uuid);
  const r = await db.query(
    `SELECT s.id, s.name, s.starts_at, s.ends_at, s.is_active,
            ps.position, ps.coins, ps.kills, ps.deaths, ps.rating, ps.bp_level, ps.points
     FROM player_season_stats ps
     INNER JOIN seasons s ON s.id = ps.season_id
     WHERE ps.uuid = $1
     ORDER BY s.starts_at DESC`,
    [id]
  );
  return r.rows;
}

async function getSeasonWinCount(uuid) {
  const id = normalizeUuid(uuid);
  const r = await db.query(
    `SELECT COUNT(*)::int AS c
     FROM player_season_stats ps
     INNER JOIN seasons s ON s.id = ps.season_id
     WHERE ps.uuid = $1 AND ps.position = 1 AND s.is_active = false`,
    [id]
  );
  return r.rows[0]?.c || 0;
}

async function getRankPositions(uuid, current = null) {
  const id = normalizeUuid(uuid);

  const [moneyR, killsR, ratingR] = await Promise.all([
    db.query(
      `SELECT COUNT(*)::int + 1 AS pos FROM players WHERE coins > COALESCE(
         (SELECT coins FROM players WHERE uuid = $1), 0)`,
      [id]
    ),
    db.query(
      `SELECT COUNT(*)::int + 1 AS pos FROM smp_stats WHERE kills > COALESCE(
         (SELECT kills FROM smp_stats WHERE uuid = $1), 0)`,
      [id]
    ),
    db.query(
      `SELECT COUNT(*)::int + 1 AS pos FROM tierspace_stats WHERE rating > COALESCE(
         (SELECT rating FROM tierspace_stats WHERE uuid = $1), 0)`,
      [id]
    ),
  ]);

  const coins = current?.coins ?? 0;
  const kills = current?.kills ?? 0;
  const rating = current?.rating ?? 1000;
  const score = seasonPoints(coins, kills, rating);

  const overallR = await db.query(
    `SELECT COUNT(*)::int + 1 AS pos FROM (
       SELECT p.uuid,
              p.coins + COALESCE(s.kills, 0) * 50 + COALESCE(t.rating, 1000) AS pts
       FROM players p
       LEFT JOIN smp_stats s ON s.uuid = p.uuid
       LEFT JOIN tierspace_stats t ON t.uuid = p.uuid
     ) x WHERE x.pts > $1`,
    [score]
  );

  return {
    money: moneyR.rows[0]?.pos || null,
    kills: killsR.rows[0]?.pos || null,
    rating: ratingR.rows[0]?.pos || null,
    overall: overallR.rows[0]?.pos || null,
    score,
  };
}

module.exports = {
  normalizeUuid,
  seasonPoints,
  getProfileMeta,
  upsertProfileMeta,
  upsertDailySnapshot,
  getSnapshots,
  getActiveSeason,
  getAllSeasons,
  ensureDefaultSeason,
  getPlayerSeasonStats,
  upsertPlayerSeasonStats,
  getPlayerSeasonHistory,
  getSeasonWinCount,
  getRankPositions,
};
