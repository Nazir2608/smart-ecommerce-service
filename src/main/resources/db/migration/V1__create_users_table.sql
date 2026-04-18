-- V1__create_users_table.sql
-- Phase 1: Core user account table

CREATE TABLE IF NOT EXISTS users (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255),
    full_name       VARCHAR(255) NOT NULL,
    avatar_url      TEXT,
    phone_number    VARCHAR(20),
    role            VARCHAR(20)  NOT NULL DEFAULT 'BUYER',
    oauth_provider  VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    oauth_subject   VARCHAR(255),
    email_verified  BOOLEAN     NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('BUYER','SELLER','ADMIN')),
    CONSTRAINT chk_users_oauth_provider CHECK (oauth_provider IN ('LOCAL','GOOGLE','GITHUB'))
);

CREATE INDEX idx_users_email          ON users (email);
CREATE INDEX idx_users_role           ON users (role);
CREATE INDEX idx_users_is_active      ON users (is_active);
CREATE INDEX idx_users_oauth_subject  ON users (oauth_subject) WHERE oauth_subject IS NOT NULL;

-- Auto-update updated_at on every row change
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE  users                IS 'Platform user accounts — buyers, sellers and admins';
COMMENT ON COLUMN users.password_hash  IS 'BCrypt hash, NULL for OAuth-only accounts';
COMMENT ON COLUMN users.oauth_provider IS 'LOCAL = email/password; GOOGLE/GITHUB = social login';
COMMENT ON COLUMN users.oauth_subject  IS 'Provider-unique subject ID (sub claim)';
