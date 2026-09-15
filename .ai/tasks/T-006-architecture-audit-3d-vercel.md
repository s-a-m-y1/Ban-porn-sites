---
id: T-006
title: Architecture audit — 3D website + Vercel + release hosting decision
type: chore
status: DONE
owner: agent/architecture
dependencies: []
priority: high
estimate: S
---

# T-006 — Architecture audit for autonomous master

## Objective
Decide keep vs extend for 3D shield, Vercel, and release hosting per #30.

## Context
Prompt #12-15 proposes premium 3D shield (IDLE→PROTECTED→THREAT→BLOCKED) with lazy loading and Vercel deploy. Existing website is 30K single-file, no framework, no vercel.json, no three.js, but already premium (Night Indigo, SVG draw, 666 lines, self-hosted). Need ADR.

## Scope
- `website/index.html` (audit)
- `vercel.json` (check absence)
- `android/app/build/outputs/apk` (artifact)

## Implementation Requirements
1. Inspect `website/index.html` SVG vs 3D tradeoff: performance (mobile fallback), bundle size, DPR, prefers-reduced-motion
2. Evaluate Vercel vs static GitHub Pages per #30 (cost, CDN, versioning)
3. Decision: **Keep 2D SVG for V1**, 3D as P3 extension point (lazy import `three` via dynamic import, fallback to 2.5D CSS) — reuse > replace

## Acceptance Criteria
- [ ] ADR-004 written: 3D deferred to P3, V1 keep SVG

## Handoff
→ Next: T-007, needs: ADR-004
