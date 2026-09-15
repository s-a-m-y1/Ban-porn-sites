# F7 — App shell (MainActivity, splash, welcome, report bar, UiAnim)

## TASK ID
F7-app-shell

## STATUS
DONE

## WHAT WAS IMPLEMENTED
A full motion-and-polish pass over the app shell, keeping every frozen
ID and API contract intact. No builds were run (per mandate), so all
verification was by full-file re-reads plus palette/token existence
greps.

- UiAnim.kt internals modernized onto a 150/250/400ms duration scale
  with shared FastOutSlowIn (entrances) and OvershootInterpolator(1.1f)
  (release) curves. Every helper is now safe to call twice on the same
  view (cancel-first restarts).
- Bottom nav shell (activity_main.xml): fragment container now fills
  via layout weight, a 1dp theme-aware hairline (?attr/dividerColor)
  separates content from the bar, and the nav keeps labeled mode with
  the stable IDs fragmentContainer / bottomNav. The active-indicator
  pill is re-tinted programmatically to the brand palette (see
  IMPORTANT DECISIONS 2).
- Nav item colors (res/color + res/color-night): both selectors now
  agree structurally — checked = teal (night-resolves to #8FC4B6),
  unchecked = stone (light) / stone_light (night).
- Splash (SplashActivity.kt): every beat now rides FastOutSlowIn;
  exit transition into MainActivity/WelcomeActivity replaced the
  android.R.anim fade pair with a new calm f7_activity_enter/exit
  crossfade. ALL beat timings deliberately preserved (see KNOWN
  ISSUES 3 for the corrected AVD math).
- Welcome (activity_welcome.xml + WelcomeActivity.kt): 9-child entrance
  stagger via UiAnim.staggeredEntrance (no new view IDs), a quiet input
  well (new f7_input_bg drawable on the unified radius scale), a single
  primary CTA (startButton on F9's btn_primary), and a powered_by trust
  footnote reusing the existing bilingual string.
- report_bar.xml: kept quiet — 6dp bar fill, label margin 6dp + maxLines 1
  + 9sp ?attr/textSecondary, no dismiss affordance. Stable IDs
  barFill / barLabel.
- 7 anim XMLs retimed onto the same scale (see FILES CHANGED for
  per-file durations); pulse.xml deliberately untouched; 2 new f7_
  activity-transition anims added.

## FILES CHANGED
All paths relative to /home/sami/App-bloking-sex/android/app/src/main/.

Modified (15):
- java/com/contentfilter/app/UiAnim.kt
- java/com/contentfilter/app/MainActivity.kt
- java/com/contentfilter/app/SplashActivity.kt
- java/com/contentfilter/app/WelcomeActivity.kt
- res/layout/activity_main.xml
- res/layout/activity_welcome.xml
- res/layout/report_bar.xml
- res/anim/fade_in.xml      (250ms, fast_out_slow_in)
- res/anim/fade_out.xml    (150ms, fast_out_linear_in)
- res/anim/card_in.xml     (400ms rise+fade)
- res/anim/header_in.xml   (400ms scale 0.92→1 + fade, fillAfter)
- res/anim/pop_in.xml      (250ms scale 0.6→1 overshoot + fade)
- res/anim/slide_in.xml    (400ms −4%p slide + fade)
- res/color/nav_item_color.xml
- res/color-night/nav_item_color.xml

New (3):
- res/anim/f7_activity_enter.xml  (400ms alpha 0→1, fast_out_slow_in)
- res/anim/f7_activity_exit.xml   (250ms alpha 1→0, fast_out_linear_in)
- res/drawable/f7_input_bg.xml    (bg_dark_surface well, 1dp 24%-alpha
  stone stroke, 12dp radius)

Intentionally untouched (3): res/anim/pulse.xml (cadence mandate),
res/menu/bottom_nav.xml (needed no change), res/layout/activity_splash.xml
(timings/layout sound; read-only verification only).

## IMPORTANT DECISIONS
1. UiAnim API — FROZEN SURFACE UNCHANGED. The four existing signatures
   used by HomeFragment.kt (pressable, staggeredEntrance, breathe,
   countUp) keep their exact shapes:
     pressable(vararg views: View)
     staggeredEntrance(vararg views: View, startDelayMs: Long = 60)
     breathe(view: View, active: Boolean)
     countUp(view: android.widget.TextView, target: Int, dur: Long = 800)
   ONE additive helper (does not collide with anything):
     fadeIn(view: View, startDelayMs: Long = 0L, durationMs: Long = 400L)
   Internal-only improvements (no signature impact): press is 150ms
   down / 250ms Overshoot(1.1f) release, touch listener returns false
   so clicks still fire; entrances are 400ms FastOutSlowIn with
   cancel-first; countUp resets scaleX, guards twice-safety via a
   WeakHashMap entry removed on animation end, and bounces scaleX
   1→1.12→1 for 250ms after dur.
2. Bottom-nav active pill tint: NavIndicator (F1's themes.xml) carries
   an off-brand body #1A00BFA5. Routed around programmatically —
   activity_main.xml keeps app:itemActiveIndicatorStyle for shape, and
   MainActivity sets nav.itemActiveIndicatorColor =
   ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_12)).
   green_12 is the palette's ready-made low-alpha teal and resolves per
   current night mode automatically (12% Guard Teal light / 20% #8FC4B6
   night). Integration recommendation for F1: fix the NavIndicator body
   at the source.
3. Splash timings all preserved on purpose. The AVD's own animators
   overlap (startOffsets), so its true final frame is ~1.95s, not the
   ~3.05s naive sum — every code beat (2.1s onward) already runs after
   the AVD finishes, and the 4.4s exit leaves a quiet beat of
   stillness. Only the interpolators were modernized and the exit
   transition swapped to the f7_ pair.
4. Welcome child stagger reuses staggeredEntrance with the spread
   operator over welcomeContainer's 9 children (60ms beat; last child
   starts at 480ms, ends ~880ms) — no new view IDs introduced, keeping
   the frozen-XML contract.
