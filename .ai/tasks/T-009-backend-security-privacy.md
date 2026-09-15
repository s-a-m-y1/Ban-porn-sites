---
id: T-009
title: Backend security + privacy — API_KEY re-audit + secrets
type: refactor
status: DONE
owner: agent/backend
dependencies: []
priority: high
estimate: S
---

# T-009 — Backend security re-audit

## Objective
Verify P0-1 fix (ApiKeyGuard fail-closed) and audit remaining secrets per #16, #24.

## Context
Previous audit found `process.env.API_KEY === undefined` bypass (fixed 71/71). Need to verify no commit of secrets, no hardcoded keys, TLS.

## Scope
- `backend/src/common/guards/api-key.guard.ts` (verify)
- `backend/.env.example` (verify placeholder not real)
- `backend/src/main.ts` (verify CORS allowlist)

## Acceptance Criteria
- [ ] `grep -r API_KEY backend --include=*.ts` shows fail-closed
- [ ] `npm test` 71/71 still
- [ ] No `.env` committed
