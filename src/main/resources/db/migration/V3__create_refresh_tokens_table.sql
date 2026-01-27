-- Refresh token cookie flow (rotation + reuse detection)
CREATE TABLE refresh_tokens
(
    id                    UUID PRIMARY KEY,
    user_id               BIGINT      NOT NULL,
    -- Store as raw bytes (SHA-256 output). Unique for fast lookup.
    token_hash            BYTEA       NOT NULL UNIQUE,
    family_id             UUID        NOT NULL,
    device_id             UUID        NOT NULL,
    expires_at            TIMESTAMPTZ NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at            TIMESTAMPTZ NULL,
    revoke_reason         VARCHAR(32) NULL,
    replaced_by_token_id  UUID        NULL,
    created_by_ip         INET        NULL,
    created_by_user_agent TEXT        NULL,

    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_replaced_by FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens (id) ON DELETE SET NULL,
    CONSTRAINT check_revoke_reason_when_revoked CHECK (
        (revoked_at IS NULL AND revoke_reason IS NULL) OR
        (revoked_at IS NOT NULL AND revoke_reason IS NOT NULL))
);

-- Lookup and cleanup / revocation queries
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_device_id ON refresh_tokens (device_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
CREATE INDEX idx_refresh_tokens_revoked_at ON refresh_tokens (revoked_at);
CREATE INDEX idx_refresh_tokens_user_device ON refresh_tokens (user_id, device_id);
