import { randomUUID } from "node:crypto";
import { describe, expect, it } from "vitest";
import { GoogleOAuth } from "../src/modules/google/google.oauth.js";
import { FakeGoogle } from "./fake-google.js";
import { signUp, useTestApp } from "./helpers.js";

/** Who each server auth code belongs to, as Google's token endpoint would answer. */
const identities = new Map<string, { sub: string; email: string; email_verified: boolean; name?: string; offline?: boolean }>();
const tokenRequests: URLSearchParams[] = [];

const fakeFetch: typeof fetch = async (_input, init) => {
  const body = new URLSearchParams(typeof init?.body === "string" ? init.body : "");
  tokenRequests.push(body);
  const who = identities.get(body.get("code") ?? "");
  if (!who) return Response.json({ error: "invalid_grant" }, { status: 400 });
  const { offline = true, ...claims } = who;
  const idToken = ["e30", Buffer.from(JSON.stringify(claims)).toString("base64url"), "sig"].join(".");
  return Response.json({
    access_token: `access-${randomUUID()}`,
    expires_in: 3600,
    scope: "openid email https://www.googleapis.com/auth/tasks https://www.googleapis.com/auth/calendar",
    id_token: idToken,
    ...(offline ? { refresh_token: `refresh-${randomUUID()}` } : {}),
  });
};

const settings = {
  clientId: "web-client.apps.googleusercontent.com",
  clientSecret: "secret",
  redirectUri: "https://api.test/v1/google/callback",
};
const ctx = useTestApp(
  {
    GOOGLE_CLIENT_ID: settings.clientId,
    GOOGLE_CLIENT_SECRET: settings.clientSecret,
    GOOGLE_REDIRECT_URI: settings.redirectUri,
    SECRETS_KEY: Buffer.alloc(32, 9).toString("base64"),
  },
  { google: { oauth: new GoogleOAuth(settings, fakeFetch), api: () => new FakeGoogle() } },
);
const off = useTestApp();

let counter = 0;
function code(identity: { sub?: string; email?: string; email_verified?: boolean; name?: string; offline?: boolean } = {}) {
  const n = ++counter;
  const c = `code-${n}`;
  identities.set(c, { sub: `g-${n}`, email: `google${n}@example.com`, email_verified: true, ...identity });
  return c;
}

async function signInWithGoogle(app = ctx.app, c: string, extra: object = {}) {
  return app.inject({ method: "POST", url: "/v1/auth/google", payload: { code: c, deviceName: "Pixel", ...extra } });
}

async function googleStatus(accessToken: string) {
  const res = await ctx.app.inject({ method: "GET", url: "/v1/google/status", headers: { authorization: `Bearer ${accessToken}` } });
  return res.json<{ connected: boolean; email: string | null }>();
}

describe("Sign in with Google", () => {
  it("tells the app which web client to request a code for", async () => {
    const on = await ctx.app.inject({ method: "GET", url: "/v1/auth/google/config" });
    expect(on.json()).toEqual({ enabled: true, webClientId: settings.clientId });
    const disabled = await off.app.inject({ method: "GET", url: "/v1/auth/google/config" });
    expect(disabled.json()).toEqual({ enabled: false, webClientId: null });
    expect((await signInWithGoogle(off.app, "anything")).statusCode).toBe(503);
  });

  it("creates an account and connects Tasks and Calendar in one step", async () => {
    const c = code({ name: "Mohamed" });
    const res = await signInWithGoogle(ctx.app, c, { locale: "ar" });
    expect(res.statusCode).toBe(200);
    const { user, tokens } = res.json<{ user: { email: string; displayName: string; locale: string }; tokens: { accessToken: string } }>();
    expect(user).toMatchObject({ email: identities.get(c)!.email, displayName: "Mohamed", locale: "ar" });
    expect(await googleStatus(tokens.accessToken)).toMatchObject({ connected: true, email: identities.get(c)!.email });

    // Android server auth codes carry no redirect URI and no PKCE verifier.
    const exchange = tokenRequests.find((r) => r.get("code") === c)!;
    expect(exchange.get("redirect_uri")).toBe("");
    expect(exchange.get("code_verifier")).toBeNull();
  });

  it("signs back into the same account by Google identity, even after the email changes", async () => {
    const first = await signInWithGoogle(ctx.app, code({ sub: "stable-sub", email: "old@example.com" }));
    const again = await signInWithGoogle(ctx.app, code({ sub: "stable-sub", email: "renamed@example.com" }));
    expect(again.json().user.id).toBe(first.json().user.id);
  });

  it("links an existing password account with the same verified email", async () => {
    const existing = await signUp(ctx);
    const res = await signInWithGoogle(ctx.app, code({ email: existing.email.toUpperCase() }));
    expect(res.statusCode).toBe(200);
    expect(res.json().user.id).toBe(existing.user.id);
    // The password still works too.
    const login = await ctx.app.inject({
      method: "POST",
      url: "/v1/auth/login",
      payload: { email: existing.email, password: existing.password },
    });
    expect(login.statusCode).toBe(200);
  });

  it("refuses an unverified email and an email linked to another Google account", async () => {
    expect((await signInWithGoogle(ctx.app, code({ email_verified: false }))).json().error.code).toBe("email_not_verified");

    await signInWithGoogle(ctx.app, code({ sub: "owner-sub", email: "shared@example.com" }));
    const other = await signInWithGoogle(ctx.app, code({ sub: "intruder-sub", email: "shared@example.com" }));
    expect(other.statusCode).toBe(409);
    expect(other.json().error.code).toBe("google_account_mismatch");
  });

  it("rejects a code Google does not accept", async () => {
    const res = await signInWithGoogle(ctx.app, "not-a-real-code");
    expect(res.statusCode).toBe(400);
    expect(res.json().error.code).toBe("invalid_google_code");
  });

  it("signs in without connecting when Google grants no offline access", async () => {
    const res = await signInWithGoogle(ctx.app, code({ offline: false }));
    expect(res.statusCode).toBe(200);
    expect(await googleStatus(res.json().tokens.accessToken)).toMatchObject({ connected: false });
  });

  it("gives a Google-only account no password; deleting it is confirmed with its email", async () => {
    const c = code();
    const { tokens, user } = (await signInWithGoogle(ctx.app, c)).json<{ tokens: { accessToken: string }; user: { email: string } }>();
    const guess = await ctx.app.inject({
      method: "POST",
      url: "/v1/auth/login",
      payload: { email: user.email, password: "anything at all" },
    });
    expect(guess.statusCode).toBe(401);

    const headers = { authorization: `Bearer ${tokens.accessToken}` };
    const wrong = await ctx.app.inject({ method: "DELETE", url: "/v1/account", headers, payload: { password: "someone@else.com" } });
    expect(wrong.statusCode).toBe(403);
    const ok = await ctx.app.inject({ method: "DELETE", url: "/v1/account", headers, payload: { password: user.email } });
    expect(ok.statusCode).toBe(204);
  });
});
