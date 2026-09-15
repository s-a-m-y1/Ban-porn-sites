# F6 — PIN Entry UX Polish

## TASK ID
F6 (pin-ux) — PIN entry UX across PinActivity, SetPinActivity, AppLockActivity

## STATUS
DONE

## WHAT WAS IMPLEMENTED

### 1. View inventory (all IDs, per activity)
- **PinActivity** binds: `pinIcon`, `pinReason`, `dotsFrame` (layout-only), `dotsRow`, `dot1`–`dot6`, `pinInput`, `pinErrorRow`, `pinError` (static `@string/pin_wrong` in XML; layout-only text), `keypad` (layout-only), `key1`–`key9`, `key0`, `keyBackspace`, `pinConfirm`.
- **SetPinActivity** binds: `setPinTitle`, `entryDotsFrame`, `entryDotsRow`, `entryDot1`–`entryDot6`, `pinInput1`, `confirmDotsFrame`, `confirmDotsRow`, `confirmDot1`–`confirmDot6`, `pinInput2`, `setPinErrorRow`, `setPinError`, `key1`–`key9`, `key0`, `keyBackspace`, `setPinSave`, `setPinCancel` (`keypad` layout-only).
- **AppLockActivity** binds: `lockIcon`, `lockHero`, `lockRule` (all three NEW, added in layout + Kotlin together), `lockedAppName`, `blockedMessage`, `backHomeButton`.

### 2. PIN dot states (three screens, one family)
8 new drawables prefixed `f6_`: `f6_pin_dot_empty` (stroke `?attr/strokeColor`), `f6_pin_dot_filled` (`?attr/accentColor`), `f6_pin_dot_error` (`@color/clay`), `f6_pin_dot_success` (`@color/teal`), `f6_key_bg` (ripple `#338FC4B6` over `?attr/cardBgElevated`, 14dp corners, 1dp `?attr/strokeColor` stroke), `f6_ic_backspace` (vector, `?attr/textSecondary`), `f6_ic_error` (vector, `@color/clay`), `f6_icon_circle_lock` (`@color/bg_dark_elevated` oval + 1dp `@color/brass_12` ring). All 16dp dots, 6dp side margins — identical across pin/set-pin.

### 3. Keypad (LTR + press feedback + targets)
- Keypad container has `android:layoutDirection="ltr"` so 1–9,0 never mirrors in the RTL app; keys are literal digits (no mirroring risk in content).
- Keys are 64×56dp TextViews (+4dp key gaps, 8dp row gaps) over 56dp-tall touch rows — generous targets; `UiAnim.pressable` (scale 0.96/90ms) + ripple `#338FC4B6` + 40ms haptic per key.
- IBM Plex (`@font/plex`) 22sp for digits; backspace is a centered 24dp vector with `contentDescription="حذف"`.

### 4. Errors / confirmation
- Wrong-PIN (PinActivity): watcher hides the stale error on any text change; the failure branch re-shows the row (clay glyph + bold clay text `pin_wrong`), paints all dots clay, shakes the row (480ms translationX), haptic, then the 5-attempt kick-home (unchanged).
- Save errors (SetPin): `showErr` shows the row (clay glyph + text); `markRowError` paints the offending row clay + shakes it (validate error → entry row; mismatch → confirm row) and routes the keypad to that row.
- Success (PinActivity): all dots flip teal, haptic, 180ms delay, `RESULT_OK` + finish (unchanged).

### 5. App-lock screen
Ink-dark (`@color/bg_dark`) in both themes. 112dp `bg_dark_elevated` disc with 1dp brass_12 ring + 56dp `ic_gate` (alpha 0.9) → 52sp Amiri `@string/attqi_allah` hero → 72dp×1dp brass rule → app name / message → full-width primary button. `UiAnim.staggeredEntrance` stages icon→hero→rule→name→message→button (60ms steps); `UiAnim.pressable` on the button.

### 6. Consistency
- Spacing rhythm 4/8/12/16/24 only; radius family 14dp (keys) / oval (dots, icon discs).
- All colors via `?attr/` tokens or night-safe `@color/` (teal/clay/brass have values-night overrides; bg_dark family always dark). Both themes correct.
- Fonts: Cairo default, Plex digits, Amiri hero — all through the font resources.

