const db = require('../database');
const { col } = require('../database');

async function recordOnlineSample(count) {
  const safe = Math.max(0, parseInt(count, 10) || 0);
  await db.query('INSERT INTO online_samples (count) VALUES ($1)', [safe]).catch(() => {});
  await db.query(
    `DELETE FROM online_samples WHERE sampled_at < NOW() - INTERVAL '48 hours'`
  ).catch(() => {});
}

async function snapshotDaily() {
  const today = new Date().toISOString().slice(0, 10);
  const [players, coins, kills] = await Promise.all([
    db.query('SELECT COUNT(*)::int AS c FROM players'),
    db.query(`SELECT COALESCE(SUM(${col('coins')}), 0)::bigint AS t FROM players`),
    db.query('SELECT COALESCE(SUM(kills), 0)::bigint AS t FROM smp_stats').catch(() => ({ rows: [{ t: 0 }] })),
  ]);

  await db.query(
    `INSERT INTO stats_daily (day, player_count, total_coins, total_kills)
     VALUES ($1, $2, $3, $4)
     ON CONFLICT (day) DO UPDATE SET
       player_count = EXCLUDED.player_count,
       total_coins = EXCLUDED.total_coins,
       total_kills = EXCLUDED.total_kills`,
    [
      today,
      players.rows[0]?.c || 0,
      Number(coins.rows[0]?.t) || 0,
      Number(kills.rows[0]?.t) || 0,
    ]
  );
}

async function incrementDailyJoin() {
  const today = new Date().toISOString().slice(0, 10);
  await db.query(
    `INSERT INTO stats_daily (day, joins_count) VALUES ($1, 1)
     ON CONFLICT (day) DO UPDATE SET joins_count = stats_daily.joins_count + 1`,
    [today]
  ).catch(() => {});
}

async function incrementDailyKill() {
  const today = new Date().toISOString().slice(0, 10);
  await db.query(
    `INSERT INTO stats_daily (day, kills_count) VALUES ($1, 1)
     ON CONFLICT (day) DO UPDATE SET kills_count = stats_daily.kills_count + 1`,
    [today]
  ).catch(() => {});
}

async function getHistory(days = 14) {
  const d = Math.min(90, Math.max(1, parseInt(days, 10) || 14));

  const [daily, online, recentOnline] = await Promise.all([
    db.query(
      `SELECT day, player_count, total_coins, total_kills, joins_count, kills_count
       FROM stats_daily
       WHERE day >= CURRENT_DATE - $1::int
       ORDER BY day ASC`,
      [d]
    ).catch(() => ({ rows: [] })),
    db.query(
      `SELECT count, sampled_at AS t
       FROM online_samples
       WHERE sampled_at >= NOW() - INTERVAL '24 hours'
       ORDER BY sampled_at ASC`
    ).catch(() => ({ rows: [] })),
    db.query(
      `SELECT COUNT(*)::int AS c FROM players
       WHERE updated_at > NOW() - INTERVAL '15 minutes'`
    ).catch(() => ({ rows: [{ c: 0 }] })),
  ]);

  await recordOnlineSample(recentOnline.rows[0]?.c || 0);
  await snapshotDaily().catch(() => {});

  return {
    days: d,
    onlineNow: recentOnline.rows[0]?.c || 0,
    online: online.rows.map((r) => ({
      t: r.t,
      count: Number(r.count) || 0,
    })),
    playerGrowth: daily.rows.map((r) => ({
      date: r.day,
      count: Number(r.player_count) || 0,
    })),
    killsPerDay: daily.rows.map((r) => ({
      date: r.day,
      kills: Number(r.kills_count) || 0,
    })),
    economy: daily.rows.map((r) => ({
      date: r.day,
      coins: Number(r.total_coins) || 0,
    })),
    joinsPerDay: daily.rows.map((r) => ({
      date: r.day,
      joins: Number(r.joins_count) || 0,
    })),
  };
}

function startSchedulers() {
  setInterval(() => snapshotDaily().catch(() => {}), 60 * 60 * 1000);
  setInterval(async () => {
    try {
      const r = await db.query(
        `SELECT COUNT(*)::int AS c FROM players WHERE updated_at > NOW() - INTERVAL '15 minutes'`
      );
      await recordOnlineSample(r.rows[0]?.c || 0);
    } catch { /* ignore */ }
  }, 5 * 60 * 1000);
}

module.exports = {
  getHistory,
  recordOnlineSample,
  snapshotDaily,
  incrementDailyJoin,
  incrementDailyKill,
  startSchedulers,
};
