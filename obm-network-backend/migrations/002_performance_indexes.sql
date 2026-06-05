CREATE INDEX IF NOT EXISTS idx_players_uuid ON players (uuid);
CREATE INDEX IF NOT EXISTS idx_players_coins_desc ON players (coins DESC);
CREATE INDEX IF NOT EXISTS idx_players_emeralds_desc ON players (emeralds DESC);
CREATE INDEX IF NOT EXISTS idx_smp_stats_kills_desc ON smp_stats (kills DESC);
CREATE INDEX IF NOT EXISTS idx_smp_stats_playtime_desc ON smp_stats (playtime DESC);
CREATE INDEX IF NOT EXISTS idx_hc_stats_kills_desc ON hc_stats (hc_kills DESC);
CREATE INDEX IF NOT EXISTS idx_hc_stats_wins_desc ON hc_stats (hc_wins DESC);
CREATE INDEX IF NOT EXISTS idx_tierspace_rating_desc ON tierspace_stats (rating DESC);
CREATE INDEX IF NOT EXISTS idx_matches_played_at ON matches (played_at DESC);
