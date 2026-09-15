# F10 — Website v2 (single-file polish)

## TASK ID
F10-website-v2

## STATUS
DONE

## WHAT WAS IMPLEMENTED
Full v2 rewrite of the single-file Arabic RTL landing page (all CSS + JS
inline; no new files; no external assets beyond the pre-existing Google
Fonts links). All seven improvement areas from the brief:

1. Hero rhythm: intro draw-in sequence re-timed (mark outline draw 0.15s →
   fill 1.95s → slit 2.85s → h1 3.15s → sub 3.5s → actions 3.75s → note
   3.9s), new .hero-actions row, and a secondary anchor "جرّب الحماية أولًا"
   (#demo) with an underline-offset treatment and a chevron that slides
   down on hover. CTA gained full hover/focus/active states — hover inverts
   to indigo with a thin amber keyline (never disappears into the hero),
   active snaps in 0.06s.
2. Demo as the star: phone realism (status bar with 9:41 + speaker/wifi/
   battery glyphs, RTL browser chrome with reload icon and a lock-bearing
   URL pill with LTR direction and ellipsis overflow), ok↔blocked views now
   crossfade via opacity/visibility/transform with .is-live (NOT the hidden
   attribute — visibility transition-delayed 0s/.32s keeps the outgoing
   view inert), blocked screen is a full indigo gate treatment carrying
   the brand mark with the slit cut through to the surface. Chips are real
   <button>s with aria-pressed, radiogroup-style keyboard nav
   (Left/Down = next, Right/Up = prev — RTL-correct: ArrowLeft moves
   forward, plus Home/End), :focus-visible rings, and an sr-only
   aria-live polite region announcing each state change in Arabic.
3. Trust: 5-item list is now a hairline-grid card layout
   (auto-fit minmax(240px,1fr), 1px hairline gaps via background), each
   item led by a one-weight inline SVG icon (viewBox 0 0 32 32,
   stroke-width 1.6) alternating sage/amber accents, all RTL-correct.
4. Footer: mark + حِصن + HISN lockup in a hairline-bordered row, wordmark
   at 24px/800, letter-spaced Latin echo pushed to the inline-end, and
   the tagline "ما لا يليق لا يصل." beneath.
5. Global: :root extended with a 10-step spacing scale (--sp-1 4px →
   --sp-10 120px) used for every gap/padding, radii tokens, --maxw 1020px,
   phone/CTA shadows, dark+light hairline tokens; amber-on-indigo
   ::selection; smooth scroll gated behind no-preference.
6. A11y: skip link to #demo (translateY reveal on focus), single h1,
   reduced-motion media query blanket-kills ALL animation and transition
   (including every new v2 motion), body-text colors at ≥4.5:1
   (chip-url darkened to rgba(43,42,40,.68) to pass), consistent
   amber focus-visible rings with an indigo re-color inside the .on-light
   demo scope.
7. Local serve sanity check: attempted; see TESTS RUN.

Also harmonized one trust item from Egyptian colloquial ("أي حد يقدر
يراجعه… مفيش") to MSA to match the page voice.

## FILES CHANGED
- /home/sami/App-bloking-sex/website/index.html (only file; rewritten 420 → 668 lines)

No other files touched. No commits, no staging, no git write commands run.

## IMPORTANT DECISIONS
1. .skip-anim (returning visitor) reworked from the v1 blanket `*`
   animation kill to scoped intro-only selectors (.skip-anim .rise and
   .hero-mark parts with animation:none!important) — returning visitors
   now still get the demo crossfade, chip hovers, and link transitions;
   the reduced-motion query remains the only blanket kill, as required.
2. View crossfade uses stacked absolute views + opacity/visibility/
   transform with .is-live; JS mirrors state with aria-hidden (not the
   hidden attribute) and a polite live region announces "url — status"
   so the demo is fully usable non-visually.
3. The mark's slit (rect) fills with a per-call-site --mark-surface CSS
   var (defaults to indigo), so the same SVG reads as a cut-through on
   the hero, the blocked screen, and the footer without duplicating code.
4. Keyboard nav treats the vertical chip column + RTL page together:
   ArrowLeft/ArrowDown = next, ArrowRight/ArrowUp = prev (in RTL,
   ArrowLeft is the visually-forward key); selection follows focus.
5. Dead .latin utility removed after a defined-vs-referenced class audit
   (IBM Plex is applied directly at each call site); .on-light is a
   descendant scope (.on-light :focus-visible), not an unreferenced class.
6. Kept 100vh + 100svh double-declaration, pathLength="100" dash-draw
   technique, sessionStorage 'hisn_intro_played' with 4650ms write
   (= last delay 3.9s + duration 0.75s), and try/catch around storage.

## TESTS RUN
1. Python static-verification script over the final file (tag balance,
   CSS/JS brace + paren balance, defined-vs-referenced class audit,
   JS-referenced id audit, required tokens: sessionStorage flag, GitHub
   CTA URL, dir=rtl/lang=ar, prefers-reduced-motion, --mark-surface,
   aria-pressed, role=group, skip-link).
2. Manual review passes over: full <script> block, hero/demo/trust/footer
   markup, head + :root tokens, hero CTA states, view-block/chip CSS,
   reduced-motion + skip-anim rules, mark-surface consistency at all
   three call sites, initial aria state (viewOk live + first chip
   aria-pressed=true + viewBlock aria-hidden=true).
3. python3 -m http.server + curl sanity check: attempted 5 times across
   the session; every attempt was rejected with the Bash-classifier
   "temporarily unavailable" error. Waited 60–120s between retries per
   the task brief; read-only commands kept working throughout, so the
   outage was specific to the classifier, not the sandbox. Not retried
   further (task brief marks the serve check as optional "MAY").

## TEST RESULTS
1. Static script: PASS. div/span/section/p/button/a/svg/ul/li/main/
   footer/style/script/h1/h2 all balanced (path/rect are self-closing
   SVG voids — correctly unclosed). CSS braces 130/130, JS braces 15/15,
   parens 55/55. Zero classes defined-but-unreferenced and zero
   markup-classes-without-CSS after the .latin removal. All
   JS-referenced ids present (demoUrlText, okTitle, viewAnnounce,
   viewBlock, viewOk) + querySelector('.chips') target exists. Exactly
   one h1; skip-link target #demo exists; GitHub CTA URL present 3×
   (hero, trust body, trust-cta) with correct href; aria-pressed 4×,
   aria-live 1×, role="group" 1×, aria-labelledby 1×. No leftover
   __CHUNK markers from the chunked assembly.
2. Manual review: PASS. All three animation states (first visit,
   returning visitor via .skip-anim, prefers-reduced-motion) land on a
   fully-visible hero; the .rise opacity-0 base is countered in both
   the reduce block and the skip-anim scope. Footer/blocked-view marks
   sit on indigo surfaces matching their --mark-surface. Initial JS
   state matches server-rendered markup.
3. Serve + curl: NOT RUN (classifier outage; optional per brief).

## KNOWN ISSUES
1. The HTTP-serve sanity check could not be executed due to repeated
   Bash-classifier outages this session (same environment note as T3's).
   The file was instead verified statically (structure, syntax balance,
   reference integrity) and by full manual review of every block. First
   human/orchestrator run of any static server will confirm HTTP 200;
   no reason to expect otherwise for a plain index.html.
2. viewBlock carries aria-hidden="true" in markup AND its content is
   announced only via the live region; this is intentional (crossfade
   semantics) — the blocked screen's text reaches AT through the polite
   announcement, not through the hidden view itself.
3. The demo uses sessionStorage with a graceful catch, so browsers with
   storage blocked simply replay the intro — documented in-code.

## NEXT DEPENDENCIES
None for F10 — the task is self-contained; website/index.html is final
for this pass and no other file references it. If a future agent takes
a website-v3 pass, the whole token system + .is-live crossfade pattern
are in place to build on; anything visual can be re-tuned via :root
tokens without touching markup.
