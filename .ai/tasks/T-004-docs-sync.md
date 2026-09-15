---
id: T-004
title: Sync documentation — setup.md + architecture-docs.md + changelog
type: docs
status: DONE
owner: agent
dependencies: [T-002]
priority: medium
estimate: S
---

# T-004 — Sync documentation

## Objective
Update `docs/setup.md`, `docs/architecture-docs.md`, `CHANGELOG.md` to reflect P0-P2 changes (PBKDF2, categories, adaptive overlay, feedback API).

## Context
README 363 already expanded, but deep docs still point to old behavior (single category, SHA-256, no feedback). Need sync per `core/agent-rules` “update docs when behavior/contracts change”.

## Scope
- `docs/setup.md` (modify) — add `CORS_ORIGIN`, `DATABASE_URL` required, `LD_LIBRARY_PATH` note
- `docs/architecture-docs.md` (modify) — add ADR-001/002/003 summary + category M2M + BlockAttemptTracker
- `CHANGELOG.md` (modify) — entry 4038e59

## Acceptance Criteria
- [ ] `grep -q PBKDF2 docs/architecture-docs.md` == 1
- [ ] `grep -q "adult-content.*gambling" docs/setup.md` == 1

## Handoff
→ Next: none, project ready for P3 family
