# Ban porn sites — Content Filter (Android + NestJS)

Local DNS-based porn site blocker for Android using VpnService, with a NestJS backend for centralized blocklist management and Redis caching.

## Architecture

```
┌─────────────────┐         ┌──────────────────┐         ┌─────────────┐
│  Android App     │  HTTP   │   NestJS Backend  │         │  PostgreSQL │
│  (VPNService)    │◄───────►│   (REST API)      │◄───────►│  Database   │
└─────────────────┘         └──────────────────┘         └─────────────┘
                                       │
                                       ▼
                              ┌──────────────────┐
                              │  Redis Cache      │
                              └──────────────────┘
```

**How it works:**
1. The Android app runs a local VPNService that intercepts DNS packets.
2. Each queried domain is checked against a local Room (SQLite) blocklist — no internet needed for the check itself.
3. Blocked domains get an NXDOMAIN response; allowed traffic passes through untouched.
4. A WorkManager job syncs the blocklist with the backend every 24h (`/blocklist/version` → `/blocklist` or `/blocklist/diff`).
5. The backend merges the StevenBlack porn-only hosts list daily (cron) and caches responses in Redis.

## Repository layout

```
backend/    NestJS API (TypeORM + PostgreSQL + Redis + @nestjs/schedule)
android/    Android app (Kotlin, VpnService + Room + Retrofit + WorkManager)
docker-compose.yml   Postgres + Redis + backend, one command
```

## Backend quickstart

```bash
cd backend
npm install
cp ../.env.example .env    # adjust values
docker compose up postgres redis -d   # or install locally
npm run start:dev
```

Seed the blocklist from the external source (runs daily via cron too):

```bash
curl -X POST http://localhost:3000/api/blocklist/sync   # (optional manual trigger)
```

## Full stack with Docker

```bash
docker compose up --build
```

Endpoints:

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/blocklist` | Full active domain list (cached 1h) |
| GET | `/api/blocklist/version` | Current version string (cached 5m) |
| GET | `/api/blocklist/diff?since=1.4.0` | Domains added since version |
| POST | `/api/stats/blocked` | Report a blocked attempt (anonymous) |
| GET | `/api/stats/summary/:deviceId` | Stats summary (requires `X-Api-Key`) |

## Android app

Open `android/` in Android Studio, set your backend URL in `BlocklistRepository` prefs (`backend_url`, emulator default `http://10.0.2.2:3000`), build and run.

Key components:

| File | Purpose |
|---|---|
| `MainActivity.kt` | Toggle UI + stats |
| `FilterVpnService.kt` | Local VPN, intercepts DNS, returns NXDOMAIN for blocked |
| `DnsPacketParser.kt` | DNS packet parsing + NXDOMAIN response builder |
| `BlocklistDatabase.kt` | Room DB of blocked domains |
| `BlocklistRepository.kt` | Asset seed + backend sync |
| `SyncWorker.kt` | 24h periodic sync (WorkManager) |

## Notes

- DNS check is fully local (Room); backend only supplies list updates.
- `deviceId` is a random UUID — no personal data collected.
- Rate limiting: 30 req/min default (Throttler), stricter per-endpoint limits on stats.
- Set `API_KEY` env var on the backend and send it as `X-Api-Key` for trusted endpoints.
