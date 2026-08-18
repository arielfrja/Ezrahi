# Todo — Phase 5: Single-Activity & Jetpack Compose UI Migration

> Source: `modernization-roadmap.md` "🎨 Phase 5" (Days 23–32) + checklist items 5.1–5.4.
> Also **executes the deferred deletions** from Phase 1 (legacy `entities.Activity`, `Callback`, `DataRepoFactory`) and Phase 2 Task 2.6 (duplicate classes) — legacy UI is finally retired here.
> Phases 0–4 complete. Current commits: `e97171d` (back-press fix) on `refactor/modernization-v2`.

## Objective
Replace all separate legacy Activities (`StartupActivity`, `LoginActivity`, `SignupActivity`, `UI.Main.MainActivity`) and XML Fragments with a single ultra-fast Jetpack Compose app. Modern `MainActivity` becomes the launcher; legacy UI is removed.

## Current State (verified — most of Phase 5 is ALREADY BUILT from pre-restore work)
- ✅ **Task 5.1 Theme** — `app/ui/theme/Theme.kt` exists, matches roadmap exactly (`EzrahiTheme`, dark/light Material 3 schemes); `Type.kt` present.
- ✅ **Task 5.2 MainActivity + NavHost** — `com.arielfaridja.ezrahi.MainActivity` exists: `@AndroidEntryPoint`, `EzrahiTheme`, `EzrahiNavApp` with `ModalNavigationDrawer` + `NavHost` (`auth` → `map/{eventId}` + placeholder routes for `messages/{eventId}`, `speed_dial`, `activity_overview`, `settings`). Manifest entry present but **`exported="false"` — not the launcher**.
- ✅ **Task 5.3 AuthScreen** — `app/ui/auth/AuthScreen.kt` matches roadmap (email/password, sign-in, auto-create on failure, Hebrew labels).
- ✅ **Task 5.4 MapScreen + MapViewModel** — `app/ui/map/MapScreen.kt` + `MapViewModel.kt` exist and are **ahead of the roadmap**: osmdroid (not Google Maps), live participant markers, report long-press → `getReports`, SOS FAB (`triggerSOS`), FGS start via `startForegroundService`, Phase 4 background-location/battery-opt flow.
- ✅ Compose + Hilt + Navigation deps all present; build green.
- ⚠️ `EzrahiRepository` has **no "list events/activities" API** — modern app hardcodes `map/demo_event_123` on auth success (roadmap stub). Real events live in Firestore `events` collection (`events/test-event-ariel`) + legacy `Activities` collection. **Map would open empty without an event picker.**

## Tasks

### Task 5.1–5.4: Verify existing Compose UI (DONE — verify only)
- [x] Theme, NavHost, AuthScreen, MapScreen/MapViewModel all match/exceed roadmap.

### Task 5.5: Make modern `MainActivity` the launcher
- [ ] **Decision needed (small):** add `events`-list capability OR a lightweight event picker screen. Recommended: add `EzrahiRepository.getEvents(): Flow<List<FieldEvent>>` (Firestore `events` collection snapshot → Room `cached_events`), plus a simple Compose `EventPickerScreen` (list of events, tap → `map/{eventId}`). This replaces the hardcoded `demo_event_123` stub with real data. (If PO prefers the stub for now, mark deferred — but a picker is ~1 small screen.)
- [ ] Implement `getEvents()` in `EzrahiRepository` + `EzrahiRepositoryImpl` (mirror `getEventUpdates` pattern: Firestore listener → `dao.insertEvent`, emit from `dao.observeEvent`/new `observeAllEvents`). Add `observeEvents(): Flow<List<EventLocalEntity>>` to `EzrahiDao` if not present.
- [ ] Add `EventPickerScreen` under `app/ui/events/` (list, loading, empty state, Hebrew labels); wire route `events` as post-auth destination instead of `map/demo_event_123`.
- [ ] Manifest: modern `.MainActivity` → `exported="true"` + `MAIN`/`LAUNCHER` intent-filter. Legacy `.UI.Main.MainActivity` → remove launcher intent-filter (keep exported=false dormant until Task 5.7 cleanup, so both icons never coexist).
- [ ] Verify only ONE launcher icon after change.

