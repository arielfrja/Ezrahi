# Todo — Audit Fix 1: Firestore SnapshotListener Memory Leaks

> Source: `../summaries/refactor-fixes.md` §1 Critical Bugs #1 — "Firestore SnapshotListener Memory Leak".
> Series: follow-up fixes to the modernization roadmap (Phases 0–7 done, HEAD `800732e`). This is a new numbering series: `todo-fix-N.md`.

## Objective
Every Firestore `addSnapshotListener` in the repository must be lifecycle-managed: attach inside `callbackFlow`, remove in `awaitClose`. No listener may outlive its collector (rotation/screen-close must not re-accumulate listeners → memory leak + multiplied Firebase read costs).

## Current State (audited)
`EzrahiRepositoryImpl` had 7 `addSnapshotListener` call sites but only 2 `awaitClose` (in `getUserEvents`). Leaky methods:
- `getEvents()` — listener attached per call, never removed
- `getEventUpdates(eventId)` — same
- `getParticipants(eventId)` — same
- `getMessages(eventId)` — same
- `getReports(actId)` — same

## Tasks
- [x] Wrap each of the 5 methods in `callbackFlow { ... awaitClose { registration.remove() } }`, preserving the offline-first pattern: listener writes to Room cache, emitted Flow observes Room (`dao.observe*`) inside the callbackFlow (inner `launch` job cancelled in `awaitClose`).
- [x] Keep `getUserEvents` as-is (already correct).
- [x] Add private mapper extensions (`toFieldEvent`, `toEventParticipant`, `toFieldMessage`, `toFieldReport`) to avoid duplicated mapping blocks.
- [x] Remove now-unused `flow.map` import (nothing else uses it).
- [x] Build green (`sh gradlew :app:assembleDebug`), APK staged.
- [ ] PO smoke test: app behaves identically (auth → picker → map → reports) — this is a behavior-preserving refactor.
- [ ] Commit; update `../summaries/refactor-fixes.md` (mark bug #1 done).

## Constraints / Notes
- No data-model or rule changes — Firestore writes/reads stay exactly as-is (rules already hardened in Phase 6).
- `getEvents()` has no UI consumer (picker uses `getUserEvents`) — fixed anyway (public API).
- Don't convert listeners into `await()` one-shot calls: live updates are required by the map/chat screens.