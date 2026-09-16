---
id: T-015
title: Coins/Points system — backend source of truth
type: feature
status: DONE
owner: agent/backend
dependencies: [T-014]
priority: high
estimate: M
---

# T-015 — Coins system

## Objective
Backend-scoped coins with balance, earn/spend, history, anti-replay.

## Context
No coins exists. Need per-user balance, transaction log, reason, progress-linked, future levels.

## Scope
- `backend/src/coins/` (create) — `coins.module.ts`, `coins.service.ts`, `coins.controller.ts`, `entities/coin-transaction.entity.ts`, `entities/user-coins.entity.ts`, `dto/earn.dto.ts`
- `backend/src/app.module.ts` (modify)
- `android/app/src/main/java/com/contentfilter/app/CoinsRepository.kt` (create)
- `android/app/src/main/java/com/contentfilter/app/CoinsFragment.kt` (create)

## Implementation Requirements
1. UserCoins {userId PK, balance int default 0, updatedAt}, CoinTransaction {id, userId, amount +/- , reason, createdAt}, unique constraint on (userId, reason, createdAt) per day to prevent replay
2. Endpoints: `GET /api/coins/balance` (jwt), `POST /api/coins/earn` {amount>0, reason}, `POST /api/coins/spend` {amount>0}, `GET /api/coins/history`
3. Service: balance update in transaction, prevent negative, log reason, anti-replay idempotency key

## Acceptance Criteria
- [ ] Balance starts 0, earn +10 → 10, spend -3 → 7, history shows 2
- [ ] Negative spend → 400, replay same reason+day → no double credit

## Handoff
→ Next: T-016 progress
