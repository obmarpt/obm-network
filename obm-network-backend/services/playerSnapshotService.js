const profileRepo = require('../db/playerProfileRepository');
const requestCache = require('./requestCache');

async function onPlayerSaved(uuid, row, profileMeta) {
  if (!uuid || !row) return;

  const stats = {
    coins: row.coins,
    kills: row.smp_kills,
    deaths: row.smp_deaths,
    rating: row.rating,
    bp_level: profileMeta?.bp_level ?? 0,
  };

  try {
    const season = await profileRepo.ensureDefaultSeason();
    await profileRepo.upsertDailySnapshot(uuid, stats);
    if (season?.id) {
      await profileRepo.upsertPlayerSeasonStats(uuid, season.id, stats);
    }
    if (profileMeta) {
      await profileRepo.upsertProfileMeta(uuid, profileMeta);
    }
    requestCache.invalidate(`public:profile:${uuid}`);
    requestCache.invalidate(`public:player:${uuid}`);
  } catch (err) {
    console.warn('[Profile] snapshot failed:', err.message);
  }
}

module.exports = { onPlayerSaved };
