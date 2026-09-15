# F2-home-polish Handoff

## TASK ID
F2-home-polish

## STATUS
DONE

## WHAT WAS IMPLEMENTED
Home screen deep polish pass on `fragment_home.xml` + `HomeFragment.kt` (UI layer only; all behavior/logic identical):

1. **Hierarchy — one Display-level focal point.** Greeting demoted 22sp cairo_extrabold → 16sp cairo_bold (ambient context, not a headline). How-it-works + trust titles demoted 20sp extrabold → 12sp hand-set green labels. Footer shrunk (24dp gate mark, 15sp wordmark). The previously split how-it-works header/steps containers merged into one `stepsCard` for a clean section rhythm. Result: the gate is the only heavy element on the page.
2. **The gate elevated.** New layered halo drawables `f2_halo_off` (3 stepped ink-alpha ovals + hairline rim) / `f2_halo_on` (3 teal-alpha layers + rim) swapped by setUiState. New `f2_btn_gate` ripple face (flat ink default, deepens + brighter rim + parchment ripple on press). Elevation 0dp — halo carries the accent, face sits flat. powerLabel bumped 14→15sp. Entrance `staggeredEntrance` now covers all six sections, hero leading.
3. **Hadith card quiet treatment.** Amiri + `@dimen/hadith_text_size` + lineSpacing 1.75 unchanged; margins balanced to 32dp start/end, 24dp top/bottom. Title 11sp, letter-spacing removed (was harming the Arabic half of the bilingual "خُطوة إلى النور · A step toward light"). Hadith CONTENT untouched (still `R.array.hadiths_ar` pool, rotation logic identical).
4. **Steps + trust + stats.** Steps rebuilt on the settings-screen idiom: `@style/IconCircle` + `icon_circle_green` + three NEW 20dp glyphs (`f2_ic_step_tap` scan `f2_ic_step_shield`), 14dp text gap, dividers `marginStart 56dp` so they align with the text column (24 padding + 42 icon + 14 gap = 80dp text start). All spacing snapped to 4/8/12/16/24 grid (14→12 row padding, 28→24 container padding, 52→48 divider height, 18→16/10→8/2→4 micro gaps). statsCard: Plex Mono numbers preserved (`@dimen/stat_value_size`, 32sp day / 22sp land), `blockedCountText` in clay — the strong number treatment.
5. **HomeFragment.kt UI polish only.** Two coordinated edits (same change as the layout rewrite, so no ID ever dangled): (a) setUiState halo swap → f2_halo_on/f2_halo_off; (b) onViewCreated pressable(powerButton, emailButton) + six-section staggeredEntrance. UiAnim frozen API used exclusively (pressable / staggeredEntrance / breathe / countUp). No logic touched.
6. **Light + night.** Every color via `?attr/` tokens or `@color/` (teal/clay/brass/stone/parchment/hadith_card_bg/green_light — all DayNight-switching) except deliberate ink literals inside the two gate drawables (ink face + parchment rim — same in both themes by design).

