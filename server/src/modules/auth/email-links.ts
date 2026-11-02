import type { Config } from "../../shared/config.js";
import { type Db, type Queryable, withTx } from "../../shared/db.js";
import type { Mailer } from "../../shared/mailer.js";
import { digest, newToken } from "../../shared/tokens.js";
import { type UserRow, usersRepository as users } from "../users/users.repository.js";
import { hashPassword } from "./passwords.js";

export type LinkPurpose = "verify_email" | "reset_password";

/** How long each kind of link works. A reset link is short-lived because it grants account access. */
const LIFETIME_MINUTES: Record<LinkPurpose, number> = { verify_email: 3 * 24 * 60, reset_password: 60 };

/** Why a link did not work, shown on the page it opens. */
export type LinkProblem = "invalid" | "expired" | "used";

/**
 * One-time links sent by email: confirming the address (AM05) and resetting a forgotten password
 * (AM03/AM04). Requests never reveal whether an email has an account.
 */
export class EmailLinks {
  constructor(
    private readonly db: Db,
    private readonly config: Config,
    private readonly mailer: Mailer,
  ) {}

  /** Sends the confirmation link to a new account. */
  async sendVerification(user: Pick<UserRow, "id" | "email" | "locale">): Promise<void> {
    const token = await this.issue(this.db, user.id, "verify_email");
    const url = `${this.base()}/v1/auth/email/verify?token=${token}`;
    await this.mailer.send(verifyMail(user.email, user.locale, url));
  }

  /** Sends a new confirmation link, if the email belongs to an unconfirmed account. */
  async resendVerification(email: string): Promise<void> {
    const user = await users.findByEmail(this.db, email);
    if (user && !user.emailVerifiedAt) await this.sendVerification(user);
  }

  /** Sends a reset link, if the email belongs to an account. */
  async requestPasswordReset(email: string): Promise<void> {
    const user = await users.findByEmail(this.db, email);
    if (!user) return;
    const token = await this.issue(this.db, user.id, "reset_password");
    const url = `${this.base()}/v1/auth/password/reset?token=${token}`;
    await this.mailer.send(resetMail(user.email, user.locale, url));
  }

  /** Confirms the email of the link's account. */
  async verifyEmail(token: string): Promise<LinkProblem | { locale: string }> {
    return withTx(this.db, async (tx) => {
      const found = await this.consume(tx, token, "verify_email");
      if (typeof found === "string") return found;
      await users.markEmailVerified(tx, found.id);
      return { locale: found.locale };
    });
  }

  /**
   * Sets a new password from a reset link and signs every device out, since whoever had the old
   * password may still hold a session. Opening the link also proves the email address.
   */
  async resetPassword(token: string, password: string): Promise<LinkProblem | { locale: string }> {
    const passwordHash = await hashPassword(password);
    return withTx(this.db, async (tx) => {
      const found = await this.consume(tx, token, "reset_password");
      if (typeof found === "string") return found;
      await users.setPasswordHash(tx, found.id, passwordHash);
      await users.markEmailVerified(tx, found.id);
      await tx.query("UPDATE sessions SET revoked_at = now() WHERE user_id = $1 AND revoked_at IS NULL", [found.id]);
      return { locale: found.locale };
    });
  }

  /** Whether a reset link can still be used, before showing its form. */
  async checkReset(token: string): Promise<LinkProblem | { locale: string }> {
    const { rows } = await this.db.query<{ expired: boolean; used: boolean; locale: string }>(
      `SELECT t.expires_at < now() AS expired, t.used_at IS NOT NULL AS used, u.locale
       FROM email_tokens t JOIN users u ON u.id = t.user_id WHERE t.token_hash = $1 AND t.purpose = 'reset_password'`,
      [digest(token)],
    );
    const row = rows[0];
    if (!row) return "invalid";
    if (row.used) return "used";
    if (row.expired) return "expired";
    return { locale: row.locale };
  }

  private async issue(q: Queryable, userId: string, purpose: LinkPurpose): Promise<string> {
    const token = newToken();
    // A new link replaces older unused ones of the same kind.
    await q.query("DELETE FROM email_tokens WHERE user_id = $1 AND purpose = $2 AND used_at IS NULL", [userId, purpose]);
    await q.query(
      `INSERT INTO email_tokens (token_hash, user_id, purpose, expires_at) VALUES ($1, $2, $3, now() + make_interval(mins => $4))`,
      [digest(token), userId, purpose, LIFETIME_MINUTES[purpose]],
    );
    return token;
  }

  private async consume(q: Queryable, token: string, purpose: LinkPurpose): Promise<LinkProblem | UserRow> {
    const { rows } = await q.query<{ userId: string; expired: boolean; used: boolean }>(
      `SELECT user_id AS "userId", expires_at < now() AS expired, used_at IS NOT NULL AS used
       FROM email_tokens WHERE token_hash = $1 AND purpose = $2 FOR UPDATE`,
      [digest(token), purpose],
    );
    const row = rows[0];
    if (!row) return "invalid";
    if (row.used) return "used";
    if (row.expired) return "expired";
    await q.query("UPDATE email_tokens SET used_at = now() WHERE token_hash = $1", [digest(token)]);
    const user = await users.findById(q, row.userId);
    return user ?? "invalid";
  }

  private base(): string {
    return this.config.PUBLIC_URL.replace(/\/+$/, "");
  }

  /** Housekeeping: removes links that can no longer be used. */
  async deleteExpired(): Promise<number> {
    const { rowCount } = await this.db.query("DELETE FROM email_tokens WHERE expires_at < now() - interval '1 day'");
    return rowCount ?? 0;
  }
}

const esc = (s: string) => s.replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);

function mail(to: string, locale: string, subject: string, lines: string[], action: string, url: string) {
  const ar = locale === "ar";
  const html = `<div dir="${ar ? "rtl" : "ltr"}" style="font-family:sans-serif;color:#213B35;line-height:1.8;max-width:480px">
<h2 style="color:#05473F">${esc(subject)}</h2>${lines.map((l) => `<p>${esc(l)}</p>`).join("")}
<p><a href="${esc(url)}" style="display:inline-block;background:#05473F;color:#fff;padding:12px 22px;border-radius:15px;text-decoration:none">${esc(action)}</a></p>
<p style="color:#68776E;font-size:13px">${esc(url)}</p></div>`;
  return { to, subject, html, text: `${lines.join("\n")}\n\n${action}: ${url}` };
}

function verifyMail(to: string, locale: string, url: string) {
  return locale === "ar"
    ? mail(
        to,
        locale,
        "خطوة أخيرة، ونتقابل.",
        ["افتح الرابط لتأكيد بريدك وتفعيل حساب بوصلة.", "الرابط يعمل لمدة ٣ أيام."],
        "تأكيد البريد",
        url,
      )
    : mail(
        to,
        locale,
        "One last step.",
        ["Open the link to confirm your email for Bawsala.", "The link works for 3 days."],
        "Confirm email",
        url,
      );
}

function resetMail(to: string, locale: string, url: string) {
  return locale === "ar"
    ? mail(
        to,
        locale,
        "نرجّع لك الوصول.",
        ["طلبت إعادة تعيين كلمة المرور. الرابط يعمل لمدة ساعة.", "لو ما طلبتها، تجاهل الرسالة؛ حسابك بأمان."],
        "تعيين كلمة مرور جديدة",
        url,
      )
    : mail(
        to,
        locale,
        "Let's get you back in.",
        ["You asked to reset your password. The link works for one hour.", "If you didn't ask, ignore this email; your account is safe."],
        "Set a new password",
        url,
      );
}
