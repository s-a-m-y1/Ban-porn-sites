# Architecture — حِصن (HISN)

## System Overview
On-device DNS filtering via `VpnService` (no external server), NestJS blocklist API, Qt6 desktop controller, single-file website. See `README.md` diagram + `/.ai/decisions/ADR-00*`.

## ADRs
- **ADR-001** Blocking V1 = DNS NXDOMAIN + adaptive overlay (not HTML injection): `FilterVpnService.kt:86` NXDOMAIN + `BlockAttemptTracker` + `ChallengeRepository` 4-stage, `AppLockActivity` 2 buttons, `HisnAccessibilityService` <100ms. Safe, Play-compliant.
- **ADR-002** Privacy: on-device first, server 7-day anonymized `hash(deviceId)+category+hour` (current `BlockedAttempt` raw flagged for migration).
- **ADR-003** P3 extension points: `families/members` + `progression_events` not shipped, only `BlocklistService.getAllActiveDomains(categories?)` filter ready.

## Containers
- **android/**: `FilterVpnService` TUN `10.111.0.1/10.111.0.2` MTU 1500, `DnsPacketParser` IPv4 UDP:53 only, `BlocklistIndex` HashSet O(1), `BlocklistDatabase` Room `blocked_domains/category`, `PinManager` PBKDF2 100k + lockout, `Categories` porn+gambling default, `SyncWorker` 24h.
- **backend/**: `getAllActiveDomains(categories?)` + `getDomainsAddedSince(version,categories?)` + `In(cats)`, `POST /api/feedback` (1-5), `GET /api/blocklist?categories` + ETag future, `POST /api/stats/blocked` throttled 120/min, `GET /summary/:deviceId` ApiKeyGuard fail-closed.
- **desktop/**: `AdbManager` `listDevices` + `ScrcpyManager` `start/stop` + `SettingsManager` 4 tabs + `MainWindow` 748 lines, `LD_LIBRARY_PATH` fix.
- **website/**: `index.html` 666 lines, self-hosted 9 woff2 (Cairo+IBM), CSP `default-src 'self'`, tokens night-indigo `#1B1F3B` etc.

## Data Model (P0-3)
- `domains {id, domain UNIQUE, category adult-content|porn|gambling, active, addedAt}` + `blocklist_versions {version, updatedAt}` + `blocked_attempts {deviceId(64), domain, timestamp}` + `feedbacks {deviceId, rating 1-5, context, reason, comment, createdAt}` — future `families/members` deferred.

## Security
- `ApiKeyGuard` fail-closed + placeholder reject, CORS allowlist, `whitelist/forbidNonWhitelisted`, `synchronize:false`, PBKDF2 + `removeActiveAdmin` PIN-gated, `SYSTEM_ALERT_WINDOW` + `QUERY_ALL_PACKAGES` documented.

## Performance
- `dnsCache` LRU 500→400 + 60s sweep, 4-socket `protect()` pool, `BlocklistIndex` 76k-249k HashSet, `IMPORTANCE_MIN` channels.

## Decisions Log
- See `.ai/decisions/ADR-00*` + `.ai/reports/AUDIT-REPORT*` + `FINAL-REPORT`.

## Validation
- `backend 71/71` + `android 77/77` + `desktop 18/18` + on-device RMX3760 ping/overlay.
