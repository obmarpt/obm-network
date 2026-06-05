-- MineSpace / OBM Network — PostgreSQL schema

CREATE TABLE IF NOT EXISTS players (
    uuid        VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(32) NOT NULL DEFAULT 'Unknown',
    coins       INTEGER NOT NULL DEFAULT 0,
    emeralds    INTEGER NOT NULL DEFAULT 0,
    rank        VARCHAR(32) NOT NULL DEFAULT 'bronze',
    kills       INTEGER NOT NULL DEFAULT 0,
    playtime    INTEGER NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_players_coins ON players (coins DESC);
CREATE INDEX IF NOT EXISTS idx_players_emeralds ON players (emeralds DESC);
CREATE INDEX IF NOT EXISTS idx_players_kills ON players (kills DESC);
CREATE INDEX IF NOT EXISTS idx_players_playtime ON players (playtime DESC);

CREATE TABLE IF NOT EXISTS audit_logs (
    id         BIGSERIAL PRIMARY KEY,
    staff      VARCHAR(32) NOT NULL,
    action     VARCHAR(64) NOT NULL,
    target     VARCHAR(64) NOT NULL DEFAULT '',
    value      VARCHAR(256) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON audit_logs (created_at DESC);