## FILES CHANGED
- `res/layout/fragment_home.xml` — full rewrite (527 lines). 5 new section IDs: `hadithCard`, `statsCard`, `stepsCard`, `trustCard`, `footerCard` (all LinearLayout, matching the Kotlin `findViewById<LinearLayout>` choreography). All previously bound IDs preserved.
- `java/com/contentfilter/app/HomeFragment.kt` — 2 edits (setUiState halo swap; onViewCreated entrance choreography). Everything else verbatim.
- NEW `res/drawable/f2_halo_off.xml` — 3 stepped ink-alpha ovals (#0D/#14/#1E26374A) + 1dp #3326374A rim at 10dp inset.
- NEW `res/drawable/f2_halo_on.xml` — 3 teal-alpha ovals (#12/#1E/#2E2F7A6B) + #4D2F7A6B rim.
- NEW `res/drawable/f2_btn_gate.xml` — ripple #40F2EFE9 over selector (pressed #16242F + 1dp #4DF2EFE9 rim; default #22333F + 1dp #2EF2EFE9 rim).
- NEW `res/drawable/f2_ic_step_tap.xml` / `f2_ic_step_scan.xml` / `f2_ic_step_shield.xml` — 20dp teal-stroke step glyphs.

## IMPORTANT DECISIONS
- **Hand-set demoted titles, not the SectionHeader style.** SectionHeader (12sp bold caps ls 0.10) exists but was unused; textAllCaps + wide tracking damage Arabic script. Demoted titles are hand-set: 12sp bold `@color/green_light` `@font/cairo_bold`, no caps, no tracking. (The style itself is F1's — untouched.)
- **Letter-spacing removed from Arabic-rendering text.** journeyChip (was 0.04) and hadithTitle (was 0.08) render Arabic (`hadith_card_title` is the bilingual "خُطوة إلى النور · A step toward light" from values/hadiths.xml:107 — Arabic-first in both locales). Latin-only footer wordmark keeps ls 0.35.
- **How-it-works containers merged.** The split header/body/steps containers became one stepsCard; simpler section rhythm, cleaner Kotlin binding (one ID instead of an ad-hoc entrance list).
- **Divider alignment math.** marginStart 56dp = IconCircle 42dp + 14dp gap (measured from container content edge) → divider start == text column start (80dp from screen edge).
- **Superseded drawables left on disk.** `power_bg_on`, `power_bg_off`, `btn_gate` are no longer referenced by any layout but exist for F9's inventory/cleanup. All existing drawables untouched.
- **Ink-literal gate face.** The two gate drawables intentionally hardcode ink/parchment hex (same values in light + night) — the ink face is the one place where a deliberate single-look commitment reads as intent, and it avoids relying on tokens F1 may still be moving.
- **values-land compact variants still honored.** All landscape tuning flows through the same `@dimen` tokens (gate_halo 140dp, gate_button 112dp, stat_value 22sp, hadith_text 14sp, etc.), so the rewrite inherits it automatically.

## TESTS RUN (self-checks; no builds per hard rule)
1. Read full `fragment_home.xml` post-write — XML well-formed, all IDs bound by Kotlin present, all @string/@color/@drawable/@dimen/@style refs resolve to pre-existing or my f2_ resources, Arabic tools:text intact.
2. Read full `HomeFragment.kt` post-edit — valid Kotlin, both edits in place, LinearLayout import present, all behavior preserved (PIN reqCode 3, VpnService.prepare reqCode 1, hadith rotation, countUp, overlay dialog reqCode 8, onResume state sync).
3. Read all 6 new f2_ drawables — well-formed XML (layer-list/ripple/selector/vector), balanced tags, valid hex alphas, @color/teal strokes auto day/night.

## TEST RESULTS
All 3 self-checks PASS. No build/gradle verification performed (hard rule: no builds — parallel runs race).

## KNOWN ISSUES
- `@color/hadith_card_title` was resolving to different values/line numbers across two greps minutes apart (#B8DCD3C6 at colors.xml:57, then #F4F1EA at colors.xml:67) — F1 is concurrently editing colors.xml. My layout references the token, so it tracks F1's final resolution; verify at integration that the title color has adequate contrast on `hadith_card_bg` (#2F7A6B both themes).
- `power_bg_on` / `power_bg_off` / `btn_gate` are now unreferenced (superseded by f2_halo_* / f2_btn_gate) — flagged for F9's cleanup pass, not deleted by me (existing drawable files are F9/F7-owned).
- Build not verified per the no-gradlew rule; the orchestrator's integration build is the first real compile check.

## NEXT DEPENDENCIES
- Orchestrator integration (both files are self-consistent as a pair; IDs and Kotlin were updated in the same change).
- F1 token finalization — hadith_card_title color (see KNOWN ISSUES).
- F9 inventory of superseded drawables (power_bg_on/off, btn_gate).
