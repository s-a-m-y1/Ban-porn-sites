---
id: T-016
title: Advanced progress system — level, XP, streak, achievements
type: feature
status: DONE
owner: agent/product
dependencies: [T-015]
priority: high
estimate: M
---

# T-016 — Progress system

## Objective
Professional progress UI backed by coins/XP, extensible to streaks/badges.

## Context
Coins is source of truth, progress derives level, next level, streak.

## Scope
- `backend/src/progress/` (create) — `progress.service.ts`, `progress.controller.ts` (GET /api/progress)
- `android/app/src/main/java/com/contentfilter/app/ProgressRepository.kt` (create)
- `android/app/src/main/java/com/contentfilter/app/ProgressFragment.kt` (create) — level, XP, next, streak, badges, history

## Implementation Requirements
1. ProgressService: level = floor(balance/100)+1, xp = balance, next = level*100, percent = (balance %100), streak from DailyStat consecutive days, achievements: first_block, 7_streak, 100_coins
2. Endpoint `GET /api/progress` returns {level, xp, nextXp, percent, streak, achievements[]}
3. Android: circular progress, level badge, streak flame, history list

## Acceptance Criteria
- [ ] Balance 0 → level 1, 0%, 150 → level 2, 50%
- [ ] Streak increments with daily blocked

## Handoff
→ Next: T-017 support
