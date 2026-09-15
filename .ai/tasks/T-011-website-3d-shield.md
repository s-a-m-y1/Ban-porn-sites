---
id: T-011
title: Website premium 3D shield — IDLE→PROTECTED→THREAT→BLOCKED
type: feature
status: DONE
owner: agent/frontend
dependencies: []
priority: high
estimate: M
---

# T-011 — 3D Shield for website

## Objective
Add calm 3D shield as signature visual, with lazy loading, DPR cap, reduced-motion, fallback to 2D SVG.

## Context
Prompt #12-14: website must feel premium immersive, shield states, scroll storytelling, WebGL→2.5D→STATIC fallback, mobile lighter. Current is 2D SVG only (ADR-004 deferred).

## Scope
- `website/index.html` (modify) — hero-mark replace/enhance with <canvas id="shield-canvas">
- `website/shield-3d.js` (create) — Three.js r160 via CDN importmap, dynamic import, DPR Math.min(devicePixelRatio,1.5), compressed geometry, lazy
- `website/shield-fallback.css` (modify)

## Implementation Requirements
1. Dynamic import `three` only when `matchMedia('(prefers-reduced-motion: no-preference)')` and `WEBGL.isWebGLAvailable()`
2. States: IDLE (slow rotate), PROTECTED (glow), THREAT (pulse), BLOCKED (shake) — calm, not violent
3. DPR capped, geometry <10K vertices, dispose on page hide
4. Fallback: existing SVG if WebGL fails or reduced-motion

## Acceptance Criteria
- [ ] `grep three website/index.html` == 1 (dynamic import)
- [ ] Lighthouse perf >90, no CLS
- [ ] Reduced-motion → SVG only

## Handoff
→ Next: T-015 perf
