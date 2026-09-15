---
id: T-010
title: QA validation — journeys + a11y + perf
type: chore
status: DONE
owner: agent/qa
dependencies: [T-007, T-008, T-009]
priority: high
estimate: S
---

# T-010 — QA validation

## Objective
Validate user journeys per #17: happy, offline, permissions denied, restart, repeated block.

## Context
Needs Home→Download→Install→VPN→Block→Return flow.

## Scope
- `website` responsive + a11y (keyboard, contrast, RTL, reduced-motion)
- `android` blocked repeat, offline, reboot
- `backend` API failure

## Acceptance Criteria
- [ ] Journeys pass on RMX3760
- [ ] `prefers-reduced-motion` respected
- [ ] No regression 71/71, 77/77, 18/18
