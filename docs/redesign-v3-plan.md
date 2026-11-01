# Bawsala V3 redesign: implementation plan

Goal: the Android app matches the **Bawsala V3** design (Figma file *"بوصلة · V3 — GTD Mobile + Tablet"* and the `Bawsala-V3-Prototype.html` prototype) screen for screen, on phones (390 × 844) and tablets (1280 × 900).

**Sources.** This plan was built from the HTML prototype: its design atlas, all 16 screens, its CSS tokens and its sample data. The Figma file could not be read while writing it, because the Figma plan's MCP call limit was reached. Phase 0 includes checking the Figma file node by node against this plan.

---

## 1. What changes, at a glance

| Today's app | V3 design |
| --- | --- |
| Tabs: Today · Inbox · Plan · More, plus a floating capture button | Tabs: **اليوم** Today · **الفرز** Triage · **التقاط** Capture (raised center button) · **المشاريع** Projects · **المراجعة** Review |
| English first, Arabic optional | **Arabic first, right to left.** English stays as a second language. |
| System font | **Noto Sans Arabic** for the interface, **Aref Ruqaa** for the wordmark |
| Inbox list, then a process screen | **One card at a time** triage, with swipe and a progress bar ("١ من ٧") |
| Plan tab: day timeline, week, agenda | **Weekly planner** (tablet: drag tasks onto a 7-day grid) and an **AI rescheduling suggestion** |
| Lists and contexts | **PARA:** Projects · Areas · Resources · Archive (مشاريع · مجالات · مراجع · أرشيف), each project with progress and its next action |
| No weekly review | **Weekly review**: three simple steps, stats, and an AI summary |
| Focus screen | **Calm focus**: Pomodoro/open toggle, ring timer, checklist, and capturing a stray thought without leaving focus |
| — | **Energy and context filters** (منخفضة · متوسطة · مرتفعة / في المنزل · على الكمبيوتر · …) on the next actions |
| Phone layout only | **Tablet layout**: right-side navigation rail, two-pane screens |

---

## 2. Design system (Phase 1)

Copy the tokens exactly from the prototype into `ui/theme`, replacing today's `AppColors` and `AppType`.

**Colors**

| Token | Hex | Use |
| --- | --- | --- |
| `green900` | `#05473F` | primary, priority card, active chip, capture button |
| `green800` | `#0E5B50` | primary pressed |
| `green100` | `#E8EFDF` | active nav pill, avatar |
| `sage400` | `#B2C18F` | accent: "Start focus" button on the dark card, progress |
| `sage100` | `#F1F4E9` | soft buttons, pills |
| `paper` | `#F7F8F3` | background |
| `white` | `#FFFFFF` | surface, cards |
| `ink` | `#213B35` | text |
| `muted` | `#68776E` | secondary text |
| `line` | `#E1E8DC` | borders, dividers |
| `amber` / `amberTint` | `#A56A2A` / `#FBF1E3` | greeting sun, "waiting" and family blocks |
| `lilac` / `lilacInk` | `#EAE6F4` / `#6D6089` | AI suggestions, meetings |
| `danger` / `dangerTint` | `#A94E43` / `#FAECE8` | errors, delete |

**Shape and spacing:** radius 12 / 18 / 26 dp, buttons 15 dp, chips 14 dp, the priority card 24 dp, and the capture button 22 dp. Spacing is 4, 8, 12, 16, 20, 24, 32, 40 and 48. The minimum touch target is 48 dp.

**Type (Noto Sans Arabic):**

