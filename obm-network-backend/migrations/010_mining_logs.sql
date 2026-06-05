CREATE TABLE IF NOT EXISTS mining_logs (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(64) NOT NULL,
    username VARCHAR(32),
    block_type VARCHAR(32) NOT NULL,
    world VARCHAR(64),
    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    z INTEGER NOT NULL,
    blocks_before INTEGER DEFAULT -1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mining_logs_uuid ON mining_logs (uuid, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_mining_logs_created ON mining_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_mining_logs_block ON mining_logs (block_type);
