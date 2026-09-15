---
id: T-013
title: Onboarding interactive polish
type: feature
status: DONE
owner: agent/product
dependencies: []
priority: medium
estimate: S
---

# T-013 — Onboarding

## Objective
Make Welcome explain VPN simply, request overlay/battery in context, strict mode opt, confirm protection (per #18).

## Context
WelcomeActivity exists but not interactive per spec — need 3-step explain.

## Scope
- `android/app/src/main/java/com/contentfilter/app/WelcomeActivity.kt` (modify)
- `android/app/src/main/res/layout/activity_welcome.xml` (modify)

## Acceptance Criteria
- [ ] 3 steps: what→VPN why→permissions→strict→confirm
