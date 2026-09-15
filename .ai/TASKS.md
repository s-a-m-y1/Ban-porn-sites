# Hisn — Task Board

Orchestrator session started 2026-09-14. Request: "continue and test".
Phase 2 started 2026-09-14. Request: "improve the front end and the layout etc." — 10 agent sessions. Production UI files ARE in scope this phase.

## Tasks

| ID | Task | Status | Owner | Depends on | Files affected |
|----|------|--------|-------|------------|----------------|
| T1 | Desktop GoogleTest suite: run tests (spdlog via LD_LIBRARY_PATH) | [x] | orchestrator | — | none |
| T2 | Backend unit tests: write jest specs for blocklist/stats/guard, make `npx jest` pass | [x] | Agent A (backend-tests) | — | backend/src/**/*.spec.ts |
| T3 | Android unit tests: JUnit for pure-logic Kotlin classes, `testDebugUnitTest` green | [x] | Agent B (android-tests) | — | android/app/src/test/** |
| T4 | Website smoke check: serve `website/`, verify page + demo assets | [x] | orchestrator | — | none |
| T5 | Integration: review T2/T3 diffs, backend build, full test pipeline, commits | [x] | orchestrator | T1,T2,T3,T4 | commits only |

## Phase 2 — Frontend & Layout Improvements (F1–F10)

Started 2026-09-14. Ten parallel agent sessions. Board carving rules:
- **File-scope isolation is absolute** — every file is owned by exactly one task.
- **F1 (design tokens) owns ALL `android/app/src/main/res/values*/`** (colors, dimens, strings, themes, styles). It may only ADD tokens or edit existing values — never rename or delete (other agents may cite new tokens once F1 lands; they are also free to hardcode in their own layouts meanwhile).
- **F7 owns `UiAnim.kt`** (public API frozen: all existing method signatures must keep working) and must keep `container`, `bottomNav`, and report-bar view IDs stable.
- **F9 owns `res/drawable*/` (existing files)** — other agents create NEW drawables only with their task prefix (f3_, f6_, …) to avoid collisions.
- **F7 also owns `res/anim/`, `res/menu/bottom_nav.xml`, and `res/color/` + `res/color-night/` (nav selectors)** — motion + bottom-nav surfaces; other agents may *reference* existing anims/colors but not modify them; new anim files also need the creator's f#_ prefix.
- **F8 owns `res/font/` additively** — no renames (font references live in themes.xml, which is F1's file).
- **New user-facing copy**: F1 owns every strings.xml — agents write new Arabic copy **inline** in their own layouts (literal `android:text="…"`), never by editing strings.xml; hoisting to resources can happen at integration if the orchestrator chooses.
- **No agent runs a build** (10 parallel gradle runs would race on the daemon) — orchestrator runs `./gradlew assembleDebug testDebugUnitTest` once at integration. Syntax self-check = Read the edited XML fully after editing.
- **No agent commits** (D1). Handoff to `.ai/HANDOFFS/F<n>-<slug>.md` in the standard format.
- Website (single file `website/index.html`) is F10's surface.

| ID | Task | Status | Owner | Depends on | Files affected |
|----|------|--------|-------|------------|----------------|
| F1 | Design tokens pass: refine values.xml/dimens/themes/styles across light+night, ship new token set for all agents | [x] DONE — handoff F1-design-tokens.md | Agent F1 (design-tokens) | — | res/values*/ |
| F2 | Home screen deep-pass: hero/gate/hadith/stats/how-it-works/trust/footer layout & typography polish | [x] DONE — handoff F2-home-polish.md | Agent F2 (home-polish) | — | fragment_home.xml, HomeFragment.kt |
| F3 | Features screen redesign: cards grid, icons, copy alignment | [x] DONE — handoff F3-features-screen.md | Agent F3 (features-screen) | — | fragment_features.xml, FeaturesFragment.kt, new drawables f3_* |
| F4 | Settings screen: rows/sections/toggles spacing & hierarchy polish | [x] DONE — handoff F4-settings-screen.md | Agent F4 (settings-screen) | — | fragment_settings.xml, SettingsFragment.kt |
| F5 | Stats screen: heatmap + stats cards layout refresh | [x] DONE — handoff F5-stats-screen.md | Agent F5 (stats-screen) | — | fragment_stats.xml, StatsFragment.kt, HeatmapView.kt |
| F6 | PIN entry UX: PIN pads (activity_pin, activity_set_pin) & app-lock screen polish | [x] DONE — handoff F6-pin-ux.md | Agent F6 (pin-ux) | — | activity_pin.xml, activity_set_pin.xml, activity_app_lock.xml, PinActivity.kt, SetPinActivity.kt, AppLockActivity.kt, new drawables f6_* |
| F7 | App shell: MainActivity/bottom-nav/splash/welcome/report-bar; motion polish; UiAnim public API frozen | [x] DONE — handoff F7-app-shell.md | Agent F7 (app-shell) | — | activity_main.xml, bottom_nav.xml, activity_splash.xml, activity_welcome.xml, report_bar.xml, MainActivity.kt, SplashActivity.kt, WelcomeActivity.kt, UiAnim.kt, BaseActivity.kt |
| F8 | App typography & RTL audit-fix: fonts/weights/line-height/letterSpacing, margins mirrored, graph/GLSurfaceView RTL mirroring | [x] DONE — handoff F8-rtl-typography.md (audit-only, 0 file changes) | Agent F8 (rtl-typography) | — | fonts.xml (if exists), res/font/* only if renames needed, All layouts for RTL audit [read+report] |
| F9 | Drawable inventory cleanup: unify corner radii/paddings/stroke styles, orphan scan, NEW unified components (btn_*, card_*, chip_* families) | [x] DONE — handoff F9-drawable-cleanup.md | Agent F9 (drawable-cleanup) | — | res/drawable*/ (existing files only) |
| F10 | Website frontend v2: improve hero/demo/trust/footer layout, JS demo a11y & keyboard nav; single-file constraint | [x] DONE — handoff F10-website-v2.md | Agent F10 (website-v2) | — | website/index.html |

Integration (F-INT) status 2026-09-15:
- All F1–F9 handoffs verified + boarded. Integration build #3 GREEN (BUILD SUCCESSFUL 35s) + Android 77/77.
- Per-task commits landed: F1 `f01d375`, F2 `45f34b8`, F3 `c0822b4`, F4 `f21f523`, F5 `4c1fcd8`, F6 `44893ae`, F7 `19e3f04`, F9 `a45e48a` (F8 audit-only, no commit).
- Interpolator fix (framework PathInterpolator) landed inside F7's commit.
- Orphan prune (16 files, re-verified zero-ref) pending one `git rm` blocked by classifier — retry on timer.
- F10 SPAWNED (after ~29 classifier rejections); running on website/index.html.
- F10 DONE + orchestrator-verified (full-file read vs all checklist axes: single-file intact, tokens/brand hues kept, GitHub CTA 3×, dir=rtl+lang=ar, sessionStorage skip + reduced-motion blanket, single h1, skip-link, chips real buttons + aria-pressed + RTL arrow nav + live region, initial state matches JS, --mark-surface at all 3 call sites). website/index.html 421→667 lines.
- Remaining: prune commit (git rm 16 orphans; classifier retry loop) → .ai state + HANDOFFS + PROGRESS.md commit → F10 website commit → website smoke test → APK rebuild (validates prune) + `adb install -r` on user's phone (Realme RMX3760 detected via USB, 2026-09-15) → on-device full feature test → final report. Live tracker: PROGRESS.md at repo root.

## Notes

- Working tree was clean at `1be3139` (landing page v2 + work docs).
- Agents do NOT commit; orchestrator reviews diffs and commits per task to avoid index-lock races.
- Production code is off-limits for T2/T3 unless a real bug is found — then document it in the handoff.
