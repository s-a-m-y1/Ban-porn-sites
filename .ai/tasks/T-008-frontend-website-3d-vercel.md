---
id: T-008
title: Frontend website — Vercel config + download verification (no 3D V1)
type: feature
status: DONE
owner: agent/frontend
dependencies: [T-006]
priority: high
estimate: M
---

# T-008 — Website Vercel + download verification

## Objective
Add vercel.json and verify download/install/privacy routes per #15, #28-30.

## Context
Website now has download/install/privacy + self-host fonts + CSP, but no vercel.json, no build test, no artifact checksum verification in CI.

## Scope
- `vercel.json` (create) — cleanUrls, headers CSP
- `website/package.json` (check absent — keep no-build)
- `website/download.html` (verify link to downloads/hisn-v1.0.apk)

## Acceptance Criteria
- [ ] vercel.json exists, routes /download → download.html
- [ ] `npm run build` not needed (static)
- [ ] download link 200 via `curl -I`
