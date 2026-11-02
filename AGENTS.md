# Working on Bawsala (for AI agents and new contributors)

Read this first, then `docs/status.md` (what is done and what is next) and `docs/decisions.md`
(decisions already taken — do not reopen them without the owner). `CONTRIBUTING.md` has the layout,
commands and Git workflow; this file adds what is easy to miss.

## What Bawsala is

بوصلة (Bawsala, "compass"): an **Arabic-first, prayer-centred personal planner** for Android.
Capture ideas fast, triage them one at a time, organize with PARA (projects, areas, resources,
archive), plan the week around prayer times, focus, and review weekly. It is local-first: the
phone works offline and syncs to a self-hostable server. AGPL-3.0.

- `android/app`: the app (Kotlin, Jetpack Compose). `android/core`: pure-Kotlin rules (prayer
  engine, triage, scheduling), separate Gradle build.
- `server/`: Node 22 + Fastify 5 + Zod + PostgreSQL. Sync, auth (email + Google), email links
  (Resend), media, Google Tasks/Calendar, and an MCP server so AI assistants can read and plan.
- `web/`: an older web prototype (not the product).
- Live server: `https://bawsala-i1jm.onrender.com` (Render free plan + Neon Postgres).
- Design source of truth: the **Penpot** file "Bawsala V3" (mobile M01–M08, OM/AM boards; tablet
  T01–T08, OT/AT boards). References are exported to `docs/design/reference/`. The redesign plan
  is `docs/redesign-v3-plan.md`. The product requirements (PRD, IDs like `REL02`, `CAP05`) are
  held by the owner, not in the repo; IDs in comments refer to it.

## How to work here

- **Ask the owner at each meaningful step** (scope, UX choices, anything outward-facing) and
  **open a pull request for every change**. Small, focused PRs; one feature per PR.
- The owner **squash-merges**. A PR stacked on another branch then conflicts with `main`; fix by
  merging `origin/main` into each remaining branch in order (prefer the branch side, `-X ours`;
  the classic leftover is `ui/process/ProcessScreen.kt`, which stays deleted). Never force-push.
- **Android may not build in a sandbox** (Google's Maven can be blocked). Then CI is the compiler:
  push, read the failure annotations with `gh api .../check-runs/<id>/annotations`, fix, repeat.
  Format Kotlin with ktfmt (Kotlin style) before pushing; `ktfmtCheck` runs in CI.
- **CI runs on pull requests**, not on plain branch pushes. Waiting for a run on a branch with no
  PR waits forever.
- **Server tests** need PostgreSQL (`TEST_DATABASE_URL`, default `postgres://postgres@127.0.0.1:5433/planner_test`).
- **Screenshots:** `V3Screenshots` (Roborazzi + Robolectric) renders the main screens on a phone
  and a tablet with the Penpot boards' sample data. CI publishes them to the `v3-screenshots`
  branch under `pr-<number>/`; compare them with `docs/design/reference/`. Artifacts may not be
  downloadable from a sandbox; read the branch instead.
- **Releases:** release-please opens `chore(main): release x.y.z`; merging it tags and builds the
  signed APK/AAB with the live server URL.

## Conventions that matter

- **Text:** every user-facing string is a key in `ui/i18n/Strings.kt` (`"key" to ("English" to
  "العربية")`), read with `tr(key)` in Compose or `translate(lang, key)` elsewhere. Arabic is the
  default language and layouts are right-to-left. Keys must be unique (a later duplicate silently
  wins) — check before adding.
- **Design system:** use V3 tokens (`ui/theme`: `Palette`, `TypeScale`, `Radius`) and components
  (`ui/design`: `ActionButton`, `Pill`, `PanelSheet`, `PageHeader`, `OptionRow`, …). Older
  `ui/components` are restyled wrappers kept for remaining screens.
- **Icons:** Lucide only, as vector drawables `res/drawable/ic_lucide_*.xml`. Convert new ones from
  `lucide-static` with path data normalized (arc flags separated); directional icons are
  `autoMirrored` and drawn left-to-right (back = `arrow_left`, forward = `chevron_right`).
- **Screen logic** goes in plain, tested functions (`build…State`, `…Rules`), not in composables.
  Rules without Android go to `android/core` when they are domain rules.
- **Prayer features** can be switched off (`Prefs.prayerFeatures`); prayer times must be read
  through `DayPlanner.anchors()` so the switch keeps working.
- **Sync:** every synced table is a JSON document entity; declare its field shapes in
  `server/src/modules/sync/entities.ts`, and add the Android table, `Codec`, `Snapshot` field and
  `Repository` methods. Unknown fields are kept, so older app versions do not lose data.
- **No AI features** in the product for now (see decisions). No energy levels.
- Commit messages end with the session's attribution lines when an agent writes them; never put
  model names in commits, PRs or code.
