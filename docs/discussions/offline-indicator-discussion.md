# Discussion: Offline / Queued-Status Indicator (deferred)

Context: During the Phase 2 smoke test, the PO observed there is no offline / queued-status indicator in the app and asked whether one should exist.

Decision: Deferred — PO to decide later. No change to Phase 2 scope.

Analysis — three distinct concepts often conflated as offline indicator:
1. Device connectivity banner — shows you are offline/online. Not in roadmap. Effort: medium (observe NetworkCapabilities).
2. Pending outbox count badge — shows N messages queued (incl. SOS). Not in roadmap, but natural Phase 2 companion. Effort: low — `TacticalDispatchEngine.pendingCount()` (TacticalDispatchEngine.kt:82) already exists; needs a reactive StateFlow + small UI badge.
3. Participant staleness tint — other people's ACTIVE/STALE/DISCONNECTED/FADED states. Already planned in Phase 3 (plan lines 356-369).

Recommendation (recorded for later): add concept 2 (pending-outbox badge) as a low-effort, high-value Phase 2 companion so users are not left wondering if an SOS was lost. Concept 1 is nice-to-have but larger. Concept 3 is already scheduled.

Note: absence of an indicator is NOT a Phase 2 defect — Phase 2 scope was only the queue + flush engine, which was verified working during the smoke test.
