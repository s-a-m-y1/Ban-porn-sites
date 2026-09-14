# T2 — Backend unit tests (jest)

## TASK ID
T2-backend-tests

## STATUS
DONE

## WHAT WAS IMPLEMENTED
Created the backend's first jest spec files (0 existed at audit time),
covering the blocklist and stats modules end to end — services, controllers,
DTO validation, the API-key guard, the TypeORM config parser, and the
scheduled hosts-file update task. All specs are pure unit tests with
mocked repositories/cache/fetch; no database or Redis needed.

- blocklist.service.spec.ts (12 tests) — getAllActiveDomains (active-only,
  domain-column select), getCurrentVersion (latest + '0.0.0' fallback),
  getDomainsAddedSince (unknown-version fallback + query-builder path),
  mergeDomains (clean/dedupe/insert-new-only, custom category, empty input
  short-circuit, all-existing skip), incrementVersion (patch bump,
  '0.0.1' start, missing-patch-segment handling), invalidateCache (both keys).
- blocklist.controller.spec.ts (9 tests) — delegation to the service and
  route metadata via Reflect (paths, HTTP methods, CacheInterceptor +
  TTLs on /blocklist and /blocklist/version, no caching on /blocklist/diff).
- blocklist-update.task.spec.ts (9 tests) — parseHostsFile (hosts format,
  comments, IPs, dotless entries) and updateFromExternalSource (happy
  path merge→version→cache-invalidate, no-op without config, error
  handling for HTTP failure / fetch rejection / DB failure).
- stats.service.spec.ts (4 tests) — recordBlockedAttempt (lowercasing),
  getSummary (total + 24h window + top-10 query-builder chain, empty device).
- stats.controller.spec.ts (5 tests) — delegation + route metadata
  (throttler 120/min on POST /stats/blocked, ApiKeyGuard on summary,
  unguarded reporting).
- report-blocked-attempt.dto.spec.ts (9 tests) — class-validator rules
  for deviceId (required/string/non-empty/≤64) and domain.
- api-key.guard.spec.ts (7 tests) — matching, rejection, case sensitivity,
  header-name strictness, non-string values.
- typeorm.config.spec.ts (10 tests) — parseDatabaseUrl (all components,
  port default, percent-encoding, credential defaults) and factory config
  (env override, dev/prod synchronize flags).

## FILES CHANGED
- backend/src/blocklist/blocklist.service.spec.ts (new)
- backend/src/blocklist/blocklist.controller.spec.ts (new)
- backend/src/blocklist/blocklist-update.task.spec.ts (new)
- backend/src/stats/stats.service.spec.ts (new)
- backend/src/stats/stats.controller.spec.ts (new)
- backend/src/stats/dto/report-blocked-attempt.dto.spec.ts (new)
- backend/src/common/guards/api-key.guard.spec.ts (new)
- backend/src/database/typeorm.config.spec.ts (new)

No production files modified.

## IMPORTANT DECISIONS
1. No mock of TypeORM's generic `save<T extends DeepPartial<Entity>>` via
   `mockImplementation(async (e) => e)` — that signature fails tsc against
   the generic overload. Where the service discards save's return value,
   the default `jest.fn()` mock is used instead (blocklist.service.spec).
2. Route/throttle/cache expectations are asserted through Nest's Reflect
   metadata keys rather than spinning up the full testing module — keeps
   the specs pure-JVM-fast and avoids CacheModule/Throttler wiring.
3. class-validator/class-transformer are exercised with real
   `plainToInstance` + `validate` calls (they are pure JS libs) rather
   than mocking them.
4. api-key.guard.spec.ts pins a PRODUCTION BUG as a test (see KNOWN ISSUES)
   rather than fixing it — task rules forbid touching production code.
5. typeorm.config.spec.ts restores process.env.DATABASE_URL / NODE_ENV in
   afterEach so specs can't leak env into other suites.

## TESTS RUN
Command (from /home/sami/App-bloking-sex/backend):
  npx jest          → Test Suites: 8 passed, 8 total. Tests: 69 passed, 69 total. (9.7s)
  npx tsc -p tsconfig.json --noEmit   → exit 0, no errors.

## TEST RESULTS
All 69 tests green across 8 suites; type-check clean.

## KNOWN ISSUES
Production observations pinned by tests (behavior as-built, not bugs
fixed — per task rules these were only noted):
1. ApiKeyGuard: when API_KEY is unset and the request carries no
   x-api-key header, `undefined === undefined` is true, so the request
   is AUTHORIZED. An unconfigured server is effectively unguarded on
   protected routes (GET /stats/summary/:deviceId). Recommend requiring
   API_KEY at boot (fail fast) or treating an unset key as reject-all.
   (test: "currently ALLOWS the request when API_KEY is unset...")
2. BlocklistService.mergeDomains inserts with a hardcoded default
   category 'adult-content' — any future category-aware flows must
   pass it explicitly (test documents the default).
3. Environment note: repeated Bash-classifier outages blocked the jest
   run repeatedly during the session; the run itself then passed with
   no further fixes once the save-mock lines were removed.

## NEXT DEPENDENCIES
None — task complete. Follow-ups worth their own sessions:
- Fix-fail-fast for missing API_KEY (production change, needs review).
- e2e tests with a real Postgres (testcontainers) once Docker is available.
