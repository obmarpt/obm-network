-- OBM Network — schema normalizado (PostgreSQL / Render)

CREATE TABLE IF NOT EXISTS players (
    uuid          VARCHAR(36) PRIMARY KEY,
    username      VARCHAR(32) NOT NULL DEFAULT 'Unknown',
    coins         INTEGER NOT NULL DEFAULT 0,
    emeralds      INTEGER NOT NULL DEFAULT 0,
    smp_level     INTEGER NOT NULL DEFAULT 1,
    hc_level      INTEGER NOT NULL DEFAULT 1,
    global_level  INTEGER NOT NULL DEFAULT 1,
    rank          VARCHAR(32) NOT NULL DEFAULT 'bronze',
    world         VARCHAR(64),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS smp_stats (
    uuid          VARCHAR(36) PRIMARY KEY REFERENCES players(uuid) ON DELETE CASCADE,
    kills         INTEGER NOT NULL DEFAULT 0,
    deaths        INTEGER NOT NULL DEFAULT 0,
    playtime      INTEGER NOT NULL DEFAULT 0,
    money_earned  BIGINT NOT NULL DEFAULT 0,
    blocks_broken INTEGER NOT NULL DEFAULT 0,
    last_seen     TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS hc_stats (
    uuid              VARCHAR(36) PRIMARY KEY REFERENCES players(uuid) ON DELETE CASCADE,
    hc_kills          INTEGER NOT NULL DEFAULT 0,
    hc_deaths         INTEGER NOT NULL DEFAULT 0,
    hc_playtime       INTEGER NOT NULL DEFAULT 0,
    hc_totems_used    INTEGER NOT NULL DEFAULT 0,
    hc_longest_life   INTEGER NOT NULL DEFAULT 0,
    hc_wins           INTEGER NOT NULL DEFAULT 0,
    hc_losses         INTEGER NOT NULL DEFAULT 0,
    hc_mobs           INTEGER NOT NULL DEFAULT 0,
    hc_lives          INTEGER NOT NULL DEFAULT 1,
    hc_time_alive     INTEGER NOT NULL DEFAULT 0,
    hc_level          INTEGER NOT NULL DEFAULT 1,
    hc_xp             INTEGER NOT NULL DEFAULT 0,
    last_seen         TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS tierspace_stats (
    uuid              VARCHAR(36) PRIMARY KEY REFERENCES players(uuid) ON DELETE CASCADE,
    rating            INTEGER NOT NULL DEFAULT 1000,
    wins              INTEGER NOT NULL DEFAULT 0,
    losses            INTEGER NOT NULL DEFAULT 0,
    winstreak         INTEGER NOT NULL DEFAULT 0,
    best_winstreak    INTEGER NOT NULL DEFAULT 0,
    last_seen         TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS player_kits (
    id          BIGSERIAL PRIMARY KEY,
    uuid        VARCHAR(36) NOT NULL REFERENCES players(uuid) ON DELETE CASCADE,
    kit_name    VARCHAR(64) NOT NULL,
    kit_data    JSONB NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (uuid, kit_name)
);

CREATE TABLE IF NOT EXISTS parties (
    party_id    BIGSERIAL PRIMARY KEY,
    owner_uuid  VARCHAR(36) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS party_members (
    party_id    BIGINT NOT NULL REFERENCES parties(party_id) ON DELETE CASCADE,
    uuid        VARCHAR(36) NOT NULL,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (party_id, uuid)
);

CREATE TABLE IF NOT EXISTS matches (
    id              BIGSERIAL PRIMARY KEY,
    player1_uuid    VARCHAR(36) NOT NULL,
    player2_uuid    VARCHAR(36) NOT NULL,
    winner_uuid     VARCHAR(36),
    mode            VARCHAR(32) NOT NULL DEFAULT 'ranked',
    played_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_players_coins ON players (coins DESC);
CREATE INDEX IF NOT EXISTS idx_players_emeralds ON players (emeralds DESC);
CREATE INDEX IF NOT EXISTS idx_smp_kills ON smp_stats (kills DESC);
CREATE INDEX IF NOT EXISTS idx_smp_playtime ON smp_stats (playtime DESC);
CREATE INDEX IF NOT EXISTS idx_hc_kills ON hc_stats (hc_kills DESC);
CREATE INDEX IF NOT EXISTS idx_hc_wins ON hc_stats (hc_wins DESC);
CREATE INDEX IF NOT EXISTS idx_tierspace_rating ON tierspace_stats (rating DESC);

CREATE TABLE IF NOT EXISTS audit_logs (
    id         BIGSERIAL PRIMARY KEY,
    staff      VARCHAR(32) NOT NULL,
    action     VARCHAR(64) NOT NULL,
    target     VARCHAR(64) NOT NULL DEFAULT '',
    value      VARCHAR(256) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS commands (
    id         BIGSERIAL PRIMARY KEY,
    command    TEXT NOT NULL,
    executed   BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_commands_pending ON commands (executed) WHERE executed = false;
