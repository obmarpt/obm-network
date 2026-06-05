const { Router } = require('express');
const rateLimit = require('express-rate-limit');
const playerRepo = require('../db/playerRepository');
const requestCache = require('../services/requestCache');
const statsHistory = require('../services/statsHistoryService');
const playerProfileService = require('../services/playerProfileService');
const mojang = require('../services/mojangService');
const db = require('../database');
const { optionalPlayerAuth } = require('../middleware/playerAuth');
const { internalError, badRequest } = require('./validation');

const router = Router();

const publicLimiter = rateLimit({
  windowMs: 60_000,
  max: 120,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'too_many_requests' },
});

router.use(publicLimiter);

router.get('/api/public/server', async (_req, res) => {
  try {
    const data = await requestCache.wrap('public:server', 15_000, async () => {
      const hist = await statsHistory.getHistory(1).catch(() => ({ onlineNow: 0 }));
      let totalPlayers = 0;
      if (db.isDatabaseAvailable()) {
        const r = await db.query('SELECT COUNT(*)::int AS c FROM players');
        totalPlayers = r.rows[0]?.c || 0;
      }
      return {
        name: process.env.SERVER_NAME || 'MineSpace',
        ip: process.env.SERVER_IP || 'play.minespace.pt',
        version: process.env.SERVER_VERSION || '1.21',
        online: hist.onlineNow || 0,
        totalPlayers,
        modes: ['SMP Rush', 'Hardcore', 'TierSpace'],
        discord: process.env.DISCORD_INVITE || null,
      };
    });
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'public/server');
  }
});

const METRICS = {
  kills: 'kills',
  money: 'coins',
  hc: 'hc_kills',
  rating: 'rating',
  emeralds: 'emeralds',
};

const PUBLIC_BOARD_KEYS = ['money', 'kills', 'rating'];

router.get('/api/public/leaderboards', async (_req, res) => {
  try {
    const data = await requestCache.wrap('public:lbs:bundle', 30_000, async () => {
      const boards = {};
      for (const key of PUBLIC_BOARD_KEYS) {
        const metric = METRICS[key] || key;
        const result = await playerRepo.getLeaderboard(metric, 10);
        boards[key] = {
          metric: key,
          entries: result.error ? [] : (result.entries || []),
        };
      }
      return { boards, cachedAt: Date.now() };
    });
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'public/leaderboards');
  }
});

router.get('/api/public/leaderboard/:metric', async (req, res) => {
  const metric = METRICS[req.params.metric] || req.params.metric;
  try {
    const data = await requestCache.wrap(`public:lb:${metric}`, 30_000, async () => {
      const result = await playerRepo.getLeaderboard(metric, 10);
      if (result.error) return { error: result.error, entries: [] };
      return { metric: req.params.metric, entries: result.entries || [] };
    });
    if (data.error) return res.status(400).json(data);
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'public/leaderboard');
  }
});

router.get('/api/public/stats', async (_req, res) => {
  try {
    const data = await requestCache.wrap('public:stats', 20_000, async () => {
      const hist = await statsHistory.getHistory(7);
      let totals = { players: 0, totalCoins: 0, totalEmeralds: 0, totalKills: 0 };
      if (db.isDatabaseAvailable()) {
        const r = await db.query(`
          SELECT
            (SELECT COUNT(*)::int FROM players) AS players,
            (SELECT COALESCE(SUM(coins),0)::bigint FROM players) AS total_coins,
            (SELECT COALESCE(SUM(emeralds),0)::bigint FROM players) AS total_emeralds,
            (SELECT COALESCE(SUM(kills),0)::bigint FROM smp_stats) AS total_kills
        `);
        totals = {
          players: r.rows[0]?.players || 0,
          totalCoins: Number(r.rows[0]?.total_coins || 0),
          totalEmeralds: Number(r.rows[0]?.total_emeralds || 0),
          totalKills: Number(r.rows[0]?.total_kills || 0),
        };
      }
      return {
        onlineNow: hist.onlineNow || 0,
        growth: hist.growth || [],
        economy: hist.economy || [],
        totals,
      };
    });
    res.json(data);
  } catch (err) {
    return internalError(res, err, 'public/stats');
  }
});

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

async function resolvePlayerId(idOrName) {
  const raw = String(idOrName || '').trim();
  if (!raw) return null;
  if (UUID_RE.test(raw)) return raw.toLowerCase();
  const profile = await mojang.lookupUsername(raw);
  return profile?.uuid || null;
}

function formatProfile(row) {
  if (!row) return null;
  const kd = row.smp_deaths > 0 ? (row.smp_kills / row.smp_deaths).toFixed(2) : row.smp_kills;
  return {
    uuid: row.uuid,
    username: row.username,
    rank: row.rank || 'bronze',
    coins: row.coins,
    emeralds: row.emeralds,
    smpLevel: row.smp_level,
    hcLevel: row.hc_level,
    globalLevel: row.global_level,
    smp: {
      kills: row.smp_kills,
      deaths: row.smp_deaths,
      playtime: row.smp_playtime,
      kd,
    },
    hc: {
      kills: row.hc_kills,
      deaths: row.hc_deaths,
      playtime: row.hc_playtime,
      wins: row.hc_wins,
      level: row.hc_stat_level,
    },
    tierspace: {
      rating: row.rating,
      wins: row.ts_wins,
      losses: row.ts_losses,
      winstreak: row.winstreak,
    },
    memberSince: row.created_at,
  };
}

router.get('/api/public/player/search', async (req, res) => {
  const q = String(req.query.q || '').trim();
  if (q.length < 2) return badRequest(res, 'query demasiado curta');
  try {
    const data = await requestCache.wrap(`public:search:${q.toLowerCase()}`, 60_000, async () => {
      const r = await db.query(
        `SELECT uuid, username FROM players WHERE LOWER(username) LIKE $1 ORDER BY username LIMIT 10`,
        [`%${q.toLowerCase()}%`]
      );
      return r.rows;
    });
    res.json({ results: data });
  } catch (err) {
    return internalError(res, err, 'public/player/search');
  }
});

router.get('/api/public/player/:id', optionalPlayerAuth, async (req, res) => {
  try {
    const uuid = await resolvePlayerId(req.params.id);
    if (!uuid) return res.status(404).json({ error: 'player_not_found' });

    const data = await requestCache.wrap(`public:player:${uuid}`, 30_000, async () => {
      let row = await playerRepo.getPlayerFull(uuid);
      if (!row) {
        await playerRepo.ensurePlayer(uuid);
        row = await playerRepo.getPlayerFull(uuid);
      }
      return formatProfile(row);
    });

    if (!data) return res.status(404).json({ error: 'player_not_found' });

    const isSelf = req.player?.uuid && req.player.uuid === data.uuid;
    res.json({ profile: data, isSelf });
  } catch (err) {
    return internalError(res, err, 'public/player');
  }
});

router.get('/api/public/player/:id/profile', optionalPlayerAuth, async (req, res) => {
  try {
    const uuid = await resolvePlayerId(req.params.id);
    if (!uuid) return res.status(404).json({ error: 'player_not_found' });

    const data = await requestCache.wrap(`public:profile:${uuid}`, 45_000, async () => {
      const full = await playerProfileService.buildFullProfile(uuid);
      return full;
    });

    if (!data) return res.status(404).json({ error: 'player_not_found' });

    const isSelf = req.player?.uuid && req.player.uuid === data.profile.uuid;
    res.json({ ...data, isSelf });
  } catch (err) {
    return internalError(res, err, 'public/player/profile');
  }
});

module.exports = router;