### Task 5.6: Runtime switchover smoke test
- [ ] Install → app boots straight to modern Compose (no legacy splash/activity).
- [ ] Auth → event picker lists real event(s) → map opens with participants + reports.
- [ ] Report long-press → marker appears (Phase 0/3 path still works in modern UI).
- [ ] SOS FAB → message sent; FGS notification appears + background location flow runs (Phase 4).
- [ ] Back navigation + drawer routes work; no crash.

### Task 5.7: Execute deferred deletions (Phase 1.1/1.3 + Phase 2 Task 2.6) — AFTER switchover gate
- [ ] **Legacy UI deletion** (roadmap 5.x "replace all Activities/Fragments"): remove `UI/` package (19 files) + `StartupActivity` + `ui/StartupActivity.kt` + legacy `LoginActivity`/`SignupActivity`/`SignupActivityViewModel`/`LoginViewModel`.
- [ ] **Delete legacy `entities.Activity`** (Phase 1.1) — after removing `UI/` (its only consumers), rename/repoint to `FieldEvent` or delete; update `Data`/`FirebaseDataRepo` references accordingly.
- [ ] **Delete legacy `Callback.java`** (Phase 1.3) — becomes unused once `UI/` + `FirebaseDataRepo` are gone.
- [ ] **Delete `DataRepoFactory.kt`** (Phase 1.3) — its only consumers are legacy; after their removal, drop `DataRepoEntryPoint` + the `@Binds IDataRepo` in `RepositoryModule`.
- [ ] Decide `FirebaseDataRepo`/`IDataRepo`: if legacy is fully deleted and the modern `EzrahiRepositoryImpl` covers all paths, **delete both** + `AppDatabase`/`LocalDao`/legacy entities (`User`, `ActUser`, `ActPermission`, `Report`, legacy `Activity`). Confirm nothing modern imports them. (Phase 3 note: this also kills the second Room DB `"database"` — only `"ezrahi_local_db"` remains. ✅)
- [ ] **Duplicate `LocationTrackingService`** (Phase 2 Task 2.6): keep modern `service/LocationTrackingService`; delete legacy `com.arielfaridja.ezrahi.LocationTrackingService` + its manifest entry.
- [ ] Update manifest: remove legacy activity/service declarations entirely (only `.MainActivity`, modern `.service.LocationTrackingService`, `EzrahiApp` remain).
- [ ] Prune unused resources/strings if flagged by lint (optional).

### Task 5.8: Final Phase 5 gate
- [ ] `sh gradlew :app:assembleDebug` → BUILD SUCCESSFUL (no legacy refs).
- [ ] Full modern smoke test (auth → picker → map → report → SOS → FGS → settings placeholder).
- [ ] `todo-5.md` fully checkmarked; deferred items explicitly marked.

## Constraints / Notes
- **Two-launcher-icon hazard**: never have both `.MainActivity` and `.UI.Main.MainActivity` with launcher intent-filters at once — switch in the same commit.
- **Case-insensitive FS**: deleting `UI/` (uppercase) — use `git rm` with the tracked path (`app/src/main/java/com/arielfaridja/ezrahi/UI/...`) to avoid path-mismatch commits.
- Keep the modern `app/ui/` namespace (case-safe vs legacy `UI/`).
- `EzrahiRepository`/`EzrahiRepositoryImpl` are the only data path post-deletion — verify report/event/participant/message flows all map correctly before deleting legacy repo.
- Roadmap Phase 5 lists only 5.1–5.4; deletions are the deferred closure of Phase 1/2 items (mark them as such in the doc).

## Definition of Done (Phase 5)
- [ ] Modern `MainActivity` is the sole launcher; legacy activities/services removed from manifest.
- [ ] Event picker replaces the `demo_event_123` stub; map shows real data.
- [ ] Legacy UI (`UI/`, Startup/Login/Signup), `entities.Activity`, `Callback`, `DataRepoFactory`, duplicate `LocationTrackingService` deleted.
- [ ] Single Room DB (`"ezrahi_local_db"`) remains; `AppDatabase`/`LocalDao` gone.
- [ ] `RepositoryModule` binds only modern interfaces; no `IDataRepo`.
- [ ] Build green + full modern smoke test passes.
- [ ] `todo-5.md` + `todo-1.md` deferred items closed.

## Next
- [ ] Create `todo-6.md` for Phase 6 (Firestore Security Rules hardening) after Phase 5 gate passes.
- [ ] Note: `firestore.rules` (legacy collections `Activities`/`ActUsers`/`Reports`) already deployed in Phase 0; Phase 6 adds the modern `events` rules from the roadmap.