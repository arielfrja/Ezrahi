# Todo — Phase 6: Cloud Security & Rules Hardening

> Source: `../roadmaps/modernization-roadmap.md` "🔒 Phase 6: Cloud Security & Rules Hardening" (Days 33–36), Task 6.1.
> Phase 5 complete (commits `1890cf5`, `0fc44ae` on `refactor/modernization-v2`); legacy app fully deleted — rules no longer need to serve the old activities flow.

## Objective
Protect minor participants' real-time location data and restrict unauthorized database tampering using strict Firebase Cloud Security Rules — without breaking the modern single-activity app's data paths.

## Current State
- `firestore.rules` existed with Phase 0 legacy rules (`Users`/`Activities`/`ActUsers`/`Reports`) + roadmap-style `events` rules (open `isSignedIn()` everywhere, legacy collections open).
- Modern app data paths that the rules MUST keep working:
  - Auth (any signed-in user) → `events` reads (picker via `whereIn(documentId)` + `whereEqualTo("managerId")`) + `events` writes.
  - **`collectionGroup("participants")` query** in `getUserEvents` (event picker) — requires a `{path=**}` rule or the picker breaks (rules deny collection-group queries without a matching pattern).
  - `getParticipants`/`updateLocation` on `events/{eid}/participants/{uid}` — note the **manager may not be a participant** doc; manager read access must be granted explicitly.
  - `Reports` (legacy top-level collection, still the live report path): read `whereEqualTo("ActId")`, create with `ReporterId` = uid.
  - `Users` profiles: `registerUser` writes `Users/{uid}` (`Email`, `FirstName`, `LastName`, `Phone`, `LastUpdate`).
  - `messages` sub-collection: only `getMessages` listener + future sends (SOS uses `sendMessage`).

## Tasks

### Task 6.1: Update and deploy `firestore.rules`
- [x] Harden rules (see `firestore.rules`):
  - [x] Drop legacy `Activities`/`ActUsers` rules (nothing reads/writes them post-Phase 5).
  - [x] `Users/{userId}`: own-document read/write only (`uid == userId`).
  - [x] `Reports`: signed-in read; create only with `ReporterId == request.auth.uid`; no update/delete.
  - [x] `events`: read/create signed-in; update/delete manager-only.
  - [x] `participants`: read via `canAccessEvent` (participant **or manager**); write own doc only.
  - [x] `messages`: read via `canAccessEvent`; create participant-only with `senderId == uid`.
  - [x] **`match /{path=**}/participants/{userId}`** collection-group rule: read only own participant doc (required by `getUserEvents`).
- [x] Deploy: `firebase deploy --only firestore:rules` (CLI 15.8.0, project `ezrahi`) — dry-run passed, deployed 2026-08-19.
- [ ] Post-deploy verification: app still works — auth → picker (collectionGroup) → map (participants/reports) → add report (Reports create) → location updates (participants write).

### Task 6.2: Storage rules review (optional)
- [ ] Review `storage.rules` (currently referenced by `firebase.json`) — decide keep-as-is or harden (no storage paths used by the app yet).

### Task 6.3: Regression + docs gate
- [ ] Full modern smoke test after deploy.
- [ ] `todo-6.md` checkmarked; note any deferred items.
- [ ] Create `todo-7.md` for Phase 7 (Mesh/LoRa abstraction) after gate passes.

## Constraints / Notes
- **Collection-group queries are denied unless a `match /{path=**}/...` rule allows them** — this was the biggest deploy risk for the picker.
- Managers are not automatically participants; use `canAccessEvent` (participant OR manager) for content reads.
- `Reports` stays a top-level legacy-schema collection (Phase 0/3 path); `events/{eid}/reports` migration is NOT in scope for Phase 6.
- Rules simulate locally first: `firebase emulators:exec` or `firebase deploy --only firestore:rules --dry-run` (CLI supports `--dry-run` for rules) — use before real deploy.

## Definition of Done (Phase 6)
- [ ] `firestore.rules` deployed and app fully functional post-deploy.
- [ ] Anonymous/unauthenticated reads blocked everywhere; writes scoped to owner/manager.
- [ ] Event picker collection-group query verified working after deploy.
