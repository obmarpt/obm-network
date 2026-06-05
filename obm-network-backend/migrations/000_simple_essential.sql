-- Schema essencial (simples) — PostgreSQL Render

CREATE TABLE IF NOT EXISTS players (
    uuid          VARCHAR(36) PRIMARY KEY,
    username      VARCHAR(32) NOT NULL DEFAULT 'Unknown',
    coins         INTEGER NOT NULL DEFAULT 0,
    emeralds      INTEGER NOT NULL DEFAULT 0,
    smp_level     INTEGER NOT NULL DEFAULT 1,
    hc_level      INTEGER NOT NULL DEFAULT 1,
    global_level  INTEGER NOT NULL DEFAULT 1,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS smp_stats (
    uuid      VARCHAR(36) PRIMARY KEY REFERENCES players(uuid) ON DELETE CASCADE,
    kills     INTEGER NOT NULL DEFAULT 0,
    deaths    INTEGER NOT NULL DEFAULT 0,
    playtime  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS hc_stats (
    uuid           VARCHAR(36) PRIMARY KEY REFERENCES players(uuid) ON DELETE CASCADE,
    hc_kills       INTEGER NOT NULL DEFAULT 0,
    hc_deaths      INTEGER NOT NULL DEFAULT 0,
    hc_playtime    INTEGER NOT NULL DEFAULT 0,
    hc_totems_used INTEGER NOT NULL DEFAULT 0,
    hc_level       INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS tierspace_stats (
    uuid       VARCHAR(36) PRIMARY KEY REFERENCES players(uuid) ON DELETE CASCADE,
    rating     INTEGER NOT NULL DEFAULT 1000,
    wins       INTEGER NOT NULL DEFAULT 0,
    losses     INTEGER NOT NULL DEFAULT 0,
    winstreak  INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_players_coins_desc ON players (coins DESC);
CREATE INDEX IF NOT EXISTS idx_smp_kills_desc ON smp_stats (kills DESC);
CREATE INDEX IF NOT EXISTS idx_hc_kills_desc ON hc_stats (hc_kills DESC);
CREATE INDEX IF NOT EXISTS idx_tierspace_rating_desc ON tierspace_stats (rating DESC);
