# Decisions

Decisions the owner has taken, newest last within each area. Do not reverse one without asking.
Links point to the pull request or issue where it was made or built.

## Product

1. **Name and identity:** Bawsala · بوصلة ("compass"), app id `app.bawsala`; the "Busla Masar
   Ruqaa" logo pack; Aref Ruqaa for the wordmark, Noto Sans Arabic for text. License AGPL-3.0.
2. **Arabic first**, English kept; the layout mirrors for right-to-left. Arabic is the default.
3. **Prayer times are central** and shown as blocks (Today's "next stop", amber blocks in the week
   planner, protected time).
4. **No AI features for now.** Everything AI in the design is left out ("بوصلة تقترح", AI
   reschedule T05, the review's AI note). Capture tags come from simple text rules.
5. **No energy levels** (the design's energy pill and filter are left out).
6. **Lists stay separate from projects.** A task has a list and may also belong to a project
   (PARA: projects, areas, resources = reference items, archive).
7. **Prayer is a module, not a separate app** (#74): a "Prayer features" switch turns everything
   prayer-related off (#78); part 2 moves prayer code to its own Gradle module, which could later
   also power a standalone prayer app built from the same code.
8. **The week starts on Saturday** (review and planner), as in the design.

## Design

9. **Penpot is the source of truth** for V3 (Figma was the original; its plan limit made Penpot the
   working copy). Match boards closely; screens without a board follow the nearest V3 pattern.
10. **Triage:** four main decisions on the card (next action, project, reference, later) plus swipe
    (toward the end = next action, toward the start = reference); Do Now, waiting, split and review
    dates under page options; delete asks first; every decision can be undone.
11. **Sign-in:** the AM/AT boards, with "Continue with Google" kept under the forms and "use
    without an account" added so the app still works offline. Tablet sign-in has the brand panel.
12. **Onboarding:** the three OM boards; the city is asked on Today, the language in Preferences.

## Features

13. **Sign in with Google** signs in and connects Google Tasks and Calendar in one step.
14. **Email:** confirmation and password-reset links through Resend; links open small pages on the
    server; a new password signs every device out. Accounts work before confirmation (see #76).
15. **Location for prayer times** (#62): "Use my location" with approximate location; when the
    app opens more than 50 km away, Today **asks** to update (no background tracking); the
    country's usual calculation method is **suggested**, never applied on its own.
16. **Widget** (#63): one resizable widget — next stop + capture; wide adds the priority action.
17. **Gmail / YouTube / Drive** (#65): polish sharing only (titles, thumbnails, links); no new
    Google permissions, no reading Gmail or YouTube accounts.

## Engineering

18. **Hosting:** Render (free plan, cold starts accepted for now) + Neon PostgreSQL.
19. **Local-first sync:** JSON-document entities with an outbox; the server validates known fields
    and keeps unknown ones.
20. **Screenshot tests** with Roborazzi; explicit recording and committed Arabic/English baselines,
    verified in CI. Comparison artifacts and optional `v3-screenshots` publication support review.
21. **Squash merges** on `main`; release-please for versions and changelog.
