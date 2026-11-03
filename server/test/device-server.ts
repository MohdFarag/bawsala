/** Disposable emulator backend. Never shipped in dist/ or exposed by the production API. */
import { createServer } from "node:http";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { buildApp } from "../src/app.js";
import { loadConfig } from "../src/shared/config.js";
import { createPool, migrate } from "../src/shared/db.js";
import { FakeGoogle } from "./fake-google.js";
import { GoogleOAuth } from "../src/modules/google/google.oauth.js";

const database = process.env.TEST_DATABASE_URL;
if (!database || !["localhost", "127.0.0.1"].includes(new URL(database).hostname))
  throw new Error("A local disposable TEST_DATABASE_URL is required");
const db = createPool(database);
await migrate(db);
const mediaDir = await mkdtemp(path.join(tmpdir(), "bawsala-device-"));
const config = loadConfig({ DATABASE_URL: database, MEDIA_DIR: mediaDir, AUTH_RATE_LIMIT_PER_MINUTE: "1000" });
const { app } = await buildApp({
  config,
  db,
  logger: false,
  mailer: { send: async () => {} },
  google: {
    oauth: new GoogleOAuth({ clientId: "test", clientSecret: "test", redirectUri: "http://localhost/callback" }, () =>
      Promise.reject(new Error("External OAuth is disabled in device tests")),
    ),
    api: () => new FakeGoogle(),
  },
});
const unavailable = createServer((_request, response) => {
  response.writeHead(503);
  response.end("Temporary test failure");
});
unavailable.listen(8081, "0.0.0.0");
await app.listen({ port: 8080, host: "0.0.0.0" });
async function shutdown() {
  await app.close();
  unavailable.close();
  await db.end();
  await rm(mediaDir, { recursive: true, force: true });
  process.exit(0);
}
process.on("SIGTERM", () => {
  void shutdown();
});
process.on("SIGINT", () => {
  void shutdown();
});
