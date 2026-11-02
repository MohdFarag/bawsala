/** An email to one person, with a plain-text fallback. */
export interface Mail {
  to: string;
  subject: string;
  html: string;
  text: string;
}

export interface Mailer {
  send(mail: Mail): Promise<void>;
}

/** Sends through Resend's HTTP API (https://resend.com/docs/api-reference/emails/send-email). */
export class ResendMailer implements Mailer {
  constructor(
    private readonly apiKey: string,
    private readonly from: string,
    private readonly fetchImpl: typeof fetch = fetch,
  ) {}

  async send(mail: Mail): Promise<void> {
    const res = await this.fetchImpl("https://api.resend.com/emails", {
      method: "POST",
      headers: { authorization: `Bearer ${this.apiKey}`, "content-type": "application/json" },
      body: JSON.stringify({ from: this.from, to: [mail.to], subject: mail.subject, html: mail.html, text: mail.text }),
    });
    if (!res.ok) throw new Error(`Resend refused the email: ${res.status} ${await res.text()}`);
  }
}

/** Used when no provider is configured: the email is logged, so local development still works. */
export class LogMailer implements Mailer {
  constructor(private readonly log: (message: string) => void) {}

  send(mail: Mail): Promise<void> {
    this.log(`Email to ${mail.to} (no RESEND_API_KEY set): ${mail.subject}\n${mail.text}`);
    return Promise.resolve();
  }
}
