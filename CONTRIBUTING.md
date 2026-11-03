# Contributing

## Layout

| Path           | What                                                                                                        |
| -------------- | ----------------------------------------------------------------------------------------------------------- |
| `android/core` | Pure Kotlin domain rules (triage, scheduling, prayer engine). Separate Gradle build; no Android SDK needed. |
| `android/app`  | Android app (Compose UI, storage, background work).                                                         |
| `server/`      | Backend API (Node, Fastify, PostgreSQL). See `server/README.md`.                                            |
| `web/`         | Web prototype of the Figma design.                                                                          |

## Build and test

```bash
cd android
./gradlew -p core test ktfmtCheck                        # domain rules + formatting
./gradlew :app:ktfmtCheck :app:verifyRoborazziDebug :app:lintDebug :app:assembleDebug
```

Kotlin is formatted with [ktfmt](https://github.com/facebook/ktfmt) in the Kotlin style; run
`./gradlew -p core ktfmtFormat` and `./gradlew :app:ktfmtFormat` before committing.
TypeScript (server and web) is formatted with Prettier (`.prettierrc.json`); run `npm run format`.

```bash
cd server && npm ci && npm run format:check && npm run lint && npm run typecheck && npm test   # needs PostgreSQL, see server/README.md
```

```bash
cd web && npm ci && npm run format:check && npm test && npm run build
```

Dependency versions live in `android/gradle/libs.versions.toml`; Dependabot proposes updates weekly.

## Workflow

Trunk-based: `main` is always releasable, and every change reaches it through a short-lived branch
and a squash-merged pull request.

- **Branch names:** `<type>/<topic>`, for example `feat/sign-in`, `fix/photo-rotation`, `ci/release-please`.
  Types: `feat`, `fix`, `refactor`, `perf`, `docs`, `test`, `build`, `ci`, `chore`.
- **PR titles follow [Conventional Commits](https://www.conventionalcommits.org):** `type(scope): summary`,
  for example `feat(android): sign in and sync`. The title becomes the squash commit on `main` and
  drives the version and changelog. `feat` bumps the minor version, `fix` the patch, and `!` or a
  `BREAKING CHANGE:` footer the major. Scopes: `android`, `core`, `server`, `web`, `ci`, `deps`.
- Keep PRs focused; stack dependent PRs (base one PR on the other's branch) instead of mixing topics.
- The **Repository quality gate** must pass before merging: formatting, lint, tests and builds for the touched projects, plus PR
  hygiene (title and branch name), security checks and applicable device smoke tests.
  Resolved conversations are required; external approvals are optional for this solo-owned project.
- Put business rules in `android/core` with unit tests; keep the app module for UI and Android integration.
- Keep each layer to one job. Android: screens render state and forward events to controllers in
  `viewmodel/`, which call `domain/` and `data/`. Server: routes handle HTTP, services hold the rules,
  repositories hold the SQL, and a module reaches another module only through its service
  (ESLint enforces this). Web prototype: screens use the store; data rules live in `state/`.
- Reference PRD requirement IDs (for example `REL02`) in code comments where a rule comes from the PRD, and in the pull request. Link an issue with observable acceptance criteria so
  contributors do not need access to the private PRD.
- New user-facing text needs both English and Arabic strings, and layouts must work right-to-left.

## Releases

Releases are automated with [release-please](https://github.com/googleapis/release-please):

1. Each merge to `main` updates an open **release PR** (`chore(main): release x.y.z`) with the next
   version and `CHANGELOG.md`, computed from the Conventional Commit titles since the last release.
2. Merging the release PR tags `vX.Y.Z` and creates a **draft**. Full validation runs on that exact
   commit, then builds and verifies the signed APK/AAB, checksums and staged server image.
3. Only after all checks pass are packages attached, the release published and stable image tags
   promoted at `ghcr.io/mohdfarag/bawsala/server` (with build provenance and an SBOM).

The version lives in `version.txt` and is mirrored into `server/package.json` and `web/package.json`.
Manual `v*` tags must be reachable from `main` and match `version.txt`; they receive the same
validation. Reruns verify published packages rather than overwrite them. Dispatch a retry on its tag
if the release commit differs from the current branch head.

The Android job runs in the `release` environment. Signing uses these secrets (set them on the
environment, or on the repository):

| Secret                      | Value                    |
| --------------------------- | ------------------------ |
| `ANDROID_KEYSTORE_BASE64`   | `base64 -w0 release.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password        |
| `ANDROID_KEY_ALIAS`         | Key alias                |
| `ANDROID_KEY_PASSWORD`      | Key password             |

## Automated checks

Repository CI runs for every PR and `main` push, nightly at 03:30 UTC, and on demand.
Project selection happens inside CI, so **Repository quality gate** always reports. Android, server
and web changes run their own formatting/lint/tests/builds; Android or server changes also run the
API 35 device smoke suite. Workflow/tooling changes and release validation run every project.
Security checks always run; PR titles/branch names are checked on PR events.
Nightly device testing expands to API 28/35 permission, sharing, widget and triage flows.
Fork PRs need no secrets; annotation/publication extras are skipped when they require write tokens.
Actions stay SHA-pinned, builds use read-only tokens, and checkout credentials are not persisted.

### Visual regressions

Normal CI verifies 44 Arabic/English phone/tablet screenshots with fixed time and isolated fixtures.
To intentionally update them on the same Linux/JDK 17 environment as CI:

```bash
cd android
./gradlew :app:recordRoborazziDebug
./gradlew :app:verifyRoborazziDebug
```

Commit baseline changes from `app/src/test/screenshots/`, review every difference against the design
references, and explain the intended UI change. CI never automatically accepts new baselines.
Unexpected/missing images fail verification; expected/actual/difference reports are uploaded.

### Device tests

Use a disposable PostgreSQL database, never production. From `server/`:

```bash
npm ci
TEST_DATABASE_URL=postgres://postgres:postgres@localhost:5432/planner_device node --import tsx test/device-server.ts
```

In another terminal, from `android/` (Android SDK and virtualization required):

```bash
./gradlew :app:phoneApi35DebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.testServer=http://10.0.2.2:8080 \
  -Pandroid.testInstrumentationRunnerArguments.class=app.bawsala.android.device.PlannerSmokeTest,app.bawsala.android.device.SyncSmokeTest
```

Omit the class filter for the extended suite; use `phoneApi28DebugAndroidTest` for compatibility.
Each test runs in an isolated process with cleared app data through Android Test Orchestrator.
The test-only backend uses temporary media, a local database and fake email/Google dependencies.
It exposes a temporary-failure server on 8081 for recovery testing and is excluded from production builds.
No assertion retries hide failures; inspect the reports/backend logs uploaded by CI.

## Repository and release settings

See [the hardening rollout checklist](docs/repository-hardening.md) and the desired settings in
`.github/repository-settings.json`. Applying them requires GitHub administration permission;
files alone do not configure GitHub. Keep the Actions token default read-only and allow Actions to
create release PRs. Publication requires `API_SERVER_URL`, the four existing signing secrets and
`ANDROID_SIGNING_CERT_SHA256` (an environment/repository variable from the last trusted APK).
Missing or changed signing configuration must block official publication. Do not replace the key
merely to make CI pass; users must be able to upgrade without losing installed data.
