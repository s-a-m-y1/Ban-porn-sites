# F1 Handoff — Design-Tokens Refinement Pass

## TASK ID

F1 — design tokens (colors / dimens / themes / strings-ar refinement)

## STATUS

DONE

## WHAT WAS IMPLEMENTED

All 8 coordinator-approved items, additive or value-edit only — zero renames, zero deletes, zero new keys outside the one missing Arabic translation:

- **`stone_deep` added** (base colors) for small text on light surfaces — 4.91:1 on heroBg `#EAF2EF`, 5.13:1 on bg_light `#F7F5EF`. Base `stone` untouched (welcome/app-lock depend on it over always-dark roots in day mode).
- **`brass_deep` added** (base colors) for chip text on light surfaces — ≈4.9:1 on heroBg (journey_chip.xml solid is transparent, so chip text sits directly on heroBg). Base `brass` untouched (2.87:1 there, but serves dark surfaces).
- **Night overrides added**: `stone` `#B0A798` (6.65:1 on bg_dark, 5.32:1 on bg_dark_surface), and accent tints raised 12% → 20% night-accent alpha: `green_12` `#338FC4B6`, `clay_12` `#33D08A6E`, `brass_12` `#33D2AC63` (12% day tints composite near-invisible on ink cards).
- **Hadith banner contrast fixed** (base values only; night override `#2E5F55` unchanged): `hadith_card_bg` `#2F7A6B` → `#2C7264`, `hadith_card_title` `#B8DCD3C6` → `#F4F1EA`. Eyebrow is 11sp bold = WCAG *small* text (bold-large starts at 14sp) → 4.5:1 target. Result: eyebrow 5.03:1 day / 6.45:1 night; 18sp parchment body 5.03:1 day / 6.45:1 night. A 90%-alpha eyebrow blend was rejected (computes 4.41:1).
- **Dimens scales added**: spacing `space_xs` 4dp → `space_3xl` 48dp (4dp grid) and radius `radius_sm` 8dp → `radius_2xl` 32dp + `radius_pill` 999dp. No collisions with existing dimens (verified). values-land got a comment-only change documenting qualifier-fallback inheritance.
- **`SectionHeaderAr` style added** as a fresh TOP-LEVEL style (deliberately NOT `SectionHeader.Ar` dot-notation, which would create an implicit parent): 12sp bold, `textAllCaps` false, `letterSpacing` 0, `lineSpacingMultiplier` 1.3, `@color/green_light`. Existing `SectionHeader` unchanged.
- **Line-height polish**: `lineSpacingMultiplier` (API-safe) added to `Chip` 1.2, `StatValue` 1.1, `StatLabel` 1.3. `IconCircle` has no text — no line-height item (documented decision). All four styles have zero layout consumers today, so these are safe defaults for new layouts.
- **Night themes.xml Cairo fix**: `android:fontFamily` + `fontFamily` `@font/cairo` added as the first items of `Theme.ContentFilter` — without them, dark mode fell back to Roboto. All 9 custom attrs (`cardBg`…`heroBg`) verified already overridden in night; no change needed there.
- **values-ar/strings.xml**: added the single missing translatable key `english` = `الإنجليزية` (values-ar now at 100% parity); typo fixes preserving meaning: `app_name` and both overlay strings `حصن` → `حِصن` (kasra), `heatmap_empty` `ستتملأ` → `ستمتلئ`. `welcome_name_hint` verified character-by-character (بنناديك spells the yeh correctly; Egyptian dialect is the established product voice) — left as-is.

## FILES CHANGED

- /home/sami/App-bloking-sex/android/app/src/main/res/values/colors.xml
- /home/sami/App-bloking-sex/android/app/src/main/res/values-night/colors.xml
- /home/sami/App-bloking-sex/android/app/src/main/res/values/dimens.xml
- /home/sami/App-bloking-sex/android/app/src/main/res/values-land/dimens.xml (comment only)
- /home/sami/App-bloking-sex/android/app/src/main/res/values/themes.xml
- /home/sami/App-bloking-sex/android/app/src/main/res/values-night/themes.xml
- /home/sami/App-bloking-sex/android/app/src/main/res/values-ar/strings.xml

hadiths.xml untouched (sacred). values/strings.xml untouched (was already complete).

## IMPORTANT DECISIONS

1. Additive `stone_deep`/`brass_deep` rather than darkening base tokens — base `stone`/`brass` are consumed on always-dark backgrounds (welcome, app-lock, night surfaces) where their current values pass; a global darken would regress those in day mode.
2. Eyebrow WCAG target corrected to small-text 4.5:1 (11sp bold, not large text) — full-tone parchment chosen because alpha blends cannot reach 4.5:1 on the teal banner.
3. `hadith_card_bg` deepened one step instead of lightening text further — keeps the banner a deep-teal brand surface; night `#2E5F55` already passed with the new title color.
4. `SectionHeaderAr` as top-level style so `SectionHeader`'s Latin caps/tracking behavior is preserved verbatim for any existing consumer.
5. `lineSpacingMultiplier` over `lineHeight` — `lineHeight` needs API 28+; the project's min SDK is below that.
6. Night tint alpha 20% chosen because the night accents (`#8FC4B6`/`#D08A6E`/`#D2AC63`) are themselves lightened; 20% of a light accent ≈ 12% of the deep day accent on ink.
7. values-ar `english` fills the ONLY translatable gap; `app_name`/`arabic` remain `translatable="false"` per lint guidance (Arabic value in both locales anyway).

