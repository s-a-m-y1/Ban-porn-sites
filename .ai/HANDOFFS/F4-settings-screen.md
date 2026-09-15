# F4 — Settings screen hierarchy & polish

## TASK ID
F4-settings-screen

## STATUS
DONE

## WHAT WAS IMPLEMENTED
Rebuilt fragment_settings.xml from a flat 6-row card into three labeled
sections over separate card surfaces, and added UI-layer-only polish to
SettingsFragment.kt. Behavior is identical; all 13 view IDs are unchanged.

Layout (fragment_settings.xml, full rewrite):
- Three sections with Arabic titles styled @style/SectionHeaderAr,
  placed ABOVE their cards (same pattern as fragment_features.xml):
    - التطبيق (App): language, dark mode
    - الحماية (Protection): PIN lock, change PIN, uninstall protection
    - الشبكة (Network): autostart, always-on VPN
  Section titles are new copy, inline android:text in my layout only
  (per task rule — existing security_title/protection_title strings were
  checked and rejected: wrong semantics).
- Every row now carries the fleet icon pattern: 42dp IconCircle frame
  (12%-tint circle drawable) with a 20dp white vector centered:
    langRow ic_lang@green, themeRow f4_ic_moon@purple,
    rowPin f4_ic_lock@green, rowChangePin f4_ic_key@f4_icon_circle_brass,
    rowAdmin ic_shield@f4_icon_circle_clay, rowAutostart ic_power@green,
    rowAlwaysOn ic_gate@purple.
- All five switches themed via new f4_switch_thumb / f4_switch_track
  state drawables built on theme tokens (?attr/accentColor track when
  checked / ?attr/strokeColor when off; ?attr/onAccent knob when
  checked / ?attr/textSecondary when off). SwitchCompat IGNORES platform
  thumbTint/trackTint, so custom thumb/track drawables referenced via
  android:thumb / android:track is the reliable route (minSdk 24 allows
  theme attrs inside drawables — precedent: card_bg.xml uses ?attr/cardBg).
  On/off is unambiguous in both light and night; knob is 16dp in a
  20dp box (2dp optical inset).
- RTL-correct alignment throughout: label blocks weight=1 with
  layout_marginStart (start-aligned), switches marginStart 8dp
  (end-packed). Chevrons on both action rows (langRow, rowChangePin)
  use android:autoMirrored="true" so they point with travel direction.
- Dividers between rows inside a section: 1dp ?attr/dividerColor,
  inset 74dp to align with the label column (18 row pad + 42 icon +
  14 gap; the old file had 68dp — fixed). Gaps between sections:
  24dp marginTop on 2nd/3rd headers, 8dp header-to-card.
- Spacing rhythm on the 4/8/12/16/24 grid; consistent 20dp screen-side
  padding (old file had an extra 20dp marginStart on the title —
  double-indent removed); sublabels unified on @style/SettingsSublabel
  (old file styled them ad-hoc with inline 12sp/textSecondary attrs).
- Color semantics: destructive rowAdmin icon circle is clay-tinted
  (f4_icon_circle_clay over @color/clay_12); the change-PIN action
  label keeps its primary teal (@color/teal); langValue stays teal.
  NOTE: existing icon_circle_red was avoided — despite the name it is
  teal-tinted; ic_globe avoided too (it is a house shape — ic_lang is
  the globe).
- rowChangePin ordering trick: default android:visibility="gone", with
  the divider placed AFTER it — SettingsFragment sets it VISIBLE when
  PinManager.isPinSet, and hiding it still leaves rowPin | divider |
  rowAdmin. rowChangePin sits between rowPin and the divider so the
  visible structure is correct in both states.

Kotlin (SettingsFragment.kt, one addition at end of onViewCreated):
- UiAnim.pressable(...) on all 7 rows (langRow, themeRow, rowPin,
  changePinRow via the existing local val, rowAdmin, rowAutostart,
  rowAlwaysOn) — visual-only touch scale; UiAnim.pressable returns
  false so row clicks still fire. Frozen existing helper, same package
  (no import needed), HomeFragment precedent. Nothing else touched.

