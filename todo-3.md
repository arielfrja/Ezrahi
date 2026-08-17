# Todo — Phase 3: Offline-First Data Layer with Room

> Source: `modernization-roadmap.md` "💾 Phase 3" (Days 12–17) + checklist items 3.1–3.4.
> Phase 2 is **complete** (`2ecaf60`): `FirebaseDataRepo` Hilt-bound, `DataRepoFactory` is an `@EntryPoint` facade. Smoke test passed.

## Objective
Build an offline-first data layer so the app retains cached events, participants, messages, and reports when there's no reception — reading instantly from Room, syncing from Firestore in real time.

## Current State (verified)
- **Room scaffold already exists** (matches roadmap 3.1–3.4 almost exactly, from pre-restore modern work):
  - ✅ `data/local/LocalEntities.kt`: `EventLocalEntity` (`cached_events`), `ParticipantLocalEntity` (`cached_participants`), `MessageLocalEntity` (`cached_messages`) — byte-for-byte the roadmap spec.
  - ✅ `data/local/EzrahiDao.kt`: `observeEvent`, `insertEvent`, `observeParticipants`, `insertParticipants`, `observeMessages`, `insertMessage` — matches roadmap.
  - ✅ `data/local/EzrahiDatabase.kt`: `@Database(entities=[Event, Participant, Message], version=1)`, `exportSchema=false`.
  - ✅ `di/DatabaseModule.kt`: Room builder `"ezrahi_local_db"` + `fallbackToDestructiveMigration()`, provides `EzrahiDao`.
  - ✅ `data/repository/EzrahiRepositoryImpl.kt`: Firestore listeners write into Room; `Flow` emits from Room (offline-first). Includes my Phase 1 additions (`getReports`/`addReport`).
  - ✅ Build green.
- **Gaps vs. roadmap / real gaps to close:**
  - ⚠️ **Reports are NOT cached offline.** `getReports()` uses a `MutableStateFlow` fed only by a Firestore listener (`EzrahiRepositoryImpl.kt:166-176`) — no Room table, so cached reports vanish when offline and the flow has no offline-first behavior. This is the main Phase 3 gap (report work continuation).
  - ⚠️ **Two Room databases coexist:** legacy `AppDatabase` (`"database"`, entities `ActUser`/`ActPermission`, `LocalDao`) built inside `FirebaseDataRepo`, and modern `EzrahiDatabase` (`"ezrahi_local_db"`). They are independent today (no migration conflict, but duplicated state). Roadmap doesn't address the legacy DB explicitly.
  - ⚠️ `EzrahiRepositoryImpl.getReports()` creates a *single shared* `MutableStateFlow` for all `actId`s — concurrent observers of different activities would clobber each other. Should be per-`actId` (e.g., `@Query` from Room or a keyed flow).

---

## Task 3.1: Room entities & DAOs — mostly DONE (verify + extend for reports)
- [x] `EventLocalEntity`, `ParticipantLocalEntity`, `MessageLocalEntity` present (match roadmap).
- [x] `EzrahiDao` queries/inserts present.
- [x] `EzrahiDatabase` + `DatabaseModule` present.
- [ ] Add `ReportLocalEntity` (`@Entity(tableName = "cached_reports")`): id PK, actId, reporterId, title, description, latitude, longitude, reportTime, status, type — mirrors `FieldReport`.
- [ ] Add DAO: `observeReports(actId): Flow<List<ReportLocalEntity>>` + `insertReports(List<ReportLocalEntity>)` (REPLACE on conflict).

## Task 3.2: Offline-first repository — extend for reports
- [ ] `EzrahiRepositoryImpl.getReports(actId)`: add a Firestore listener that writes report snapshots into Room (`insertReports`), then **emit from Room** (`dao.observeReports(actId)` → `FieldReport`) instead of the single shared `MutableStateFlow`.
- [ ] Remove the per-instance shared `reportsFlow`; verify no other consumers depend on it (only `getReports` used it).
- [ ] `addReport` stays as-is (writes Firestore); consider local insert on success for optimistic UI (optional, Phase 5+).

## Task 3.3: Verify offline-first behavior
- [ ] Confirm event/participant/message flows already read from Room first (they do — `dao.observe*().map {...}`).
- [ ] Confirm Room writes happen via `CoroutineScope(Dispatchers.IO)` in the impl.
- [ ] Decide legacy `AppDatabase` handling: keep both DBs during migration, or fold legacy `ActUser`/`ActPermission` caching into `EzrahiDatabase`. **Recommend: keep separate until Phase 5** (legacy `FirebaseDataRepo` owns it); note in docs.
- [ ] Build + smoke test: launch app, open map, add a report → verify it appears (and ideally survives a disconnect if tested).

## Constraints / Notes
- **Keep build green + app running.** Legacy `FirebaseDataRepo` (which manages its own `AppDatabase`) is untouched by Phase 3 changes — only the modern `EzrahiRepositoryImpl`/`EzrahiDao` gain report caching.
- Room schema change (`new entity`) requires a **version bump** (1 → 2) or a destructive-migration fallback. `EzrahiDatabase` uses `fallbackToDestructiveMigration()` so `version = 2` is safe for dev.
- Field names must match the canonical Firestore `Reports` schema (ActId, ReporterId, Title, Description, Location GeoPoint, Time, Status int, Type int) via `FieldReportMapper`.
- Roadmap Phase 3 does **not** mention reports/messages-route-caching beyond messages; reports caching here is the "report work" continuation from Phase 1.

## Definition of Done (Phase 3)
- [ ] `ReportLocalEntity` + `observeReports`/`insertReports` in Room.
- [ ] `getReports()` is offline-first (Room-backed flow, Firestore writes into Room).
- [ ] Shared `MutableStateFlow` bug removed (per-actId flows).
- [ ] `EzrahiDatabase` version bumped (2) with destructive fallback.
- [ ] `sh gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] Smoke test: map → add report → marker appears.

## Next
- [ ] Create `todo-4.md` for Phase 4 (Permissions & Background Location) after Phase 3 gate passes.