CREATE TABLE IF NOT EXISTS server_events (
    id          BIGSERIAL PRIMARY KEY,
    event_type  VARCHAR(32) NOT NULL,
    actor_uuid  VARCHAR(36),
    actor_name  VARCHAR(32),
    target_uuid VARCHAR(36),
    target_name VARCHAR(32),
    detail      TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_server_events_type_created
    ON server_events (event_type, created_at DESC);

ALTER TABLE commands ADD COLUMN IF NOT EXISTS source VARCHAR(32) DEFAULT 'system';
ALTER TABLE commands ADD COLUMN IF NOT EXISTS result TEXT;
ALTER TABLE commands ADD COLUMN IF NOT EXISTS executed_at TIMESTAMPTZ;