## FILES CHANGED
- /home/sami/App-bloking-sex/android/app/src/main/res/layout/fragment_settings.xml (rewritten, 256 lines)
- /home/sami/App-bloking-sex/android/app/src/main/java/com/contentfilter/app/SettingsFragment.kt (one block added in onViewCreated)
- /home/sami/App-bloking-sex/android/app/src/main/res/drawable/f4_switch_track.xml (new)
- /home/sami/App-bloking-sex/android/app/src/main/res/drawable/f4_switch_thumb.xml (new)
- /home/sami/App-bloking-sex/android/app/src/main/res/drawable/f4_ic_moon.xml (new)
- /home/sami/App-bloking-sex/android/app/src/main/res/drawable/f4_ic_lock.xml (new)
- /home/sami/App-bloking-sex/android/app/src/main/res/drawable/f4_ic_key.xml (new)
- /home/sami/App-bloking-sex/android/app/src/main/res/drawable/f4_ic_chevron.xml (new)
- /home/sami/App-bloking-sex/android/app/src/main/res/drawable/f4_icon_circle_clay.xml (new)
- /home/sami/App-bloking-sex/android/app/src/main/res/drawable/f4_icon_circle_brass.xml (new)

All new drawables are NEW files prefixed f4_ (no existing drawable
touched). res/values*/ untouched (F1's). No builds, no commits, no
staging — working tree only, orchestrator integrates.

## IMPORTANT DECISIONS
1. SectionHeaderAr cross-dependency on F1: my three section titles
   use @style/SectionHeaderAr, which F1 added to values/themes.xml
   mid-flight (Arabic-correct: textAllCaps=false, letterSpacing=0,
   lineSpacingMultiplier 1.3, @color/green_light, 12sp bold). Verified
   present in F1's current themes.xml (lines 61-71). IF F1's pass is
   reverted, my section titles break at build time — fallback: replace
   the three style refs with the equivalent inline attributes on the
   TextViews (12sp / bold / @color/green_light / lineSpacingMultiplier
   1.3). Existing security_title ("Security"/"الأمان") and
   protection_title ("Protection Status"/"حالة الحماية") strings were
   checked and NOT reused — neither fits semantically.
2. SwitchCompat theming route: android:thumbTint/trackTint are platform
   attrs that SwitchCompat ignores, so I built state-list drawables
   over theme tokens and wired them via android:thumb/track (which
   SwitchCompat honors). Theme-attr references inside drawables are
   resolved at minSdk 24 — safe here, precedent card_bg.xml.
   Checked ON: teal track + onAccent knob (white light / ink night —
   the app's canonical pairing). Checked OFF: strokeColor track +
   textSecondary knob — reads clearly on white (light) and #22344A
   (night) cards.
3. Divider alignment fixed from 68dp to 74dp (card padding 8 + row
   padStart 18 + icon 42 + label gap 14 = 82dp to label text; 74dp
   aligns the divider with the ICON CIRCLE's trailing edge → the
   label column start, matching Material inset-divider convention).
4. rowChangePin kept at visibility="gone" by default with the divider
   AFTER it, preserving rowPin | divider | rowAdmin when it hides —
   SettingsFragment's existing visibility logic is untouched and keeps
   working.
5. Destructive semantics: rowAdmin (device-admin / uninstall
   protection) reads clay via f4_icon_circle_clay. The existing
   icon_circle_red was rejected — it is teal-tinted despite the name.
   rowChangePin uses a NEW brass circle (f4_icon_circle_brass over
   brass_12) — a credential action, not destructive, not primary-teal.
6. Kotlin changes are strictly UI-layer: one UiAnim.pressable call on
   the 7 rows. No behavior, listeners, prefs keys, or request codes
   touched — verified by full-file read against the pre-edit content.
7. Naming traps documented for the fleet: ic_globe is a HOUSE (ic_lang
   is the globe); icon_circle_red is teal-tinted; chip_teal is
   stroke-only transparent.

## TESTS RUN
No builds allowed (task rule: no ./gradlew). Self-checks performed:
1. Full Read of the rewritten fragment_settings.xml (256 lines) —
   well-formed XML, all 13 view IDs present and each used exactly
   once (langRow, langValue, themeRow, themeSwitch, rowPin, pinSwitch,
   rowChangePin, rowAdmin, adminSwitch, rowAutostart, autostartSwitch,
   rowAlwaysOn, alwaysOnSwitch), three Arabic section titles intact
   (التطبيق / الحماية / الشبكة), rowChangePin default gone, divider
   placement after it, teal on pin_change_title and langValue, chevron
   autoMirrored on both action rows, switches end-aligned with
   marginStart 8dp.
2. Full Read of SettingsFragment.kt (226 lines) — valid Kotlin
   end-to-end, pressable block at lines 49-57 with all 7 rows and the
   changePinRow local reuse, braces balanced, no imports changed, all
   behavior branches byte-identical to pre-edit (setupLanguage /
   setupTheme / setupSecurity / onActivityResult untouched).
3. xmllint --noout on fragment_settings.xml and all 8 f4_ drawables —
   all well-formed.
4. ID census via grep: each of the 13 IDs exactly once.
5. RTL audit via grep for marginLeft/marginRight/paddingLeft/
   paddingRight/left=/right= — zero hits (start/end only).
6. Drawable existence sweep: all 15 referenced drawables exist on disk
   (card_bg, icon_circle_green, icon_circle_purple, ic_lang, ic_shield,
   ic_power, ic_gate + the 8 f4_ files).
7. Cross-screen consistency check: fragment_features.xml places its
   SectionHeader above its card with marginBottom — my SectionHeaderAr
   titles match that placement pattern.

## TEST RESULTS
All self-checks passed:
- xmllint: LAYOUT_OK + DRAWABLES_OK (fragment_settings.xml and all 8
  f4_ drawables well-formed)
- ID census: 13/13 IDs, each exactly once (no accidental dupes)
- RTL audit: zero left/right-directional hits
- Drawable existence: 15/15 present, no dangling @drawable refs
- Full-file Reads verified structure, Arabic intact, valid Kotlin,
  behavior-identical Kotlin (pressable only)
No compile or runtime verification was possible (builds forbidden by
task rule) — the orchestrator's integration build is the first real
compile gate.

## KNOWN ISSUES
1. SectionHeaderAr dependency: if F1's themes.xml pass is reverted,
   my three section titles fail to resolve the style at build time.
   Fallback documented in IMPORTANT DECISIONS #1 (inline attrs).
2. f4_switch_thumb/track rely on SwitchCompat honoring android:thumb /
   android:track — verified by AOSP SwitchCompat source semantics;
   recommend a quick visual check in the integration build (both
   themes) since I could not compile.
3. Divider inset 74dp is tuned to the current SettingsRow padding
   (18dp) + IconCircle (42dp) + label gap (14dp). If F1 later changes
   SettingsRow/IconCircle metrics, the three dividers need the same
   retune (74 = 8 card + 18 + 42 + 14 - 8 card pad; direct child of
   the card so card padding shifts it).
4. langValue text is set from Kotlin on every onViewCreated — its
   teal color is layout-side (android:textColor), so the color survives
   the recreate() on language change.
5. No way to visually verify light/night rendering without a build;
   both modes were reasoned from the token table (accentColor teal in
   both themes, onAccent #FFFFFF light / ink night, strokeColor
   #E4DDCF light / #3A4E62 night, textSecondary #26374A light /
   #DCD3C6 night — all combinations give ≥ 2:1 contrast on their
   surfaces).

## NEXT DEPENDENCIES
1. F1 (themes.xml owner): SectionHeaderAr must survive F1's final
   pass — see KNOWN ISSUES #1. Verified present as of this handoff.
2. Orchestrator integration build: first compile gate for my layout +
   drawables + Kotlin (I could not build per task rule). Watch for
   AAPT errors on ?attr refs inside the f4_ drawables (should be clean
   at minSdk 24) and SwitchCompat thumb/track rendering.
3. F9/F7 own existing drawables/anims — I did not touch any; my 8
   f4_ files are additive only, no rename collisions with F3's
   f3_ic_lock.
4. If a later pass wants section headers tappable (collapse/expand) or
   additional sections, the three titles are plain TextViews with no
   IDs — trivial to promote.
