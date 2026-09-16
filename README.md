# HISN — Website Visual Audit + Landing Page Plan
> **Status: PLANNING — DO NOT IMPLEMENT YET.** Awaiting human review of this plan.
> Captured: 2026-09-16 on realme RMX3760 (Android 13) via `adb screencap` — REAL app, no mockups.

---

## 1. Current website audit
`website/index.html` (860 lines, single-file, vanilla JS):
- **Good:** self-host Cairo/IBM Plex (9 woff2), CSP, RTL, skip-link, `prefers-reduced-motion`, real download (`downloads/hisn-v1.0.apk` 9.2MB + sha256), 10 sections, 3D `shield-3d.js` (fortress walls/sphere/particles, lazy `three@0.160`, DPR≤1.5).
- **Problems found:**
  1. **Fake product visuals** — hero phone (WHAT IS section) is hand-built divs mimicking HISN, NOT the real app (violates "no invented interfaces").
  2. Demo phone shows fake wiki/bank content — not HISN's actual block screen.
  3. No light-mode design (dark indigo hero + white sections mix, no coherent dual system).
  4. Story order diverges from app reality (problem before product explanation).
  5. Floating hero cards clip at 320px (just fixed via `@media 480 hide`).
  6. No AR/EN switch (link only), auth links dead (`location.href` to .kt file — bug).
  7. No coins/progress/screenshots anywhere — despite systems existing in app.

## 2. Problems found (summary)
Fake UI > real UI; no screenshot use; weak light mode; dead auth links; generic SaaS mid-sections; animation only in hero (rest static); no scroll storytelling after hero.

## 3. Current HISN visual identity (from `android/app/src/main/res/values/colors.xml` + screenshots)
| Token | Value | Use |
|---|---|---|
| `ink` | `#16242F` | dark bg (NOT indigo — deeper blue-green) |
| `ink_2` | `#26374A` | dark surface |
| `teal` | `#2F7A6B` | primary accent (gate on, success) |
| `teal_night` | `#8FC4B6` | dark-mode teal |
| `brass` | `#B8863B` / `brass_light #CDA55C` | highlights, status dot |
| `clay` | `#B4573E` | error/blocked accents |
| `stone_light` | `#DCD3C6` / `parchment #F4F1EA` | light surfaces |
| `bg_light` | `#F7F5EF` | light bg |
**IMPORTANT:** Website currently uses `#1B1F3B` indigo — **WRONG vs app `#16242F` ink-teal**. Website must adopt app palette.

Typography: **Cairo** (UI, 400/500/700/800) + **Amiri** (اتقي الله hero, religious) + **IBM Plex Sans** (latin/numbers). Radius: 8/12/16/24dp. Terminology: البوابة (gate), اتقي الله, خُطوة إلى النور, اليوم ٢ من رحلتك.

## 4. Screenshot inventory (17 PNGs, real device)
```
docs/hisn-screenshots/
01-splash/splash.png                     — ink bg, gate AVD, teal line
02-home/home.png, home_gate_closed.png   — greeting+sami, gate مغلقة, hadith card, stats 84/76771
03-protection/features_categories.png    — حظر المواقع الإباحية/القمار toggles
04-block-screen/block_overlay.png        — اتقي الله 52sp Amiri + brass rule + domain
05-pin/pin_entry.png                     — dots+keypad, teal success/clay error
06-settings/settings.png, settings_logout_row.png — grouped rows: لغة/وضع/PIN/مسؤول/تشغيل/VPN
07-stats/stats.png, stats_light.png      — cards+bar+heatmap
08-auth/login.png, signup.png           — Cairo labels, teal primary btn
10-light-dark/home_dark.png, home_light.png, settings_dark.png, settings_light.png — BOTH MODES REAL
11-states/support_tab.png                — ادعم حِصن + email + payments placeholder
```
Missing (todo in capture phase 2): onboarding Welcome, schedule dialog, feedback 1-5 stars, EN locale variants.

## 5. New visual direction
**"التطبيق نفسه جاء للحياة"** — ink-teal cinematic fortress (matches app `#16242F`+`#2F7A6B`), NOT website's current indigo. Calm, spiritual, premium. Glow = teal on dark / brass on light.

## 6. Landing page structure
1. HERO — real home screenshot in 3D phone + fortress backdrop
2. WHAT IS HISN — app identity, gate concept (from `how_it_works_body` strings)
3. THE PROBLEM — from app journey (المشكلة ليست في إرادتك)
4. HOW HISN PROTECTS — 4-layer diagram DEVICE→HISN→DNS→ALLOW/BLOCK
5. REAL APP EXPERIENCE — scroll through REAL screenshots (home→stats→features)
6. BLOCK EXPERIENCE — real `block_overlay.png` + intervention stages
7. FEATURES — real capabilities (PIN/Admin/Accessibility/schedule/applock)
8. COINS/PROGRESS — real (backend source of truth, screenshots from app when shipped)
9. PRIVACY — trust list (existing copy is good)
10. DOWNLOAD — Android v1.0 9.2MB sha256 + Desktop, install.html
11. SUPPORT — support_tab content
12. FOOTER

## 7. Hero concept
Phone (real `home_gate_closed.png` on 3D device) enters from depth → gate closes → particles settle → mouse tilts phone (±8°) → scroll dollies camera out revealing fortress silhouette → headline `حافظ على تركيزك` rises. 2D fallback: static screenshot + SVG gate.

## 8. 2D strategy
SVG gate mark (existing), 4-layer diagram (shield-story, animated path draw), feature icons (trust-icon family), hadith card, floating UI chips (real terminology), section transitions (GSAP or CSS scroll-driven).

