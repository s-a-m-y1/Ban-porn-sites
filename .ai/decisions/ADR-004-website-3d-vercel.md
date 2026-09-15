# ADR-004 — Website 3D + Vercel: Keep 2D SVG for V1, Defer 3D

- Status: accepted
- Date: 2026-09-15
- Deciders: agent/architecture + human (autonomous prompt §12-15)

## Context
Prompt #12-15 proposes premium 3D shield (IDLE→PROTECTED→THREAT→BLOCKED) with lazy loading, DPR limits, WebGL fallback. Existing website is 30K single-file, no framework, no vercel.json, no three.js, but already premium (Night Indigo, SVG draw 1.8s, self-hosted woff2, CSP, 666 lines). Need to decide V1 scope vs over-engineering (#42).

## Options Considered
| Option | Pros | Cons |
|---|---|---|
| A. Keep 2D SVG (current hero-mark 150px, stroke-dash draw) | 0KB JS, `prefers-reduced-motion` already, no DPR/WebGL fallback, no build, GitHub Pages works | Less immersive than 3D |
| B. Add Three.js shield (dynamic import `three`, lazy, fallback 2.5D→2D→STATIC per #14) | Premium immersive, shield states, scroll storytelling | +150KB, needs `package.json`, `vercel.json`, DPR handling, mobile lighter strategy, extra QA |

## Decision
**A for V1, B as P3 extension point.** V1 ships 2D SVG; leave `website/shield-3d/` stub with `import('three')` lazy + `if (!WEBGL) fall back to SVG` per #14. Vercel vs GitHub Pages: keep GitHub Pages for V1 (static, no secrets), add `vercel.json` only for headers (`cleanUrls`, `CSP`) — static still works on both.

## Consequences
- Positive: No bundle bloat, no WebGL fallback complexity, `prefers-reduced-motion` already, download platform unaffected.
- Negative: Misses 3D “wow” for V1 — acceptable per #10 “Do not overbuild V1”.
- Revisit trigger: If conversion data shows need for immersive, implement B with `three@0.160` + `drei` + `r3f` dynamic.

## Compliance
- Check: `grep -c three website/` == 0 in V1 (verified), `vercel.json` will have `cleanUrls:true` + `headers` CSP.
