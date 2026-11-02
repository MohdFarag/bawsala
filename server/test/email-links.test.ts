import { describe, expect, it } from "vitest";
import type { Mail, Mailer } from "../src/shared/mailer.js";
import { useTestApp } from "./helpers.js";

class CapturingMailer implements Mailer {
  sent: Mail[] = [];
  async send(mail: Mail) {
    this.sent.push(mail);
  }
  /** The token in the last link sent to [to]. */
  lastToken(to: string): string {
    const mail = [...this.sent].reverse().find((m) => m.to === to);
    const match = mail?.text.match(/token=([A-Za-z0-9_-]+)/);
    if (!match?.[1]) throw new Error(`No link sent to ${to}`);
    return match[1];
  }
}

const mailer = new CapturingMailer();
const ctx = useTestApp({ PUBLIC_URL: "https://bawsala.example/" }, { mailer });
let n = 0;
const email = () => `mail${Date.now()}${n++}@example.com`;
const password = "correct horse battery";

async function register(address: string, locale = "en") {
  const res = await ctx.app.inject({
    method: "POST",
    url: "/v1/auth/register",
    payload: { email: address, password, displayName: "Mohamed", locale },
  });
  expect(res.statusCode).toBe(201);
  return res.json<{ user: { displayName: string; emailVerified: boolean }; tokens: { accessToken: string } }>();
}

const login = (address: string, pass = password) =>
  ctx.app.inject({ method: "POST", url: "/v1/auth/login", payload: { email: address, password: pass } });

describe("email confirmation", () => {
  it("emails a link on sign-up that confirms the address once", async () => {
    const address = email();
    const created = await register(address, "ar");
    expect(created.user).toMatchObject({ displayName: "Mohamed", emailVerified: false });
    const mail = mailer.sent.at(-1)!;
    expect(mail.to).toBe(address);
    expect(mail.subject).toBe("خطوة أخيرة، ونتقابل.");
    expect(mail.text).toContain("https://bawsala.example/v1/auth/email/verify?token=");

    const token = mailer.lastToken(address);
    const page = await ctx.app.inject({ method: "GET", url: `/v1/auth/email/verify?token=${token}` });
    expect(page.statusCode).toBe(200);
    expect(page.headers["content-type"]).toContain("text/html");
    expect(page.body).toContain("تم تأكيد بريدك.");

    const again = await ctx.app.inject({ method: "GET", url: `/v1/auth/email/verify?token=${token}` });
    expect(again.body).toContain("استُخدم من قبل");

    const me = await ctx.app.inject({
      method: "GET",
      url: "/v1/me",
      headers: { authorization: `Bearer ${created.tokens.accessToken}` },
    });
    expect(me.json()).toMatchObject({ emailVerified: true });
  });

  it("resends only to unconfirmed accounts, and says nothing about unknown emails", async () => {
    const address = email();
    await register(address);
    const before = mailer.sent.length;
    const resend = (e: string) => ctx.app.inject({ method: "POST", url: "/v1/auth/email/resend", payload: { email: e } });
    expect((await resend(address)).statusCode).toBe(204);
    expect(mailer.sent.length).toBe(before + 1);
    expect((await resend("nobody@example.com")).statusCode).toBe(204);
    expect(mailer.sent.length).toBe(before + 1);
  });

  it("rejects a made-up token", async () => {
    const page = await ctx.app.inject({ method: "GET", url: `/v1/auth/email/verify?token=${"x".repeat(43)}` });
    expect(page.body).toContain("غير صالح");
  });
});

describe("password reset", () => {
  it("sets a new password, signs every device out, and works once", async () => {
    const address = email();
    const created = await register(address);
    const forgot = await ctx.app.inject({ method: "POST", url: "/v1/auth/password/forgot", payload: { email: address } });
    expect(forgot.statusCode).toBe(204);
    const token = mailer.lastToken(address);
    expect(mailer.sent.at(-1)!.text).toContain("/v1/auth/password/reset?token=");

    const form = await ctx.app.inject({ method: "GET", url: `/v1/auth/password/reset?token=${token}` });
    expect(form.body).toContain('name="password"');

    const reset = await ctx.app.inject({
      method: "POST",
      url: "/v1/auth/password/reset",
      payload: { token, password: "a brand new password" },
    });
    expect(reset.statusCode).toBe(204);
    expect((await login(address)).statusCode).toBe(401);
    expect((await login(address, "a brand new password")).statusCode).toBe(200);

    const oldSession = await ctx.app.inject({
      method: "GET",
      url: "/v1/me",
      headers: { authorization: `Bearer ${created.tokens.accessToken}` },
    });
    expect(oldSession.statusCode).toBe(401);

    const reuse = await ctx.app.inject({
      method: "POST",
      url: "/v1/auth/password/reset",
      payload: { token, password: "another password!" },
    });
    expect(reuse.statusCode).toBe(400);
    expect(reuse.json()).toMatchObject({ error: { code: "link_used" } });
  });

  it("works from the emailed page's form, and keeps the form on a short password", async () => {
    const address = email();
    await register(address);
    await ctx.app.inject({ method: "POST", url: "/v1/auth/password/forgot", payload: { email: address } });
    const token = mailer.lastToken(address);
    const post = (pass: string) =>
      ctx.app.inject({
        method: "POST",
        url: "/v1/auth/password/reset/form",
        headers: { "content-type": "application/x-www-form-urlencoded" },
        payload: new URLSearchParams({ token, password: pass }).toString(),
      });
    const short = await post("short");
    expect(short.statusCode).toBe(400);
    expect(short.body).toContain("10 characters");
    const ok = await post("from the web form");
    expect(ok.statusCode).toBe(200);
    expect(ok.body).toContain("Your new password is set.");
    expect((await login(address, "from the web form")).statusCode).toBe(200);
  });

  it("sends nothing for unknown emails and only the newest link works", async () => {
    const before = mailer.sent.length;
    await ctx.app.inject({ method: "POST", url: "/v1/auth/password/forgot", payload: { email: "ghost@example.com" } });
    expect(mailer.sent.length).toBe(before);

    const address = email();
    await register(address);
    await ctx.app.inject({ method: "POST", url: "/v1/auth/password/forgot", payload: { email: address } });
    const first = mailer.lastToken(address);
    await ctx.app.inject({ method: "POST", url: "/v1/auth/password/forgot", payload: { email: address } });
    const res = await ctx.app.inject({
      method: "POST",
      url: "/v1/auth/password/reset",
      payload: { token: first, password: "replaced password" },
    });
    expect(res.json()).toMatchObject({ error: { code: "link_invalid" } });
  });
});
