-- Player profile: seasons, snapshots, meta

CREATE TABLE IF NOT EXISTS seasons (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    mode        VARCHAR(32) NOT NULL DEFAULT 'global',
    starts_at   TIMESTAMPTZ NOT NULL,
    ends_at     TIMESTAMPTZ,
    is_active   BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS player_season_stats (
    season_id   INT NOT NULL REFERENCES seasons(id) ON DELETE CASCADE,
    uuid        VARCHAR(36) NOT NULL REFERENCES players(uuid) ON DELETE CASCADE,
    position    INT,
    coins       INT NOT NULL DEFAULT 0,
    kills       INT NOT NULL DEFAULT 0,
    deaths      INT NOT NULL DEFAULT 0,
    rating      INT NOT NULL DEFAULT 1000,
    bp_level    INT NOT NULL DEFAULT 0,
    points      BIGINT NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (season_id, uuid)
);

CREATE TABLE IF NOT EXISTS player_stats_snapshots (
    id              BIGSERIAL PRIMARY KEY,
    uuid            VARCHAR(36) NOT NULL REFERENCES players(uuid) ON DELETE CASCADE,
    snapshot_date   DATE NOT NULL,
    coins           INT NOT NULL DEFAULT 0,
    kills           INT NOT NULL DEFAULT 0,
    deaths          INT NOT NULL DEFAULT 0,
    rating          INT NOT NULL DEFAULT 1000,
    bp_level        INT NOT NULL DEFAULT 0,
    rank_money      INT,
    rank_kills      INT,
    rank_rating     INT,
    rank_overall    INT,
    UNIQUE (uuid, snapshot_date)
);

CREATE TABLE IF NOT EXISTS player_profile_meta (
    uuid                VARCHAR(36) PRIMARY KEY REFERENCES players(uuid) ON DELETE CASCADE,
    bp_level            INT NOT NULL DEFAULT 0,
    bp_xp               INT NOT NULL DEFAULT 0,
    achievements        JSONB NOT NULL DEFAULT '[]',
    cosmetics_unlocked  JSONB NOT NULL DEFAULT '[]',
    cosmetics_active    JSONB NOT NULL DEFAULT '{}',
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_snapshots_uuid_date ON player_stats_snapshots (uuid, snapshot_date DESC);
CREATE INDEX IF NOT EXISTS idx_season_stats_season_pts ON player_season_stats (season_id, points DESC);
CREATE INDEX IF NOT EXISTS idx_seasons_active ON seasons (is_active) WHERE is_active = true;
