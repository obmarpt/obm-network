CREATE TABLE IF NOT EXISTS grim_violations (
    id          BIGSERIAL PRIMARY KEY,
    uuid        VARCHAR(36) NOT NULL,
    username    VARCHAR(32),
    check_name  VARCHAR(96) NOT NULL,
    cheat_type  VARCHAR(64) NOT NULL DEFAULT 'unknown',
    severity    VARCHAR(16) NOT NULL DEFAULT 'medium',
    vl          DOUBLE PRECISION NOT NULL DEFAULT 0,
    verbose     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_grim_violations_uuid ON grim_violations (uuid, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_grim_violations_created ON grim_violations (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_grim_violations_severity ON grim_violations (severity, created_at DESC);

CREATE TABLE IF NOT EXISTS anticheat_alerts (
    id              BIGSERIAL PRIMARY KEY,
    uuid            VARCHAR(36) NOT NULL,
    username        VARCHAR(32),
    flag_count      INTEGER NOT NULL DEFAULT 0,
    window_seconds  INTEGER NOT NULL DEFAULT 10,
    reason          TEXT,
    acknowledged    BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_anticheat_alerts_open ON anticheat_alerts (acknowledged, created_at DESC);

CREATE TABLE IF NOT EXISTS anticheat_suspects (
    uuid          VARCHAR(36) PRIMARY KEY,
    username      VARCHAR(32),
    active        BOOLEAN NOT NULL DEFAULT true,
    total_flags   INTEGER NOT NULL DEFAULT 0,
    last_check    VARCHAR(64),
    last_severity VARCHAR(16),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
