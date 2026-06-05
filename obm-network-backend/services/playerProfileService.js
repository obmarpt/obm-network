const fs = require('fs');
const path = require('path');
const profileRepo = require('../db/playerProfileRepository');
const playerRepo = require('../db/playerRepository');

const catalogPath = path.join(__dirname, '..', 'data', 'game-catalog.json');

function loadCatalog() {
  return JSON.parse(fs.readFileSync(catalogPath, 'utf8'));
}

function parseJsonArray(val) {
  if (Array.isArray(val)) return val;
  if (typeof val === 'string') {
    try { return JSON.parse(val); } catch { return []; }
  }
  return [];
}

function parseJsonObject(val) {
  if (val && typeof val === 'object' && !Array.isArray(val)) return val;
  if (typeof val === 'string') {
    try { return JSON.parse(val); } catch { return {}; }
  }
  return {};
}

const TRIGGER_MAP = {
  kills: (row) => Number(row.smp_kills) || 0,
  duel_wins: (row) => Number(row.ts_wins) || 0,
  coins: (row) => Number(row.coins) || 0,
  killstreak: () => 0,
  hc_minutes: (row) => Math.floor(Number(row.hc_playtime) || 0),
};

function fmtRank(name) {
  if (!name) return 'Bronze';
  return String(name).replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
}

function deriveBadges(row, rankings, season, seasonHistory, meta) {
  const badges = [];
  const positions = [rankings.money, rankings.kills, rankings.rating, rankings.overall, season?.position];
  if (positions.some((p) => p != null && p <= 10)) {
    badges.push({ id: 'top10', name: 'Top 10', icon: '🏅', tier: 'gold' });
  }
  const wins = seasonHistory.filter((s) => s.position === 1 && !s.is_active).length;
  if (wins > 0) {
    badges.push({ id: 'season_winner', name: 'Season Winner', icon: '👑', tier: 'legendary' });
  }
  const created = row.created_at ? new Date(row.created_at) : null;
  if (created) {
    const days = (Date.now() - created.getTime()) / 86400000;
    if (days >= 90) {
      badges.push({ id: 'veteran', name: 'Veteran', icon: '⭐', tier: 'epic' });
    }
  }
  const completed = Array.isArray(meta?.achievements) ? meta.achievements.length : 0;
  if (completed >= 5) {
    badges.push({ id: 'achiever', name: 'Achiever', icon: '🎯', tier: 'rare' });
  }
  return badges;
}

function buildAchievements(row, meta) {
  const catalog = loadCatalog();
  const completedSet = new Set(Array.isArray(meta?.achievements) ? meta.achievements : []);

  return (catalog.achievements || []).map((a) => {
    const current = TRIGGER_MAP[a.trigger]?.(row) ?? 0;
    const storedDone = completedSet.has(a.id);
    const done = storedDone || current >= a.target;
    const pct = Math.min(100, Math.round((current / a.target) * 100));
    return {
      id: a.id,
      name: a.name,
      description: a.description,
      rarity: a.rarity,
      reward: a.reward,
      target: a.target,
      current,
      completed: done,
      progress: pct,
    };
  });
}

function buildCosmetics(row, meta) {
  const catalog = loadCatalog();
  const unlockedSet = new Set(Array.isArray(meta?.cosmetics_unlocked) ? meta.cosmetics_unlocked : []);
  const active = meta?.cosmetics_active && typeof meta.cosmetics_active === 'object' ? meta.cosmetics_active : {};

  const items = (catalog.cosmetics || []).map((c) => {
    const unlocked = unlockedSet.has(c.id);
    const isActive = Object.values(active).includes(c.id);
    return {
      id: c.id,
      name: c.name,
      type: c.type,
      rarity: c.rarity,
      unlock: c.unlock,
      unlocked,
      active: isActive,
    };
  });

  return {
    unlocked: items.filter((i) => i.unlocked),
    active: items.filter((i) => i.active),
    catalog: items,
  };
}

function buildCharts(snapshots) {
  const money = [];
  const kills = [];
  const ranking = [];

  for (const s of snapshots) {
    const date = s.snapshot_date instanceof Date
      ? s.snapshot_date.toISOString().slice(0, 10)
      : String(s.snapshot_date).slice(0, 10);
    money.push({ date, value: Number(s.coins) || 0 });
    kills.push({ date, value: Number(s.kills) || 0 });
    ranking.push({ date, value: Number(s.rank_overall) || 0 });
  }

  return { money, kills, ranking };
}

