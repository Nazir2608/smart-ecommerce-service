-- V2__create_addresses_table.sql
-- Phase 1: User delivery addresses

CREATE TABLE IF NOT EXISTS addresses (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label        VARCHAR(20) NOT NULL DEFAULT 'HOME',
    full_name    VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    line1        TEXT        NOT NULL,
    line2        TEXT,
    city         VARCHAR(100) NOT NULL,
    state        VARCHAR(100) NOT NULL,
    postal_code  VARCHAR(20) NOT NULL,
    country      VARCHAR(100) NOT NULL DEFAULT 'IN',
    is_default   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_addresses_label CHECK (label IN ('HOME','WORK','OTHER'))
);

CREATE INDEX idx_addresses_user_id ON addresses (user_id);
CREATE INDEX idx_addresses_default ON addresses (user_id, is_default) WHERE is_default = TRUE;

-- Partial unique index: only one default address per user
CREATE UNIQUE INDEX uq_addresses_one_default_per_user
    ON addresses (user_id)
    WHERE is_default = TRUE;

CREATE TRIGGER trg_addresses_updated_at
    BEFORE UPDATE ON addresses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE  addresses            IS 'Saved delivery addresses per user (max 10)';
COMMENT ON COLUMN addresses.is_default IS 'Only one address per user may be the default';
