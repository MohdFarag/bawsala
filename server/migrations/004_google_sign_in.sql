-- Sign in with Google: accounts created that way have no password, and every account can be
-- linked to one Google identity (the ID token's stable "sub"), independent of a Tasks/Calendar
-- connection, which can be disconnected without losing the way to sign in.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD COLUMN google_sub text;
CREATE UNIQUE INDEX users_google_sub_key ON users (google_sub) WHERE google_sub IS NOT NULL;