| Style | Size / line height / weight |
| --- | --- |
| tiny | 11 / 1.8 |
| small | 13 / 1.8 |
| body | 15 / 1.9 |
| overline | 12 / 600 |
| section title | 17 / 650 |
| page title | 19 / 650 |
| heading | 25 / 650 |
| heading-lg | 28–29 / 650 |
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
| M01 / T01 | **Today**: greeting and date, "يومك، باتجاه أوضح.", energy and free-minutes pills, the priority card with **Start focus**, next actions, next stop (the next appointment). Tablet: plus a "calmer mind" inbox card and daily progress. | `TodayScreen` | Today's "priority" task, energy level, free minutes until the next appointment |
| M02 / T02 | **Quick capture (writing)**: "فكرة الآن، مكانها بعدين." sheet, smart detected tags (tomorrow, 6:00, outside the home) that can be removed, voice button, "Save to triage". | `CaptureSheet` | **Smart tags** detected from the text (date, time, place, context) |
| M03 | **Quick capture (voice)**: live waveform, transcript, timer, stop. Holding the capture button starts voice capture directly. | `CaptureSheet` + `AudioRecorder` | **Speech-to-text transcript** (Android `SpeechRecognizer`, on device) |
| M04 / T03 | **Triage (one item)**: progress "١ من ٧", the item card with source and tags, "Is there an action?" → Next action / Project / Reference / Later; delete; undo. Swipe left or right. | `ProcessScreen` + `DecisionPanels` | Swipe gestures, progress, "Bawsala suggests…" hint on tablet |
| M05 / T07 | **Projects (PARA)**: tabs Projects · Areas · Resources · Archive, priority filter, project cards (icon, status pill, area, progress "٨ من ١٢", next action). | `TasksScreen` / lists | **New entities:** Project (status, area, next action), Area, Resource |
| M06 / T08 | **Weekly review**: "مساحة لإعادة التوجيه." card with Start, stats (actions done, projects advanced, focus sessions), "three simple steps", plan your week; tablet: AI note. | new | **New flow:** guided steps that write a review record |
| M07 / T06 | **Calm focus**: Pomodoro / open toggle, ring timer, task, checklist, pause, stop; "a stray thought? leave it here." | `FocusScreen` | Ring timer, per-session checklist |
| M08 | **Capture during focus**: "اترك الفكرة، وكمّل تركيزك." sheet; the timer keeps running. | `FocusScreen` + `CaptureSheet` | Capture without ending focus |
| T04 | **Weekly planner (tablet)**: 7-day grid with time rows, colored blocks (default / lilac / amber), the next-actions panel with energy and context filters, drag a card into the week, "Schedule my day". | `PlanScreen` / `DayTimeline` | Week grid with drag and drop |
| T05 | **AI reschedule suggestion**: "مساحة أهدأ ليومك." two proposed moves with the reason; Accept / Keep as is. | new | **Suggestion engine** (see the decisions in section 5) |

**Kept but restyled to V3**, since the design doesn't show them: Settings, Account and sync, Google sign-in, Prayer times, Time blocks, Lists, Trash, Search, and Onboarding. They get the V3 components and tokens, but no new layout.

---

## 4. Phases

| Phase | Work | Result you can check |
| --- | --- | --- |
| **0. Lock the design** | Read the Figma file node by node (once the Figma limit allows). Record any difference from the prototype in this plan. Export the Lucide icon list and fonts. | This plan updated; no open visual questions |
| **1. Design system** | Tokens, fonts, icons, and every component in section 2. Arabic becomes the default language. A **component gallery** screen in debug builds. | Gallery screenshot matches the prototype's "Foundations" board |
| **2. Shell and navigation** | 5-slot bottom navigation with the raised capture button and badges. Tablet navigation rail at width ≥ 840 dp. New routes: Triage, Projects, Review. | Navigation works on phone and tablet |
| **3. Today, capture, focus** | M01, M02, M03, M07, M08, T01, T02, T06 | Daily flow end to end |
| **4. Triage and projects** | M04, M05, T03, T07, plus the Project / Area / Resource model in `core`, Android storage, server sync and Google Tasks mapping | Inbox to project flow end to end |
| **5. Planning and review** | T04 (week grid with drag), T05 (suggestions), M06 / T08 (weekly review) | Weekly loop end to end |
| **6. Restyle the rest** | Settings, account, prayer times, blocks, lists, trash, search, onboarding in V3 style | No screen left in the old style |
| **7. Match check and release** | Screenshot tests (Roborazzi) for each screen on phone and tablet, compared side by side with the prototype's renders. Fix differences above a small threshold. Release. | A comparison sheet for all 16 screens |

Each phase is one or more PRs. Every PR shows before/after screenshots.

---

## 5. Decisions needed before Phase 4

1. **Prayer-aware planning.** V3 never shows prayer times, yet they're Bawsala's core feature. Proposal: prayer times appear as blocks in the Today "next stop" and the weekly grid, in the design's block style.
2. **AI features.** V3 shows "Bawsala suggests" (priority, triage hint), smart tags, the reschedule suggestion and the review summary.
   - **Proposal:** smart tags and the triage hint use on-device rules. The reschedule suggestion and the review summary use the server, calling Claude.
   - **Needs:** an API key and a privacy notice, per PRD SEC04 (optional assistant, explains what data is sent).
3. **English.** V3 is Arabic only. Proposal: Arabic is the default and English stays as a translation of the same layout, left to right.
4. **Projects vs. lists.** PARA projects and areas replace today's lists. Existing lists become **Areas**, and their tasks stay attached.
5. **Energy level.** A new task field (low / medium / high). Google Tasks has no such field, so it is stored in the app only.

## 6. Risks

- **Matching exactly:** Compose on Android renders text slightly differently from a browser, mainly Arabic line heights. Phase 7 accepts small font-rendering differences, but layout, spacing, colors and sizes must match.
- **Scope:** Projects/PARA, weekly review and AI suggestions are new features (much of P1), not only a reskin. Phases 4–5 are the largest.
- **Sync and server:** new entities (projects, areas, resources, reviews, energy) need server tables, sync and MCP tools, so the server changes alongside.
