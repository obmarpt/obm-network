CREATE INDEX IF NOT EXISTS idx_server_events_actor_uuid ON server_events (actor_uuid, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_server_events_target_uuid ON server_events (target_uuid, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_players_updated_at ON players (updated_at DESC NULLS LAST);
