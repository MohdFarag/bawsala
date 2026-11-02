import { one, type Queryable } from "../../shared/db.js";

export interface UserRow {
  id: string;
  email: string;
  /** Null for accounts created with Google that never set a password. */
  passwordHash: string | null;
  googleSub: string | null;
  displayName: string | null;
  locale: string;
  /** When the email address was confirmed; null until the link is opened. */
  emailVerifiedAt: Date | null;
  createdAt: Date;
}

const USER_COLUMNS = `id, email, password_hash AS "passwordHash", google_sub AS "googleSub", display_name AS "displayName", locale,
  email_verified_at AS "emailVerifiedAt", created_at AS "createdAt"`;

/**
 * SQL for user rows. The user is the root every other table hangs off, so this is the one
 * repository other modules may use (for example to lock a user's row for a per-user check).
 */
export const usersRepository = {
  async findByEmail(q: Queryable, email: string): Promise<UserRow | undefined> {
    const { rows } = await q.query<UserRow>(`SELECT ${USER_COLUMNS} FROM users WHERE lower(email) = lower($1)`, [email]);
    return rows[0];
  },

  async findById(q: Queryable, id: string): Promise<UserRow | undefined> {
    const { rows } = await q.query<UserRow>(`SELECT ${USER_COLUMNS} FROM users WHERE id = $1`, [id]);
    return rows[0];
  },

  async findByGoogleSub(q: Queryable, googleSub: string): Promise<UserRow | undefined> {
    const { rows } = await q.query<UserRow>(`SELECT ${USER_COLUMNS} FROM users WHERE google_sub = $1`, [googleSub]);
    return rows[0];
  },

  async insert(
    q: Queryable,
    user: { email: string; passwordHash: string | null; googleSub?: string; displayName: string | null; locale: string },
  ): Promise<UserRow> {
    const { rows } = await q.query<UserRow>(
      `INSERT INTO users (email, password_hash, google_sub, display_name, locale) VALUES ($1, $2, $3, $4, $5) RETURNING ${USER_COLUMNS}`,
      [user.email, user.passwordHash, user.googleSub ?? null, user.displayName, user.locale],
    );
    return one(rows);
  },

  async markEmailVerified(q: Queryable, id: string): Promise<void> {
    await q.query("UPDATE users SET email_verified_at = coalesce(email_verified_at, now()) WHERE id = $1", [id]);
  },

  async setPasswordHash(q: Queryable, id: string, passwordHash: string): Promise<void> {
    await q.query("UPDATE users SET password_hash = $2 WHERE id = $1", [id, passwordHash]);
  },

  async setGoogleSub(q: Queryable, id: string, googleSub: string): Promise<void> {
    await q.query("UPDATE users SET google_sub = $2 WHERE id = $1", [id, googleSub]);
  },

  /** Row-locks the user for the rest of the transaction, serializing per-user checks (e.g. quota). */
  async lock(q: Queryable, id: string): Promise<void> {
    await q.query("SELECT 1 FROM users WHERE id = $1 FOR UPDATE", [id]);
  },

  async delete(q: Queryable, id: string): Promise<void> {
    await q.query("DELETE FROM users WHERE id = $1", [id]);
  },
};
