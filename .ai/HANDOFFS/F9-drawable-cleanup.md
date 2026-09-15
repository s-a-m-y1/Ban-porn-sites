# F9 — Drawable cleanup & unification

## TASK ID
F9-drawable-cleanup

## STATUS
DONE

## WHAT WAS IMPLEMENTED
Full inventory of all 45 XML drawables in res/drawable/ (drawable-xhdpi/
is EMPTY — no bitmaps exist, so part 7 is a null report), followed by an
in-place unification of every shape/ripple onto one radius scale, one stroke
treatment, and one ripple convention; two semantic color bugs fixed
(chip_red and icon_circle_red both rendered green); one off-brand color
fixed (ripple_soft used Android material green #4CAF50); the 4 bottom-nav
tab icons normalized to the app-wide #FFFFFFFF fill convention; and a new
unified f9_* family of 7 drawables created (buttons primary/secondary/ghost,
cards surface/elevated, chips bg/selected). No file was renamed, deleted,
or had its semantic role changed; layouts, Kotlin, values/, and all other
agents' files were never touched.

### Full drawable inventory (45 files, after unification)

| name | type | radius | stroke | usage |
|---|---|---|---|---|
| bar_fill | shape rect | 8dp (was 3) | — | layout/report_bar.xml |
| btn_danger | ripple+selector | 8dp (was 4) | — | ORPHAN |
| btn_gate | ripple+selector (ovals) | n/a | — | layout/fragment_home.xml |
| btn_primary | ripple+shape | 16dp (was 14) | — | activity_pin, activity_app_lock, fragment_home, activity_welcome, activity_set_pin, fragment_features |
| card_bg | shape | 24dp (was 26) | — | fragment_stats, fragment_settings, fragment_features |
| card_outlined | shape | 16dp | 1dp ?attr/strokeColor (was 1.5dp) | ORPHAN |
| chip_green | shape | 16dp (was 14) | 1dp #332F7A6B | ORPHAN |
| chip_red | shape | 16dp (was 14) | 1dp #33B4573E (solid now @color/clay_12 — was green) | ORPHAN |
| chip_teal | shape | 16dp (was 14) | 1dp #33DCD3C6 | ORPHAN |
| hero_card | shape rect | none (full-bleed) | — | fragment_home.xml |
| ic_block | vector 24dp | — | — | fragment_stats.xml |
| ic_gate | vector 200dp, evenOdd | — | — | activity_app_lock, activity_welcome, fragment_home |
| ic_globe | vector 24dp #FFFFFF | — | — | ORPHAN |
| ic_lang | vector 24dp #FFFFFF | — | — | fragment_settings.xml |
| ic_launcher_background | vector 108dp (gradient) | — | — | mipmap-anydpi-v26 adaptive (internal) |
| ic_launcher_foreground | vector 108dp, viewport 200, scale 0.5 | — | — | mipmap-anydpi-v26 adaptive (internal) |
| ic_mail | vector 24dp #FFFFFF | — | — | ORPHAN |
| ic_notif_transparent | vector 24dp, transparent | — | — | java/FilterVpnService.kt |
| ic_power | vector 48dp (viewport 24), white strokes | — | — | ORPHAN |
| ic_shield | vector 24dp #FFFFFF | — | — | activity_pin, activity_set_pin, fragment_settings |
| ic_splash_gate | vector 200dp, named paths | — | 6 #F2EFE9 | ORPHAN |
| ic_splash_outline | vector 200dp, evenOdd, named paths | — | 2.5 #F2EFE9 | SplashActivity.kt (via splash_gate_avd) |
| ic_splash_slit | vector 200dp #16242F | — | — | activity_splash.xml |
| ic_splash_solid | vector 200dp #F2EFE9 | — | — | activity_splash.xml |
| ic_tab_features | vector 24dp, fill now #FFFFFFFF (was #FF000000) | — | — | menu/bottom_nav.xml |
| ic_tab_home | vector 24dp, fill now #FFFFFFFF | — | — | menu/bottom_nav.xml |
| ic_tab_settings | vector 24dp, fill now #FFFFFFFF | — | — | menu/bottom_nav.xml |
| ic_tab_stats | vector 24dp, fill now #FFFFFFFF | — | — | menu/bottom_nav.xml |
| icon_circle_green | oval @color/green_12 | — | — | activity_pin, activity_set_pin, fragment_settings |
| icon_circle_purple | oval #1E26374A | — | — | fragment_settings.xml |
| icon_circle_red | oval @color/clay_12 (was green_12 — bug) | — | — | fragment_stats.xml |
| journey_chip | shape | 40dp pill-via-clamp | 1dp #33B8863B (was #55) | fragment_home.xml |
| pin_dot_empty | oval 16dp | — | 2dp ?attr/strokeColor | activity_pin.xml + PinActivity.kt |
| pin_dot_error | oval @color/clay 16dp | — | — | PinActivity.kt |
| pin_dot_filled | oval ?attr/textPrimary 16dp | — | — | PinActivity.kt |
| pin_dot_success | oval @color/teal 16dp | — | — | PinActivity.kt |
| power_bg_off | oval #1E26374A | — | 2dp ring #3326374A | fragment_home.xml + HomeFragment.kt |
| power_bg_on | oval @color/green_12 | — | 2dp ring #332F7A6B | HomeFragment.kt |
| ripple_mask | shape 12dp, transparent | 12dp | — | internal-only (used by ripple_soft) |
| ripple_soft | ripple #332F7A6B (was #294CAF50 Android green) + mask | 12 | — | ORPHAN |
| splash_bg | layer-list ink | — | — | activity_splash.xml |
| splash_gate_avd | animated-vector → ic_splash_outline | — | — | SplashActivity.kt |
| splash_line | shape @color/teal | 8dp (was 2) | — | ORPHAN |
| status_dot_off | oval ?attr/textSecondary | — | — | fragment_home.xml + HomeFragment.kt |
| status_dot_on | oval @color/brass | — | — | HomeFragment.kt |

(Note: other agents have since added f2_*/f3_* etc. drawables — those are
out of F9 scope and untouched.)

## FILES CHANGED
All under /home/sami/App-bloking-sex/android/app/src/main/res/drawable/ —
17 files edited in place:
- bar_fill.xml (radius 3→8dp)
- btn_danger.xml (radius 4→8dp in both selector states)
- btn_gate.xml (ripple alpha #26F2EFE9→#33F2EFE9, 15%→20%)
- btn_primary.xml (radius 14→16dp; ripple #33000000→#33F2EFE9 — was the
  only black ripple in the app; comment updated)
- card_bg.xml (radius 26→24dp)
- card_outlined.xml (stroke 1.5dp→1dp)
- chip_green.xml (radius 14→16dp)
- chip_red.xml (radius 14→16dp; solid @color/green_12→@color/clay_12 and
  stroke #332F7A6B→#33B4573E — semantic bug fix, "red" now renders clay)
- chip_teal.xml (radius 14→16dp)
- icon_circle_red.xml (@color/green_12→@color/clay_12 — semantic bug fix)
- journey_chip.xml (stroke #55B8863B→#33B8863B, 33%→20% alpha)
- ripple_soft.xml (ripple #294CAF50→#332F7A6B — off-brand Android material
  green replaced with brand teal at 20% alpha)
- splash_line.xml (radius 2→8dp — renders identically, Android clamps to
  half the 6dp-tall bar)
- ic_tab_home.xml, ic_tab_stats.xml, ic_tab_features.xml,
  ic_tab_settings.xml (fillColor #FF000000→#FFFFFFFF — visually identical
  under the nav's SRC_IN itemIconTint, consistent with every other UI icon)

7 NEW files (f9_ family — no existing name overwritten):
- f9_btn_primary.xml, f9_btn_secondary.xml, f9_btn_ghost.xml,
  f9_card_surface.xml, f9_card_elevated.xml, f9_chip_bg.xml,
  f9_chip_selected.xml

No layouts, Kotlin, values*/, manifests, or other agents' files modified.
No builds, no commits, no staging.

## IMPORTANT DECISIONS
1. UNIFIED RADIUS SCALE: 8 / 12 / 16 / 24 dp. 8dp = small controls
   (bars, compact buttons); 12dp = ripple mask bound; 16dp = buttons,
   chips, outlined cards; 24dp = large cards. Pills (journey_chip 40dp,
   bar_fill on a 6dp-tall bar) deliberately keep oversized radii — Android
   clamps a radius to half the smaller dimension, so any radius ≥ half
   height renders as the same full pill; this is the standard mechanism,
   not an off-scale value.
2. UNIFIED STROKE TREATMENT: 1dp width at 20% alpha (#33 prefix) for all
   rectangle borders (chips, outlined cards, ghost button, journey chip).
   EXCEPTION kept and documented: circular RING strokes — power_bg_off/on
   (2dp #33...) and pin_dot_empty (2dp ?attr/strokeColor) — stay 2dp
   because they are rings drawn around a small circle, not card/chip
   borders; shrinking them to 1dp would materially thin the power button
   ring and PIN dots on screens owned by other agents. Their alphas are
   already the unified 20%. Splash outline strokes (2.5/6dp) are brand
   art at 200dp scale, untouched.
3. UNIFIED PADDING RHYTHM: drawables carry NO android:padding (verified
   across all 45) — padding lives in layouts, which other agents own.
   F9 keeps drawables padding-free so layout paddings remain the single
   source of spacing truth. New f9_* files follow the same rule.
4. UNIFIED RIPPLE CONVENTION: 20% alpha everywhere. Dark-filled surfaces
   ripple with parchment (#33F2EFE9 — btn_gate, btn_danger, btn_primary,
   f9_btn_primary, f9_btn_secondary); light/transparent surfaces ripple
   with brand teal (#332F7A6B — ripple_soft, f9_btn_ghost) so feedback is
   visible on both light and dark surfaces. btn_primary's #33000000 black
   ripple and ripple_soft's #294CAF50 Android-green ripple were the two
   outliers; both fixed.
5. SEMANTIC COLOR BUGS FIXED (role preservation): chip_red and
   icon_circle_red both referenced @color/green_12 (copy-paste origin),
   rendering teal-green despite "red" names — icon_circle_red is consumed
   by fragment_stats.xml. Both now use @color/clay_12 / #33B4573E, matching
   the red_primary→clay alias in colors.xml. Zero-reference chip_red also
   fixed for whenever it is adopted.
6. VECTOR CONVENTIONS (normalized, no redesigns): UI icons are all 24dp
   viewport; brand/splash art is 200dp; launcher layers are 108dp with the
   66dp safe-zone handled by the 0.5-scale group. fillType evenOdd present
   exactly where cut-through geometry needs it (ic_gate,
   ic_launcher_foreground, ic_splash_outline). Tab icons' #FF000000 fill
   normalized to #FFFFFFFF — visually identical because
   activity_main.xml applies app:itemIconTint="@color/nav_item_color"
   (SRC_IN discards intrinsic RGB, keeps alpha). All vector pathData
   untouched.
7. ORPHAN LIST (zero external references in res/ + java/ + Manifest,
   verified by a fresh re-scan AFTER other agents' concurrent layout
   edits — count unchanged from first scan; DO NOT DELETE per task rules,
   F9 deleted nothing):
   - btn_danger
   - card_outlined
   - chip_green
   - chip_red
   - chip_teal
   - ic_globe
   - ic_mail
   - ic_power
   - ic_splash_gate
   - ripple_soft
   - splash_line
   - ripple_mask (internal-only: referenced solely by orphan ripple_soft;
     if ripple_soft is ever adopted, ripple_mask goes with it)
   No dynamic getIdentifier lookups exist in java/, so there are no
   runtime-only consumers hiding from grep.
8. NEW f9_* FAMILY INVENTORY (all follow the scales above; theme-aware
   via ?attr where relevant; colors referenced from F1's colors.xml or
   hardcoded — values/ untouched):
   - f9_btn_primary — ripple #33F2EFE9 + ?attr/accentColor solid, 16dp,
     explicit mask. Main CTA.
   - f9_btn_secondary — ripple #33F2EFE9 + @color/ink_2 solid, 16dp,
     mask. Mirrors btn_danger/btn_gate dark-fill buttons.
   - f9_btn_ghost — ripple #332F7A6B + transparent solid + 1dp
     ?attr/strokeColor, 16dp, mask. Low-emphasis action.
   - f9_card_surface — ?attr/cardBg solid, 24dp, stroke-free. Resting
     cards (pairs with existing card_bg, same geometry).
   - f9_card_elevated — ?attr/cardBgElevated solid, 24dp, stroke-free.
     Dialogs/sheets/top-most layer (cardBgElevated was defined in themes
     but unused in drawables until now).
   - f9_chip_bg — ?attr/cardBg + 1dp ?attr/strokeColor, 16dp. Resting
     chip, theme-aware in light and dark.
   - f9_chip_selected — @color/green_12 + 1dp #332F7A6B, 16dp. Selected
     chip, mirrors chip_green so "selected" reads as "on".
   All 7 use bounded ripples (explicit @android:id/mask item) so feedback
   never escapes the corner radius.
9. BITMAPS (part 7): res/drawable-xhdpi/ contains ZERO files — no bitmaps
   anywhere in the drawable tree. Nothing to report or modify.

## TESTS RUN
- xmllint --noout on all 24 touched/created files (17 edited + 7 new)
- Fresh orphan re-scan (grep for @drawable/NAME and R.drawable.NAME across
  res/ + java/ + AndroidManifest.xml) after other agents' concurrent edits
- Viewport/fillType convention sweep across all 23 vector files
- itemIconTint verification for the tab icons (menu + activity_main.xml +
  res/color/nav_item_color.xml existence)
- Full re-read of every edited file post-edit
- NO Gradle builds (per task rules)

## TEST RESULTS
- xmllint: all 24 files OK, zero parser errors
- Orphan re-scan: identical to first scan — the 11 zero-reference files
  plus internal-only ripple_mask; no orphan was adopted by concurrent
  agents' edits (F4 examined ic_globe and chip_teal but has not referenced
  them yet)
- Vectors: every UI icon 24/24 viewport; brand art 200/200; launcher
  108 (bg) and 108/200-scale-0.5 (fg); evenOdd on exactly the 3 files that
  need it
- Tab icons: nav uses @color/nav_item_color selector via itemIconTint, so
  the #FF000000→#FFFFFFFF change is provably visual no-op

## KNOWN ISSUES
1. Orphans are listed, not deleted (per task rules). Several are stale
   duplicates of live drawables: chip_green/chip_red/chip_teal vs the new
   theme-aware f9_chip_bg/f9_chip_selected; card_outlined vs
   f9_btn_ghost's stroke treatment; ic_power/ic_globe/ic_mail vs live
   icons. The orchestrator may prune them after layout work settles —
   ripple_soft and ripple_mask must be deleted or adopted TOGETHER.
2. ic_power is a 48dp-wide vector on a 24 viewport (deliberate oversize
   for the power glyph's 2.4dp strokes) — an orphan, so left as-is rather
   than reshaping an icon no one consumes.
3. pin_dot_empty carries android:width/height 16dp INTRINSIC size while
   the sibling pin_dot_* ovals do not (they rely on layout sizing).
   Harmless (layout overrides), but an inconsistency to note if the PIN
   screen is ever rebuilt.
4. f9_chip_* chips are 16dp-radius rectangles per the unified chip scale;
   journey_chip remains a true pill — intentional (a journey pill is a
   badge, not a selectable chip).
5. ic_splash_gate is an orphan whose named-path structure (outline/fill/
   slit with alpha 0) suggests it was an earlier AVD experiment; the live
   AVD (splash_gate_avd) targets ic_splash_outline instead. Deleting
   ic_splash_gate is safe once confirmed orphan by the orchestrator.
6. Concurrent-agent hazard: layout agents were still editing while the
   final orphan scan ran (F2/F3/F5/F6 mid-flight at last check). If any of
   them adopts an orphaned drawable in a last-minute edit, that orphan is
   no longer an orphan — re-grep before pruning.

## NEXT DEPENDENCIES
- Layout agents (F2 home, F3 features, F4 settings, F5 stats, F6 PIN)
  can now reference the f9_* family: f9_btn_primary/secondary/ghost for
  buttons, f9_card_surface/f9_card_elevated for card containers,
  f9_chip_bg/f9_chip_selected for selectable chips (both states, theme-
  aware). They drop in with no values/ changes needed.
- icon_circle_red in fragment_stats.xml now renders CLAY red instead of
  green — F5 (stats screen) should be told, as their screen's status
  indicator changes color (intended fix, but visible).
- Orchestrator: prune the 12-file orphan list (with the ripple_soft+
  ripple_mask pairing caveat) only after all layout agents finish.
- F1 (values owner): no action required — F9 referenced only existing
  colors and theme attrs; nothing new needed from colors.xml.
