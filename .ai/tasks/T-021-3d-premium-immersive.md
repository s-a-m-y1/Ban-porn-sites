---
id: T-021
title: 3D premium immersive — fortress + scroll + micro-interactions
type: feature
status: DONE
owner: agent/frontend
dependencies: [T-018]
priority: high
estimate: M
---

# T-021 — 3D premium

## Objective
Make 3D feel distinctive HISN (fortress, not generic) with cinematic scroll, micro-interactions, responsive lighter.

## Context
Current shield-3d.js is basic extruded shape, not fortress, no scroll storytelling beyond 4 states, no parallax depth.

## Scope
- `website/shield-3d.js` (modify) — fortress geometry (walls + gate), particles, scroll camera, DPR cap, cleanup
- `website/index.html` — sticky hero, progress bar, parallax layers

## Acceptance Criteria
- [ ] Shield feels like fortress, calm
- [ ] Scroll transforms camera, not just rotation
- [ ] Mobile fallback lighter
