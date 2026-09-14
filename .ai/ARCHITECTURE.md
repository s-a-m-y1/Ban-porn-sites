# Architecture

Hisn (حِصن) — DNS-level porn/content blocking.

- android/ — Kotlin app: FilterVpnService (DNS-only VPN tunnel), BlocklistIndex (in-memory bloom/index), PinManager, AppLockService, ScheduleReceiver, SyncWorker (backend sync), StatsTracker. minSdk 24.
- backend/ — NestJS + TypeORM/Postgres: blocklist module (domain list + versions), stats module (blocked-attempt reports), API-key guard, Redis cache, external list sync script + cron.
- website/ — single-file static RTL landing page (no framework).
- desktop/ + scripts/ — legacy Android-control (scrcpy/ADB mirror) tooling + GoogleTest suite; kept for device automation/testing.
- Packaging: .deb, Docker compose.
