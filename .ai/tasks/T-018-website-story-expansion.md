---
id: T-018
title: Website story — add PROBLEM/SHIELD/HOW/BLOCK/AWARENESS sections with scroll storytelling
type: feature
status: DONE
owner: agent/frontend
dependencies: []
priority: high
estimate: M
---

# T-018 — Website story expansion

## Objective
Transform 3-section site (hero/demo/trust) into full narrative per #4: INTRO→PROBLEM→SHIELD→HOW→BLOCK→AWARENESS→PRIVACY→DOWNLOAD→SUPPORT with scroll storytelling, parallax, 2D diagrams.

## Context
Current has hero/demo/trust only — missing PROBLEM, SHIELD, HOW IT WORKS diagram, BLOCK EXPERIENCE, AWARENESS. Need distinctive HISN story, not generic SaaS.

## Scope
- `website/index.html` (modify) — insert 5 new sections between demo and trust, each with `data-reveal` scroll, parallax `transform: translateY`, SVG diagrams
- `website/story.css` (create) or inline — section transitions, sticky 3D, progress bar

## Implementation Requirements
1. PROBLEM: stats + calm copy, no fear
2. SHIELD: 3D canvas sticky + 2D SVG fortress diagram
3. HOW: DEVICE→HISN→DNS→ALLOW/BLOCK diagram (4 steps, animated on scroll)
4. BLOCK: full-screen intervention preview (phone with “اتقي الله”)
5. AWARENESS: hadith/education card (from hadiths.xml pool)

## Acceptance Criteria
- [ ] 8 sections total, each reveals on scroll
- [ ] No fake content — all copy from HISN docs
- [ ] Reduced-motion → no parallax, static
