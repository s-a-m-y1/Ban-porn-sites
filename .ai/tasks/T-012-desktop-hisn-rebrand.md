---
id: T-012
title: Desktop rebrand — Android Control → HISN
type: refactor
status: DONE
owner: agent/desktop
dependencies: []
priority: medium
estimate: S
---

# T-012 — Desktop rebrand

## Objective
Make desktop feel like HISN family: title, icon, colors share Night Indigo/Amber.

## Context
Desktop still "Android Control — USB • ADB • scrcpy" title, icon android-control.svg, not HISN shield.

## Scope
- `desktop/src/MainWindow.cpp` title, About dialog
- `desktop/resources/icons/hisn.svg` (create, copy shield)
- `packaging/desktop-entry/com.github.hisn.desktop` (create)

## Acceptance Criteria
- [ ] Title "حِصن — تحكم آمن" + About HISN 1.0
- [ ] Icon shows shield
