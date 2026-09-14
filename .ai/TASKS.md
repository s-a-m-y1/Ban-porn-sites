# Hisn — Task Board

Orchestrator session started 2026-09-14. Request: "continue and test".

## Tasks

| ID | Task | Status | Owner | Depends on | Files affected |
|----|------|--------|-------|------------|----------------|
| T1 | Desktop GoogleTest suite: run tests (spdlog via LD_LIBRARY_PATH) | [x] | orchestrator | — | none |
| T2 | Backend unit tests: write jest specs for blocklist/stats/guard, make `npx jest` pass | [x] | Agent A (backend-tests) | — | backend/src/**/*.spec.ts |
| T3 | Android unit tests: JUnit for pure-logic Kotlin classes, `testDebugUnitTest` green | [x] | Agent B (android-tests) | — | android/app/src/test/** |
| T4 | Website smoke check: serve `website/`, verify page + demo assets | [x] | orchestrator | — | none |
| T5 | Integration: review T2/T3 diffs, backend build, full test pipeline, commits | [x] | orchestrator | T1,T2,T3,T4 | commits only |

## Notes

- Working tree was clean at `1be3139` (landing page v2 + work docs).
- Agents do NOT commit; orchestrator reviews diffs and commits per task to avoid index-lock races.
- Production code is off-limits for T2/T3 unless a real bug is found — then document it in the handoff.