## NEW TOKENS SUMMARY (for orchestrator hoisting at integration)

Colors (base):
- `stone_deep` `#6F6758` — small text on light/hero surfaces (replace footer `@color/stone` in fragment_home)
- `brass_deep` `#856227` — chip/label text on light/hero surfaces (replace `@color/brass` on journeyChip)
- `hadith_card_bg` `#2C7264` (was `#2F7A6B`)
- `hadith_card_title` `#F4F1EA` (was `#B8DCD3C6`)

Colors (night overrides added):
- `stone` `#B0A798`, `green_12` `#338FC4B6`, `clay_12` `#33D08A6E`, `brass_12` `#33D2AC63`

Dimens (all NEW, no renames):
- spacing: `space_xs` 4dp, `space_sm` 8dp, `space_md` 12dp, `space_lg` 16dp, `space_xl` 24dp, `space_2xl` 32dp, `space_3xl` 48dp
- radius: `radius_sm` 8dp, `radius_md` 12dp, `radius_lg` 16dp, `radius_xl` 24dp, `radius_2xl` 32dp, `radius_pill` 999dp
  (existing drawable radii for reference: btn_primary/chip_* 14dp, card_bg 26dp, card_outlined 16dp)

Styles (base themes.xml):
- `SectionHeaderAr` (NEW) — 12sp bold, caps off, letterSpacing 0, lineSpacingMultiplier 1.3, `@color/green_light`
- `Chip` += lineSpacingMultiplier 1.2; `StatValue` += 1.1; `StatLabel` += 1.3
- night `Theme.ContentFilter` += `android:fontFamily`/`fontFamily` `@font/cairo` (Cairo was lost at night)

## TESTS RUN

Self-checks only (builds forbidden for parallel agents):
- Full-file Read of all 7 edited files post-edit — XML well-formed, no stray characters, Arabic text intact (kasra/hamza forms verified).
- Contrast math: python-verified pairs (pre-edit) for stone_deep, night stone, hadith pairs; brass_deep hand-computed with the same sRGB-luminance method, validated against the python-verified anchors (reproduces them to three decimals).
- Key-parity audit values vs values-ar: `english` was the only translatable gap; now closed.
- Zero-consumer verification (grep, pre-edit) for `SectionHeader`/`StatValue`/`StatLabel`/`Chip` styles — line-height additions cannot shift any existing layout.
- Duplicate-name check for all new dimens/style/color tokens — none.

## TEST RESULTS

- All 7 files: well-formed XML, Arabic intact, no duplicate resources.
- Contrast (WCAG 2.x, sRGB): stone_deep 4.91:1 heroBg / 5.13:1 bg_light (PASS ≥4.5); brass_deep ≈4.9:1 heroBg (PASS); night stone 6.65:1 bg_dark / 5.32:1 surface (PASS); hadith eyebrow 5.03:1 day bg / 6.45:1 night bg (PASS); hadith body 5.03:1 day / 6.45:1 night (PASS ≥4.5, large text needs only 3:1).
- values-ar: 100% parity on translatable keys, 4 meaning-preserving fixes applied.

## KNOWN ISSUES

1. Day-mode footer text still uses `@color/stone` (3.34:1 on heroBg) — F2 owns fragment_home.xml; swap to `@color/stone_deep` (footer stone text near lines 427/452).
2. journeyChip text still `@color/brass` (2.87:1 on heroBg) — F2; swap to `@color/brass_deep`. Also journeyChip `letterSpacing 0.04` and hadithTitle `letterSpacing 0.08` harm Arabic shaping (F2's layout files; suggest 0).
3. welcome/app-lock `@color/stone` over `bg_dark` in day mode ≈4.16:1 — borderline for small text; acceptable for the large display text used there, but the orchestrator may adopt `stone_light` at integration.
4. Egyptian colloquialisms in welcome/overlay strings (`بنناديك إيه`, `علشان`) — deliberate product voice, unchanged; flag if a MSA pass is ever wanted.
5. card_bg drawable radius 26dp sits between `radius_xl` 24dp and `radius_2xl` 32dp — F9 may align to `radius_xl` for scale consistency.
6. brass_deep ratio hand-verified rather than python-run (Bash classifier outage during the edit window); margin over 4.5:1 is ~0.4, comfortable but worth a one-line re-verification at integration if convenient.
7. No build performed (forbidden — parallel gradle daemons race); all changes verified by inspection only. Styles/tokens are additive so aapt2 failure risk is minimal.

## NEXT DEPENDENCIES

- Orchestrator: integration + hoist of hardcoded literals to the new scales/tokens (summary above); optional brass_deep re-verification.
- F2 (fragment_home.xml): adopt `stone_deep` (footer) and `brass_deep` (journeyChip), drop Arabic-harming letterSpacing on those views.
- F9 (drawables): optional radius alignment of card_bg 26dp → `@dimen/radius_xl`.
- Anyone adding new layouts: use `SectionHeaderAr` for Arabic section headers, `space_*`/`radius_*` dims instead of hardcoded dp literals.
