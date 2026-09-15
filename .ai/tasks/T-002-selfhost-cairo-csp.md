---
id: T-002
title: Self-host Cairo + CSP + light/dark verification (P2-3)
type: refactor
status: DONE
owner: agent
dependencies: []
priority: high
estimate: M
---

# T-002 — Self-host Cairo + CSP + light/dark verification

## Objective
Eliminate Google Fonts leak and verify light `warm ivory` / dark `muted green` tokens meet §25-28 brand.

## Context
Audit P2-3: website leaks IP to Google Fonts, no CSP, light/dark tokens exist but not verified against §26. Current website uses `fonts.googleapis.com` + inline script without nonce. Need self-host + CSP meta + token audit.

## Scope
### Files
- `website/index.html` (modify) — replace Google Fonts link with self-hosted `@font-face`, add `<meta http-equiv="Content-Security-Policy">`
- `website/fonts/` (create) — woff2 files for Cairo 400/500/700/800 + IBM Plex Sans 400/500/600
- `website/index.html` CSS tokens verification (no code change, just audit)

## Implementation Requirements
1. Download woff2 for Cairo + IBM Plex Sans, add `@font-face` with `font-display:swap`, remove `preconnect` to googleapis
2. Add CSP: `default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; font-src 'self'; img-src 'self' data:;`
3. Verify tokens: light `warm ivory #F7F1E6` not pure white, dark `muted #1B1F3B/#242952` not pure black/navy — document verification

## Acceptance Criteria
- [ ] No request to `fonts.googleapis.com` (checked via `grep` + network)
- [ ] CSP meta present and allows inline script/style (site still renders)
- [ ] Light/dark tokens verified per §26 (record in decision log)

## Testing Requirements
- Unit: none
- Integration: `python3 -m http.server` + `curl` checks for font URLs + CSP header
- Regression: hero/demo/trust still render, RTL still

## Validation
- [ ] `grep -c googleapis website/index.html` == 0
- [ ] `grep -c Content-Security-Policy website/index.html` == 1
- [ ] Visual check: hero indigo, amber CTA, sage success

## Handoff
→ Next: T-003 orphan prune, needs: T-002 DONE
