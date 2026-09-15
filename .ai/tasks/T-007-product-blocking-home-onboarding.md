---
id: T-007
title: Product UX — blocking experience + home + onboarding verification
type: feature
status: DONE
owner: agent/product
dependencies: [T-006]
priority: high
estimate: M
---

# T-007 — Product UX verification

## Objective
Validate blocking flow (full-screen HISN, no blocked bytes, safe return) and home protection-first per #7-9.

## Context
P1 already shipped adaptive overlay (ChallengeRepository 4-stage) + HisnAccessibilityService + stop flow. Need to verify against #13 story (DEVICE→HISN→DNS→ALLOW/BLOCK).

## Scope
- `FilterVpnService.kt` (verify NXDOMAIN)
- `AppLockActivity.kt` (verify 2 buttons, no back to blocked URL)
- `HomeFragment.kt` (verify protection-first)
- `WelcomeActivity.kt` (verify onboarding VPN explain)

## Acceptance Criteria
- [ ] Blocked site shows HISN not site
- [ ] Back → previous safe page
- [ ] Home shows gate status first
