---
id: T-030
title: Rebuild landing as documented React app — Tajawal, oklch tokens, AppTour 3GX
type: feature
status: IN_PROGRESS
owner: agent/frontend
dependencies: []
priority: high
estimate: L
---

# T-030 — Documented landing implementation

## Objective
Build the documented site (Tajawal, Tailwind v4 oklch tokens, 7 sections, AppTour 3GX 5-screen walkthrough) as a real React app per the pasted spec.

## Scope
- `website-v2/` (create) — Vite + React + TS + Tailwind v4
- `src/routes/index.tsx`, `src/routes/__root.tsx` (layout), `src/components/AppTour.tsx`, `src/styles.css`
- Root `vercel.json` → outputDirectory `website-v2/dist`

## Deviations from doc (documented)
1. `hero-shield.jpg` → inline SVG illustration (can't generate raster; crisp + themeable)
2. Tajawal **self-hosted** (not Google Fonts link) — HISN privacy promise "لا شيء يخرج" (same visual, no IP leak)
3. Stats: real figures (٧٦٧٧١ قائمة الحجب، ٨٤ محاولة، ٤ فئات) — doc's own note "replace with real figures"
4. Single static route (SPA) — head tags in index.html (SEO equivalent)

## Acceptance Criteria
- [ ] `npm run build` green; dist deploys to Vercel prod 200
- [ ] 7 sections + AppTour (5 screens, autoplay 5.2s, 3D transitions, reduced-motion guard)
- [ ] All colors via oklch @theme tokens, no hardcoded hex in components
