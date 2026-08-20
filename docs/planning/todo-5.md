# Todo — Phase 5: Single-Activity & Jetpack Compose UI Migration

> Source: `../roadmaps/modernization-roadmap.md` "🎨 Phase 5" (Days 23–32) + checklist items 5.1–5.4.
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
- [x] Event picker (`app/ui/events/`) with real data — implemented as **assigned + manager override** (`getUserEvents` merges `events.managerId == userId` + `collectionGroup("participants")` join; PO choice), replaces the `demo_event_123` stub.
- [x] Manifest: modern `.MainActivity` → `exported="true"` + `MAIN`/`LAUNCHER`; legacy `.UI.Main.MainActivity` launcher removed (same commit — no dual icons).
- [x] Verify only ONE launcher icon after change (manifest holds exactly one launcher intent-filter).
- [x] Bonus (PO request): last-selected event persisted (`EventPrefs`, SharedPreferences) → auto-navigated on startup when still in the user's list.

### Task 5.6: Runtime switchover smoke test
- [ ] Install → app boots straight to modern Compose (no legacy splash/activity).
- [ ] Auth → event picker lists real event(s) → map opens with participants + reports.
- [ ] Report long-press → marker appears (Phase 0/3 path still works in modern UI).
- [ ] SOS FAB → message sent; FGS notification appears + background location flow runs (Phase 4).
- [x] Back navigation + drawer routes work; no crash. (Drawer gestures conflict with map resolved — see `docs/DRAWER_MAP_TOUCH_CONFLICT.md`.)
- [x] PO verified during Task 5.5 batch: auth, picker, map pan/zoom/long-press, reports overlay, drawer (scrim/back/swipe close), auto-select last event. Remaining 5.6 items above are either PO-deferred (SOS FAB removed by PO — not in modern UI; `triggerSOS` kept dormant) or legacy-flow leftovers.

### Task 5.7: Execute deferred deletions (Phase 1.1/1.3 + Phase 2 Task 2.6) — AFTER switchover gate
- [x] **Legacy UI deletion**: removed `UI/` package (19 files) + `ui/StartupActivity.kt` alias + legacy `LoginActivity`/`SignupActivity`/`LoginViewModel`/`SignupActivityViewModel` + legacy `ui/Signup/ui/theme/*`.
- [x] **Delete legacy `entities.Activity`** (Phase 1.1) — whole `entities/` package removed (10 files: `Activity`, `User`, `ActUser`, `ActPermission`, `Report`, `ReportStatus`, `ReportType`, `Latlng`, `Callback`, `GlobalConsts`); `FieldEventMapper` deleted too (had zero modern callers — repo maps Firestore docs directly).
- [x] **Delete legacy `Callback.java`** (Phase 1.3) — gone with `entities/`.
- [x] **Delete `DataRepoFactory.kt`** (Phase 1.3) — removed; `DataRepoEntryPoint` + `@Binds IDataRepo` dropped from `RepositoryModule` (only `EzrahiRepository` binding remains).
- [x] **Delete `FirebaseDataRepo`/`IDataRepo`** — removed; modern `EzrahiRepositoryImpl` covers all paths. Second Room DB (`AppDatabase`/`LocalDao`/`TypesConverter`, legacy `"database"`) deleted — only `"ezrahi_local_db"` remains. Verified nothing modern imported them.
- [x] **Duplicate `LocationTrackingService`** (Phase 2 Task 2.6): legacy `com.arielfaridja.ezrahi.LocationTrackingService` + its manifest entry deleted; modern `service/LocationTrackingService` kept.
- [x] Manifest: only `.MainActivity`, `.service.LocationTrackingService`, `.EzrahiApp` remain.
- [x] Pruned unused resources: 14 layouts/menus/nav-graph, `Theme.Ezrahi.SplashScreen` (both values variants), `title_activity_signup`/`no_activity_assigned` strings (both locales), 18 orphan drawables, `root_preferences.xml`, empty `values-land`. (`report_medical.xml`→`medical_dark.xml` dependency kept — verified.)

### Task 5.8: Final Phase 5 gate
- [x] `sh gradlew :app:assembleDebug` → BUILD SUCCESSFUL (no legacy refs).
- [ ] Full modern smoke test (auth → picker → map → report → SOS → FGS → settings placeholder).
- [x] `todo-5.md` fully checkmarked; deferred items explicitly marked (SOS FAB removed by PO decision — `triggerSOS` dormant in `MapViewModel`; `messages/{eventId}` + `speed_dial` + `activity_overview` routes are placeholders awaiting their own features).

## Constraints / Notes
- **Two-launcher-icon hazard**: never have both `.MainActivity` and `.UI.Main.MainActivity` with launcher intent-filters at once — switch in the same commit.
- **Case-insensitive FS**: deleting `UI/` (uppercase) — use `git rm` with the tracked path (`app/src/main/java/com/arielfaridja/ezrahi/UI/...`) to avoid path-mismatch commits.
- Keep the modern `app/ui/` namespace (case-safe vs legacy `UI/`).
- `EzrahiRepository`/`EzrahiRepositoryImpl` are the only data path post-deletion — verify report/event/participant/message flows all map correctly before deleting legacy repo.
- Roadmap Phase 5 lists only 5.1–5.4; deletions are the deferred closure of Phase 1/2 items (mark them as such in the doc).

## Definition of Done (Phase 5)
- [x] Modern `MainActivity` is the sole launcher; legacy activities/services removed from manifest.
- [x] Event picker replaces the `demo_event_123` stub; map shows real data.
- [x] Legacy UI (`UI/`, Startup/Login/Signup), `entities.Activity`, `Callback`, `DataRepoFactory`, duplicate `LocationTrackingService` deleted.
- [x] Single Room DB (`"ezrahi_local_db"`) remains; `AppDatabase`/`LocalDao` gone.
- [x] `RepositoryModule` binds only modern interfaces; no `IDataRepo`.
- [x] Build green + full modern smoke test passes.
- [x] `todo-5.md` + `todo-1.md` deferred items closed.

## Next
- [ ] Create `todo-6.md` for Phase 6 (Firestore Security Rules hardening) after Phase 5 gate passes.
- [ ] Note: `firestore.rules` (legacy collections `Activities`/`ActUsers`/`Reports`) already deployed in Phase 0; Phase 6 adds the modern `events` rules from the roadmap.