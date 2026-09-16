---
id: T-003
title: Prune 16 orphaned res files (12 drawables + 4 anims) — PROGRESS P1
type: chore
status: DONE
owner: agent
dependencies: []
priority: medium
estimate: S
---

# T-003 — Prune 16 orphaned res files

## Objective
Remove 16 zero-ref verified orphaned resources to clean build.

## Context
PROGRESS.md P1: 12 drawables + 4 anims verified zero-ref via grep. Previous auto-detection found 26 candidates including f9_* not yet wired. Need precise 16 list from Handoff F9. For now, skip auto-rm to avoid breaking build — verify via `grep -r @drawable/name` for each before rm.

## Scope
### Files
- `android/app/src/main/res/drawable/*.xml` (delete 12)
- `android/app/src/main/res/anim/*.xml` (delete 4)

## Implementation Requirements
1. Generate list via `python3` grep for `@drawable/name` / `R.drawable.name` zero hits
2. `git rm` each, `assembleDebug` must still BUILD SUCCESSFUL
3. Commit `refactor(android): prune 16 orphaned drawables and animations`

## Acceptance Criteria
- [ ] 16 files removed, `git status` shows deletions
- [ ] `assembleDebug` still BUILD SUCCESSFUL
- [ ] No reference to deleted names in code (`grep` 0 hits)

## Blocker
- Resolved: F9 list had 12 drawables, but re-scan after F2-F6 concurrent edits showed `chip_green` and `ic_power` now referenced (fragment_features:chip_green, fragment_settings:ic_power). Removed 10 drawables + 4 anims = 14 (instead of 16), build verified via `assembleDebug` BUILD SUCCESSFUL.

## Validation
- [x] 14 files removed (`git rm`), `assembleDebug` BUILD SUCCESSFUL (30s), `grep` 0 hits for deleted names
- Restored `chip_green.xml` + `ic_power.xml` as they are now live.

## Handoff
→ Next: T-004 docs sync, needs: T-003 DONE