## 9. 3D strategy
`three@0.160` lazy (keep current loader): device mockup (rounded box + screenshot texture via `CanvasTexture` from PNG), fortress walls + sphere (already built), particles teal (recolor from amber), fog ink. Mouse→rotation, scroll→camera Y, hover→elevation. Mobile: no particles, DPR 1.

## 10. Animation plan (per section)
| Section | Appears | Trigger | Purpose | Mobile | Reduced-motion |
|---|---|---|---|---|---|
| Hero | phone rise+gate close | load | product = real app | same, lighter | static |
| What is | text stagger | scroll-in | clarity | same | fade only |
| Problem | cards stagger | scroll | empathy | 1col | fade |
| How | path draw | scroll | education | vertical | static SVG |
| App exp | screenshots crossfade | scroll snap | credibility | swipe | static |
| Block | overlay slides over browser mock | scroll | the promise | same | static |
| Features | cards + icon pop | scroll | proof | 1col | fade |
| Coins | counter-up | scroll | reward feel | same | static number |
| Privacy | list stagger | scroll | trust | same | fade |
| Download | CTA glow | hover | conversion | tap | static |
| Support | fade | scroll | community | same | fade |

## 11. Real app screenshot integration
All 17 PNGs → `website/screens/` (copied, optimized ≤150KB each). Used as: 3D phone textures (hero, app-exp), floating cards (block, coins), static lightbox. **No invented UI anywhere.**

## 12. Color system (website = app)
Dark: bg `#16242F`, surface `#22344A`, elevated `#26374A`, text `#F4F1EA`, teal `#8FC4B6`, brass `#CDA55C`.
Light: bg `#F7F5EF`, surface `#FFFFFF`, text `#182634`, teal `#2F7A6B`, brass `#B8863B`, clay `#B4573E` (errors only).

## 13. Typography
Cairo (ar UI), Amiri (اتقي الله only), IBM Plex Sans (latin/URLs/numbers). Scale: 48/32/24/18/16/14/12. Line-height 1.6-1.85 (matches app).

## 14. Responsive strategy
Breakpoints 480/720/1024/1440. Desktop: side-by-side hero. Tablet: stacked, 3D on. Mobile: own composition — single column, screenshots full-bleed, 3D minimal (phone only), no fortress walls.

## 15. Mobile strategy
Phone-first story: real screenshots vertical swipe; particles off; DPR 1; fortress → flat SVG silhouette; CTA sticky bottom on download section.

## 16. Performance strategy
Lazy `three` (existing), screenshots `loading=lazy`, woff2 subset (existing), no GSAP if CSS scroll-driven suffices (evaluate in impl), total JS <25KB, LCP = hero screenshot <200KB.

## 17. Accessibility
Contrast AA (teal on ink = 6.9:1 ✓), keyboard chips (existing pattern), `aria-live` for demo, focus-visible, skip-link, reduced-motion full fallback (static).

## 18. Download experience
Prominent: WHAT (حِصن فلترة DNS), PLATFORM (Android 7+), FILE (hisn-v1.0.apk 9.2MB v1.0), SHA256 full + copy btn, install.html 6 steps, Desktop secondary (GitHub releases).

## 19. Support section
From `SupportConfig`: message, `support@hisn.app` mailto, payments placeholder (Paymob/Fawry/Vodafone Cash — disabled, "قريباً"), CTA mailto.

## 20. Technical architecture
Keep single-file + `shield-3d.js` (no React — site is static, Vercel zero-build; React adds bundle for little gain at this scope; GSAP optional via dynamic import only if CSS scroll-driven insufficient). New: `website/screens/` assets, `website/phone-3d.js` (device mock), palette swap to app colors.

## 21. Proposed components
`<section-hero>` (3D phone + fortress), `<app-showcase>` (screenshot scroller), `<block-demo>` (browser→overlay), `<diagram-flow>` (SVG 4-layer), `<coins-progress>` (fetch /api optional), `<download-cards>`, `<support-cards>`, `<theme-toggle>` (existing).

## 22. Asset requirements
17 screenshots (have), optimize to WebP ≤150KB (keep PNG originals in docs/), fortress silhouette SVG, gate mark (have), favicon (have).

## 23. Implementation phases (after approval)
P1: palette swap + screenshot integration + hero phone 3D (1d)
P2: story reorder + block demo + app showcase scroller (1d)
P3: animations + light/dark coherence (1d)
P4: QA responsive/a11y/perf + Vercel (0.5d)

## 24. Agent/task assignments
- agent/frontend: P1-P2 (components, 3D)
- agent/design: palette/typography/dark-light audit (P1, P3)
- agent/product: story order + copy from app strings (P2)
- agent/qa: P4 journeys + Lighthouse
- Tasks: will create T-026..T-029 in `.ai/tasks/` upon approval.

## 25. Risks & technical challenges
1. Screenshot textures on 3D phone need aspect fit (720×1600 → 9/19.5 crop).
2. CSS scroll-driven animation Safari <15 fallback → IntersectionObserver fallback.
3. Light mode needs full section redesign (current sections are dark-locked).
4. Amiri webfont not self-hosted yet (app has TTF; need woff2 conversion or fallback Cairo).

## 26. Definition of Done
- [ ] Every product visual = real screenshot (17+)
- [ ] Palette = app colors (ink/teal/brass, both modes)
- [ ] Hero: 3D phone w/ real home screenshot, mouse+scroll reactive, static fallback
- [ ] 11-section story per §6, each animated w/ purpose + reduced-motion
- [ ] Download: real APK + sha256 + install guide, obvious in <3s of landing
- [ ] Lighthouse ≥90 perf/a11y, LCP <2.5s, mobile excellent
- [ ] AR/EN toggle functional; auth links → real /download or app
- [ ] Vercel prod + all journeys verified on device

---
**STOP — plan complete. Awaiting review before implementation.**
