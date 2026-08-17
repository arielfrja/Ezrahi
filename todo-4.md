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
- ⚠️ **`ACCESS_BACKGROUND_LOCATION` is declared but never requested at runtime.** Modern `MapScreen` requests only FINE/COARSE/POST_NOTIFICATIONS. On Android 11+, background location **cannot** be requested via dialog — it requires a `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`-style flow or the app-settings screen. The foreground-service path works without it (FGS is exempt while started from foreground), so this is **optional** unless true background (non-FGS) delivery is required. **Recommend: document + defer; add app-settings redirect if the PO requires background updates beyond FGS.**
- ⚠️ **Two services coexist:** legacy `com.arielfaridja.ezrahi.LocationTrackingService` (plain, used by the running legacy app) and modern `com.arielfaridja.ezrahi.service.LocationTrackingService` (Hilt). Both in manifest. Legacy one stays until Phase 5 removes legacy UI. (Already flagged in `todo-2.md` Task 2.6.)
- ⚠️ Modern `MapScreen`/modern service are not end-to-end reachable yet — the legacy `UI/Main/MainActivity` is still the launcher (Phase 5 wires the Compose `MainActivity`). FGS behavior of the modern path can't be fully smoke-tested until Phase 5.
- ⚠️ Battery-optimization exemption not handled (no `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`). Optional; note for PO.

## Tasks
### Task 4.1: Verify manifest permissions (DONE)
- [x] FINE/COARSE/BACKGROUND location, FOREGROUND_SERVICE(+_LOCATION), INTERNET, POST_NOTIFICATIONS present.
- [x] Both service declarations `foregroundServiceType="location"`, `exported="false"`.

### Task 4.2: Verify/build the foreground location service (DONE)
- [x] Modern `service/LocationTrackingService.kt` matches roadmap spec (inject repo, FusedLocation, 10s/5s updates, persistent notification, START_STICKY, API-34 FGS type).
- [x] Runtime permission request + `startForegroundService` wired in modern `MapScreen`.
- [ ] Optional hardening: request battery-optimization exemption (only if PO wants maximal uptime). → **deferred, optional.**

### Task 4.3: Runtime smoke test (legacy path — the one that runs today)
- [ ] Launch legacy app → login → open map → confirm the tracking notification appears ("Ezrahi Field Tracking Active").
- [ ] Confirm location updates keep flowing with screen off / app backgrounded (check Firestore `participants.<userId>.lastSeenTimestamp` advancing, or notification stays).
- [ ] Toggle tracking off (service stop) → notification disappears, updates stop.

## Constraints / Notes
- Keep build green; do not touch legacy service until Phase 5.
- `ACCESS_BACKGROUND_LOCATION` runtime flow deferred (Android 11+ requires settings redirect; FGS is exempt). Flag to PO.
- Modern FGS full test must wait for Phase 5 (Compose launcher).

## Definition of Done (Phase 4)
- [ ] Manifest permissions verified (all present) — **done**.
- [ ] Modern service verified against spec — **done**.
- [ ] Legacy runtime smoke test: notification shows on map open; updates flow in background.
- [ ] `sh gradlew :app:assembleDebug` → BUILD SUCCESSFUL (unchanged code, verify).
- [ ] Battery-opt / background-location decision recorded for PO.

## Next
- [ ] Create `todo-5.md` for Phase 5 (Single-Activity + Compose UI migration) after Phase 4 gate passes.