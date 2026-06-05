const db = require('../database');
const playerRepo = require('./playerRepository');
const { col } = require('../database');

function normalizeUuid(uuid) {
  return String(uuid || '').trim().toLowerCase();
}

async function getGlobalStats() {
  const [players, coins, emeralds, kills, deaths, matches, events] = await Promise.all([
    db.query('SELECT COUNT(*)::int AS c FROM players'),
    db.query(`SELECT COALESCE(SUM(${col('coins')}), 0)::bigint AS t FROM players`),
    db.query(`SELECT COALESCE(SUM(${col('emeralds')}), 0)::bigint AS t FROM players`),
    db.query('SELECT COALESCE(SUM(kills), 0)::bigint AS t FROM smp_stats').catch(() => ({ rows: [{ t: 0 }] })),
    db.query('SELECT COALESCE(SUM(deaths), 0)::bigint AS t FROM smp_stats').catch(() => ({ rows: [{ t: 0 }] })),
    db.query('SELECT COUNT(*)::int AS c FROM matches').catch(() => ({ rows: [{ c: 0 }] })),
    db.query('SELECT COUNT(*)::int AS c FROM server_events').catch(() => ({ rows: [{ c: 0 }] })),
  ]);

  const totalPlayers = players.rows[0]?.c || 0;
  const totalCoins = Number(coins.rows[0]?.t) || 0;

  return {
    totalPlayers,
    totalCoins,
    totalEmeralds: Number(emeralds.rows[0]?.t) || 0,
    avgCoins: totalPlayers > 0 ? Math.round(totalCoins / totalPlayers) : 0,
    totalKills: Number(kills.rows[0]?.t) || 0,
    totalDeaths: Number(deaths.rows[0]?.t) || 0,
    totalMatches: matches.rows[0]?.c || 0,
    totalEvents: events.rows[0]?.c || 0,
    databaseAvailable: db.isDatabaseAvailable(),
  };
}

async function listPlayers({ q = '', page = 1, limit = 25 } = {}) {
  const lim = Math.min(100, Math.max(1, parseInt(limit, 10) || 25));
  const pg = Math.max(1, parseInt(page, 10) || 1);
  const offset = (pg - 1) * lim;
  const search = String(q || '').trim().toLowerCase();
  const params = [];
  let where = '';

  if (search) {
    params.push(`%${search}%`, `%${search}%`);
    where = `WHERE LOWER(p.username) LIKE $1 OR LOWER(p.uuid::text) LIKE $2`;
  }

  const countSql = `SELECT COUNT(*)::int AS c FROM players p ${where}`;
  const countRes = await db.query(countSql, params);
  const total = countRes.rows[0]?.c || 0;

  params.push(lim, offset);
  const listSql = `
    SELECT
      p.uuid,
      p.username,
      p.${col('coins')} AS coins,
      p.${col('emeralds')} AS emeralds,
      p.smp_level,
      p.hc_level,
      p.global_level,
      p.world,
      p.updated_at,
      COALESCE(s.kills, 0) AS smp_kills,
      COALESCE(s.deaths, 0) AS smp_deaths,
      COALESCE(h.hc_kills, 0) AS hc_kills,
      COALESCE(t.rating, 1000) AS tierspace_rating
    FROM players p
    LEFT JOIN smp_stats s ON s.uuid = p.uuid
    LEFT JOIN hc_stats h ON h.uuid = p.uuid
    LEFT JOIN tierspace_stats t ON t.uuid = p.uuid
    ${where}
    ORDER BY p.updated_at DESC NULLS LAST, p.created_at DESC
    LIMIT $${params.length - 1} OFFSET $${params.length}
  `;

  const listRes = await db.query(listSql, params);

  return {
    page: pg,
    limit: lim,
    total,
    pages: Math.max(1, Math.ceil(total / lim)),
    players: listRes.rows.map((row) => ({
      uuid: row.uuid,
      username: row.username,
      name: row.username,
      coins: Number(row.coins) || 0,
      emeralds: Number(row.emeralds) || 0,
      smp_level: Number(row.smp_level) || 1,
      hc_level: Number(row.hc_level) || 1,
      global_level: Number(row.global_level) || 1,
      world: row.world,
      smp_kills: Number(row.smp_kills) || 0,
      smp_deaths: Number(row.smp_deaths) || 0,
      hc_kills: Number(row.hc_kills) || 0,
      tierspace_rating: Number(row.tierspace_rating) || 1000,
      updated_at: row.updated_at,
    })),
  };
}

