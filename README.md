<p align="center">
  <img src="docs/brand/bawsala-logo.svg" alt="Bawsala · بوصلة" width="400">
</p>

<p align="center">
  <b>Your day, in the right direction.</b><br>
  A calm planner that captures everything first, then helps you plan your day around your prayers.
</p>

<p align="center">
  <a href="https://github.com/MohdFarag/bawsala/releases/latest"><img alt="Latest release" src="https://img.shields.io/github/v/release/MohdFarag/bawsala?label=release&color=225C46"></a>
  <a href="https://github.com/MohdFarag/bawsala/actions/workflows/android.yml"><img alt="Android CI" src="https://github.com/MohdFarag/bawsala/actions/workflows/android.yml/badge.svg"></a>
  <a href="https://github.com/MohdFarag/bawsala/actions/workflows/server.yml"><img alt="Server CI" src="https://github.com/MohdFarag/bawsala/actions/workflows/server.yml/badge.svg"></a>
  <a href="LICENSE"><img alt="License: AGPL-3.0" src="https://img.shields.io/badge/license-AGPL--3.0-225C46"></a>
</p>

<p align="center" dir="rtl">
  <b>بوصلة</b>: مخطِّط هادئ يلتقط كل شيء أولًا، ثم يساعدك على تخطيط يومك حول صلواتك.
</p>

---

## What Bawsala does

| | |
| --- | --- |
| **Capture first, decide later** | Send anything to the inbox: text, lists, links, voice notes, photos, videos and files, or share straight from Google Keep and other apps. Nothing needs a project or a date to be saved. |
| **Process the inbox** | Turn each item into one or several tasks or subtasks, a reference, a "someday" idea, or a "waiting for", or do it now. Every decision can be undone. |
| **Plan around your prayers** | Prayer times are calculated offline for your city (method, Asr madhab, iqama, end of the prayer period). Plan sessions at a fixed time or relative to a prayer, for example "30 minutes after Asr". Prayer times stay protected in your plan. |
| **Time blocks** | Guiding or protected blocks for work, family or study, with conflict checks and free-slot suggestions before you save. |
| **Focus** | A focus timer with breaks and extensions, and a log of the time you actually worked. |
| **Google Tasks and Calendar** | Sign in with Google to sync tasks both ways, publish sessions to Google Calendar with a reminder, and see your appointments in the plan. |
| **Works offline, syncs everywhere** | Everything is saved on the phone first and syncs when you're online, merging edits field by field across devices. |
| **AI assistant access (MCP)** | Let an assistant such as Claude read your plan and add or schedule tasks through its own revocable token. |
| **Arabic and English** | Full Arabic interface with right-to-left layout. |

## Get the app

Download `app-release.apk` from the [latest release](https://github.com/MohdFarag/bawsala/releases/latest) and open it on an Android phone (Android 8.0 or newer). Allow installing from your browser if Android asks. Updates install over the previous version.

## How it fits together

```text
 Android app (Kotlin, Jetpack Compose)          Server (Node 22, Fastify, PostgreSQL 16)
 ┌────────────────────────────────────┐        ┌─────────────────────────────────────────┐
 │ UI ─ controllers ─ repository       │  HTTPS │ accounts · sync · media · MCP            │
 │ SQLite (local-first) + sync outbox  │ ─────► │ Google Tasks & Calendar (server-side)   │
 │ core: prayer times, scheduling,     │        │ OpenAPI docs at /docs                   │
 │ triage (pure Kotlin, tested)        │        └─────────────────────────────────────────┘
 └────────────────────────────────────┘                      ▲
                                                              │ MCP (HTTP)
                                                       AI assistants
```

| Folder | What |
| --- | --- |
| [`android/`](android/) | The Android app and its pure-Kotlin domain core. Features and build: [android/README.md](android/README.md) |
| [`server/`](server/) | The backend API. Endpoints, sync protocol and configuration: [server/README.md](server/README.md) |
| [`web/`](web/) | A clickable web prototype of the original design (Vite + React, browser storage only) |
| [`docs/brand/`](docs/brand/) | Logo and icon (SVG) |

## Run it yourself

**Server** (needs Docker):

```bash
cd server && docker compose up --build
```

The API runs on http://localhost:8080, with interactive docs at http://localhost:8080/docs.

**Android app** (needs JDK 17 and the Android SDK, or Android Studio):

```bash
cd android && ./gradlew :app:assembleDebug
```

Point the app at your server under **Account & sync → Advanced: change server**. The emulator reaches your computer at `http://10.0.2.2:8080`.

Build, test and release details are in [CONTRIBUTING.md](CONTRIBUTING.md).

## Self-hosting

The official build talks to its own server, but you can run your own:

1. **Server:** run the image `ghcr.io/mohdfarag/bawsala/server` (or build `server/`) with PostgreSQL 16 behind HTTPS, configured from [`server/.env.example`](server/.env.example). Any Docker host works. Render with a Neon database is a free option.
2. **Google (optional):**
   1. Create a Google Cloud project and enable the **Google Tasks API** and **Google Calendar API**.
   2. Create a **Web application** OAuth client with redirect URI `https://<server>/v1/google/callback`, and set `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI` and `SECRETS_KEY` on the server.
   3. For **Sign in with Google**, also create an **Android** OAuth client with your app's package name and signing-key SHA-1.
   4. While the consent screen is in testing, add your users as test users.
3. **App builds:** set the `API_SERVER_URL` repository variable so release builds use your server by default, and the `ANDROID_KEYSTORE_*` secrets so releases are signed with your key.

## Privacy

- Your data lives on your phone first. It only reaches a server when you sign in.
- Google tokens are encrypted on the server and never sent to the phone.
- Assistant tokens can be revoked at any time.
- You can export your data as JSON or delete your account from the app at any time.

Security reports: see [SECURITY.md](SECURITY.md).

## Contributing

Issues and pull requests are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) for the workflow, checks and conventions.

## License

Copyright (C) 2026 Mohamed Farag. Bawsala is free software under the [GNU Affero General Public License v3.0](LICENSE) (AGPL-3.0-only): you may use, study, change and share it, and anyone who distributes it, or runs a modified version as a network service, must publish their source under the same license.
