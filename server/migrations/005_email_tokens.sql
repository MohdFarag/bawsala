-- Email confirmation and password reset (V3 AM03-AM05). Accounts that existed before count as
-- confirmed; Google confirms the email of accounts made with it.
ALTER TABLE users ADD COLUMN email_verified_at timestamptz;
UPDATE users SET email_verified_at = created_at;

-- One-time links sent by email. Only a SHA-256 digest of the token is stored.
CREATE TABLE email_tokens (
    token_hash  bytea PRIMARY KEY,
    user_id     uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose     text NOT NULL CHECK (purpose IN ('verify_email', 'reset_password')),
    expires_at  timestamptz NOT NULL,
    used_at     timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX email_tokens_user ON email_tokens (user_id, purpose);
