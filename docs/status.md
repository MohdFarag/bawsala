# Status

Where Bawsala stands, for whoever continues the work. Keep this short and current: update it in
the same PR as the change. The live backlog is the [GitHub issues](https://github.com/MohdFarag/bawsala/issues).

_Last updated: version 0.9.0, November 2026._

## Done

- **Foundation (P0):** capture (text, voice, photo, files, share sheet), inbox and triage, tasks
  and subtasks, lists, sessions and prayer-relative rules, time blocks, reminders, focus timer,
  local storage with sync, sign-in (email and Google), media upload, Google Tasks and Calendar
  sync, MCP server for AI assistants, data export, trash with retention.
- **V3 redesign (phases 0–7, `docs/redesign-v3-plan.md`):** design system (tokens, fonts, Lucide
  icons, components, gallery), navigation (bottom bar, tablet rail, capture button), Today, quick
  capture, calm focus, one-card triage, Projects/PARA (data model and screen), weekly review,
  weekly planner with prayer blocks, restyle of the remaining screens, More and search sheet,
  onboarding and sign-in/sign-up from the Penpot boards, task detail, screenshot tests.
- **Server:** email confirmation and password reset through Resend.
- **P1:** prayer times from the phone's location (#62), home-screen widget (#63), YouTube and
  Drive shares with titles and links (#65).

## In progress

- **Prayer module:** part 1, the "Prayer features" switch (#78, open). Part 2: move prayer code
  (engine, settings, rules, location) into its own Gradle module with a narrow interface; no
  visible change (decision 7 in `docs/decisions.md`, issue #74).

## Next (backlog)

| Issue | What |
| --- | --- |
| #77 | **Important:** device (end-to-end) tests on an emulator in CI |
| #76 | Fix: email confirmation is never enforced (decide the rule, then build it) |
| #75 | Change password, email or name while signed in |
| #72 | Hijri calendar everywhere, including scheduling, repeats and reminders |
| #73 | Ramadan mode (suhoor/iftar, lighter plans, Taraweeh) |
| #69 | Repeating tasks (calendar schedules; Hijri too) |
| #71 | Undo for deletes and edits |
| #70 | Restore from backup (import the export) |
| #68 | Dark mode (needs dark tokens; no boards yet) |
| #67 | Pull down to refresh |
| #64 | Transitions, animations, smoothness |

Suggested order: #77 first (it protects everything after it), then #76 and #75, then the
prayer-calendar group (#72, #73, #69), then the rest.

## Known gaps and notes

- **Render free plan** sleeps after ~15 minutes idle; the first request then takes 30–60 s.
  Accepted for now; upgrade when real users depend on it.
- **Not yet tried on a real device:** location (#62), widget (#63), sharing (#65), prayer switch.
- **Needs the owner:** the PRD's full P2 list (only voice transcription is known), and Penpot
  boards for screens that have none (dark mode, task detail, settings).
- Email is sent from `onboarding@resend.dev` until a domain is verified in Resend (`MAIL_FROM`).

## Start here

1. Read `AGENTS.md`, then this file and `docs/decisions.md`.
2. Pick an issue with the owner; branch `<type>/<topic>` from `main`.
3. Server: `cd server && npm ci && npm test` (PostgreSQL needed). Android: see `CONTRIBUTING.md`;
   if Gradle cannot reach Google's Maven, open the PR and let CI compile.
4. Open a PR early; check CI annotations and, for UI, the screenshots on `v3-screenshots`.
