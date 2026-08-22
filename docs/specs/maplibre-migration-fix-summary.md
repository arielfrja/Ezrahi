# MapLibre Migration & Crash Resolution Document

This document summarizes the investigation, architectural findings, and concrete code changes implemented to resolve the MapLibre map engine crashes. It is designed to allow other models or developers to seamlessly continue, verify, and deploy the Phase 4 modernization changes.

---

## 1. Executive Summary & Root Cause Analysis

### The Problem
During Phase 4 of the Ezrahi modernization, migrating from `osmdroid` to `MapLibre Android 11.8.0` resulted in an immediate app crash upon navigating to the Map screen (showing a loading animation and then instantly terminating). No logs or crash details appeared in Firebase Firestore's `app_errors` collection or standard JVM logs.

### Root Cause 1: Missing Mandatory Singleton Initialization (Native SIGSEGV/SIGABRT)
* **Finding:** `MapLibre.getInstance(context)` was completely missing from the application initialization path and Map view.
* **Mechanism:** MapLibre Android (an OpenGL C++ map engine compiled into `libmaplibre.so`) relies on mandatory singleton setup to configure native assets, tile paths, and HTTP clients. When `MapView` is instantiated or rendered without this call, internal JNI checks or null pointer dereferences occur in the C++ layer.
* **Impact:** This triggers an OS-level signal (`SIGSEGV` / Fatal Signal 11 or `SIGABRT`) in `libmaplibre.so`. Because the crash happens inside native binary code, it bypasses the JVM's `Thread.UncaughtExceptionHandler` (your custom `GlobalCrashHandler`), preventing the error from being saved to disk or logged to Firebase.

### Root Cause 2: Unprotected LocationComponent Activation (`SecurityException`)
* **Finding:** In `MapScreen.kt`, the map's `LocationComponent` was being activated inside the `onMapReady` callback regardless of whether location permissions had already been granted.
* **Mechanism:** Calling `locationComponent.activateLocationComponent(...)` with `useDefaultLocationEngine(true)` before the user has granted `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` triggers a `SecurityException` inside MapLibre's background thread, crashing the map.

### Root Cause 3: Brittle Lifecycle Binding & State Restoration
* **Finding:** `MapLibreView.kt` passed an empty non-null `Bundle()` to `mapView.onCreate(Bundle())` and had unguarded lifecycle states.
* **Mechanism:** Passing a blank `Bundle()` instead of `null` causes MapLibre to attempt state restoration from missing/invalid keys. In addition, calling lifecycle functions without try-catch can trigger crashes if callbacks race with composition or disposal.

---

## 2. Implemented Code Corrections

### A. Global Initialization
**File modified:** `app/src/main/java/com/arielfaridja/ezrahi/EzrahiApp.kt`
* **Changes:** Added `org.maplibre.android.MapLibre` import and called `MapLibre.getInstance(this)` inside `onCreate()`. This initializes `libmaplibre.so` and JNI bindings on app startup.

### B. Robust Compose Map Host
**File modified:** `core/map-engine/src/main/java/com/arielfaridja/ezrahi/core/mapengine/MapLibreView.kt`
* **Changes:**
  1. Wrapped `MapLibre.getInstance(context)` inside a `remember(context)` block as an extra safety measure.
  2. Changed `mapView.onCreate(Bundle())` to `mapView.onCreate(null)` to prevent state restoration failures.
  3. Added `runCatching` blocks around all lifecycle callbacks (`onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`) to ensure that asynchronous disposal races never trigger native crashes.
  4. Wrapped map initialization and `setStyle` callbacks in `runCatching` to gracefully log errors rather than killing the app.

### C. Permission-Guarded Location Setup
**File modified:** `app/src/main/java/com/arielfaridja/ezrahi/app/ui/map/MapScreen.kt`
* **Changes:**
  1. Removed the direct `LocationComponent` activation from `onMapReady` lambda.
  2. Added a robust `LaunchedEffect(mapState.value, permissionsGranted)` block that activates and enables the map's `LocationComponent` only *after* both the map is ready and permissions are verified.

### D. Firebase Crashlytics NDK Setup
**Files modified:** `gradle/libs.versions.toml` and `app/build.gradle.kts`
* **Changes:** Added Firebase Crashlytics alongside the Firebase Crashlytics NDK library.
* **Benefit:** Any future C++ native crashes (signals, memory faults) will now automatically be caught by Crashlytics NDK and reported to Firebase.

---

## 3. Playbook for Testing and Next Steps

1. **Local Build & Installation:**
   Run the following from a safe execution context (or have the user build and run):
   ```bash
   bash gradlew assembleDebug
   ```
   Install the output APK onto the device.

2. **Verification Checklist:**
   * **Verification 1:** Launch the app and select an event.
   * **Verification 2:** Go to the Map screen. The circular loading indicator should appear briefly while the style is resolved, followed by the MapLibre map rendering smoothly.
   * **Verification 3:** Location prompt should appear. Accept location permission; the map should smoothly center on the blue user location dot without crashing.

3. **In Case of Further Native Failures:**
   * Because we registered the Firebase Crashlytics NDK dependency, any crash will appear under the Firebase Console's Crashlytics panel.
   * To upload symbols for full class/function name resolution in Crashlytics:
     ```bash
     bash gradlew app:uploadCrashlyticsSymbolFileDebug
     ```
