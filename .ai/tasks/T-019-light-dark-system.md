---
id: T-019
title: Light/dark premium system — warm ivory vs muted green
type: refactor
status: DONE
owner: agent/design
dependencies: []
priority: medium
estimate: S
---

# T-019 — Light/dark

## Objective
Ensure light `warm ivory #F7F1E6` and dark `muted #0F1F1A` are intentional, not invert, with teal/green + sand accents per #12.

## Context
Current has indigo dark, sand light, but not full system — need `prefers-color-scheme` + `data-theme` toggle.

## Scope
- `website/index.html` CSS `:root` + `[data-theme="dark"]` + `prefers-color-scheme` media
- `website/shield-3d.js` — material color per theme

## Acceptance Criteria
- [ ] Light and dark both feel premium, not inverted