async function getOnlinePlayers() {
  const result = await db.query(
    `SELECT p.uuid, p.username, p.world, p.updated_at,
            COALESCE(s.kills, 0) AS smp_kills
     FROM players p
     LEFT JOIN smp_stats s ON s.uuid = p.uuid
     WHERE p.updated_at > NOW() - INTERVAL '15 minutes'
     ORDER BY p.updated_at DESC
     LIMIT 200`
  );
  return { players: result.rows, count: result.rows.length };
}

async function getPlayerProfileFull(uuid) {
  const profile = await getPlayerProfile(uuid);
  if (!profile) return null;

  const id = normalizeUuid(uuid);
  const bansRepo = require('./bansRepository');

  const [bans, events, matches, reports] = await Promise.all([
    bansRepo.listBanHistory(id),
    db.query(
      `SELECT event_type, actor_name, target_name, detail, created_at
       FROM server_events
       WHERE actor_uuid = $1 OR target_uuid = $1
       ORDER BY created_at DESC LIMIT 40`,
      [id]
    ).catch(() => ({ rows: [] })),
    db.query(
      `SELECT id, mode, result, created_at FROM matches
       WHERE player1_uuid = $1 OR player2_uuid = $1
       ORDER BY created_at DESC LIMIT 20`,
      [id]
    ).catch(() => ({ rows: [] })),
    db.query(
      `SELECT id, reporter_name, reason, status, created_at FROM reports
       WHERE target_uuid = $1 OR reporter_uuid = $1
       ORDER BY created_at DESC LIMIT 20`,
      [id]
    ).catch(() => ({ rows: [] })),
  ]);

  const activeBan = await bansRepo.getActiveBan(id);

  return {
    ...profile,
    active_ban: activeBan,
    history: {
      bans: bans,
      events: events.rows,
      matches: matches.rows,
      reports: reports.rows,
    },
  };
}

async function getPlayerProfile(uuid) {
  const id = normalizeUuid(uuid);
  let row = await playerRepo.getPlayerFull(id);
  if (!row) {
    await playerRepo.ensurePlayer(id, 'Unknown');
    row = await playerRepo.getPlayerFull(id);
  }
  if (!row) return null;

  return {
    uuid: row.uuid,
    username: row.username,
    coins: Number(row.coins) || 0,
    emeralds: Number(row.emeralds) || 0,
    smp_level: Number(row.smp_level) || 1,
    hc_level: Number(row.hc_level) || 1,
    global_level: Number(row.global_level) || 1,
    created_at: row.created_at,
    updated_at: row.updated_at,
    smp: {
      kills: Number(row.smp_kills) || 0,
      deaths: Number(row.smp_deaths) || 0,
      playtime: Number(row.smp_playtime) || 0,
    },
    hc: {
      hc_kills: Number(row.hc_kills) || 0,
      hc_deaths: Number(row.hc_deaths) || 0,
      hc_wins: Number(row.hc_wins) || 0,
      hc_playtime: Number(row.hc_playtime) || 0,
      hc_totems_used: Number(row.hc_totems_used) || 0,
    },
    tierspace: {
      rating: Number(row.rating) || 1000,
      wins: Number(row.ts_wins) || 0,
      losses: Number(row.ts_losses) || 0,
      winstreak: Number(row.winstreak) || 0,
    },
  };
}

async function updateEconomy({ uuid, username, coinsDelta, emeraldsDelta, setCoins, setEmeralds }) {
  const id = normalizeUuid(uuid);
  await playerRepo.ensurePlayer(id, username || 'Unknown');

  if (setCoins !== undefined && setCoins !== null && setCoins !== '') {
    const v = Math.max(0, parseInt(setCoins, 10) || 0);
    await db.query(
      `UPDATE players SET ${col('coins')} = $2, updated_at = NOW() WHERE uuid = $1`,
      [id, v]
    );
  } else if (coinsDelta !== undefined && coinsDelta !== null) {
    const delta = parseInt(coinsDelta, 10) || 0;
    if (delta !== 0) {
      await playerRepo.addCoins(id, delta, username);
    }
  }

  if (setEmeralds !== undefined && setEmeralds !== null && setEmeralds !== '') {
    const v = Math.max(0, parseInt(setEmeralds, 10) || 0);
    await db.query(
      `UPDATE players SET ${col('emeralds')} = $2, updated_at = NOW() WHERE uuid = $1`,
      [id, v]
    );
  } else if (emeraldsDelta !== undefined && emeraldsDelta !== null) {
    const delta = parseInt(emeraldsDelta, 10) || 0;
    if (delta !== 0) {
      await db.query(
        `UPDATE players SET ${col('emeralds')} = GREATEST(0, ${col('emeralds')} + $2), updated_at = NOW() WHERE uuid = $1 RETURNING ${col('emeralds')} AS emeralds`,
        [id, delta]
      );
    }
  }

  return getPlayerProfile(id);
}

