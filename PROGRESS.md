# Hisn — PROGRESS.md (task tracker)

Last updated: 2026-09-15 · maintained by the orchestrator (Claude Code) session.
Detail: `.ai/TASKS.md` (board) · `.ai/SESSION_LOG.md` (history) · `.ai/HANDOFFS/` (agent reports).
Statuses: Done / Pending. This is the single source of truth for remaining work.

## Phase 1 — Continue & Test (T1–T5) — ALL DONE ✅

| ID | Task | Status | Result |
|----|------|--------|--------|
| T1 | Desktop GoogleTest suite (spdlog via LD_LIBRARY_PATH) | Done | 18/18 green |
| T2 | Backend jest specs (blocklist/stats/guard) | Done | 69/69 green (8 suites) |
| T3 | Android JUnit unit tests | Done | 77/77 green (3 classes) |
| T4 | Website smoke check (v1) | Done | PASS — index 200, RTL/SVG markers, 404 correct |
| T5 | Integration review + per-task commits | Done | through `1be3139`; 164 automated checks green |

## Phase 2 — Frontend & Layout (F1–F10) — ALL DONE ✅

| ID | Task | Status | Commit |
|----|------|--------|--------|
| F1 | Design tokens — colors/dimens/themes, light+night+ar | Done | `f01d375` |
| F2 | Home screen deep-pass (hero/gate/hadith/stats/journey/footer) | Done | `45f34b8` |
| F3 | Features screen redesign (cards grid) | Done | `c0822b4` |
| F4 | Settings screen polish (rows/sections/toggles) | Done | `f21f523` |
| F5 | Stats screen refresh (cards/bar/heatmap) | Done | `4c1fcd8` |
| F6 | PIN entry UX (pin/set-pin/app-lock) | Done | `44893ae` |
| F7 | App shell + motion (splash/welcome/nav/UiAnim) | Done | `19e3f04` |
| F8 | RTL & typography audit | Done — audit-only; fixes folded into F1/F7 + F-INT | — |
| F9 | Drawable inventory cleanup (radius/stroke/ripple unification) | Done | `a45e48a` |
| F10 | Website v2 — single-file rewrite (421→667 lines), verified | Done — commit = P3 below | — |
| F-INT | Integration build #3 + full JUnit | Done | BUILD SUCCESSFUL 35s · 77/77 |

## Pending chain (P1–P7) — in execution

| # | Task | Status | Note |
|---|------|--------|------|
| P1 | `git rm` 16 orphaned res files (12 drawables + 4 anims, zero-ref verified) + commit `refactor(android): prune 16 orphaned drawables and animations` | Pending | auto-retry loop — external classifier outage (intermittent) |
| P2 | Commit `.ai/` state + all handoffs + this tracker — `chore: session state + F1–F10 handoffs for frontend phase` | Pending | follows P1 |
| P3 | Commit website v2 — `feat(web): website v2 — hero/demo/trust/footer + a11y` | Pending | follows P2 |
| P4 | Website smoke test (python3 http.server + curl markers) | Pending | follows P3 |
| P5 | Rebuild debug APK (validates the prune) + install on user's phone (`adb install -r`) | Pending | NEW user request 2026-09-15 — phone must be connected w/ USB debugging |
| P6 | On-device full feature test: launch, all bottom tabs, gate toggle, VPN active check, blocked-domain check, screenshots | Pending | NEW user request 2026-09-15 — follows P5 |
| P7 | Final report (fleet, commits, tests, phone results, findings) | Pending | follows P6 |

## Documented findings (carried, out of scope — for the team)

- 🔴 Backend ApiKeyGuard: unset `API_KEY` authorizes keyless requests (tests document current behavior).
- 🟡 DNS blocklist: public-suffix rule over-blocks subdomains.
- 🟡 Android PinManager: error precedence (length before mismatch).
- 🟢 Hardcoded upstream resolver `10.111.0.2`.
