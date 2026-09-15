# ADR-003 — P3 Extension Points (Family/Progression) — No Overbuild
- Status: accepted
- Date: 2026-09-15
- Deciders: audit + human (standing authorization)

## Context
Spec §12-17 envisions progression (points/streak), family/guardian, account — but V1 must not overbuild. Current schema is flat (Domain/category, BlockedAttempt). Need clean extension points without premature complexity.

## Decision
**Do not ship P3 in V1.** Leave these extension points:

1. **Progression:** `progression_events` table stub `{id, deviceId_hash, type: unlock|streak|challenge_complete, ts}` + `BlockAttemptTracker` already has `totalToday/catCount` → future can derive `streak = consecutive days with totalToday==0 blocked?`. No UI now; `ChallengeRepository.Kind.CHALLENGE` already returns challenge on 4th attempt — future unlocks can hook there.
2. **Family:** `families` `{id, name, pinHash (PBKDF2), createdAt}` + `members` `{id, familyId, role: parent/child, ageBucket, categories: jsonb, deviceIds: jsonb}` — not created in DB V1, but `BlocklistService.getAllActiveDomains(categories?)` already supports per-member filter `?categories=`; `DeviceId` hash (ADR-002) maps to member.
3. **Account:** `users` placeholder — core protection works offline, `StatsTracker.recordBlocked` is local-first; server `feedback` already throttled/anonymized. Add `users` only after auth design (`architecture/auth-architecture.md`).
4. **Awareness library (§29):** Keep Home `hadithCard` (currently random hadith) — future library is `Home card → content library` not top-level tab. No new nav item now.

## Consequences
- Positive: No V1 bloat, migration cost minimal (add tables, not refactor), `AppLockActivity` adaptive already uses category/attempt so progression hooks are one line.
- Negative: Family dashboard not usable until P3 sprint — acceptable per §12 “NOT necessarily part of V1”.

## Compliance
- Check: `backend/src/app.module.ts` not yet importing `FamilyModule`/`ProgressionModule` — absence is expected. Review verifies no P3 code shipped.
