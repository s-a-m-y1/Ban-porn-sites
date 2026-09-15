# F5 — Stats Screen Refresh

## TASK ID
F5-stats-screen

## STATUS
DONE

## WHAT WAS IMPLEMENTED

### fragment_stats.xml (full layout redesign)
- Stat-card row: three weighted cards (weekly total / today / daily average), 26dp radius on ?attr/cardBg, spacing mirrored RTL-correctly via marginStart/marginEnd (no left/right anywhere).
- Card hierarchy: value on top (Plex Mono Semibold 24sp, ?attr/textPrimary, `android:textDirection="ltr"`, includeFontPadding=false) over label (Cairo Medium 11sp, ?attr/textSecondary, marginTop 4dp). Numbers render LTR inside RTL text.
- Weekly report card: `icon_circle_red` (F9's clay fix) + `ic_block` header, `reportBars` bar row, designed empty state `reportEmpty` (Cairo Regular 13sp, centered, textSecondary, 1.6 line spacing).
- Heatmap card: `icon_circle_green` + `ic_shield` header, `heatmapSelected` readout (Cairo Medium 12sp, textSecondary), 170dp HeatmapView, designed empty state `heatmapEmpty`.
- `reportCount` header counter REMOVED — the weekly total now lives in the first stat card (single source of truth); StatsFragment was updated to match (see below).

### HeatmapView.kt (visual polish only — public API unchanged)
- Public contract preserved exactly: class/package, `@JvmOverloads constructor(context, attrs)`, `Cell(day, count, week, dow)`, `selectedDay` (private set), `setData(Map<String,Int>, Boolean)`, `onCellClick((String,Int) -> Unit)`. Everything StatsFragment calls is identical.
- Rounded cells (radius 0.30 x cell), width-scaled gap clamped 3–10dp.
- Intensity ramps. Light: level-0 empty cell = subtle 12% teal wash (0x1E2F7A6B), then light-teal → teal → brass → clay. Dark: level-0 = raised surface, then deep teal → night teal → sand amber → night clay. No gray mush.
- Month header: Arabic month names (Cairo Medium 10sp, textSecondary) computed oldest→newest (`sortedBy { it.day }`) with direction-corrected anchors/alignment — fixes the old bug (newest-first iteration + LTR-only spacing check produced at most one misplaced label).
- Weekday labels: Arabic names (Cairo Regular 9sp, textSecondary) on the reading-start edge, sparse set on rows 1/3/5 (Mon/Wed/Fri: اثنين، أربعاء، جمعة) so they never crowd.
- RTL: one private `Grid` geometry helper shared by cell drawing, selection ring, and touch hit-test. The mirror (`layoutDirection == LAYOUT_DIRECTION_RTL`) happens once inside Grid, so canvas, ring, and taps can never drift apart. `performClick()` overridden for accessibility.
- Selection ring defaults to today (`selectedDay ?: todayKey`), so readout text and grid agree before the first tap.

### StatsFragment.kt (UI-layer only — data calls and behavior identical)
- `statTotalValue` bound via frozen `UiAnim.countUp` — replaces the dangling `R.id.reportCount` reference (id existed in no layout → guaranteed compile error).
- `statTodayValue`: yyyy-MM-dd Locale.US key lookup into the 7-day list, defaults 0.
- `statAverageValue`: total / 7 (integer division).
- Empty week (total == 0): `reportEmpty` visible AND `reportBars` hidden — the designed empty state, not seven minimum-height slivers.
- Heatmap: `allStats()` `List<Pair<String,Int>>` contract preserved via `.toMap()` → Map<String,Int>; `heatmapEmpty` visible iff all-time sum == 0; `heatmapSelected` re-synced to @string/heatmap_today after every `setData` (setData resets selection to today, so readout and ring always agree, including across tab switches).
- Bar inflation logic unchanged from the original (report_bar.xml used as-is, h*3 fill height, day label substring(8), oldest-first order).
- No `staggeredEntrance`: MainActivity shows/hides fragments so onResume fires on every tab return — reload quietly each time.

## FILES CHANGED
- /home/sami/App-bloking-sex/android/app/src/main/res/layout/fragment_stats.xml (rewritten)
- /home/sami/App-bloking-sex/android/app/src/main/java/com/contentfilter/app/HeatmapView.kt (rewritten)
- /home/sami/App-bloking-sex/android/app/src/main/java/com/contentfilter/app/StatsFragment.kt (rewritten)

No other files touched. No new f5_* drawables were needed (existing icon circles + bar_fill covered everything).

## IMPORTANT DECISIONS
- **reportCount removed**: the old header counter duplicated the weekly total the stat-card row now owns. Removing it simplifies hierarchy AND fixes the compile (StatsFragment referenced a nonexistent id).
- **Hardcoded 24sp stat values instead of @dimen/stat_value_size (32sp)**: 32sp overflows a three-across row. res/values* is F1's surface — never edited — so the override is inline in my layout with this rationale recorded here.
- **Inline Arabic copy** ("المتوسط اليومي" average-card label in fragment_stats.xml; monthNames/dowNames in HeatmapView.kt): res/values* is F1's, so new copy lives inline in my files per the ownership rules. Tradeoff accepted: English-locale devices see Arabic for these labels. If F1 later adds proper keys, there are exactly three sites to swap (fragment_stats.xml average label; HeatmapView monthNames; HeatmapView dowNames).
- **Month-label bug fix**: old code iterated Sundays newest-first with an LTR-only spacing check → at most one wrongly-placed label. Now Sundays are sorted oldest-first and spacing/anchors respect layoutDirection.
- **Shared Grid geometry**: draw, ring, and hit-test all derive from one Grid instance per measure, eliminating the classic "ring drawn here, tap lands there" drift in mirrored custom views.
- **heatmapSelected re-sync every reload**: without it, a returning tab could show a stale "day — N blocked" readout under a today-ring.
- **Low-activity ramp**: max <= 4 scales proportionally with minimum level 1, so light-activity periods still show visible color rather than a flat wash.
- **F9's icon_circle_red** (now clay) is referenced, not modified — existing drawables are F9's surface.

## TESTS RUN
No builds permitted (no ./gradlew) — verification is full-file reads plus cross-file contract checks:
- Full re-read of final fragment_stats.xml (298 lines): well-formed XML; every id StatsFragment references exists (statTotalValue / statTodayValue / statAverageValue / reportBars / reportEmpty / heatmap / heatmapSelected / heatmapEmpty); no reportCount anywhere; all @string keys verified present in values-ar/strings.xml.
- Full re-read of final HeatmapView.kt (251 lines): valid Kotlin structure; public signatures match the API StatsFragment calls; Arabic month/weekday strings intact.
- Full re-read of final StatsFragment.kt (100 lines): all imports used; smart-casts valid after `?: return@launch` guards; R.string.heatmap_today / heatmap_day_fmt / R.layout.report_bar / barFill / barLabel all resolve.
- Contract checks against read-only sources: BlocklistRepository.kt:80-84 (`dailyStats(7): List<DailyStat>`, `allStats(): List<Pair<String,Int>>`), report_bar.xml structure, frozen UiAnim.countUp signature, values-ar strings, dimens.

## TEST RESULTS
PASS at self-check level:
- XML well-formed; layout/fragment ids consistent; no dangling references.
- Kotlin reads as valid: imports all used, nullable guards correct, strings and fonts resolve.
- `allStats().toMap()` compiles against the confirmed List<Pair<String,Int>> return.
- NOT compiled or run — gradle builds are forbidden in this workstream; needs a smoke build at integration.

## KNOWN ISSUES
- English-locale devices see Arabic for the average-card label and heatmap month/weekday names (values* is F1's surface; inline Arabic was the sanctioned workaround).
- Stat values hardcoded at 24sp inline — if F1 later rescales stat_value_size (32sp), this one site should be revisited.
- Heatmap weekday labels are a fixed sparse set (rows 1/3/5) with no per-locale fallback.
- No build/runtime verification was possible here (orchestrator owns the integration build); findings are from full-file reads and cross-referencing only.

## NEXT DEPENDENCIES
- Orchestrator integration build: compile + smoke-run the stats tab in light and night — first actual runtime verification of these three files together.
- F1 (values): if proper string keys are added for the inline Arabic sites (three locations listed under IMPORTANT DECISIONS) and/or stat_value_size is revisited, swap them in.
- F8 (RTL audit): HeatmapView.kt was rewritten around a single Grid geometry helper — audit the new file, not earlier notes about the old implementation.
- F9: icon_circle_red clay change already referenced as-is; nothing further needed from F9 on this screen.
