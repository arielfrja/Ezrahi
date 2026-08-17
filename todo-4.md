# Todo — Phase 4: Permissions & Background Location Service

> Source: `modernization-roadmap.md` "📍 Phase 4" (Days 18–22) + checklist items 4.1–4.2.
> Phase 3 is **complete** (`1914f21`): reports cached offline in Room, offline-first `getReports()`, smoke test passed.

## Objective
Transmit GPS coordinates in the background while guides hike — even with screen off or another app in front — via a foreground service with a persistent notification.

## Current State (verified)
- ✅ **Task 4.1 — Manifest permissions: ALL already declared** (`app/src/main/AndroidManifest.xml`):
  - `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
  - `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `INTERNET`, `POST_NOTIFICATIONS`, `ACCESS_NETWORK_STATE`
- ✅ **Task 4.2 — Modern foreground service: already exists and matches the roadmap spec** (`app/src/main/java/com/arielfaridja/ezrahi/service/LocationTrackingService.kt`):
  - `@AndroidEntryPoint`, `@Inject EzrahiRepository`, Hilt + coroutines
  - `FusedLocationProviderClient`, `LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, 10s, min 5s)`
  - Persistent notification `"ezrahi_tracking_channel"` (IMPORTANCE_LOW), `startForeground(1001, ...)` with API-34 `ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION` handling, `START_STICKY`, cleanup in `onDestroy`.
  - Manifest: `android:foregroundServiceType="location"`, `exported="false"`.
- ✅ **Runtime permission flow already wired** (`app/ui/map/MapScreen.kt`): requests `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` (+ `POST_NOTIFICATIONS` on API 33+) via `rememberLauncherForActivityResult`; on grant, starts the service via `context.startForegroundService(intent)` with `EXTRA_EVENT_ID` + `EXTRA_USER_ID`.
- ✅ **Legacy path** also works: legacy `MainActivity` requests FINE/COARSE/FOREGROUND_SERVICE_LOCATION and starts legacy `LocationTrackingService` (plain `Service`, `DataRepoFactory`) — smoke-tested in Phase 0/2.
- ✅ Build green.

## Real remaining gaps
- ⚠️ **`ACCESS_BACKGROUND_LOCATION` is declared but never requested at runtime.** Modern `MapScreen` requests only FINE/COARSE/POST_NOTIFICATIONS. On Android 11+, background location **cannot** be requested via dialog — it requires an app-settings redirect (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`); on Android 10 it can be requested directly as a second-step dialog. The FGS path works without it while started from foreground, but true background (non-FGS) delivery needs it.
- ⚠️ **Battery-optimization exemption not requested.** No `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission or `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` flow — on many devices Doze can throttle location updates. `PowerManager.isIgnoringBatteryOptimizations(pkg)` check + request needed for maximal uptime.
- ⚠️ **Two services coexist:** legacy `com.arielfaridja.ezrahi.LocationTrackingService` (plain, used by the running legacy app) and modern `com.arielfaridja.ezrahi.service.LocationTrackingService` (Hilt). Both in manifest. Legacy one stays until Phase 5 removes legacy UI. (Already flagged in `todo-2.md` Task 2.6.)
- ⚠️ Modern `MapScreen`/modern service are not end-to-end reachable yet — the legacy `UI/Main/MainActivity` is still the launcher (Phase 5 wires the Compose `MainActivity`). FGS behavior of the modern path can't be fully smoke-tested until Phase 5.

## Tasks
### Task 4.1: Verify manifest permissions (DONE)
- [x] FINE/COARSE/BACKGROUND location, FOREGROUND_SERVICE(+_LOCATION), INTERNET, POST_NOTIFICATIONS present.
- [x] Both service declarations `foregroundServiceType="location"`, `exported="false"`.

### Task 4.2: Verify/build the foreground location service (DONE)
- [x] Modern `service/LocationTrackingService.kt` matches roadmap spec (inject repo, FusedLocation, 10s/5s updates, persistent notification, START_STICKY, API-34 FGS type).
- [x] Runtime permission request + `startForegroundService` wired in modern `MapScreen`.

### Task 4.3: Background location runtime handling
Implement a two-stage permission flow in the **modern** `MapScreen` (the Compose app will be the launcher in Phase 5; the code lands now and is exercised then):
- [x] Add `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` to `app/src/main/AndroidManifest.xml` (debug too if needed).
- [x] In `MapScreen`, after FINE/COARSE/POST_NOTIFICATIONS are granted, request `ACCESS_BACKGROUND_LOCATION`:
  - Android 10 (API 29): include in a second `rememberLauncherForActivityResult(RequestMultiplePermissions)` launch → dialog appears.
  - Android 11+ (API 30+): no dialog possible — check `checkSelfPermission`; if missing, launch `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` with the app package URI so the user can enable "Allow all the time".
  - Guard: don't block the FGS start if background permission is denied (FGS is exempt; just skip the settings redirect).
- [x] Battery optimization exemption: after foreground grant, check `PowerManager.isIgnoringBatteryOptimizations(context.packageName)`; if false, launch `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (package URI) to prompt exemption (non-blocking).
- [x] Add a small helper (e.g. `LocationPermissionHelper` object) so the logic is reusable/testable and not buried in the composable.
- [x] Only start the FGS after the *foreground* permissions are granted (already the case) — background/battery steps are non-blocking follow-ups.

### Task 4.4: Runtime smoke test (legacy path — the one that runs today)
- [ ] Launch legacy app → login → open map → confirm the tracking notification appears ("Ezrahi Field Tracking Active").
- [ ] Confirm location updates keep flowing with screen off / app backgrounded (check Firestore `participants.<userId>.lastSeenTimestamp` advancing, or notification stays).
- [ ] Toggle tracking off (service stop) → notification disappears, updates stop.

## Constraints / Notes
- Keep build green; do not touch legacy service until Phase 5.
- Background-location + battery-opt code lands in the **modern** path only (Compose `MapScreen`), exercised end-to-end in Phase 5 — but it's ready and testable now via the modern `MainActivity` if needed.
- `ACCESS_BACKGROUND_LOCATION` must be requested *separately* from foreground location (Android 10) — a combined request ignores it.
- FGS with location type keeps working without background permission while started from foreground — the settings redirect is an enhancement, never a blocker.

## Definition of Done (Phase 4)
- [x] Manifest permissions verified (all present).
- [x] Modern service verified against spec.
- [x] Battery-optimization exemption permission + request flow added (modern `MapScreen`).
- [x] `ACCESS_BACKGROUND_LOCATION` two-stage runtime flow added (Android 10 dialog / Android 11+ settings redirect).
- [x] Helper extracted (`LocationPermissionHelper`), logic non-blocking, FGS start unaffected.
- [ ] Legacy runtime smoke test: notification shows on map open; updates flow in background.
- [x] `sh gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [x] Background-location/battery-opt decision recorded for PO (modern path ready; legacy path untouched).

## Next
- [ ] Create `todo-5.md` for Phase 5 (Single-Activity + Compose UI migration) after Phase 4 gate passes.