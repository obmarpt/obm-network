CREATE TABLE IF NOT EXISTS security_logs (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(64),
    username VARCHAR(64) NOT NULL DEFAULT 'Unknown',
    event_type VARCHAR(64) NOT NULL,
    detail TEXT,
    world VARCHAR(64),
    ip_address VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_security_logs_created ON security_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_security_logs_uuid ON security_logs (uuid);
CREATE INDEX IF NOT EXISTS idx_security_logs_event ON security_logs (event_type);
