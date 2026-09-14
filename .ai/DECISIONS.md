# Decisions

## D1 — 2026-09-14: Agents do not commit; orchestrator owns git

Reason: parallel agents committing concurrently causes index-lock races and entangled diffs.
Affected tasks: T2, T3.
