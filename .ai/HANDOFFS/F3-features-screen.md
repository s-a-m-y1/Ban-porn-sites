# F3 — Features Screen Redesign

## TASK ID

F3 (features-screen) — Features tab redesign: layout rewrite, coherent icon
set, card/elevation treatment, RTL-safe Arabic typography, UI-layer-only
Kotlin polish.

## STATUS

DONE

## WHAT WAS IMPLEMENTED

**Presentation paradigm (fragment_features.xml, full rewrite):**
- Single-column vertical list inside a ScrollView (fillViewport, no
  scrollbars). A 2-column card grid was evaluated and rejected: Arabic RTL
  reading favors a single vertical rhythm, and a grid would mirror awkwardly
  and crowd the 42dp icon + title + switch rows.
- Three card containers on `?attr/cardBg` via the existing `@drawable/card_bg`
  (24dp radius): categories+manual (f3CardCategories), schedule
  (f3CardSchedule), app lock (f3CardAppLock). 16dp internal card padding,
  14dp between cards.
- Screen keeps the Display title (`tab_features`) and gains a SectionHeader
  (`features_title`, new id f3Header) mirroring the settings-screen idiom.
- Seven icon-led rows: 42dp IconCircle (tinted oval background) + 20dp vector
  icon + title (Cairo, SettingsLabel, `?attr/textPrimary`) + control at end.
  Rows use `?android:attr/selectableItemBackground` with 12dp vertical
  padding — full-width touch targets preserved.
- Rows with existing description strings (manual, schedule, applock) get
  title+description (SettingsSublabel) at `lineSpacingMultiplier 1.4` for
  Arabic line-height. Category rows are title-only (no description strings
  exist — see decisions).
- Dividers: 1dp `?attr/dividerColor`, inset 56dp from start (42dp circle +
  14dp gap) so they align with the text column.
- manualCount upgraded from plain 13sp text to a chip_green pill (Chip style,
  `@color/teal` text, gravity center). Kotlin only sets `.text` — identical
  behavior.
- Schedule times panel (scheduleTimesRow) is the screen's single elevated
  moment: `@drawable/f3_card_elevated` on `?attr/cardBgElevated`, 16dp
  corners, 12dp padding, two 44dp btn_primary buttons (Cairo, 13sp bold,
  `?attr/onAccent`). Visibility still GONE by default; Kotlin toggles it.
- All 18 Kotlin-bound IDs preserved verbatim with identical view types
  (rowPorn, switchPorn, rowGambling, switchGambling, rowFakenews,
  switchFakenews, rowMalware, switchMalware, rowManual, manualCount,
  rowSchedule, scheduleSwitch, scheduleValue, scheduleTimesRow,
  btnStartTime, btnEndTime, rowApplock, applockSwitch, plus applockDesc).
  Four new non-bound IDs added as animation hooks: f3Header,
  f3CardCategories, f3CardSchedule, f3CardAppLock.

**Icon set (7 new vectors, one visual weight):**
24dp size / 24x24 viewport, stroke width 1.8, round caps/joins, filled
accent dots where emphasis helps. Explicit palette colors in
fill/strokeColor so icons stay visible on the 12% tinted circles and adapt
to night automatically (teal/clay/brass have values-night overrides).
- f3_ic_shield_alert (teal) — porn category
- f3_ic_bug (teal) — malware
- f3_ic_lock (teal) — app lock
- f3_ic_dice (clay, 3 filled pips) — gambling
- f3_ic_news (brass, filled headline bar) — fakenews
- f3_ic_edit_list (clay, 3 filled bullets) — manual blocklist
- f3_ic_clock (brass) — schedule
Color rotation teal → clay → brass so no two adjacent rows share a tint:
porn=teal, gambling=clay, fakenews=brass, malware=teal, manual=clay,
schedule=brass, applock=teal.

**Circle backgrounds:** teal rows reuse the existing
`@drawable/icon_circle_green` (green_12 is the teal tint). New ovals:
f3_circle_clay (@color/clay_12), f3_circle_brass (@color/brass_12). No
colors.xml changes anywhere.

**Kotlin (FeaturesFragment.kt, UI-layer only — zero behavior change):**
- `UiAnim.staggeredEntrance(f3Header, f3CardCategories, f3CardSchedule,
  f3CardAppLock)` in onViewCreated — existing frozen-API helper, re-call
  safe.
- `UiAnim.pressable(...)` on all interactive rows: the four category rows
  (inside bind()), rowManual, rowSchedule, rowApplock, and both time
  buttons. pressable's touch listener returns false so clicks/long-clicks
  still fire.
- No other logic touched: every coroutine, dialog, permission branch, alarm
  call, and prefs key is byte-identical.

## FILES CHANGED

Edited (my exclusive files):
- android/app/src/main/res/layout/fragment_features.xml (full rewrite)
- android/app/src/main/java/com/contentfilter/app/FeaturesFragment.kt
  (6 additive edits, ~15 lines)

New drawables (all f3_-prefixed, res/drawable/):
- f3_ic_shield_alert.xml, f3_ic_bug.xml, f3_ic_lock.xml, f3_ic_dice.xml,
  f3_ic_news.xml, f3_ic_edit_list.xml, f3_ic_clock.xml
