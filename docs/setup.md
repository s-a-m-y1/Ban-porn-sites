# Setup — حِصن (HISN) + Android Control

> Clean-env tested 2026-09-15 on Ubuntu 24.04, Node 22, Java 17, RMX3760 Android 13.

## Prerequisites (pinned)
- Ubuntu 24.04, `cmake 3.28`, `qt6-base-dev`, `libspdlog-dev`, `libfmt-dev`, `libgtest-dev`, `pkg-config`, `build-essential`, `android-sdk-platform-tools` (adb 34), `scrcpy 3.3`, `ffmpeg`
- Node `v22.23.2`, `npm 10`, Java `17` (Gradle 8), Python `3.12` (website)
- Postgres 16, Redis 7 (via `docker-compose.yml`)

## Steps

### 1) Clone + Backend env
```bash
git clone <repo> && cd App-bloking-sex
cp backend/.env.example backend/.env
# Edit backend/.env:
# DATABASE_URL=postgresql://blocklist:blocklist@localhost:5432/blocklist_db  # required in prod, no fallback
# REDIS_HOST=localhost  REDIS_PORT=6379
# API_KEY=<64-char random>  # required, NOT change-me-in-production (P0-1 fail-closed)
# CORS_ORIGIN=http://localhost:3000,https://s-a-m-y1.github.io
# EXTERNAL_BLOCKLIST_SOURCE=https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/porn-only/hosts
# BLOCKLIST_CRON=0 0 * * *
# PORT=3000
```

### 2) Database (no synchronize:true — P0-1)
```bash
docker compose up -d db redis  # from repo root, uses .env.example defaults
cd backend && npm ci && npm run build
# migrations: `synchronize:false` — run `npx typeorm migration:run` when migrations exist (currently autoLoadEntities, no migration yet)
```

### 3) Run backend
```bash
cd backend
npm test          # 71/71
npm run start:dev # http://localhost:3000/api/blocklist → ["example.com"]
curl http://localhost:3000/api/blocklist/version
```

### 4) Desktop (Qt6/C++20)
```bash
cmake -S desktop -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build --parallel $(nproc)
# libspdlog may be in /home/sami/.local/lib — use LD_LIBRARY_PATH if needed (P1 issue #libspdlog1.15)
LD_LIBRARY_PATH=/home/sami/.local/lib:$LD_LIBRARY_PATH ./build/android-control
LD_LIBRARY_PATH=/home/sami/.local/lib:$LD_LIBRARY_PATH ./build/tests/android_control_tests # 18/18
```

### 5) Android
```bash
cd android
./gradlew testDebugUnitTest   # 77/77
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.contentfilter.app/.SplashActivity
# Verify: Home “البوابة مغلقة” → ping pornhub.com → unknown host + overlay «التائب…»
```

### 6) Website (self-hosted fonts, CSP)
```bash
cd website
python3 -m http.server 8000
# http://localhost:8000 — no request to fonts.googleapis.com (self-hosted woff2 in fonts/), CSP meta present
curl -s http://localhost:8000 | grep -q "حِصن" && echo ok
```

## Full env matrix

| Var | dev (example) | staging | prod | Required |
|---|---|---|---|---|
| DATABASE_URL | `postgresql://blocklist:blocklist@localhost:5432/blocklist_db` | staging RDS | prod RDS + `?sslmode=require` | **yes in prod** (fail-fast) |
| API_KEY | `dev-secret-32+` | `staging-secret` | 64-char random, never `change-me` | **yes** |
| CORS_ORIGIN | `http://localhost:3000,http://localhost:8080` | `https://staging.hisn.app` | `https://hisn.app,https://s-a-m-y1.github.io` | no (defaults localhost) |
| REDIS_HOST/PORT | `localhost/6379` | `redis-staging` | `redis-prod` + `REDIS_PASSWORD` | yes |
| PORT | 3000 | 3000 | 3000 | no |

## Common Setup Issues

| Symptom | Cause | Fix |
|---|---|---|
| `libspdlog.so.1.15: cannot open` | lib in `/home/sami/.local/lib` not in `LD_LIBRARY_PATH` | `LD_LIBRARY_PATH=/home/sami/.local/lib:$LD_LIBRARY_PATH ./build/android-control` |
| `DATABASE_URL must be set in production` | `NODE_ENV=production` without `DATABASE_URL` | `export DATABASE_URL=postgresql://...` per matrix |
| `Server misconfigured: API_KEY missing` | `API_KEY` unset or `change-me` | `export API_KEY=$(openssl rand -hex 32)` |
| `pornhub.com` not blocked after install | `enabled_cats` still `porn` only (old pref) + DB missing gambling | Tap `الحماية` → toggle gambling off/on → `BlocklistIndex.load` OR `adb shell run-as … rm app_prefs.xml` then reinstall |
| `fonts.googleapis.com` still requested | Old `website/index.html` cached | Hard refresh `Ctrl+Shift+R`, verify `grep -c googleapis` 0 + `fonts/` contains 9 woff2 |

## Runbooks
- Alert `blocklist sync failed` → check `EXTERNAL_BLOCKLIST_SOURCE` reachable, `BlocklistUpdateTask` logs, `GET /api/blocklist/version` same.
