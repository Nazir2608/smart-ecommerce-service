-- V3__create_refresh_tokens_table.sql
-- Phase 1: JWT refresh tokens with session metadata

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    token       VARCHAR(36) NOT NULL,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ NOT NULL,
    is_revoked  BOOLEAN     NOT NULL DEFAULT FALSE,
    user_agent  VARCHAR(512),
    ip_address  VARCHAR(50),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_refresh_tokens_token UNIQUE (token)
);

CREATE INDEX idx_refresh_tokens_token      ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at)
    WHERE is_revoked = FALSE;

COMMENT ON TABLE  refresh_tokens             IS 'Opaque refresh tokens — one per login session';
COMMENT ON COLUMN refresh_tokens.token       IS 'UUID v4 token value sent to the client';
COMMENT ON COLUMN refresh_tokens.is_revoked  IS 'Set to TRUE on logout or password change';
COMMENT ON COLUMN refresh_tokens.user_agent  IS 'Browser/client identifier for session listing';
