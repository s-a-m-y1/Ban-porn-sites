---
id: T-024
title: Interactive phone mockup — real HISN UI (protection, coins, progress)
type: feature
status: DONE
owner: agent/frontend
dependencies: []
priority: high
estimate: M
---

# T-024 — Interactive phone

## Objective
Device mockup shows actual HISN UI: protection toggle, blocked, stats, coins, progress, settings — scroll animates.

## Context
Current demo phone is fake (wiki/bank) not HISN UI.

## Scope
- `website/index.html` demo phone (modify) — replace fake with real HISN screens (5 states)
- `website/phone-mock.js` (create) — scroll-driven

## Acceptance Criteria
- [ ] Phone shows 5 HISN screens, animates on scroll