5. Welcome is deliberately always-ink (gate stays parchment-on-ink in
   both modes). It uses @color tokens that stay ink-appropriate in both
   modes: bg_dark_surface has no night override (single definition
   #22344A), and night stone #B0A798 is MORE legible on ink than light
   stone. No ?attr usage there on purpose.
6. New resources all use the f7_ prefix per convention; no existing
   drawable/anim of another agent was modified (F9's splash AVD,
   btn_primary, bar_fill, ic_gate read-only).
7. No new string resources were needed — welcome reuses the existing
   bilingual @string/powered_by; all other referenced strings verified
   present in both locales (app_name_ar resolves from the default file
   by design, being the same Arabic wordmark everywhere).

## TESTS RUN
No Gradle builds permitted for this task, so verification was static:
- Full re-read of every edited file after editing (UiAnim.kt,
  MainActivity.kt, SplashActivity.kt, WelcomeActivity.kt, all layouts,
  all anim XMLs, both nav selectors, f7_input_bg).
- Grep verification that every referenced color token exists in both
  palettes (stone, stone_light, bg_dark_surface, green_12, teal night
  override) and every referenced string exists in both locales (the
  7-vs-6 count resolves to app_name_ar, defined once in the default
  file on purpose).
- Font existence check for @font/amiri, @font/cairo_extrabold,
  @font/plex_semibold.
- Consumer-map verification against the coordinator's map: fade_in/
  fade_out kept short (250/150ms) since they run on every nav switch;
  pulse.xml verified untouched with its reverse-mode infinite loop.

## TEST RESULTS
All 18 touched/created files verified well-formed; frozen IDs
(fragmentContainer, bottomNav, barFill, barLabel) and the UiAnim
surface confirmed intact by re-read. Arabic copy verified intact
(بنناديك إيه؟ (اختياري) / ابدأ رحلتي spot-checked in values-ar). No
build was run, so no compile proof exists — see KNOWN ISSUES 8.

## KNOWN ISSUES
1. Orphaned animations: card_in, header_in, pop_in, slide_in have ZERO
   consumers in the repo. Retimed onto the 400ms scale but not deleted;
   if the orchestrator wants them wired (feature cards, headers,
   dialogs) or removed, that is an integration-level call.
2. NavIndicator (res/values/themes.xml, F1's file) still carries the
   off-brand body #1A00BFA5. MainActivity's programmatic green_12 tint
   masks it at runtime, but the style should be fixed at the source by
   whoever owns themes.xml.
3. Splash AVD true end is ~1.95s (animators overlap; the ~3.05s figure
   from the consumer map was a naive startOffset+duration sum). Exit at
   4.4s outlasts both readings, so nothing was retimed; AVD retiming
   remains an integration recommendation only (F9's file, read-only
   here).
4. report_bar.xml is a weekly bar-chart row that StatsFragment
   inflates 7x — not a status overlay with a dismiss affordance as the
   brief implied. It was polished as what it actually is; no dismiss
   was added.
5. pulse.xml deliberately unchanged (mandate: keep its loop cadence
   recognizable). Consequently UiAnim.breathe loads it verbatim and
   overrides nothing — the reverse-mode cadence lives in the XML.
6. res/color/nav_item_color.xml (light) had a comment/code mismatch
   (comment said ink, code said stone); code kept as stone and comment
   aligned to match.
7. Two UiAnim defects found and fixed during self-check: (a) breathe
   previously set repeatMode = RESTART, overriding pulse.xml's mandated
   android:repeatMode="reverse" — override removed; (b) countUp's
   WeakHashMap guard entry never expunged (the AnimatorSet's listener
   closure strongly referenced its TextView key) — now removed in
   onAnimationEnd, which fires on both finish and cancel.
8. No build was run (prohibited by task rules), so compile-level proof
   is absent. Static verification only; risk is low (no signature
   changes, no new dependencies) but a ./gradlew assembleDebug by the
   orchestrator would confirm.
9. WeakHashMap is still keyed on TextView with an AnimatorSet value
   that transiently references the view until its run ends — acceptable
   window (bounded by the count-up duration) and cleaned deterministically.
10. f7_input_bg's stroke is a hardcoded #3D8A8272 (~24% stone) because
    the palette defines no low-alpha stone token; if F1 later adds one,
    this can be swapped to the token.

## NEXT DEPENDENCIES
- F1 (themes.xml owner): replace NavIndicator's #1A00BFA5 body with the
  brand teal at correct per-mode alpha (or adopt green_12 semantics).
  Once fixed at the source, MainActivity's programmatic tint can be
  dropped.
- F9 / integration (optional): retime the splash AVD's stroke-retire
  beat if a shorter splash is ever wanted; current code already
  outlasts it, so no action required.
- Integration (optional): wire or remove the four orphaned animations
  (card_in, header_in, pop_in, slide_in).
- F2's HomeFragment consumes pressable/staggeredEntrance/breathe/
  countUp unchanged — no coordination needed unless F2 wants the new
  fadeIn helper for secondary labels.
