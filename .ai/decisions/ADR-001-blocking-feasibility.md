# ADR-001 — Blocking V1 = DNS NXDOMAIN + Overlay (not HTML injection)
- Status: accepted
- Date: 2026-09-15
- Deciders: audit (Muse Spark) + human (standing authorization “countion”)

## Context
HISN must show a full-screen protective experience the instant a blocked site is opened, without ever rendering blocked bytes. Prompt §§7-8 states the blocked content must never appear and back must not reopen it. Current code is a VpnService that routes only `10.111.0.2/32` (FilterVpnService.kt:120) and parses only IPv4 UDP:53 (DnsPacketParser.kt:15) — DoH/DoT/IPv6/cached DNS bypass it. The existing overlay is throttled 1/min and gated by SYSTEM_ALERT_WINDOW, launched via Handler(post) — not guaranteed per-block. We must decide what V1 can actually ship safely on Android without TLS interception.

## Options Considered
| Option | Pros | Cons | Fit with NFRs |
|---|---|---|---|
| A. NXDOMAIN + overlay notification (recommended) | No blocked bytes (NXDOMAIN), no TLS MITM, Play policy SAFE (`FOREGROUND_SERVICE_SPECIAL_USE dns_content_filter`), cheap, matches current DNS pump | Overlay throttled/best-effort, history still holds URL (reload = NXDOMAIN again), DoH bypass remains | Privacy+, Security+, Battery+, UX “good enough” |
| B. DNS → 10.111.0.2 + local HTTP server serving hisn://blocked HTML | Browser tab would show HISN HTML instead of NXDOMAIN error, can include category content, no CA needed for http | Breaks HTTPS/HSTS (https://blocked.example would still NXDOMAIN or cert error), adds HTTP server to VpnService, more code | UX++ but Security- (needs careful http-only), Complexity+ |
| C. Local HTTP proxy + user-installed CA → inject HTML into TLS stream | True in-tab HISN HTML for https, can strip history | Requires user to install trusted CA, breaks pinning/HSTS, Play policy HIGH risk, privacy nightmare, fragile on Android 14+ | Rejected — security/privacy violation, §42 “never fake” |

## Decision
**Option A for V1.** Every blocked DNS gets `buildNxDomainResponse` (RCODE 3, already in DnsPacketParser.kt:86) + `showBlockToast()` + `maybeShowBlockScreen(domain, category, attemptCount)` when `canDrawOverlays` and burst throttle allows (relax throttle to per-session window, not 60s wall). For DoH/private DNS, detect via `Settings.getString(private_dns_mode)` and post notification “الحماية قد لا تعمل — أوقف DNS الآمن في Chrome”. App-lock moves from UsageStats 700ms polling (AppLockService.kt:23) to `HisnAccessibilityService` (<100ms). No CA, no proxy in V1.

## Consequences
- Positive: “never appears” holds for DNS path (no bytes fetched); back goes to previous safe page; Play-safe; no cert management.
- Negative/accepted costs: Browser shows NXDOMAIN error when overlay throttled or permission denied (still no blocked content, but not the branded screen); DoH users see bypass unless guided; URL stays in history (acceptable: reload re-blocks).
- Reversibility: Easy — V2 can add local HTTP at 10.111.0.2 for http-only blocked hosts without changing DNS logic; decision is reversible, flagged by metric `overlay_shown_ratio`.
- Revisit trigger: If Play allows `VpnService` + local HTTP without CA and UX research shows NXDOMAIN error is perceived as “Android crashed”, revisit B with security review.

## Compliance
- Verify: `FilterVpnService.kt` NXDOMAIN path covered by `DnsPacketParserTest`; overlay launched with `FLAG_ACTIVITY_NEW_TASK` + `category` extra; throttling is per-domain burst window (10s) not wall 60s; DoH check in `HomeFragment.kt` onboarding.
- Gate: `review/security-review.md` must pass before merging P0-2.