### 7. Kotlin — UI-only changes, auth flow byte-identical
- **Hidden capture-layer architecture**: the on-screen keypad writes into the pre-existing hidden EditTexts (`pinInput` / `pinInput1` / `pinInput2`, kept at 1dp×1dp as direct children of the dots FrameLayouts, all suppression attrs: no focus, no IME, transparent, no autofill). Keypad `appendDigit`/`deleteDigit` call `setText`, which drives the SAME TextWatcher — so dot rendering, auto-verify-at-6 (Pin), save-button enable/alpha (SetPin), and `suppressErrorClear` behave exactly as before.
- Removed from all three: IME machinery (`requestFocus`, `setSoftInputMode`, `imm.showSoftInput`, InputMethodManager imports; SetPin's unused AppCompatActivity import also dropped).
- SetPin adds: shared-keypad binding with active-input routing (default input1; tapping a dots row switches; auto-advance to input2 when entry hits 6 digits), dual dot-row rendering (same visibility rule `i < MAX && (i < MIN || length > MIN)` with the 1.25f/90ms pop), `markRowError` (clay + shake on the offending row).
- AppLock adds ONLY `staggeredEntrance` + `pressable`; blockedSite/blockedApp logic, packageManager label lookup, back-home intent, and `onBackPressed` are byte-identical.
- PinManager NOT touched. All preserved flows verified line-by-line during rewrite: PinActivity companion/EXTRA_REASON/attempt-kick-home, SetPin watcher/save/showErr/suppressErrorClear/prefs `pin_enabled`, AppLock both intents.

## FILES CHANGED
- `/home/sami/App-bloking-sex/android/app/src/main/res/layout/activity_pin.xml` (rewritten, 366 lines)
- `/home/sami/App-bloking-sex/android/app/src/main/res/layout/activity_set_pin.xml` (rewritten, 480 lines)
- `/home/sami/App-bloking-sex/android/app/src/main/res/layout/activity_app_lock.xml` (rewritten, 85 lines)
- `/home/sami/App-bloking-sex/android/app/src/main/res/drawable/f6_pin_dot_empty.xml` (NEW)
- `/home/sami/App-bloking-sex/android/app/src/main/res/drawable/f6_pin_dot_filled.xml` (NEW)
- `/home/sami/App-bloking-sex/android/app/src/main/res/drawable/f6_pin_dot_error.xml` (NEW)
- `/home/sami/App-bloking-sex/android/app/src/main/res/drawable/f6_pin_dot_success.xml` (NEW)
- `/home/sami/App-bloking-sex/android/app/src/main/res/drawable/f6_key_bg.xml` (NEW)
- `/home/sami/App-bloking-sex/android/app/src/main/res/drawable/f6_ic_backspace.xml` (NEW)
- `/home/sami/App-bloking-sex/android/app/src/main/res/drawable/f6_ic_error.xml` (NEW)
- `/home/sami/App-bloking-sex/android/app/src/main/res/drawable/f6_icon_circle_lock.xml` (NEW)
- `/home/sami/App-bloking-sex/android/app/src/main/java/com/contentfilter/app/PinActivity.kt` (rewritten, 197 lines)
- `/home/sami/App-bloking-sex/android/app/src/main/java/com/contentfilter/app/SetPinActivity.kt` (rewritten, 227 lines)
- `/home/sami/App-bloking-sex/android/app/src/main/java/com/contentfilter/app/AppLockActivity.kt` (rewritten, 67 lines)

## IMPORTANT DECISIONS
- **Hidden capture-EditText preserved, not replaced**: keeping the original EditTexts as invisible data sinks means the existing TextWatcher/auto-verify/save flows are untouched; the keypad is purely an input surface. This is why auth behavior is identical.
- **Failure-branch reorder in PinActivity.verify()**: original order painted clay dots then called `input.setText("")`, but the watcher's re-render inside setText overwrote the clay with empty — the designed error state never actually showed. New order: `setText("")` FIRST, then error row + clay + shake. Presentation-only; attempts/5-attempt-kick-home unchanged.
- **Error row auto-hide on retyping** (both pin screens): hides on any text change (guarded by the existing `suppressErrorClear` in SetPin; failure branches re-show it after the clear pass). Nothing reads these visibilities elsewhere — verified.
- **SetPin active-input routing + markRowError reroute**: after a validate error on a full entry row the auto-advance had left routing on the confirm field; `markRowError` now points the keypad back at the offending row.
- **Shake stays private** (per activity): UiAnim is frozen and has no shake helper; a tiny private ObjectAnimator is duplicated in PinActivity/SetPinActivity rather than touching UiAnim.
- **Keypad ripple `#338FC4B6`** (12%-alpha teal_night): visible on both light parchment and dark elevated keys; radius 14dp matches the family.
- **AppLock kept dark in both themes** (lockout moment, `bg_dark`): the disc uses `bg_dark_elevated` + brass_12 ring so it stays calm, not alarming.
- **values*/ untouched**: all theme refs via `?attr/` tokens already used by existing compiled resources; night-safe `@color/` refs only; new copy avoided entirely (all strings pre-existed in both locales).
- **New IDs only added in layout+Kotlin together** (lockIcon/lockHero/lockRule); removed IDs (old IME hints) had no external refs — verified by grep.

## TESTS RUN (self-checks)
- Read all 6 target files fully before editing (inventory step).
- Read every edited file back after writing: all 3 layouts, all 3 Kotlin files, spot-read drawables — valid XML/Kotlin, Arabic intact (`حذف`, `@string/attqi_allah`, `@string/app_name_ar`), balanced braces/tags.
- xmllint --noout on all 11 XML files: PASS.
- ID cross-check (grep): every `R.id.*` bound in each Activity exists in its layout — PASS (Pin 15/15, SetPin 33/33, AppLock 6/6).
- Isolation grep: no file outside my 3 activities references my layouts, my view IDs, or any `f6_` drawable — PASS.
- UiAnim API freeze respected: only existing helpers (`pressable`, `staggeredEntrance`) used; UiAnim.kt not modified.
- Auth-flow preservation: original SetPinActivity.kt / AppLockActivity.kt read verbatim before rewrite; watcher/save/showErr/suppressErrorClear/intents/back handling compared line-by-line.

## TEST RESULTS
- xmllint: 11/11 files well-formed.
- ID match: 54/54 bindings resolve.
- Isolation: 0 external references to my resources.
- No build performed (task rule: NO ./gradlew) — compile/runtime verification is the integrator's step.

## KNOWN ISSUES
- **Not compiled** (builds forbidden by task rules): Kotlin syntax was hand-verified line-by-line but never passed kotlinc/gradle. First compile may surface trivial issues (e.g. unused-import warnings are possible in AppLock — `View` import IS used for findViewById<View> casts, so no).
- **SendMessage to F8 never delivered** (classifier "temporarily unavailable" across ~5 retries over two segments): F8 (RTL/typography audit) may hold stale citations of activity_app_lock.xml / activity_pin.xml / activity_set_pin.xml from a pre-rewrite snapshot. Its agent id was a8c5e057842699c30. If its audit lands, orchestrator should discount any findings on my 6 files that don't match the current on-disk content.
- Old `pin_dot_*` drawables (F9's) are now unreferenced by my screens but still exist on disk — left for F9's cleanup scope, not mine.
- Shake helper duplicated (private, 2 files) — acceptable under the UiAnim freeze; if UiAnim later gains a shake, dedupe then.

## NEXT DEPENDENCIES
- **Orchestrator**: integrate + first compile (no build done); if `?attr/onAccent` or `?attr/selectableItemBackground` resolve anywhere in the existing app they will resolve here too (both already used by compiled resources).
- **F8 (RTL/typography)**: re-read my 3 layouts before flagging — keypad already LTR-pinned, marginStart/End only, Amiri hero intentional. Do NOT edit my 6 files; route requests through the orchestrator.
- **F9 (drawable cleanup)**: `pin_dot_empty/filled/error/success` originals are now unused by activities (my screens use f6_ twins); candidates for their retirement list.
- **Anyone touching PIN screens**: the hidden EditTexts (`pinInput`, `pinInput1`, `pinInput2`) are load-bearing capture layers — keep the 1dp direct-child-of-FrameLayout placement and suppression attrs, or the keypad stops feeding the watchers.