async function getAllLeaderboards(limit = 10) {
  const lim = Math.min(50, Math.max(1, parseInt(limit, 10) || 10));
  const [smpKills, smpCoins, hcKills, hcTotems, tierspace] = await Promise.all([
    playerRepo.getLeaderboard('kills', lim),
    playerRepo.getLeaderboard('coins', lim),
    playerRepo.getLeaderboard('hc_kills', lim),
    playerRepo.getLeaderboard('hc_totems_used', lim),
    playerRepo.getLeaderboard('rating', lim),
  ]);
  return {
    smp: { kills: smpKills, coins: smpCoins },
    hc: { kills: hcKills, totems: hcTotems },
    tierspace,
  };
}

async function insertServerEvent({ type, actorUuid, actorName, targetUuid, targetName, detail }) {
  try {
    await db.query(
      `INSERT INTO server_events (event_type, actor_uuid, actor_name, target_uuid, target_name, detail)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [
        String(type || 'other').slice(0, 32),
        actorUuid ? normalizeUuid(actorUuid) : null,
        actorName ? String(actorName).slice(0, 32) : null,
        targetUuid ? normalizeUuid(targetUuid) : null,
        targetName ? String(targetName).slice(0, 32) : null,
        detail != null ? String(detail).slice(0, 2000) : null,
      ]
    );
  } catch (err) {
    console.warn('[Admin] insertServerEvent:', err.message);
  }
}

async function getAdminLogs({ type = 'all', page = 1, limit = 50 } = {}) {
  const lim = Math.min(200, Math.max(1, parseInt(limit, 10) || 50));
  const pg = Math.max(1, parseInt(page, 10) || 1);
  const offset = (pg - 1) * lim;
  const filter = String(type || 'all').toLowerCase();
  const entries = [];

  if (filter === 'all' || filter === 'admin') {
    const admin = await db.query(
      `SELECT id, staff AS actor, action, target, value AS detail, created_at
       FROM logs ORDER BY created_at DESC LIMIT 500`
    ).catch(() => ({ rows: [] }));
    for (const row of admin.rows) {
      entries.push({
        id: `log-${row.id}`,
        type: 'admin',
        actor: row.actor,
        action: row.action,
        target: row.target,
        detail: row.detail,
        created_at: row.created_at,
      });
    }
  }

  if (filter === 'all' || filter === 'command') {
    const cmds = await db.query(
      `SELECT id, command, executed, source, result, created_at, executed_at
       FROM commands ORDER BY created_at DESC LIMIT 500`
    ).catch(() => ({ rows: [] }));
    for (const row of cmds.rows) {
      entries.push({
        id: `cmd-${row.id}`,
        type: 'command',
        actor: row.source || 'system',
        action: row.executed ? 'executed' : 'pending',
        target: row.command,
        detail: row.result || '',
        created_at: row.executed_at || row.created_at,
      });
    }
  }

  if (filter === 'all' || filter === 'ban') {
    const banRows = await db.query(
      `SELECT id, uuid, username, reason, banned_by, created_at, expires_at, active
       FROM bans ORDER BY created_at DESC LIMIT 200`
    ).catch(() => ({ rows: [] }));
    for (const row of banRows.rows) {
      entries.push({
        id: `ban-${row.id}`,
        type: 'ban',
        actor: row.banned_by,
        action: row.active ? 'ban' : 'unban',
        target: row.username || row.uuid,
        detail: row.reason,
        created_at: row.created_at,
      });
    }
  }

  if (filter === 'all' || filter === 'join' || filter === 'kill' || filter === 'event') {
    let eventFilter = filter;
    if (filter === 'all' || filter === 'event') eventFilter = null;
    const params = [];
    let sql = `SELECT id, event_type, actor_uuid, actor_name, target_uuid, target_name, detail, created_at
               FROM server_events`;
    if (eventFilter && eventFilter !== 'event') {
      params.push(eventFilter);
      sql += ` WHERE event_type = $1`;
    }
    sql += ' ORDER BY created_at DESC LIMIT 500';
    const ev = await db.query(sql, params).catch(() => ({ rows: [] }));
    for (const row of ev.rows) {
      entries.push({
        id: `ev-${row.id}`,
        type: row.event_type,
        actor: row.actor_name || row.actor_uuid,
        action: row.event_type,
        target: row.target_name || row.target_uuid,
        detail: row.detail,
        created_at: row.created_at,
      });
    }
  }

  entries.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
  const slice = entries.slice(offset, offset + lim);

  return {
    page: pg,
    limit: lim,
    total: entries.length,
    entries: slice,
  };
}

async function getCommandHistory(limit = 50) {
  const lim = Math.min(200, Math.max(1, parseInt(limit, 10) || 50));
  const result = await db.query(
    `SELECT id, command, executed, source, result, created_at, executed_at
     FROM commands ORDER BY created_at DESC LIMIT $1`,
    [lim]
  );
  return result.rows;
}

async function queueAdminCommand(command, source = 'admin') {
  const result = await db.query(
    `INSERT INTO commands (command, source) VALUES ($1, $2) RETURNING id, command, created_at`,
    [command, String(source).slice(0, 32)]
  );
  return result.rows[0];
}

async function getMonitor() {
  const start = Date.now();
  let dbPingMs = null;
  let dbOk = false;
  try {
    await db.ping();
    dbPingMs = Date.now() - start;
    dbOk = db.isDatabaseAvailable();
  } catch {
    dbOk = false;
  }

  const [recent, pendingCommands] = await Promise.all([
    db.query(
      `SELECT COUNT(*)::int AS c FROM players
       WHERE updated_at > NOW() - INTERVAL '15 minutes'`
    ).catch(() => ({ rows: [{ c: 0 }] })),
    db.query(
      `SELECT COUNT(*)::int AS c FROM commands WHERE executed = false`
    ).catch(() => ({ rows: [{ c: 0 }] })),
  ]);

  return {
    backend: 'online',
    databaseAvailable: dbOk,
    dbPingMs,
    playersOnlineEstimate: recent.rows[0]?.c || 0,
    pendingCommands: pendingCommands.rows[0]?.c || 0,
    uptimeSeconds: Math.floor(process.uptime()),
  };
}

async function resetPlayerStats(uuid, scope) {
  const id = normalizeUuid(uuid);
  const modes = String(scope || 'all').toLowerCase();

  if (modes === 'all' || modes === 'smp') {
    await db.query(
      `UPDATE smp_stats SET kills=0, deaths=0, playtime=0 WHERE uuid=$1`,
      [id]
    ).catch(() => {});
  }
  if (modes === 'all' || modes === 'hc') {
    await db.query(
      `UPDATE hc_stats SET hc_kills=0, hc_deaths=0, hc_playtime=0, hc_totems_used=0, hc_wins=0 WHERE uuid=$1`,
      [id]
    ).catch(() => {});
  }
  if (modes === 'all' || modes === 'tierspace' || modes === 'tier') {
    await db.query(
      `UPDATE tierspace_stats SET rating=1000, wins=0, losses=0, winstreak=0 WHERE uuid=$1`,
      [id]
    ).catch(() => {});
  }
  if (modes === 'economy' || modes === 'all') {
    await db.query(
      `UPDATE players SET ${col('coins')}=0, ${col('emeralds')}=0, updated_at=NOW() WHERE uuid=$1`,
      [id]
    );
  }
  return id;
}

async function exportCriticalData() {
  const [players, smp, hc, tierspace] = await Promise.all([
    db.query('SELECT * FROM players ORDER BY updated_at DESC NULLS LAST LIMIT 50000'),
    db.query('SELECT * FROM smp_stats'),
    db.query('SELECT * FROM hc_stats'),
    db.query('SELECT * FROM tierspace_stats'),
  ]);

  return {
    exportedAt: new Date().toISOString(),
    players: players.rows,
    smp_stats: smp.rows,
    hc_stats: hc.rows,
    tierspace_stats: tierspace.rows,
  };
}

module.exports = {
  getGlobalStats,
  getOnlinePlayers,
  listPlayers,
  getPlayerProfile,
  getPlayerProfileFull,
  updateEconomy,
  getAllLeaderboards,
  insertServerEvent,
  getAdminLogs,
  getCommandHistory,
  queueAdminCommand,
  getMonitor,
  resetPlayerStats,
  exportCriticalData,
};
