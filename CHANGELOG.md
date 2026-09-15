# Changelog

## 2026-09-15 — 4038e59 feat(hisn): master transformation P0-P2
- Audit 164 green + ADRs 001/002/003
- Security: ApiKeyGuard fail-closed, CORS allowlist, ValidationPipe whitelist, typeorm synchronize:false + 71/71
- Android: PinManager PBKDF2 100k + lockout + Admin PIN-gate (77/77), categories porn+gambling default + adaptive overlay (ChallengeRepository/BlockAttemptTracker) + HisnAccessibilityService + stop-flow 3 dialogs
- Backend: `GET /blocklist?categories`, `FeedbackModule POST /api/feedback`, cron fix, LRU 500

## 2026-09-15 — e8834f3 feat(web): self-host Cairo + CSP (P2-3)
- 9 woff2 self-hosted, CSP `default-src 'self'`, no googleapis leak

## 2026-09-15 — T-001 DONE (191a208)
- On-device RMX3760 verification: pornhub NXDOMAIN + overlay, Home hierarchy

## Unreleased (P2-3 docs sync — T-004)
- docs/setup.md (LD_LIBRARY_PATH, CORS_ORIGIN, DATABASE_URL required)
- docs/architecture-docs.md (ADRs, containers, data model)
