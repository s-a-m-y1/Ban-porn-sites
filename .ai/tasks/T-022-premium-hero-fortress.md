---
id: T-022
title: Premium hero — cinematic fortress + 3D shield + particles
type: feature
status: IN_PROGRESS
owner: agent/frontend
dependencies: []
priority: high
estimate: M
---

# T-022 — Premium hero

## Objective
Transform hero from SVG arch to cinematic fortress (mountains, fog, particles, glowing shield) with 3D + 2D integrated, AR/EN switch, login/signup.

## Context
Current hero is SVG 150px arch with 3D canvas overlay basic. Need majestic fortress per visual reference: mountains, atmosphere, clouds, glowing shield, depth.

## Scope
- `website/index.html` hero (modify) — add fortress background SVG/Canvas, particles, CTA AR/EN + login/signup
- `website/shield-3d.js` (modify) — fortress geometry, particles, mouse/scroll, DPR, fallback

## Acceptance Criteria
- [ ] Hero feels like entering HISN world, not SaaS
- [ ] CTA “ابدأ الآن” + “كيف يعمل؟” + AR/EN + تسجيل/إنشاء
- [ ] Works without WebGL (SVG fallback)
