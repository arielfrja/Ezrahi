# Todo — Phase 1: Domain Entities & Cleaning Technical Debt

> Source: `../roadmaps/modernization-roadmap.md` "🏛️ Phase 1" (Days 4–7) + checklist items 1.1–1.3.
> Phase 0 is **complete** (base `refactor/modernization-v2`, build green, user test gate passed, crash/report fixes committed in `c55adef`).

## Objective
Fix the naming collision between legacy `entities.Activity` and `android.app.Activity`, convert all legacy models into clean Kotlin `data class`es in `domain.model`, and delete the custom `Callback` interface + `DataRepoFactory`. **Included scope from report work:** the roadmap's `FieldModels.kt` sample omits a Report model — add `FieldReport`/`FieldReportStatus`/`FieldReportType` since Reports are a core map feature.

## Current State (verified)
- `domain/model/FieldModels.kt` exists with `UserRole`, `GeoPoint`, `UserProfile`, `EventParticipant`, `FieldEvent`, `FieldMessage` — but **no Report model**.
- Legacy `entities/` still has: `Activity.kt`, `User.kt`, `Latlng.kt`, `ActUser.kt`, `ActPermission.kt`, `Report.kt`, `ReportStatus.kt`, `ReportType.kt`, `GlobalConsts.kt`, `Callback.kt`.
- `entities.Activity` is used by 6 files (`ui/Main/MainActivity.kt`, `MainActivityViewModel.kt`, `ui/Fragments/Map/MapViewModel.kt`, `Settings/SettingsFragment.kt`, `SettingsViewModel.kt`, `ActivityOverview/ActivityOverviewViewModel.kt`).
- `Callback` used by 11 files; `DataRepoFactory` used by 10 files (legacy UI + legacy data layer).
- Legacy UI package (`com.arielfaridja.ezrahi.UI`, 19 files) still consumes `Callback`; modern Compose UI (`app.ui`, 5 files) exists separately and will fully replace legacy UI in Phase 5.
- Modern repo scaffold already present: `domain/repository/EzrahiRepository.kt`, `data/repository/EzrahiRepositoryImpl.kt`, `domain/mesh/MeshTransceiver.kt`.

---

## Task 1.1: Rename `Activity` → `FieldEvent`
- [x] Confirm `FieldEvent` in `domain/model/FieldModels.kt` covers legacy `entities.Activity` (id, name, routesSrc→gpxRouteUrl, owner→managerId, users→participants, permissions).
- [x] Add boundary mapper `FieldEventMapper` (legacy `entities.Activity` → `domain.model.FieldEvent`) — `data/mapper/FieldEventMapper.kt`.
- [x] Audit collision: **no file imports both `android.app.Activity` and `entities.Activity`** — collision is latent only (verified across all 6 consumers). `ActivityCompat`/`AppCompatActivity` usages unaffected.
- [ ] Migrate the 6 consumers of `entities.Activity` to `FieldEvent` — **DEFERRED to Phase 5** (legacy UI drives the running app; see Constraints).
- [ ] Delete `entities/Activity.kt` once nothing references it — **DEFERRED to Phase 5**.

## Task 1.2: Add Report domain models (report work — missing from roadmap sample)
- [x] Add to `domain/model/FieldModels.kt`:
  - [x] `enum class FieldReportStatus` (REPORTED / HANDLED / UNKNOWN, matching legacy `ReportStatus` values 1/2/-1)
  - [x] `enum class FieldReportType` (GENERAL / MEDICAL / UNKNOWN, matching legacy `ReportType` values 0/1/-1)
  - [x] `data class FieldReport(id, actId, reporterId, title, description, location: GeoPoint, time: Long, status, type)` mapping legacy `Report` + Firestore fields (writer uses `"Time"`; status/type as ints).
- [x] Add mapping helpers: `FieldReportMapper.fromSnapshot()` (Firestore `DocumentSnapshot` ↔ `FieldReport`) + `toWriteMap()` — `data/mapper/FieldReportMapper.kt`.
- [x] Wire into modern repo: `EzrahiRepository.getReports(actId): Flow<List<FieldReport>>` + `addReport(report): Result<String>`, implemented in `EzrahiRepositoryImpl` (Firestore `Reports` collection, `ActId` filter, snapshot listener).

## Task 1.3: Convert legacy models to Kotlin data classes in `domain.model`
- [x] `Latlng` → `GeoPoint` (exists in `FieldModels.kt`).
- [x] `User` → `UserProfile` (exists in `FieldModels.kt`).
- [x] `ActUser` → `EventParticipant` (exists in `FieldModels.kt`).
- [x] `ReportStatus`/`ReportType` → `FieldReportStatus`/`FieldReportType` (added this phase).
- [x] `GlobalConsts` (ACT_SP constant) — kept as-is (referenced by legacy code).
- [ ] `ActPermission` → domain role/permission model — **DEFERRED to Phase 5** (permissions only used by legacy UI).
- [ ] Delete legacy `entities/*` files after all consumers migrated — **DEFERRED to Phase 5**.

## Task 1.4: Delete custom `Callback` + `DataRepoFactory`
- [x] Modern repo path exists without `Callback`: `EzrahiRepository` (suspend fun `Result<T>` + `Flow<T>`) — added `getReports`/`addReport` this phase.
- [ ] Replace all `Callback<T>` signatures in the legacy data layer (`IDataRepo`, `FirebaseDataRepo`) with `suspend fun`/`Flow` — **DEFERRED to Phase 5** (legacy UI depends on `Callback`; see Constraints).
- [ ] Migrate the 11 files using `Callback` (legacy UI + data layer) — **DEFERRED to Phase 5**.
- [ ] Migrate the 10 files using `DataRepoFactory.getInstance()` — toward constructor injection (Phase 2 will finalize with Hilt) — **in progress in Phase 2**.
- [ ] Delete `entities/Callback.kt` and `data/DataRepoFactory.kt` once unreferenced — **DEFERRED to Phase 5**.
- [x] Verify build green: `sh gradlew :app:assembleDebug` → **BUILD SUCCESSFUL**. (<i>after report work + mappers</i>)

## Constraints / Notes
- **Keep build green after each step.** Legacy UI (19 files) still drives the running app and depends on `Callback`/`DataRepoFactory`/`entities.Activity`; deleting them before migrating legacy UI will break the build.
- **Deferred-to-Phase-5 decision:** legacy `UI/` package is replaced by Compose in Phase 5. Prefer mapping at the repo/domain boundary (keep legacy entities during Phase 1) rather than rewriting 19 legacy UI files — unless the user wants legacy UI migrated now.
- Firestore field names are canonical: reports use `"Time"`, `"ActId"`, status/type as ints (Phase 0 fixed the `ReportTime`→`Time` mismatch).

## Definition of Done (Phase 1)
- [ ] `entities.Activity` gone; `FieldEvent` used everywhere (no `android.app.Activity` collisions). — **partially done**: collision verified latent (no file imports both); `FieldEvent` + boundary mapper added; legacy deletion deferred to Phase 5.
- [x] All domain models are Kotlin `data class`es in `domain.model`, including `FieldReport` trio.
- [ ] `Callback.kt` and `DataRepoFactory.kt` deleted. — **deferred to Phase 5** (legacy UI depends on them; modern `EzrahiRepository` already `suspend`/`Flow`-based).
- [x] `sh gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [x] User smoke-tests app (login → map → add report → overview → settings) — **Phase 0 gate passed** on this code path; no UI behavior changed this phase (additive only).

## Next
- [ ] Create `todo-2.md` for Phase 2 (Dagger Hilt DI) after Phase 1 gate passes.