---
id: T-005
title: Official website + download platform — download page + installation + release metadata
type: feature
status: DONE
owner: agent
dependencies: []
priority: high
estimate: M
---

# T-005 — Official website download platform

## Objective
Transform website CTAs from fake GitHub repo link to real release artifact with version, size, checksum, and installation guide.

## Context
Audit: website CTAs `https://github.com/s-a-m-y1/Ban-porn-sites` (repo root, no version) → fake per #28. APK exists `android/app/build/outputs/apk/debug/app-debug.apk` v1.0 code 1, no release hosting, no checksum, no install guide. Need official download per #28-30.

## Scope
### Files
- `website/index.html` (modify) — fix 2 CTAs to `download.html` or direct release asset
- `website/download.html` (create) — version, date, size, checksum, supported OS, install steps
- `website/install.html` (create) — 6-step flow + troubleshooting
- `website/privacy.html` (create) — from trust section
- `scripts/release.sh` (create) — build APK → checksum → GitHub Release via `gh`

## Implementation Requirements
1. Keep single-file architecture for index — add separate `download.html` reusing tokens (`--indigo`, Cairo self-host, CSP)
2. Download page: real `app-release.apk` from `android/app/build/outputs/apk/release/` (or debug for now) — show `versionName 1.0`, `versionCode 1`, `minSdk 24`, file size via `stat`, sha256 via `sha256sum`, release date `date -I`
3. Installation guide: DOWNLOAD→INSTALL→OPEN→GRANT VPN→ENABLE→DONE per #36, with `allow unknown sources` note
4. Release hosting: `gh release create v1.0.0 android/app/build/outputs/apk/release/app-release.apk --title "HISN v1.0.0" --notes-file CHANGELOG.md` (or static hosting if no gh)

## Acceptance Criteria
- [ ] `grep -c "Ban-porn-sites" website/index.html` == 1 (only trust footer GitHub link, CTAs point to `download.html`)
- [ ] `download.html` shows version, size, sha256, and link to `app-release.apk` exists
- [ ] `install.html` 6 steps, RTL, accessible
- [ ] No fake link — `curl -I` on download asset returns 200

## Testing Requirements
- Unit: none
- Integration: `python3 -m http.server` + `curl` download link 200, checksum matches `sha256sum`
- Regression: index hero/demo/trust still 666 lines core

## Handoff
→ Next: SEO/a11y/perf, needs: T-005 DONE
