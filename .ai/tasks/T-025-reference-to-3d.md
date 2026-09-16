---
id: T-025
title: Reference image → 3D fortress (premium hero)
type: feature
status: DONE
owner: agent/frontend
dependencies: []
priority: high
estimate: M
---

# T-025 — Reference image to 3D

## Objective
Take the provided HISN fortress reference (mountains, fog, glowing shield, shield sphere, particles, fortress) and recreate as interactive 3D hero (Three/R3F) with 2D fallback.

## Context
User provided reference image with 4 sections (hero fortress, phone mockup, HOW diagram, features). Wants planning via Agents file first, then image→3D per AGENT.md boot. Existing hero is SVG arch + basic 3D shield, not cinematic fortress.

## Scope
### Files
- `website/shield-3d.js` (modify) — fortress geometry, sphere, mountains, clouds, particles, glow, mouse/scroll
- `website/index.html` hero (modify) — add fortress background, floating UI cards around shield (حماية من المشتتات etc.)
- `.ai/agents/` (read) — plan via orchestrator/roles/protocol

## Implementation Requirements
1. Inspect `AGENT.md` + `.ai/project-state.md` + `.skills/agents/*` → plan via orchestrator
2. 3D: fortress on mountain (extruded + walls), shield sphere (transparent + glow), mountains backdrop (low poly), clouds/fog (particles + Fog), not neon
3. Interaction: mouse → shield tilt, scroll → camera dolly, chips → THREAT/BLOCKED
4. Perf: DPR 1.5 cap, 80/30 particles, lazy import, fallback SVG if WebGL/reduced-motion

## Acceptance Criteria
- [ ] Hero matches reference composition: LEFT headline + RIGHT fortress/shield sphere
- [ ] 3D feels alive but calm, not gaming
- [ ] Works without WebGL (SVG fallback)

## Handoff
→ Next: T-026 phone mockup 3D
