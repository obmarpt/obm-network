CREATE TABLE IF NOT EXISTS bans (
    id          SERIAL PRIMARY KEY,
    uuid        VARCHAR(36) NOT NULL,
    username    VARCHAR(32),
    reason      TEXT NOT NULL DEFAULT 'Sem motivo',
    banned_by   VARCHAR(64) NOT NULL DEFAULT 'system',
    active      BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ,
    unbanned_at TIMESTAMPTZ,
    unbanned_by VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_bans_uuid_active ON bans (uuid, active) WHERE active = true;
CREATE INDEX IF NOT EXISTS idx_bans_created ON bans (created_at DESC);

CREATE TABLE IF NOT EXISTS reports (
    id            SERIAL PRIMARY KEY,
    reporter_uuid VARCHAR(36) NOT NULL,
    reporter_name VARCHAR(32),
    target_uuid   VARCHAR(36) NOT NULL,
    target_name   VARCHAR(32),
    reason        TEXT NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'open',
    closed_by     VARCHAR(64),
    closed_at     TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reports_status ON reports (status, created_at DESC);

CREATE TABLE IF NOT EXISTS stats_daily (
    day           DATE PRIMARY KEY,
    player_count  INTEGER NOT NULL DEFAULT 0,
    total_coins   BIGINT NOT NULL DEFAULT 0,
    total_kills   BIGINT NOT NULL DEFAULT 0,
    joins_count   INTEGER NOT NULL DEFAULT 0,
    kills_count   INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS online_samples (
    id         BIGSERIAL PRIMARY KEY,
    count      INTEGER NOT NULL DEFAULT 0,
    sampled_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_online_samples_at ON online_samples (sampled_at DESC);
