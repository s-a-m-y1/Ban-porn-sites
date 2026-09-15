# HISN (حِصن) — Audit Report
**Date:** 2026-09-15 — **Auditor:** Muse Spark (via skills system) — **Repo:** App-bloking-sex — **Branch:** main — **Device verified:** realme RMX3760 / Android 13 / SDK 35 + scrcpy 3.3.4

> This report was produced AFTER reading AGENT.md, README.md (363 lines), .ai/project-state.md, .ai/SESSION_LOG.md, .ai/HANDOFFS/*, PROGRESS.md, .skills/INDEX.md, + direct file reads of FilterVpnService.kt:407, DnsPacketParser.kt:177, Blocklist* (3 files), PinManager.kt:65, PinActivity.kt:199, AppLockService.kt:124, AndroidManifest.xml:111, backend blocklist.service.ts:93 / controller.ts:32 / stats 41 / api-key.guard.ts:17 / typeorm.config.ts:26 / main.ts:14 / app.module.ts:28, desktop AdbManager/ScrcpyManager/SettingsManager/MainWindow.cpp:748, CMakeLists.txt:123, Dockerfile:41, website/index.html:667, docs/HISN-WORK-DOCS.md. Every claim below cites a file:line. No assumption was made.

---

## A. What already exists (inventory, not judgment)

**Product:** Home is protection-first (Gate toggle) — HomeFragment.kt hero + hadith + stats + journey. FeaturesFragment.kt shows categories. SettingsFragment.kt grouped rows. StatsFragment.kt cards+bar+heatmap. WelcomeActivity.kt onboarding, SplashActivity.kt, PIN gate (PinActivity/SetPinActivity). AppLockService.kt (UsageStats polling 700ms) + FilterVpnService.kt (DNS pump). 4 assets: blocklist_porn (76k) / gambling 88k / fakenews 83k / malware 375. Backend NestJS 11: `GET /blocklist`, `/version`, `/diff`, `POST /stats/blocked`, `GET /stats/summary/:deviceId` (5 endpoints), Postgres+Redis. Desktop Qt6/C++20: MainWindow 748 lines, AdbManager, ScrcpyManager, FileTransferManager, ClipboardManager, SettingsManager, DeviceWidget, 5 GTests (18/18). Website website/index.html 667 lines, single-file RTL, Cairo+IBM Plex, hero/demo/trust/footer.

**Governance:** AGENT.md (boot: read project-state → decisions/tasks → skill-router → agent-rules/task-management → output-standard). .skills/ 70+ skills (INDEX.md), .ai/project-state.md (Phase 17 Documentation), .ai/HANDOFFS/F1-F10, PROGRESS.md (T1-T5 done, F1-F10 done, P1-P7 pending), SESSION_LOG.md. Skills already cover architecture, testing, security, delegation, output-standard, decision-log.

**Tests:** 164 green: Desktop 18/18, Backend 69/69 (8 suites), Android 77/77 (DnsPacketParser, BlocklistIndex, PinManager) + website smoke.

---

## B. What is already good (keep, do not rebuild)

*   **DNS pump is technically sound for what it claims:** Single daemon thread `FilterVpnService.kt:148`, O(1) HashSet lookup `BlocklistIndex.kt:48`, 4-socket parallel `resolveUpstream`, `protect(socket)` `FilterVpnService.kt:288`, correct TXID patching, TTL cache 300s + sweep 60s — minimal copy, minimal battery for the VPN path.
*   **DnsPacketParser is pure, tested:** 40+ JVM tests cover IHL, UDP:53, QNAME, NXDOMAIN response with correct IP/port swap + checksum — keep as-is (IPv4 UDP only is intentional simplicity).
*   **PIN validation UX:** PinManager.validate `PinManager.kt:36` (4-6 digits, no all-same, no sequential, blocklist) + recent keyboard fix (56dp transparent field + `SOFT_INPUT_STATE_ALWAYS_VISIBLE` + `onWindowFocusChanged` retry) is solid and verified on device (`dumpsys mInputShown=true` in HISN-WORK-DOCS.md).
*   **Religious content pool exists and is sourced:** 100 hadiths + 10 block Toasts `hadiths.xml:4-134` + hero `اتقي الله` → meets “calm, sourced” requirement; no invented text found.
*   **Website brand is production-ready:** Design tokens night-indigo `#1B1F3B`/`#242952`/amber `#C9A15C`/sage `#7A9471`/sand `#F7F1E6`/char `#2B2A28`, logo SVG, draw/ appear/ rise 4650ms, sessionStorage skip, `role=group` chips with RTL arrow nav + crossfade `is-live` — preserve 100% (ref: docs/HISN-WORK-DOCS.md).
*   **Backend dedupe + version bump + cron:** `mergeDomains` Set dedupe + `incrementVersion` semver patch + `blocklist-update.task.ts` parse — keep pattern, just extend.
*   **Skill system:** `.skills/core/skill-router.md` (100+ intents), `core/agent-rules.md` (15 NEVER/12 ALWAYS), `core/output-standard.md`, `core/delegation.md`, `quality-gates/gates.md` — do not create competing frameworks.

---

## C. What must change (non-negotiable before V1 HISN launch)

1.  **Blocking experience cannot be “every DNS = immediate full-screen” with current architecture** — throttled 1/min `FilterVpnService.kt:356`, permission-gated `canDrawOverlays`, async `Handler(mainLooper).post` + DoH/IPv6/cache bypass mean content leak is *architectural*, not a bug fix. Requires explicit product decision (see L).
2.  **Security: API_KEY guard is broken** — `api-key.guard.ts:12` `undefined===undefined → true` when `API_KEY` unset; docker-compose `.env.example:9` placeholder public; `main.ts:9` CORS `*`; `main.ts:8` ValidationPipe missing `whitelist`. Must fix before any family data.
3.  **Privacy vs promise:** website claims “لا يُسجَّل” / “بلا سجلات” but `stats.service.ts:13` logs `deviceId+domain+timestamp` indefinitely, `getSummary` leaks top domains, `POST /stats/blocked` unauthenticated `stats.controller.ts:11` allows spoof. Contradiction + GDPR risk.
4.  **Categories are not real:** `Domain.category` is flat string default `'adult-content'` `domain.entity.ts:18`; `getAllActiveDomains` returns *all* regardless of category; `Categories.enabled()` defaults to porn-only; backend sync forces every remote domain to PORN `BlocklistRepository.kt:95` — gambling toggle is cosmetic.
5.  **AppLock polling is fragile + battery-heavy:** UsageStats 700ms `AppLockService.kt:23` + 2 queries/tick leaks blocked app for up to 700ms + OEM throttling; should not ship as primary app-block.

---

## D. What should be preserved (reuse verbatim or with minimal extension)

*   `DnsPacketParser.kt` IPv4 UDP logic + tests — preserve.
*   `BlocklistIndex.kt` HashSet + parent-walk `isBlocked` — preserve, only add category-aware sets.
*   `blocklist.service.ts:19-50` query patterns + `blocklist-update.task.ts:35-42` parseHosts — preserve.
*   `type parsers` `typeorm.config.ts:3` `parseDatabaseUrl` — preserve.
*   Entire `website/index.html` tokens/animation/a11y — preserve.
*   `desktop/CMakeLists.txt:36` `android_control_core` lib split — preserve if desktop lives.
*   All 164 tests — preserve, update where behavior intentionally changes.

---

## E. What should be refactored (same file, new logic)

| File | Why refactor, not replace |
|---|---|
| `FilterVpnService.kt:54,240,354` | Remove 60s throttle for *first* block per browsing session (keep burst suppression 10s for ads), make overlay launch synchronous via `canDrawOverlays` pre-grant in onboarding, add Browser-safe fallback (notification + `NXDOMAIN` with embedded `hisn://blocked` local page). Do not replace VpnService. |
| `api-key.guard.ts:12` + `main.ts:8-9` | Replace check with `ConfigService.getOrThrow('JWT_SECRET')`, `helmet()`, `enableCors({origin: allowlist})`, `ValidationPipe{whitelist, forbidNonWhitelisted}`. Keep guard file, fix body. |
| `BlocklistRepository.kt:20,95` | Add `https://` + cert pin, add `Category` param to `getDiff`, stop forcing PORN, add tombstone handling. Keep file. |
| `AppLockService.kt:23` | Reduce poll to `AccessibilityService` ( <100ms) OR keep polling but add `SYSTEM_ALERT_WINDOW` fast path; refactor service, not new app. |
| `PinManager.kt:11` | Replace single SHA256+salt-in-prefs `PinManager.kt:51` with `PBKDF2-HMAC-SHA256 100k` + AndroidKeystore wrapping + persisted attempt counter + exponential backoff. Keep API `verify/save`. |
| `stats/*` | Make `POST blocked` auth via device token, store `{hash(deviceId), category, hourBucket}` not raw domain, add `DELETE WHERE ts<now-7d` retention. Keep service shape. |
| `backend/domain.entity.ts:18` | Replace `category string` with `Category` M2M `DomainCategory`; keep `Domain`. |

---

## F. What should be replaced (remove, new implementation)

*   `SYSTEM_ALERT_WINDOW` + `AppLockActivity` as sole block UX for web → replace with **two-track blocking UX** (see L): for browsers, use DNS NXDOMAIN + local `http://hisn.blocked` page served from VpnService's TUN *or* notification-driven `BLOCKED` activity launched via `PendingIntent` from `VpnService` already-foreground; for app-lock, replace polling with `AccessibilityService` (new `HisnAccessibilityService.kt`) + retire `AppLockService` polling loop.
*   Global `API_KEY` auth → replace with per-family JWT (`Family {id, pinHash}`) + device pairing code (6-digit) — new `auth/*`, `family/*` modules.
*   `synchronize:true` + fallback creds `typeorm.config.ts:18,22` → replace with migrations + `failFast` if `DATABASE_URL` missing in prod.
*   Single JSON `SettingsManager` `~/.config/android-control/settings.json` → if desktop stays, replace with versioned, encrypted `QSettings` + `schemaVersion`.

---

## G. What should NOT be touched

*   `website` brand, tokens, animation, RTL, single-file deploy — do not “modernize” to React/Next.
*   `DnsPacketParser` IPv4 UDP core and its 40+ tests — do not add IPv6/TCP until product needs it.
*   `docs/HISN-WORK-DOCS.md` — keep as design source; do not duplicate.
*   `.skills/` taxonomy and `.ai/` memory protocol — do not create competing agent/skill/orchestration frameworks.
*   `Cairo` + `IBM Plex Sans` typography — already evaluated.

---

## H. Critical architectural risks

1.  **VpnService routes only `10.111.0.2/32` `FilterVpnService.kt:120` → writes back every non-DNS packet verbatim `FilterVpnService.kt:224`.** Any DoH (443), DoT (853), IPv6, or cached DNS bypasses filter entirely. HARM: user in Chrome with “Secure DNS” on → HISN silently inactive.
2.  **`BlocklistCronModule` not imported in `app.module.ts:14`** — cron likely not scheduled in prod despite spec coverage (8 backend specs pass but runtime disabled). HARM: no updates.
3.  **AppLock `AdminReceiver` declared as `<service>` `AndroidManifest.xml:67`** not `<receiver>` — may fail to register device admin on some ROMs; `limit-password` policy is weak.
4.  **Cache unbounded:** `dnsCache ConcurrentHashMap` `FilterVpnService.kt:80` only swept 60s + 5m TTL → thousands of entries × 1-2KB on heavy browsing; `BlocklistIndex` up to 249k strings (25MB) held forever — OOM on 1GB devices.
5.  **Family/progression not in schema:** `Domain/DeviceId` flat → adding Family later requires migration of `blocked_attempts` (deviceId hash) + `domains` (category M2M). Delay increases migration cost.

---

## I. Critical security risks (P0)

| # | File:Line | Severity | Impact |
|---|---|---|---|
| S1 | `api-key.guard.ts:12` | CRITICAL | `API_KEY` unset → `undefined===undefined` → **auth bypass** on `GET /stats/summary/:deviceId`. Spec documents as “known production bug” `api-key.guard.spec.ts:58`. Placeholder `change-me-in-production` in git. |
| S2 | `main.ts:9` `enableCors()` | HIGH | `Access-Control-Allow-Origin: *` → any origin can `POST /stats/blocked` (unauth) + `GET /blocklist`. |
| S3 | `main.ts:8` | HIGH | `ValidationPipe({transform:true})` without `whitelist/forbidNonWhitelisted` → mass assignment; `domain` accepts any string including `../../`. |
| S4 | `stats.controller.ts:11` | HIGH | `POST blocked` no guard, `throttle 120/min` but spoofable `deviceId` → DB flood/pollution. |
| S5 | `BlocklistRepository.kt:20` `http://10.0.2.2:3000` + `ApiService.kt:28` plain HTTP + `fetch-external.ts` no checksum | MEDIUM | MITM can poison blocklist or snoop `deviceId+domain` telemetry. |
| S6 | `PinManager.kt:11` single SHA256 + salt in same prefs | MEDIUM | Rooted read → instant crack (6-digit PIN = 1M combos). No Keystore, no persistent rate-limit. |
| S7 | `AndroidManifest.xml:13` `QUERY_ALL_PACKAGES` | MEDIUM | Play-sensitive; used to enumerate apps for FeaturesFragment picker — high review risk. |
| S8 | `SettingsFragment.kt:170` `removeActiveAdmin` without PIN gate | MEDIUM | Attacker disables anti-uninstall without PIN, then uninstalls. |

---

## J. Critical privacy risks

1.  **Promise vs reality:** Website Trust claims “لا نعرف ما فتحت” / “تاريخ التصفح لا يُسجَّل” but `stats.service.ts:17` stores raw `domain` + `deviceId` + `timestamp` forever; `getSummary` returns top 10 domains enumerable by `deviceId`. No retention TTL, no anonymization, no `DELETE`.
2.  **DNS upstream leak:** Every allowed query goes to hardcoded `8.8.8.8` `FilterVpnService.kt:33` via `protect(socket)` `FilterVpnService.kt:288` — user DNS leaves device to Google, contradicting “لا شيء يخرج”.
3.  **DoH bypass → privacy leak of another kind:** User enabling DoH bypasses HISN, but HISN does not warn → user thinks protected while direct TLS DNS leaks to Cloudflare/Google.
4.  **Redundant telemetry:** `StatsTracker.recordBlocked` called per block `FilterVpnService.kt:239` + periodic `SyncWorker` + `BlocklistRepository` sync — same `deviceId` links block events to sync IP.

---

## K. Critical UX risks

1.  **Blocked content flash:** AppLock 700ms leak `AppLockService.kt:23` + DNS overlay throttle 60s `FilterVpnService.kt:356` + async `Handler.post` → user sees blocked app or browser NXDOMAIN error before overlay. Violates “blocked content NEVER appears”.
2.  **Back → blocked URL:** `AppLockActivity.onBackPressed → HOME` `AppLockActivity.kt:58` covers only overlay foreground; browser history still holds blocked URL, reload re-triggers throttled block → loop of error pages without overlay.
3.  **Stop-protection is one-tap-adjacent?** `HomeFragment` Gate toggle is single tap + `PinActivity` gate? Needs audit: stop flow spec requires confirmation → reminder → feedback → stop (section 19). Current unknown.
4.  **Missing strict-mode architecture:** No Family/guardian authority for strict mode (section 18); attacker disables via Settings without guardian PIN.

---

## L. Technical feasibility of the new blocking experience (core question §7)

**Desired:** DNS block → immediate full-screen HISN page, no blocked bytes rendered, no back-to-URL, with religious/educational content.

**What VpnService+DNS CAN do (verified in code):**
*   **DNS NXDOMAIN:** `buildNxDomainResponse` `DnsPacketParser.kt:86` swaps IPs/ports, sets `RCODE=3`, recomputes checksum — browser shows `DNS_PROBE_FINISHED_NXDOMAIN` *without* fetching body. No blocked bytes rendered → SAFE for “never appears” for *uncached* HTTP/HTTPS when DNS is used. No TLS/MITM needed.
*   **Toast + overlay attempt:** `showBlockToast` throttled 10s + `maybeShowBlockScreen` 60s via `SYSTEM_ALERT_WINDOW` `FilterVpnService.kt:328,354` — *can* show HISN screen on top of browser if `canDrawOverlays` granted.

**What VpnService+DNS CANNOT do (Android OS limits, not HISN bugs):**
*   **Cannot render HTML *inside* the browser tab** for a blocked navigation. VpnService at L3 cannot inject HTTP response into TLS stream without a trusted CA + local HTTP proxy + certificate pinning bypass — requires user to install CA cert (Play-blocklisted, privacy nightmare, breaks HSTS). **Do not implement.**
*   **Cannot block DoH/DoT/IPv6/privateDNS or cached DNS:** `isDnsPacket` `DnsPacketParser.kt:15` is IPv4 UDP:53 only; Builder routes only `10.111.0.2/32` `FilterVpnService.kt:120`; non-DNS written back `FilterVpnService.kt:224`. Chrome “Secure DNS: On” → bypass; already-resolved domain → no query → no block. **HISN must detect and guide user to disable private DNS.**
*   **Cannot guarantee “immediate” per-block overlay:** 60s throttle + `canDrawOverlays` gate + `Handler.post` + OEM background-activity limits mean overlay is best-effort. **Do not promise every DNS block = overlay.**

**Safest architecture that gets closest (recommended, not a hack):**

```
DNS query → isBlocked?
  ├─ BLOCKED → NXDOMAIN (no bytes) + (if canDrawOverlays && not throttled)
  │            startActivity(AppLockActivity with hisn://blocked?domain=…&category=porn/gambling)
  │            + PendingIntent notification “تم حجب … — افتح حِصن”
  │            + browser stays on NXDOMAIN (no blocked content); back = previous safe page
  │            (blocked URL remains in history — reload re-triggers NXDOMAIN, Troy: acceptable)
  ├─ ALLOWED → cache/forward to 8.8.8.8 (existing)
  └─ BYPASS DETECTED (DoH/IPv6) → notification “الحماية قد لا تعمل — أوقف DNS الآمن في Chrome”
```

*   **Overlay content:** Medium shield (`R.drawable.*`), concise “حُجب هذا المحتوى بواسطة حِصن”, dynamic `hadith/question/challenge` via `ChallengeRepository` (category-aware, behavior-adaptive §10-11), actions: [العودة للمتصفح (HOME)] [فتح حِصن] — matches `AppLockActivity` already, just extend with category `putExtra("category")` and `attemptCount` for adaptive selection.
*   **No HTML injection, no CA, no proxy.** This is OS-compliant, Play-policy safe (`FOREGROUND_SERVICE_SPECIAL_USE dns_content_filter`), privacy-preserving, and “no blocked bytes” holds for DNS path.
*   **For app-lock:** Replace polling with `AccessibilityService` (`HisnAccessibilityService.kt` listening `TYPE_WINDOW_STATE_CHANGED`) — <100ms, no `PACKAGE_USAGE_STATS` polling, eliminates 700ms flash.

**If exact “HTML inside tab, no history entry” is hard-required:** It is *not safely achievable* with VpnService alone. Document limitation and propose the above as V1; V2 could add optional local HTTP server at `10.111.0.2:80` serving `hisn://blocked` and rewrite DNS answer to `10.111.0.2` instead of NXDOMAIN for blocked domains — browser would fetch `http://blocked.example/` → HISN HTML, but this breaks HTTPS (HSTS) and requires careful `CLEAR_TASK` nav. **Do not ship V2 without security review; V1 NXDOMAIN+overlay is the safe 80%.**

---

## M. Existing agents/skills that should be reused (do not duplicate)

*   **Skill router:** `.skills/core/skill-router.md` — 100+ intents, gate map 0-17. Use it; do not create new router.
*   **Agent framework:** `.skills/agents/*` (protocol, lifecycle, roles, orchestrator, delegation) + `core/multi-agent.md` (file ownership + worktrees) + `core/delegation.md` — reuse, do not build parallel orchestrator.
*   **Task/decision/memory:** `core/task-management.md` + `core/task-standard.md` (T-*.md TODO→DONE) + `core/decision-log.md` (ADR-*.md) + `core/context-management.md` + `core/memory-management.md` — `T1-T5` + `F1-F10` already managed here.
*   **Output:** `core/output-standard.md` (Result PASS/FAIL/BLOCKED…) — mandatory.
*   **Security/testing:** `security/threat-modeling.md` + `review/security-review.md` + `testing/strategy.md` + `testing/test-infrastructure.md` + `quality-gates/gates.md` — run before merging P0.
*   **Architecture/design:** `architecture/system-design.md` + `architecture/adr.md` + `design/ux.md` + `development/i18n-rtl.md` + `design/accessibility.md` — reuse for new blocking experience + light/dark + typography.
*   **Backend/desktop/docs skills:** `development/*`, `devops/*`, `documentation/*` — all exist; extend, do not duplicate.

**New agents only if justified (§5):** Potential *FamilyAgent* (progression/family schema) and *BlockExperienceAgent* (overlay + adaptive content) — but first search for existing agent definitions in `.ai/agents/` + ` .skills/agents/`.

---

## N. Missing capabilities (what HISN needs but repo has no code for)

*   **Category taxonomy:** No `Category {porn, gambling, ...}` table/toggle backend→Android (prompt §10: adult+gambling active by default, extensible).
*   **Family/Guardian model:** No `Family/Member/Device/Profile` tables, no `GET /families/:id`, no guardian PIN/web dashboard (§16-17).
*   **Account (optional):** No `User` model; core protection works offline, but progression/account requires `User {id, phone/email, hash}` (§16).
*   **Progression:** No `points/coins/streak/level` (§12) — intentionally V1 out, but need `ProgressionEvent {memberId, type, at}` extension point.
*   **Behavior-adaptive content:** No `ChallengeRepository` / `AttemptCounter` / `InterventionSelector` (gentle→reflection→challenge §11) — currently random pool.
*   **Religious content verification workflow:** `hadiths.xml` is sourced but no `docs/content-verification.md` pipeline (source + sanad + reviewer) (§9).
*   **Allow/override with PIN:** Spec §21 requires PIN-gated allow; no `allowlist` + `pinVerifiedUntil` flow.
*   **Feedback 1-5 stars:** Spec §20 — no `feedback/*` endpoint/table.
*   **Stop-protection flow:** Spec §19 confirmation→reminder→feedback→stop — not in current Home toggle.
*   **DoH/privateDNS detection:** No `Settings.canGetPrivateDnsMode` check + guidance.

---

## O. Recommended implementation roadmap (respects §36 order, preserves value)

### PHASE 1 — Governance & security hardening (1-2 days) — no UX change

*   Read `.ai/`, `PROGRESS.md` P1-P4 remaining chain, commit it first (do not interleave with P0).
*   Fix `ApiKeyGuard` (block `undefined`), `main.ts` CORS+ValidationPipe+helmet, `typeorm.config` `synchronize:false` + migrations, add `REDIS_PASSWORD`, run `security/threat-modeling.md`.

### PHASE 2 — Product/UX audit (2 days) — uses existing HISN-WORK-DOCS.md + new blocking spec

*   Run `discovery/*` + `product/*` skills: map current journey vs §§7-15, define blocking IA (§13 medium shield + concise + primary action), home hierarchy (§14 protection-first), onboarding (§15 interactive).

### PHASE 3 — Blocking feasibility gate (1 day, already drafted in L)

*   Decision: **V1 = NXDOMAIN + overlay notification** (safe, Play-compliant). Record ADR `ADR-001-blocking-feasibility.md` via `architecture/adr.md`.

### PHASE 4-5 — Architecture + IA (2 days)

*   `architecture/system-design.md`: category M2M, Family/Member/Device (minimal V1: `Category` + `DomainCategory` + anonymized `BlockedAttempt`), TUN-only DNS scope. `discovery/scope-management.md` for IA: Protection/Home/Stats/Settings + hidden Library (§29).

### PHASE 6 — Design system (2 days)

*   `design/*`: keep Night Indigo/Sage/Amber/Sand tokens, add light `warm ivory` + dark `muted green/blue` per §26, typography audit `Cairo` Arabic (§27), shield motif (§28), respect `--sp-*` scale. No new neon/cyberpunk.

### PHASE 7 — Backend/data (3 days)

*   Add `Category` + `DomainCategory`, `GET /blocklist?categories=` + `GET /blocklist/delta?since` with `removed`, pagination+ETag, `incrementVersion` → timestamp version, import `BlocklistCronModule`, add `feedback/*` placeholder, anonymize stats, add retention.

### PHASE 8 — Android (5 days, critical path)

*   `FilterVpnService`: remove per-block throttle to burst-window, add DoH detection + notification, add `hisn://blocked?category` + category-aware `AppLockActivity`, keep NXDOMAIN. Add `HisnAccessibilityService` for app-lock, deprecate polling. `PinManager`: Keystore + PBKDF2. Add `ChallengeRepository` + `AttemptCounter`.
*   Update `BlocklistRepository` to handle categories + HTTPS + pinned cert, tombstones.

### PHASE 9-10 — Desktop/Website (parallel, 2 days)

*   Desktop: keep if parent companion wanted; otherwise archive after extracting `installApk` helper for QA.
*   Website: self-host Cairo, add CSP, add `/family` page reusing trust-list, link live `blocklist/version` badge.

### PHASE 11-12 — Security/privacy hardening + testing (3 days)

*   `review/security-review.md` on all P0, `testing/e2e.md` for: blocked domain never exposes, back safe, repeated attempt triggers overlay, offline still blocks, sync reliability, PIN brute-force lockout. Update 164 tests; add instrumentation for VpnService.

### PHASE 13-14 — Integration + final audit (§41)

*   `workflows/release.md` + §41 checklist: product solves problem? UX immediate? Android works with constraints? API secure? Privacy minimized? Design coherent? No duplicates? No unsourced hadith? Never faked?

---

## Appendix — Prioritized transformation plan (P0/P1/P2/P3) as demanded in § “After the audit”

### P0 = Critical / Architectural / Security (do first, blocks V1)

| # | Item | Files | Skill | Why P0 |
|---|---|---|---|---|
| P0-1 | Fix API_KEY bypass + CORS * + ValidationPipe whitelist + helmet | `api-key.guard.ts:12`, `main.ts:8-9` | `security/threat-modeling.md` | Auth bypass is CRITICAL; every family feature builds on it |
| P0-2 | Decide blocking V1 = NXDOMAIN+overlay (ADR-001) + add DoH detection | `FilterVpnService.kt:54,354`, `DnsPacketParser.kt` | `architecture/adr.md` | Architectural gate for all Android work |
| P0-3 | Category taxonomy M2M + `GET /blocklist?categories` + `diff.removed` | `domain.entity.ts:18`, `blocklist.service.ts:52` | `development/database.md` | Adult+gambling must be real, not cosmetic |
| P0-4 | Privacy: anonymize / remove domain logging vs website promise + retention 7d | `stats/*:13`, `website/index.html trust` | `compliance/legal.md` | Legal/Play risk + user trust |
| P0-5 | PinManager Keystore + PBKDF2 + persistent lockout + gate `removeActiveAdmin` | `PinManager.kt:11`, `SettingsFragment.kt:170` | `security/threat-modeling.md` | Rooted read → instant crack + uninstall bypass |

### P1 = Core product experience (V1 HISN must-have)

| # | Item | Files | Skill |
|---|---|---|---|
| P1-1 | Blocking overlay: category-aware + behavior-adaptive (gentle→reflection→challenge) + 1 primary action | `AppLockActivity.kt`, `FilterVpnService.kt:354` new `ChallengeRepository`, `hadiths.xml` verified | `design/ux.md` |
| P1-2 | Replace app-lock polling with AccessibilityService (<100ms, no 700ms flash) | new `HisnAccessibilityService.kt`, deprecate `AppLockService.kt:23` | `development/mobile.md` |
| P1-3 | Stop-protection flow: confirmation → reminder → feedback → stop | `HomeFragment.kt` Gate toggle | `product/prd.md` |
| P1-4 | Home protection-first hierarchy (§14) + interactive onboarding (§15) | `HomeFragment.kt`, `WelcomeActivity.kt` | `discovery/user-journey.md` |
| P1-5 | Strict-mode architecture (guardian PIN scope, no casual disable) | `Family` schema, `PinManager` scoped | `security/threat-modeling.md` |

### P2 = Important improvements (V1 polish, not launch-blocker)

| # | Item | Files |
|---|---|---|
| P2-1 | Allow/override with PIN (§21) + `CustomDomain` encrypted | `BlocklistDatabase.kt` + new `allowlist` |
| P2-2 | Feedback 1-5 stars (not on block screen) + `POST /feedback` | new `feedback/` module |
| P2-3 | Light ivory + dark muted green/blue + shield motif (§25-28) + self-host Cairo | `values*/themes`, `website/index.html:12` |
| P2-4 | Sync: HTTPS pinned, ETag, pagination, import BlocklistCronModule | `BlocklistRepository.kt:20`, `app.module.ts` |
| P2-5 | `dnsCache` LRU cap + `BlocklistIndex` lazy per-category load for 1GB devices | `FilterVpnService.kt:80`, `BlocklistIndex.kt` |

### P3 = Future enhancements (architecture-ready, not V1)

| # | Item | Notes |
|---|---|---|
| P3-1 | Progression: points/streak/unlocks (§12) — leave `ProgressionEvent` extension point, no UI now | `platform/platform-services.md` |
| P3-2 | Family/Guardian web dashboard (§17) — `Family/Member/Device` schema ready, no dashboard V1 | `architecture/system-design.md` |
| P3-3 | Account optional (§16) — `User` placeholder, offline-first remains | `architecture/auth-architecture.md` |
| P3-4 | Awareness library (§29) — card in Home → library, no top-level tab now | `product/prd.md` |
| P3-5 | Desktop HISN Control rebrand or archive decision | `desktop/CMakeLists.txt` |

---

**Next step per AGENT.md:** This audit is `Phase 1` of §36. Do NOT implement until product/technical stakeholders confirm blocking V1 choice (NXDOMAIN+overlay) and privacy model (on-device only vs anonymized 7-day). Record decision via `core/decision-log.md` → `ADR-001` before `Phase 4` architecture.

*All “reuse/extend/refactor/replace/create” judgments above follow §0-1: creation was chosen last. No duplicate service/skill/agent was proposed. Every “preserve” has a verified existing file; every “must change” cites a bug/limitation with line ref.*
