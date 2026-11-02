# Bawsala V3 redesign: implementation plan

Goal: the Android app matches the **Bawsala V3** design (Penpot file *Bawsala* and the `Bawsala-V3-Prototype.html` prototype) screen for screen, on phones (390 × 844) and tablets (1280 × 900), apart from the parts left out in section 5.

**Sources.** The **Penpot file "Bawsala"** is the source of truth. Its pages are *00 · Foundations*, *01 · Mobile* (M01–M08, 390 × 844) and *02 · Tablet* (T01–T08, 1280 × 900). The HTML prototype `Bawsala-V3-Prototype.html` shows the same screens with working interactions. The Figma file of the same name could not be read (its plan's MCP call limit was reached); Penpot and the prototype cover the same screens.

**Penpot check (2 Nov 2026):**
- **Colors and spacing:** the library colors and the *Bawsala V3* token set (16 colors, spacing 4–48) match section 2 exactly.
- **Screens:** M01 and T04 were exported and compared with the prototype; they match.
- **Type:** every text uses Noto Sans Arabic, at weights 400 / 500 / 600 / 700 (the prototype's CSS 650 is 700 in Penpot; **Penpot wins**). Sizes used: 9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 20, 21, 22, 24, 26, 28, 29, 32 and 39.
- **Extra colors not in the library:** `#EDF0E8`, the weekly grid's hour lines and column borders (add it as the `gridLine` token). Plus a handful of one-off tints in illustrations and the waveform.
- **Navigation:** on tablet, the weekly planner (T04) is reached from the **المراجعة** (Review) item, which uses a calendar icon. It is not a separate tab.
- **Clean-up in Penpot:** T03 exists **three times** on the tablet page. Two of them are empty boards stacked on the real one; they can be deleted.
- **Text clipping in Penpot:** short weekly-grid events cut their titles ("جتماع الفريق"). In the app, titles end with an ellipsis instead.

---

## 1. What changes, at a glance

| Today's app | V3 design |
| --- | --- |
| Tabs: Today · Inbox · Plan · More, plus a floating capture button | Tabs: **اليوم** Today · **الفرز** Triage · **التقاط** Capture (raised center button) · **المشاريع** Projects · **المراجعة** Review |
| English first, Arabic optional | **Arabic first, right to left.** English stays as a second language. |
| System font | **Noto Sans Arabic** for the interface, **Aref Ruqaa** for the wordmark |
| Inbox list, then a process screen | **One card at a time** triage, with swipe and a progress bar ("١ من ٧") |
| Plan tab: day timeline, week, agenda | **Weekly planner** (tablet: drag tasks onto a 7-day grid), with prayer times shown as blocks |
| Lists and contexts | Lists stay as they are. **New, separate PARA layer:** Projects · Areas · Resources · Archive (مشاريع · مجالات · مراجع · أرشيف), each project with progress and its next action |
| No weekly review | **Weekly review**: three simple steps and stats |
| Focus screen | **Calm focus**: Pomodoro/open toggle, ring timer, checklist, and capturing a stray thought without leaving focus |
| — | **Context filters** (في المنزل · على الكمبيوتر · …) on the next actions |
| Phone layout only | **Tablet layout**: right-side navigation rail, two-pane screens |

---

## 2. Design system (Phase 1)

Copy the tokens exactly from the Penpot *Bawsala V3* token set into `ui/theme`, replacing today's `AppColors` and `AppType`.

**Colors**

| Token | Hex | Use |
| --- | --- | --- |
| `green900` | `#05473F` | primary, priority card, active chip, capture button |
| `green800` | `#0E5B50` | primary pressed |
| `green100` | `#E8EFDF` | active nav pill, avatar |
| `sage400` | `#B2C18F` | accent: "Start focus" button on the dark card, progress |
| `sage100` | `#F1F4E9` | soft buttons, pills |
| `paper` | `#F7F8F3` | background |
| `gridLine` | `#EDF0E8` | weekly grid hour lines and column borders (used in Penpot, not in its library) |
| `white` | `#FFFFFF` | surface, cards |
| `ink` | `#213B35` | text |
| `muted` | `#68776E` | secondary text |
| `line` | `#E1E8DC` | borders, dividers |
| `amber` / `amberTint` | `#A56A2A` / `#FBF1E3` | greeting sun, "waiting" and family blocks |
| `lilac` / `lilacInk` | `#EAE6F4` / `#6D6089` | meetings, secondary blocks |
| `danger` / `dangerTint` | `#A94E43` / `#FAECE8` | errors, delete |

**Shape and spacing:** radius 12 / 18 / 26 dp, buttons 15 dp, chips 14 dp, the priority card 24 dp, and the capture button 22 dp. Spacing is 4, 8, 12, 16, 20, 24, 32, 40 and 48. The minimum touch target is 48 dp.

**Type (Noto Sans Arabic):**

| Style | Size / line height / weight |
| --- | --- |
| tiny | 11 / 1.8 |
| small | 13 / 1.8 |
| body | 15 / 1.9 |
| overline | 12 / 600 |
| section title | 17 / 700 |
| page title | 19 / 700 |
| heading | 24 / 700 |
| heading-lg | 28–29 / 700 |
| priority title | 21 / 600 |

Bundle `NotoSansArabic` (variable) and `ArefRuqaa-Bold` as `res/font` (both under the OFL).

**Icons:** the prototype uses **Lucide** (sun, inbox, layers, calendar-days, mic, sparkles, …). Import the same SVGs as vector drawables so they match exactly; don't use Material icons.

**Components** (`ui/components`, one Compose file each), each matching its prototype class:
- **`Button`:** primary / secondary / soft / ghost / danger / dark-soft, 50 dp tall.
- **`TextButton`**, **`IconButton`** (48 dp; plain or soft).
- **`Pill`:** default / lilac / amber / outline / white.
- **`Chip`:** with icon, active state, and a sage variant.
- **`Card`** (18 dp).
- **`PriorityCard`:** dark green with two decorative rings.
- **`TaskRow`:** checkbox, title, project · context, and minutes.
- **`SectionTitle`:** with a "show all" link.
- **`Avatar`.**
- **`ProgressBar`.**
- **`BottomNav`:** 5 slots, a raised 62 dp capture button with a 5 dp paper outline, and nav badges.
- **`NavRail`** (tablet).
- **`Sheet`** (bottom sheet on phone, centered dialog on tablet).
- **`Toast`** with undo.
- **`StatusLine`:** the "saved / syncing" states from the design's interaction rules.

---

## 3. Screens (Phases 2–5)

Mobile screen codes (M01–M08) and tablet codes (T01–T08) are the prototype's.

| # | Screen | Built from | New behavior |
| --- | --- | --- | --- |
| M01 / T01 | **Today**: greeting and date, "يومك، باتجاه أوضح.", the free-minutes pill, the priority card with **Start focus**, next actions, next stop (the next appointment **or prayer**). Tablet: plus a "calmer mind" inbox card and daily progress. | `TodayScreen` | Today's priority task (the most important task due soonest; no suggestion label), free minutes until the next appointment or prayer |
| M02 / T02 | **Quick capture (writing)**: "فكرة الآن، مكانها بعدين." sheet, detected tags (tomorrow, 6:00, outside the home) that can be removed, voice button, "Save to triage". | `CaptureSheet` | Tags found by **simple text rules** (date and time words, places), as in the prototype; no AI |
| M03 | **Quick capture (voice)**: live waveform, transcript, timer, stop. Holding the capture button starts voice capture directly. | `CaptureSheet` + `AudioRecorder` | **Speech-to-text transcript** (Android `SpeechRecognizer`, on device) |
| M04 / T03 | **Triage (one item)**: progress "١ من ٧", the item card with source and tags, "Is there an action?" → Next action / Project / Reference / Later; delete; undo. Swipe left or right. | `ProcessScreen` + `DecisionPanels` | Swipe gestures, progress (the tablet "Bawsala suggests" hint is left out) |
| M05 / T07 | **Projects (PARA)**: tabs Projects · Areas · Resources · Archive, priority filter, project cards (icon, status pill, area, progress "٨ من ١٢", next action). | `TasksScreen` / lists | **New entities:** Project (status, area, next action), Area, Resource |
| M06 / T08 | **Weekly review**: "مساحة لإعادة التوجيه." card with Start, stats (actions done, projects advanced, focus sessions), "three simple steps", plan your week (the tablet AI note is left out). | new | **New flow:** guided steps that write a review record |
| M07 / T06 | **Calm focus**: Pomodoro / open toggle, ring timer, task, checklist, pause, stop; "a stray thought? leave it here." | `FocusScreen` | Ring timer, per-session checklist |
| M08 | **Capture during focus**: "اترك الفكرة، وكمّل تركيزك." sheet; the timer keeps running. | `FocusScreen` + `CaptureSheet` | Capture without ending focus |
| T04 | **Weekly planner (tablet)**: 7-day grid with time rows, colored blocks (default / lilac / amber), **prayer times as blocks**, the next-actions panel with context filters, drag a card into the week. | `PlanScreen` / `DayTimeline` | Week grid with drag and drop; prayers shown in the grid |

**Kept but restyled to V3**, since the design doesn't show them: Settings, Account and sync, Google sign-in, Prayer times, Time blocks, Lists, Trash, Search, and Onboarding. They get the V3 components and tokens, but no new layout.

---

## 4. Phases

| Phase | Work | Result you can check |
| --- | --- | --- |
| **0. Lock the design** | Done against Penpot (see Sources). Remaining: export each Penpot board as a PNG reference for the screenshot tests in phase 7, and export the Lucide icon list and fonts. | Reference images for all 15 screens |
| **1. Design system** | Tokens, fonts, icons, and every component in section 2. Arabic becomes the default language. A **component gallery** screen in debug builds. | Gallery screenshot matches the prototype's "Foundations" board |
| **2. Shell and navigation** | 5-slot bottom navigation with the raised capture button and badges. Tablet navigation rail at width ≥ 840 dp. New routes: Triage, Projects, Review. | Navigation works on phone and tablet |
| **3. Today, capture, focus** | M01, M02, M03, M07, M08, T01, T02, T06 | Daily flow end to end |
| **4. Triage and projects** | M04, M05, T03, T07, plus the Project / Area / Resource model in `core`, Android storage, server sync and Google Tasks mapping | Inbox to project flow end to end |
| **5. Planning and review** | T04 (week grid with drag, prayers as blocks), M06 / T08 (weekly review) | Weekly loop end to end |
| **6. Restyle the rest** | Settings, account, prayer times, blocks, lists, trash, search, onboarding in V3 style | No screen left in the old style |
| **7. Match check and release** | Screenshot tests (Roborazzi) for each screen on phone and tablet, compared side by side with the prototype's renders. Fix differences above a small threshold. Release. | A comparison sheet for the 15 screens in scope |

Each phase is one or more PRs. Every PR shows before/after screenshots.

---

## 5. Decisions (agreed)

1. **Prayer times stay central.** Prayers appear as blocks in Today's "next stop" and in the weekly grid, in the design's block style. The prayer settings screen is restyled in phase 6.
2. **No AI features for now.** Everything AI in the design is left out: the "Bawsala suggests" label on the priority card, the triage hint, the AI reschedule screen (T05) and the review summary. Detected capture tags come from simple text rules, as in the prototype.
3. **Arabic first, English kept.** Arabic is the default. English uses the same layout mirrored left to right.
4. **Lists stay separate.** Today's lists (and their Google Tasks sync) are unchanged. Projects, Areas, Resources and Archive are a new, separate layer: a task can belong to a list and, optionally, to a project; a project can belong to an area.
5. **No energy level.** The energy pills and filters are left out; the context filters stay.

## 6. Risks

- **Matching exactly:** Compose on Android renders text slightly differently from a browser, mainly Arabic line heights. Phase 7 accepts small font-rendering differences, but layout, spacing, colors and sizes must match.
- **Scope:** Projects/PARA and the weekly review are new features (part of P1), not only a reskin. Phases 4–5 are the largest.
- **Sync and server:** new entities (projects, areas, resources, reviews) need server tables, sync and MCP tools, so the server changes alongside.