- f3_circle_clay.xml, f3_circle_brass.xml
- f3_card_elevated.xml

Not touched (per hard rules): res/values*/, existing drawables/anims,
UiAnim.kt, any other agent's files.

## IMPORTANT DECISIONS

1. **Single-column over 2-column grid** — Arabic RTL reading is vertical;
   rows map to full-width switch/action targets; a grid would mirror badly
   and crowd. Locked early, implemented throughout.
2. **Zero new strings** — every label resolves via existing @string
   references verified present in BOTH values/ and values-ar/ (scripted
   check). Category rows are title-only because no description strings
   exist in either locale; inline copy would break the EN/AR locale switch.
3. **Explicit palette colors in icons** — the app's existing icons
   (ic_lang etc.) are white-filled with no tint and would be invisible on
   12% tinted circles. @color/teal / clay / brass carry values-night
   overrides, so f3_* icons adapt to dark mode with no drawable-night
   directory needed.
4. **Two-tier radius** — 24dp container cards (card_bg idiom) vs 16dp
   nested elevated panel (matches chip_green / card_outlined sub-surface
   idiom). Deliberate hierarchy, not inconsistency.
5. **First consumer of `?attr/cardBgElevated`** — the attr existed but no
   layout used it. F1/orchestrator should sanity-check the token now that
   it renders (light mode: cardBg and cardBgElevated are both #FFFFFF, so
   f3_card_elevated adds a 1dp `?attr/strokeColor` outline to keep the
   panel delineated).
6. **Elevation strategy** — one raised moment only (schedule times panel)
   rather than per-card elevation; container cards sit flat on cardBg.
7. **56dp divider inset** — 42dp circle + 14dp gap; dividers align with
   the text column (old layout used 68dp under different geometry).
8. **Card padding 16dp vs SettingsRow's 18dp idiom** — task spec says
   16dp; rows are custom LinearLayouts (not SettingsRow style) so the card
   keeps 16dp while rows keep full-width touch targets and 12dp vertical
   rhythm (equivalent visual density to SettingsRow's 15dp inside its 8dp
   card).
9. **Cairo for all Arabic text, never plex** — F8's audit: plex_semibold
   has zero Arabic glyphs. Titles and the count chip use Cairo; numeric
   count text inherits Cairo too.
10. **manualCount as chip pill** — presentation upgrade only; Kotlin still
    assigns `.text` ("+" or the count), so behavior is identical.

## TESTS RUN (self-checks)

- Full re-read (Read tool) of both edited files and all 11 new/edited XML
  files after writing — well-formed, Arabic intact, no truncation.
- Scripted XML parse (python3 ElementTree) of all 11 XML files — only
  ./gradlew builds are forbidden; this is a parse check.
- Scripted cross-check: all 14 @string names referenced by the layout exist
  in res/values/strings.xml AND res/values-ar/strings.xml.
- Scripted cross-check: all 14 @drawable references resolve to files in
  res/drawable/.
- ID cross-check: all 19 Kotlin-bound IDs (18 + applockDesc) present in the
  rewritten layout with unchanged view types.
- UiAnim frozen-API grep: pressable / staggeredEntrance signatures match
  my call sites; 6 call sites present; same package (no import needed).

No ./gradlew builds, no commits/staging (per hard rules).

## TEST RESULTS

ALL PASS:
- XML parse: 11/11 files well-formed.
- Strings: 14/14 present in values/, 14/14 in values-ar/ — locale switch
  intact, zero new copy.
- Drawables: 14/14 references resolve.
- IDs: 19/19 Kotlin-bound IDs preserved with matching view types.
- UiAnim: signatures verified (pressable(vararg View), staggeredEntrance(
  vararg View, startDelayMs: Long = 60)); call sites type-correct.

## KNOWN ISSUES

- SectionHeader (f3Header) relies on @color/green_light, which F1 owns —
  night-mode appearance not verified from here.
- SwitchCompat rows are unstyled and rely on the theme colorAccent
  (teal_accent) — expected correct in both modes but needs a visual check
  at integration.
- staggeredEntrance animates the three cards; scheduleTimesRow starts
  GONE and is revealed by refreshScheduleUi, so it does not participate in
  the entrance pass (intentional — it appears per switch state).
- Light-mode elevation subtlety: cardBg and cardBgElevated are both white;
  the 1dp stroke carries the distinction. If F1 later differentiates the
  surfaces by color, the stroke can be reconsidered (it is harmless).
- No build/visual verification possible in this environment (builds
  forbidden) — runtime rendering untested.

## NEXT DEPENDENCIES

- Orchestrator: integrate, build, and visually verify light + night, RTL
  (Arabic locale) and LTR.
- F1 (design tokens): confirm ?attr/cardBgElevated renders as intended
  (first consumer, f3_card_elevated.xml) and that green_light SectionHeader
  holds up at night.
- F7 (shell motion): staggeredEntrance is used here per the frozen API; no
  coordination needed, but noting the features tab now animates its cards
  on entry, consistent with HomeFragment.
- No blocked dependencies; F3 is self-contained and complete.
