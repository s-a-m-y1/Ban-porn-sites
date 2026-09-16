---
id: T-017
title: Support HISN section — message, email, payments config
type: feature
status: DONE
owner: agent/frontend
dependencies: []
priority: medium
estimate: S
---

# T-017 — Support HISN

## Objective
Independent Support section with message, email, payment methods from central config.

## Context
Need Support HISN (or better name: “ادعم حِصن”) per spec — short message, email, available payments, CTA, placeholders.

## Scope
- `android/app/src/main/java/com/contentfilter/app/SupportFragment.kt` (create)
- `android/app/src/main/res/layout/fragment_support.xml` (create)
- `android/app/src/main/java/com/contentfilter/app/SupportConfig.kt` (create) — central config
- `website/support.html` (create) — mirrors app
- `android/app/src/main/res/navigation/nav_graph.xml` (modify) — add support tab

## Implementation Requirements
1. SupportConfig: `email="support@hisn.app"`, `payments: [{id: "paymob", name: "Paymob", enabled: false, placeholder: true}, ...]`, `cta: "ادعم استمرار الحماية"` — single source, admin panel ready
2. Fragment: message, email `mailto:`, payments grid from config (show enabled only, placeholder text if none), CTA
3. Website support.html reuses tokens, RTL, CSP

## Acceptance Criteria
- [ ] Support shows message + email + payments from config (0 if none)
- [ ] CTA visible, payments driven by config, no hardcoded card numbers

## Handoff
→ Next: QA