function seasonProgress(seasonStats, row) {
  if (!seasonStats) return 0;
  const pts = Number(seasonStats.points) || 0;
  const target = 50000;
  return Math.min(100, Math.round((pts / target) * 100));
}

async function buildFullProfile(uuid) {
  let row = await playerRepo.getPlayerFull(uuid);
  if (!row) {
    await playerRepo.ensurePlayer(uuid);
    row = await playerRepo.getPlayerFull(uuid);
  }
  if (!row) return null;

  const [meta, snapshots, season, seasonHistory, rankings] = await Promise.all([
    profileRepo.getProfileMeta(uuid),
    profileRepo.getSnapshots(uuid, 30),
    profileRepo.getActiveSeason(),
    profileRepo.getPlayerSeasonHistory(uuid),
    profileRepo.getRankPositions(uuid, {
      coins: row.coins,
      kills: row.smp_kills,
      rating: row.rating,
    }),
  ]);

  const metaParsed = meta ? {
    bp_level: meta.bp_level,
    bp_xp: meta.bp_xp,
    achievements: parseJsonArray(meta.achievements),
    cosmetics_unlocked: parseJsonArray(meta.cosmetics_unlocked),
    cosmetics_active: parseJsonObject(meta.cosmetics_active),
  } : { bp_level: 0, bp_xp: 0, achievements: [], cosmetics_unlocked: [], cosmetics_active: {} };

  const seasonStats = season ? await profileRepo.getPlayerSeasonStats(uuid, season.id) : null;
  const kd = row.smp_deaths > 0 ? (row.smp_kills / row.smp_deaths).toFixed(2) : String(row.smp_kills);

  const achievements = buildAchievements(row, metaParsed);
  const cosmetics = buildCosmetics(row, metaParsed);
  const charts = buildCharts(snapshots);
  const badges = deriveBadges(row, rankings, seasonStats, seasonHistory, metaParsed);

  const completedAchievements = achievements.filter((a) => a.completed).length;

  return {
    profile: {
      uuid: row.uuid,
      username: row.username,
      rank: row.rank || 'bronze',
      rankDisplay: fmtRank(row.rank),
      coins: Number(row.coins) || 0,
      emeralds: Number(row.emeralds) || 0,
      smpLevel: Number(row.smp_level) || 1,
      hcLevel: Number(row.hc_level) || 1,
      globalLevel: Number(row.global_level) || 1,
      bpLevel: metaParsed.bp_level || Math.min(50, Math.floor((row.global_level || 1) / 2)),
      bpXp: metaParsed.bp_xp || 0,
      memberSince: row.created_at,
      smp: {
        kills: Number(row.smp_kills) || 0,
        deaths: Number(row.smp_deaths) || 0,
        playtime: Number(row.smp_playtime) || 0,
        kd,
      },
      hc: {
        kills: Number(row.hc_kills) || 0,
        deaths: Number(row.hc_deaths) || 0,
        playtime: Number(row.hc_playtime) || 0,
        wins: Number(row.hc_wins) || 0,
        level: Number(row.hc_stat_level) || 1,
      },
      tierspace: {
        rating: Number(row.rating) || 1000,
        wins: Number(row.ts_wins) || 0,
        losses: Number(row.ts_losses) || 0,
        winstreak: Number(row.winstreak) || 0,
      },
    },
    rankings: {
      smp: { money: rankings.money, kills: rankings.kills },
      tierspace: { rating: rankings.rating },
      overall: rankings.overall,
      score: rankings.score,
    },
    season: season ? {
      id: season.id,
      name: season.name,
      startsAt: season.starts_at,
      endsAt: season.ends_at,
      position: seasonStats?.position ?? null,
      points: seasonStats?.points ?? 0,
      progress: seasonProgress(seasonStats, row),
      stats: seasonStats ? {
        coins: seasonStats.coins,
        kills: seasonStats.kills,
        deaths: seasonStats.deaths,
        rating: seasonStats.rating,
        bpLevel: seasonStats.bp_level,
      } : null,
    } : null,
    seasonHistory: seasonHistory.map((s) => ({
      id: s.id,
      name: s.name,
      startsAt: s.starts_at,
      endsAt: s.ends_at,
      active: s.is_active,
      position: s.position,
      points: s.points,
      stats: {
        coins: s.coins,
        kills: s.kills,
        deaths: s.deaths,
        rating: s.rating,
        bpLevel: s.bp_level,
      },
    })),
    charts,
    achievements: {
      completed: completedAchievements,
      total: achievements.length,
      items: achievements,
    },
    cosmetics,
    badges,
    cachedAt: Date.now(),
  };
}

module.exports = { buildFullProfile, loadCatalog };
