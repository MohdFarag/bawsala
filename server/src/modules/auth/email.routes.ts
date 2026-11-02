import type { FastifyPluginAsyncZod } from "fastify-type-provider-zod";
import { z } from "zod";
import { ErrorBody } from "../../http/errors.js";
import { badRequest } from "../../shared/errors.js";
import { Email, NewPassword } from "./auth.schemas.js";
import type { EmailLinks, LinkProblem } from "./email-links.js";

const EmailBody = z.object({ email: Email });
const Token = z.string().min(20).max(200);
const TokenQuery = z.object({ token: Token });
const ResetBody = z.object({ token: Token, password: NewPassword });

/**
 * Email confirmation and password reset (V3 AM03-AM05). Requests answer 204 whether or not the
 * email has an account. The links in the emails open small pages served here, so they work without
 * the app installed.
 */
export function emailRoutes(links: EmailLinks, credentialsPerMinute: number): FastifyPluginAsyncZod {
  const credentialLimit = { rateLimit: { max: credentialsPerMinute, timeWindow: "1 minute" } };

  return async (app) => {
    // The reset page posts a plain HTML form.
    app.addContentTypeParser("application/x-www-form-urlencoded", { parseAs: "string" }, (_req, body, done) => {
      done(null, Object.fromEntries(new URLSearchParams(body as string)));
    });

    app.post(
      "/auth/password/forgot",
      {
        config: credentialLimit,
        schema: { tags: ["auth"], summary: "Email a password reset link", body: EmailBody, response: { 204: z.null() } },
      },
      async (request, reply) => {
        await links.requestPasswordReset(request.body.email);
        return reply.status(204).send(null);
      },
    );

    app.post(
      "/auth/email/resend",
      {
        config: credentialLimit,
        schema: { tags: ["auth"], summary: "Email a new confirmation link", body: EmailBody, response: { 204: z.null() } },
      },
      async (request, reply) => {
        await links.resendVerification(request.body.email);
        return reply.status(204).send(null);
      },
    );

    app.post(
      "/auth/password/reset",
      {
        config: credentialLimit,
        schema: {
          tags: ["auth"],
          summary: "Set a new password with a reset link's token",
          body: ResetBody,
          response: { 204: z.null(), 400: ErrorBody },
        },
      },
      async (request, reply) => {
        const result = await links.resetPassword(request.body.token, request.body.password);
        if (typeof result === "string") throw badRequest(`link_${result}`, problemText("en", result));
        return reply.status(204).send(null);
      },
    );

    app.get(
      "/auth/email/verify",
      { schema: { tags: ["auth"], summary: "Page opened from the confirmation email", querystring: TokenQuery } },
      async (request, reply) => {
        const result = await links.verifyEmail(request.query.token);
        const locale = typeof result === "string" ? "ar" : result.locale;
        return reply
          .type("text/html; charset=utf-8")
          .send(
            typeof result === "string"
              ? page(locale, problemText(locale, result), problemHint(locale))
              : page(
                  locale,
                  t(locale, "Email confirmed.", "تم تأكيد بريدك."),
                  t(locale, "Go back to Bawsala and sign in.", "ارجع لبوصلة وسجّل دخولك."),
                ),
          );
      },
    );

    app.get(
      "/auth/password/reset",
      { schema: { tags: ["auth"], summary: "Page opened from the reset email", querystring: TokenQuery } },
      async (request, reply) => {
        const result = await links.checkReset(request.query.token);
        reply.type("text/html; charset=utf-8");
        if (typeof result === "string") return reply.send(page("ar", problemText("ar", result), problemHint("ar")));
        return reply.send(resetForm(result.locale, request.query.token));
      },
    );

    app.post("/auth/password/reset/form", { config: credentialLimit, schema: { hide: true } }, async (request, reply) => {
      const parsed = ResetBody.safeParse(request.body);
      reply.type("text/html; charset=utf-8");
      const token = typeof (request.body as { token?: unknown })?.token === "string" ? (request.body as { token: string }).token : "";
      if (!parsed.success) {
        const check = await links.checkReset(token);
        if (typeof check === "string") return reply.status(400).send(page("ar", problemText("ar", check), problemHint("ar")));
        return reply.status(400).send(resetForm(check.locale, token, true));
      }
      const result = await links.resetPassword(parsed.data.token, parsed.data.password);
      if (typeof result === "string") return reply.status(400).send(page("ar", problemText("ar", result), problemHint("ar")));
      return reply.send(
        page(
          result.locale,
          t(result.locale, "Your new password is set.", "كلمة المرور الجديدة جاهزة."),
          t(result.locale, "Go back to Bawsala and sign in with it.", "ارجع لبوصلة وسجّل دخولك بها."),
        ),
      );
    });
  };
}

const t = (locale: string, en: string, ar: string) => (locale === "ar" ? ar : en);

function problemText(locale: string, problem: LinkProblem): string {
  switch (problem) {
    case "expired":
      return t(locale, "This link has expired.", "انتهت صلاحية هذا الرابط.");
    case "used":
      return t(locale, "This link was already used.", "هذا الرابط استُخدم من قبل.");
    default:
      return t(locale, "This link isn't valid.", "هذا الرابط غير صالح.");
  }
}

const problemHint = (locale: string) => t(locale, "Ask for a new link from the Bawsala app.", "اطلب رابطًا جديدًا من تطبيق بوصلة.");

const esc = (s: string) => s.replace(/[&<>"']/g, (c) => `&#${c.charCodeAt(0)};`);

/** A minimal page in the V3 colors. */
function page(locale: string, title: string, body: string, extra = ""): string {
  const ar = locale === "ar";
  return `<!doctype html><html lang="${ar ? "ar" : "en"}" dir="${ar ? "rtl" : "ltr"}"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>${ar ? "بوصلة" : "Bawsala"}</title>
<style>body{margin:0;background:#F7F8F3;color:#213B35;font-family:"Noto Sans Arabic",system-ui,sans-serif;line-height:1.8}
main{max-width:420px;margin:0 auto;padding:48px 22px}h1{color:#05473F;font-size:26px;margin:0 0 8px}p{color:#68776E}
label{display:block;font-weight:600;margin:18px 0 6px}input{box-sizing:border-box;width:100%;padding:14px 16px;border:1px solid #E1E8DC;border-radius:15px;font-size:16px;background:#fff}
button{width:100%;margin-top:22px;padding:14px;border:0;border-radius:15px;background:#05473F;color:#fff;font-size:15px;font-weight:600}
.err{color:#B3261E}</style></head><body><main><h1>${esc(title)}</h1><p>${esc(body)}</p>${extra}</main></body></html>`;
}

function resetForm(locale: string, token: string, invalid = false): string {
  const form = `<form method="post" action="/v1/auth/password/reset/form">
<input type="hidden" name="token" value="${esc(token)}">
<label for="password">${t(locale, "New password", "كلمة المرور الجديدة")}</label>
<input id="password" name="password" type="password" minlength="10" required autocomplete="new-password">
${invalid ? `<p class="err">${t(locale, "Use at least 10 characters.", "استخدم ١٠ أحرف على الأقل.")}</p>` : ""}
<button type="submit">${t(locale, "Save the new password", "احفظ كلمة المرور")}</button></form>`;
  return page(
    locale,
    t(locale, "Set a new password.", "عيّن كلمة مرور جديدة."),
    t(locale, "10 characters at least. Every device will sign out.", "١٠ أحرف على الأقل. ستخرج كل الأجهزة من الحساب."),
    form,
  );
}
