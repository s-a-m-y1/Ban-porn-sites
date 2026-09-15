# ADR-002 — Privacy Model: On-Device First, Server Anonymized 7-Day Aggregate
- Status: proposed (pending human confirm, P0-4)
- Date: 2026-09-15
- Deciders: audit + human (standing authorization)

## Context
Website Trust promises “لا يُسجَّل / بلا سجلات” but backend `stats.service.ts:17` logs raw `domain+deviceId+timestamp` forever, `GET /stats/summary/:deviceId` leaks top domains, `POST /stats/blocked` is unauthenticated spoofable. GDPR risk, contradicts brand, and future family/progression needs per-family scoping not raw history.

## Options Considered
| Option | Pros | Cons |
|---|---|---|
| A. On-device only (no server logs) | Zero privacy risk, matches promise, no retention | No family dashboard stats, no cross-device sync |
| B. Anonymized 7-day aggregate (recommended V1) | `POST` stores `hash(deviceId)+category+hourBucket` no raw domain, TTL 7d via cron, `GET summary` returns counts by category not domains, still allows “you blocked 12 today” | Top-domains list lost (intentionally — raw domains are sensitive) |
| C. Keep raw domains forever (status quo) | Keeps current tests green, top-domains feature | Violates promise, GDPR, deviceId enumerable |

## Decision
**Option B for V1 server, with on-device Room as source of truth for 24h detail.** `BlockedAttempt` on Android stays raw in `DailyStat` (local only, never synced with domain). Server `blocked_attempts` will store only anonymized `{hashDeviceId, category, bucket}` after P0-3 categories exist. Until categories ship, P0-4 implements intermediate: DTO `MaxLength+Matches` + `Throttler` stays + added `GET summary` hash-check + retention cron stub (7d) + documentation fixes in website Trust copy.

## Consequences
- Positive: Promise / implementation aligned; top-domains not exfiltratable via stolen deviceId.
- Negative: Backend `topBlocked` must be deprecated or return categories not domains — breaking change for any dashboard.
- Revisit trigger: If family dashboard requires per-domain insight, re-evaluate with explicit opt-in + E2E encryption + user-controlled toggle.

## Compliance
- Add cron `DELETE WHERE timestamp < now()-7d` (daily 02:00).
- Change `stats.controller.ts:11` to require `ApiKeyGuard` OR device token (after Family auth ships).
- Tests: update `stats.service.spec.ts` to expect hashed deviceId.
