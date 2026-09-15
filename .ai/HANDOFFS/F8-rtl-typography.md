# F8 — RTL Mirroring & Arabic Typography Audit

## TASK ID
F8 (rtl-typography) — audit all layouts/menus/themes + cited Kotlin for RTL correctness, Arabic typography, mixed-direction text, font correctness, and a11y-adjacent issues. Report only; res/font/ additive-only if truly needed.

## STATUS
DONE — audit complete, report delivered. Zero files modified (audit + this handoff only; no font additions — see IMPORTANT DECISIONS). All citations below re-verified against live on-disk files on 2026-09-14, after the F1–F6 rewrites landed (F5's StatsFragment.kt was still mid-flight at final read — one re-check flagged in NEXT DEPENDENCIES).

## WHAT WAS IMPLEMENTED

### A. Findings table (open issues — prioritized)

All paths relative to `/home/sami/App-bloking-sex/android/app/src/main/`. Severity: **high** = breaks rendering in RTL or harms readability; **med** = wrong typeface/spacing/targets; **low** = polish/defensive.

| [file:line] | issue | category | severity | RECOMMENDED CONCRETE FIX |
|---|---|---|---|---|
| res/layout/activity_app_lock.xml:42 | `lockHero` (`@string/attqi_allah`) is 52sp `@font/amiri` with `android:lineSpacingMultiplier="1.2"` — app's largest Arabic display text; Amiri's tall ascenders/descenders clip at 1.2 | typography | HIGH | `android:lineSpacingMultiplier="1.6"` (accept 1.5). F6's design intent (calm lockout) is unaffected. |
| res/layout/fragment_settings.xml:59 (+ SettingsFragment.kt:63-64) | `langValue` has `android:fontFamily="@font/plex_semibold"` but is fed `getString(R.string.arabic)` = "العربية" (and "الإنجليزية" in values-ar) — Plex has 0 Arabic glyphs (fontTools: 891 chars, none U+0600–06FF), so Arabic renders via system Naskh fallback: wrong typeface/weight, jarring next to Cairo rows. NOT tofu | font | MED | `android:fontFamily="@font/cairo"` on langValue (keeps 14sp bold teal). Alternatively show `english`/`arabic` as locale-neutral strings, but font swap is the one-line fix. |
| res/values/themes.xml:44 | `Display` style carries `letterSpacing -0.01`; applied to Arabic titles (fragment_settings.xml:30 "الإعدادات", fragment_features.xml:29) — negative tracking squeezes the connected script | typography | MED | Set `letterSpacing` to 0 in Display, or add `DisplayAr` (pattern: SectionHeaderAr, themes.xml:61-71) and use it for Arabic titles. |
| res/layout/activity_app_lock.xml:52-60 (+ AppLockActivity.kt:22,32) | `lockedAppName` receives Latin URLs (`blockedSite`) and app labels, but has no `textDirection`/`layoutDirection` — inside the RTL app, BiDi reordering can scramble URL punctuation/segment order | mixed-dir | MED | Apply the in-repo pattern (FeaturesFragment.kt:127-128): `android:textDirection="ltr"` on lockedAppName in the layout, or set `textDirection = TEXT_DIRECTION_LTR` in AppLockActivity before assigning text. |
| res/layout/activity_welcome.xml:44 | `welcome_subtitle` `letterSpacing 0.06` on Arabic text (values-ar welcome_subtitle) — tracking breaks letter connections in Naskh-rendered Arabic | typography | MED | Remove the `android:letterSpacing="0.06"` attribute. |
| res/layout/fragment_features.xml:357,372 | `btnStartTime`/`btnEndTime` 44dp tall — below 48dp touch target | a11y | MED | `android:layout_height="48dp"` on both. |
| res/layout/fragment_home.xml:505 | `emailButton` 46dp tall — below 48dp | a11y | MED | `android:layout_height="48dp"` (52dp matches other primaries — either passes). |
| res/layout/fragment_features.xml:272 | `manualCount` digits render in `@font/cairo` (Chip style) while every other stat number is `@font/plexmono_semibold` — inconsistent number typeface | font | MED | `android:fontFamily="@font/plexmono_semibold"` on manualCount (keep digits default "+" at :274 as-is). |
| res/layout/activity_splash.xml:48 | `splashTitleAr` "حِصن" `letterSpacing 0.02` on Arabic | typography | LOW | Remove attribute. (L59 `splashTitleLatin` 0.35 on "HISN" is intentional — keep.) |
| res/layout/activity_welcome.xml:60 | `welcome_identity` Amiri `lineSpacingMultiplier 1.5` — adequate but below the 1.6+ Amiri ideal | typography | LOW | `android:lineSpacingMultiplier="1.7"` (1.6 acceptable). |
| res/values-land/dimens.xml:18 | `hadith_text_size` 14sp for Amiri hadith in landscape — too small for Amiri's proportions | typography | LOW | 16sp (hadithText already 1.75 line-height, so only size needs the bump). |
| res/layout/activity_pin.xml:356-363, activity_set_pin.xml:455-464, activity_app_lock.xml:74-83, activity_welcome.xml:89-100 | Buttons with Arabic labels lack explicit `textAllCaps="false"` — harmless today (Arabic has no case) but the default `textAllCaps` idiom protects if a Latin string ever lands there | typography | LOW | Add `android:textAllCaps="false"` to pinConfirm, setPinSave, backHomeButton, startButton. Defensive only. |
| res/layout/fragment_home.xml:64-68; fragment_settings.xml:52,78,116,141,168,209,236 | Decorative icons lack contentDescription/`importantForAccessibility="no"` (statusDot; ic_lang, f4_ic_moon, f4_ic_lock, f4_ic_key, ic_shield, ic_power, ic_gate) — TalkBack may announce "unlabeled" inside otherwise-labeled rows | a11y | LOW | Add `android:importantForAccessibility="no"` to each (they sit inside rows that carry text labels — description would be redundant). |
| res/layout/activity_pin.xml:343, activity_set_pin.xml:442 | `keyBackspace` `contentDescription="حذف"` hardcoded Arabic — English-locale TalkBack users hear Arabic; also not translatable via values/ | mixed-dir | LOW | Move to `@string/delete` (add "حذف"/"Delete" in values-ar/values). Keypads themselves are correctly LTR-pinned (:171/:270) — F6 confirmed. |
| res/anim/slide_in.xml:5 | `fromXDelta "-4%p"` X-translation does not auto-mirror in RTL (only X-translating anim in res/) | rtl-mirror | LOW | Accept as-is (subtle 4% drift) or add a values-rtl/ anim variant. Coordinator previously deemed acceptable. |
| res/layout/fragment_home.xml:498,521 | `app_name_latin` 10sp + `powered_by` 11sp in `@color/stone` on heroBg ≈ 3.3:1 contrast (WCAG AA needs 4.5:1) | a11y | LOW | `android:textColor="@color/stone_deep"` (#6F6758, F1 purpose-added; 4.9:1 on heroBg, 5.1:1 on bg_light). |
| res/layout/activity_app_lock.xml:72 | `blockedMessage` 13sp `@color/stone` on always-dark `bg_dark` ≈ 4.16:1 (day stone) — marginal for 13sp | a11y | LOW | `android:textColor="@color/stone_light"` (#DCD3C6, 10.68:1 on bg_dark) — this screen is dark in BOTH themes so day/night split is irrelevant here. (F1 kept base stone deliberately — integration call.) |
| res/layout/activity_pin.xml:159, activity_set_pin.xml:259 | `pinError`/`setPinError` 13sp `@color/clay` ≈ 4.26–4.40:1 on light bg — just under AA | a11y | LOW | Day clay e.g. `#A0492F` (5.52:1) in values/colors.xml; night `#D08A6E` already passes (5.68:1). |
| res/values/themes.xml:47-55, 73-77, 79-84, 122-132 | Latin-idiom styles (SectionHeader 0.10+caps, StatValue 1.1 spacing, StatLabel 0.04, Chip) — dormant for Arabic but will corrupt Arabic if reused | typography | LOW | No action now; before any Arabic reuse, follow the SectionHeaderAr split pattern (themes.xml:61-71). SectionHeader is fully dormant since features:38 was fixed. |

### B. Resolved by parallel agents during audit (reported for honesty — no action needed)

| [file:line] | was | resolved by |
|---|---|---|
| HeatmapView.kt (entire, F5 rewrite) | Two MED: canvas never auto-mirrors — weekday/month labels at fixed LTR left; English "Jan".."Dec" hardcoded | F5's RTL-aware `Grid` helper (`colX` mirrors weeks when rtl, L225; touch hit-test through same helper, L229; weekday labels at reading-start edge L177-178; month header RTL-aware L153/162-163), Arabic month/dow names (L76-82), Cairo for both label paints (L68/71). Both findings fully resolved. |
| fragment_features.xml:38 | f3Header Arabic `@string/features_title` under Latin `SectionHeader` (0.10 tracking + textAllCaps) — the only SectionHeader misuse in res/ (grep-confirmed at audit time) | Orchestrator fixed on disk (now `SectionHeaderAr`, per coordinator confirmation; original misuse verified by my read + corpus grep). |
| fragment_settings.xml dividers | 68dp misaligned under icon-less rows | F4 redesign: dividers now `marginStart 74dp` aligned to 42dp-icon rows — verified aligned. |
| fragment_settings.xml:38,99,195 | — | F4 adopted `SectionHeaderAr` ×3 (correct pattern). |
| fragment_settings.xml:66,155 | — | F4 chevrons `autoMirrored="true"` (good RTL practice). |
| activity_pin.xml:171, activity_set_pin.xml:270 | PIN keypads risked mirroring | F6: both keypads `layoutDirection="ltr"`; digits in `@font/plex` (=plex_semibold, LTR ASCII — safe); marginStart/End only. Verified. |
| fragment_stats.xml:59,98,136 | stat values direction | F5: `textDirection="ltr"` + plexmono on stat values — verified. |
| StatsFragment.kt:21 | dangling `R.id.reportCount` (prior-audit note) | F5: comment at L21 documents removal — resolved. |
| fragment_home.xml hadithTitle | letterSpacing + contrast | F2+F1: `#F4F1EA` on `#2C7264` card (5.0:1 day / 6.5:1 night) — resolved. |
| values-night/themes.xml:7-8 | — | Night theme pins Cairo in both namespaces — verified. |
| activity_app_lock.xml:29 | — | F6: `ic_gate` carries `contentDescription="@string/app_name_ar"` — verified. |
| setPinCancel | old <48dp note moot | F6: 48dp TextView — verified. |

### C. Verified clean (no action)

- `supportsRtl="true"` in manifest; **zero Left/Right attributes in res/** — Start/End used throughout (grep across res/).
- activity_main.xml + res/menu/bottom_nav.xml: clean (Start/End margins, no direction hazards).
- UiAnim.kt: RTL-safe — pressable is scale-only; staggeredEntrance is translationY+alpha; countUp writes bare ASCII digits into already-plexmono views; breathe loads pulse. No X-axis translation.
- 6/7 anims RTL-safe (slide_in noted above).
- Bar-chart RTL time direction CORRECT: BlocklistDatabase.kt:78 `ORDER BY day DESC LIMIT :days` + StatsFragment.kt:65 `stats.reversed()` (oldest-first) → `addView` into auto-mirrored horizontal `reportBars` (fragment_stats.xml:205) → today renders at the LEFT in RTL — correct for RTL time flow.
- hadithText: Amiri at `lineSpacingMultiplier 1.75` (fragment_home.xml:162) — correct.
- Theme-level Cairo in BOTH namespaces, day (values/themes.xml:16-17) AND night (values-night/themes.xml:7-8).
- Home stats values plexmono; both keypads LTR; stepsCard + features + settings dividers aligned; journey_chip.xml symmetric.
- Hadith pool Arabic in both locales (HomeFragment.kt:136); powerButton contentDescription synced (HomeFragment.kt:63); `Locale.US` time formatting for the schedule string (FeaturesFragment.kt:227-228) and "$greeting يا $name" (HomeFragment.kt:86) render acceptably.
- grep corpus re-confirmed: exactly 4 letterSpacing in layouts (splash:48 LOW / splash:59 Latin-OK / welcome:44 MED / home:500 Latin-OK); no `textAllCaps="true"` anywhere in res/layout; slide_in.xml is the only X-translating anim.
- Switch contentDescriptions set from Kotlin in Features (L56/216/265) and Settings (L98-101) fragments.

### D. Font inventory (res/font/ — 11 files, NONE added by F8)

amiri.xml, amiri_bold.ttf, amiri_regular.ttf, cairo.xml, cairo_bold.ttf, cairo_extrabold.ttf, cairo_medium.ttf, cairo_regular.ttf, plex.xml (→plex_semibold 600), plex_semibold.ttf, plexmono_semibold.ttf.
Coverage verified via fontTools: cairo_regular 102 Arabic glyphs; plex_semibold 0 Arabic (891 Latin/symbol) — Arabic-in-Plex falls back to system Naskh (typeface mismatch, not tofu). Amiri correctly used ONLY for hadith + app-lock hero + welcome_identity. No missing weights block any recommended fix — every fix above uses existing files.

## FILES CHANGED
- `/home/sami/App-bloking-sex/.ai/HANDOFFS/F8-rtl-typography.md` (this report) — the only write.
- res/font/: **nothing added, renamed, or deleted** (ls verified 11 unchanged files; git status shows zero F8-attributable changes).

## IMPORTANT DECISIONS
1. **No font additions.** Every recommended fix uses existing font resources; there is no local binary source for new weights, and inventing one would violate the additive-only rule's spirit. The only optional nicety (a `plexmono.xml` family XML wrapping plexmono_semibold) is noted, not actioned.
2. **langValue finding is MED, not HIGH.** fontTools glyph inspection proved Plex has zero Arabic coverage — "العربية" renders through system Naskh fallback (wrong typeface/weight beside Cairo rows), not tofu. Honesty over certainty: the evidence supports wrong-typeface, not broken-rendering.
3. **Parallel-agent findings reported as resolved.** F5's HeatmapView rewrite and F4/F6/F3 fixes were verified on disk post-rewrite and moved to the "resolved" table rather than inflating the open-findings count.
4. **Citations drift-corrected.** Every open finding carries a line number from the live post-rewrite file (re-verified 2026-09-14): app-lock HIGH at activity_app_lock.xml:42 (F6 moved it from :26), lockedAppName at :52-60 with feeds at AppLockActivity.kt:22/32, langValue at fragment_settings.xml:59, keyBackspace contentDescription (not visible text) at activity_pin.xml:343 / activity_set_pin.xml:442.
5. **keyBackspace re-classified.** F6's final design is a vector icon with `contentDescription="حذف"` — the hardcoded Arabic is an a11y label (LOW, move to @string), not on-screen copy.
6. **Stone/clay contrast reported with numbers, left as integration calls.** F1 kept base stone deliberately; where the screen is dark in both themes (app-lock) the fix is trivial (stone_light 10.68:1), elsewhere it's a palette decision.

## TESTS RUN
- Full reads of all 11 layouts, bottom_nav.xml, themes.xml, values/colors.xml, values-night/{colors,themes}.xml, values-land/dimens.xml, values+values-ar strings.xml, and all cited Kotlin (UiAnim.kt, HeatmapView.kt, AppLockActivity.kt, PinActivity-adjacent greps, FeaturesFragment.kt, HomeFragment.kt, SettingsFragment.kt, StatsFragment.kt, BlocklistDatabase.kt) — final F6 files re-read in full (85/67 lines) after F6 declared done.
- Direct greps across res/ (Explore subagent unavailable — 403): SectionHeader usage, letterSpacing corpus, textAllCaps corpus, X-translating anims, Left/Right attributes.
- python3 fontTools glyph inspection: plex_semibold (891 chars, 0 Arabic), cairo_regular (102 Arabic) — fallback conclusion.
- python3 WCAG 4.5:1 contrast math for stone/clay/stone_deep/stone_light/clay-night on heroBg/bg_light/bg_dark.
- ls res/font/ (11 files unchanged) + `git -C /home/sami/App-bloking-sex status --short` (zero F8-attributable changes; all M/?? belong to parallel agents + orchestrator's .ai files).
- F6's handoff (.ai/HANDOFFS/F6-pin-ux.md) read in full and cross-checked against its final files.

## TEST RESULTS
- 11/11 layouts audited; 1 HIGH, 7 MED, ~11 LOW open findings, all with live-verified file:line citations and exact attribute fixes. (The 8th MED — features:38 SectionHeader misuse — was fixed on disk mid-audit and moved to the resolved table.)
- 12+ issues found mid-audit were fixed by parallel F1–F6 rewrites and verified resolved (section B).
- res/font/: 0 additions, 0 renames, 0 deletions — additive-only rule honored trivially.
- git status: no F8-attributable modifications to any layout/values/drawable/menu/anim/Kotlin file.
- No build performed (task rule: NO ./gradlew — compile/runtime verification is the integrator's step).

## KNOWN ISSUES
- Hardcoded Arabic outside strings.xml — English-locale users see/hear Arabic: fragment_stats.xml:148 "المتوسط اليومي"; fragment_settings.xml:37/98/194 "التطبيق"/"الحماية"/"الشبكة" (F4); HeatmapView.kt:76-82 month/dow names (F5 — res/values outside its ownership); keyBackspace "حذف" ×2 (F6). All are copy/i18n, not RTL breakage — integration strings pass recommended.
- values-land/dimens.xml:18 hadith 14sp (LOW, open).
- Dormant Latin-idiom styles (SectionHeader/StatValue/StatLabel/Chip) — open LOW, latent only.
- manualCount default "+" (fragment_features.xml:274) — intentional placeholder; font fix at :272 is the actionable part.
- F5 was still writing StatsFragment.kt at my final read (progress note: "Verifying final StatsFragment.kt output"); HeatmapView.kt and fragment_stats.xml citations are from the landed rewrite, but the orchestrator should re-confirm fragment_stats.xml/StatsFragment.kt line numbers at integration (reportCount removal already verified resolved in the current on-disk version).
- No visual device check was possible (no builds) — the Naskh-fallback weight mismatch on langValue and the Amiri 1.2 line-height clip are evidence-based predictions from glyph metrics and spec math, strongly grounded but unrendered.

## NEXT DEPENDENCIES
- **Orchestrator (integration)**: apply the 1 HIGH + 8 MED fixes from the table (Amiri 1.6 on app-lock hero; cairo on langValue; Display letterSpacing 0; ltr on lockedAppName; welcome letterSpacing removal; three 48dp targets; plexmono on manualCount); the LOWs are batch-optional.
- **Orchestrator (integration)**: re-verify fragment_stats.xml + StatsFragment.kt citations after F5 fully completes; re-verify PinActivity.kt/SetPinActivity.kt behavior expectations against F6's final handoff (auth flows byte-identical per F6).
- **Strings pass**: move the 4 hardcoded-Arabic spots to values/values-ar resources.
- **F9**: old `pin_dot_*` drawables now unreferenced (F6 uses f6_ twins) — noted by F6 for F9's retirement list.
- **Optional visual QA** once builds are allowed: langValue Naskh-vs-Cairo mismatch, app-lock hero line-height at 1.2 vs 1.6, lockedAppName with a Latin URL in RTL.
