---
id: T-020
title: Frontend auth/coins/progress/support integration
type: feature
status: DONE
owner: agent/frontend
dependencies: []
priority: high
estimate: M
---

# T-020 — Auth/Coins/Progress/Support in website

## Objective
Integrate backend auth/coins/progress/support into website header with balance, level, CTA.

## Context
Backend has /api/auth, /coins, /progress but website shows no auth UI, no balance, no progress.

## Scope
- `website/index.html` header — add auth state (login/signup vs balance/level) + support link
- `website/auth.js` (create) — fetch /api/me, /api/coins/balance, /api/progress
- `backend` already done

## Acceptance Criteria
- [ ] Header shows login or balance+level
- [ ] No fake data — real API or “—” if offline
