-- Leaderboard metrics: índices para tops frequentes
CREATE INDEX IF NOT EXISTS idx_hc_stats_totems ON hc_stats (hc_totems_used DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_tierspace_stats_wins ON tierspace_stats (wins DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_tierspace_stats_winstreak ON tierspace_stats (winstreak DESC NULLS LAST);
