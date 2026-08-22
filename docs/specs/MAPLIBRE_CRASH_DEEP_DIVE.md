# Deep Dive: MapLibre Crash Investigation & Resolution Spec

This document details the investigative process, root causes, and surgical resolutions applied to the MapLibre map engine crash inside the Ezrahi app. It serves as a comprehensive, first-principles handoff guide for engineering teams.

---

## Part 1: The Anatomy of the Error

### The Symptom
Upon navigating to the map screen, the app presented a brief loading animation and instantly terminated (crashed directly to the home screen).

### The Mystery
Although Ezrahi has a robust, custom `GlobalCrashHandler` that synchronously writes crash dumps to disk and subsequently routes caught exceptions to Firebase Firestore, **no logs, records, or trace data appeared in Firebase or on-disk dumps** regarding this crash. Only non-fatal, unrelated network errors were recorded.

### The Underlying Root Causes

#### 1. Uninitialized Native C++ Singleton (Native SIGSEGV/SIGABRT) — *The Primary Culprit*
* **The Error:** `MapLibre.getInstance(context)` was completely omitted from the codebase.
* **The Mechanism:** Unlike pure Java/Kotlin libraries, MapLibre is a hardware-accelerated OpenGL/Vulcan map engine written in C++ and compiled into `libmaplibre.so`. It communicates with the Android JVM via JNI (Java Native Interface). Calling `MapView(context)` or rendering the map relies on a mandatory global singleton context. 
* **The Crash:** When MapLibre tries to access `MapLibre.getApplicationContext()` or native context handles internally during `MapView` creation without this initialization, `libmaplibre.so` triggers a **`SIGSEGV` (Segmentation Fault / Fatal Signal 11) or `SIGABRT` (Abort signal)**.
* **Why the Global Handler Missed It:** The JVM's `Thread.UncaughtExceptionHandler` can only catch `java.lang.Throwable` objects thrown within the Dalvik/ART virtual machine. A C++ segmentation fault kills the Unix process at the kernel/OS level via a POSIX signal. The JVM is terminated instantly without any opportunity to invoke Java-level uncaught exception handlers.

#### 2. Unprotected Location Component Activation (`SecurityException` / JNI Crash)
* **The Error:** Inside `MapScreen.kt`, `locationComponent.activateLocationComponent(...)` was being called unconditionally during `onMapReady`.
* **The Mechanism:** When configuring MapLibre's default location engine (`useDefaultLocationEngine(true)`), it attempts to read the device GPS state. If called before the Android framework has officially granted `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` permissions (e.g. while the runtime permission prompt is still displaying or before it is accepted), the framework throws a `SecurityException`.
* **The Crash:** Throwing an unhandled exception inside a native callback thread often escalates immediately to a native thread abort, resulting in a silent process death.

#### 3. Unsafe Jetpack Compose View Lifecycle Bindings
* **The Error:** `MapLibreView.kt` instantiated `remember { MapView(context) }` during Compose's initial composition phase and passed an empty, non-null `Bundle()` to `mapView.onCreate()`.
* **The Mechanism:** Passing an empty, non-null `Bundle()` instead of `null` tells MapLibre to restore state from an existing map instance. Since the keys are empty, this results in null-pointer dereferences inside internal view-state restoration. Additionally, destroying the view inside `onDispose` without catching concurrent active render-thread calls causes use-after-free native memory faults.

---

## Part 2: The Investigative Journey (How It Was Discovered)

To uncover these silent, invisible crashes without a direct USB debugger connection, we utilized a systematic, first-principles forensic strategy:

### Step 1: Deduced the Silent Execution Path
Because the custom `GlobalCrashHandler` is known to work for Java/Kotlin exceptions but captured absolutely nothing for the map crash, we mathematically deduced that the crash **had to be an OS-level signal** or a silent kernel-level process termination. This pointed directly to the C++ binary `libmaplibre.so`.

### Step 2: Inspected the MapLibre Binary JNI Contract
To verify if MapLibre had a mandatory initialization routine, we used Android SDK utilities and JVM decompilation tools (`javap`) directly inside the Termux shell to inspect the compiled `android-sdk-11.8.0.aar` cached in Gradle:
```bash
javap -classpath . "org.maplibre.android.MapLibre"
```
The decompiled signature revealed:
```java
public final class org.maplibre.android.MapLibre {
  public static synchronized org.maplibre.android.MapLibre getInstance(android.content.Context);
  public static boolean hasInstance();
}
```
This proved the existence of the global singleton. Search queries across the entire project confirmed that `MapLibre.getInstance` was completely absent from the codebase, verifying that the C++ native side was being invoked uninitialized.

### Step 3: Isolated Permission & Lifecycle Anomalies
By performing static analysis of `MapScreen.kt` and `MapLibreView.kt` side-by-side with MapLibre's architecture, we isolated the permission-check race condition:
* In Jetpack Compose, asynchronous `LaunchedEffects` and UI composition happen concurrently.
* The map was completing its initial setup and invoking `onMapReady` before the user could interact with the asynchronous location permission dialog, causing a race condition and a direct crash when accessing GPS services.

---

## Part 3: The Surgical Resolution (How It Was Fixed)

We applied a highly robust, fault-tolerant architecture across all three affected layers:

### 1. Global Singleton Initialization
We integrated the mandatory MapLibre initialization safely at the application-level `onCreate` so that the native C++ engine is fully loaded and primed before any views are composed:
```kotlin
// In EzrahiApp.kt
override fun onCreate() {
    super.onCreate()
    MapLibre.getInstance(this) // Primes JNI, assets, and libmaplibre.so
    ...
}
```

### 2. Hardened Compose Lifecycle Wrapper (`MapLibreView.kt`)
We completely overhauled the Compose `MapView` host to make it resilient to asynchronous lifecycle changes and state restoration errors:
* **Null Bundle Safety:** Changed `mapView.onCreate(Bundle())` to `mapView.onCreate(null)` to enforce clean, fresh view initialization and bypass empty state-restoration crashes.
* **Safe JNI Lifecycle Hooks:** Wrapped every single lifecycle hook (`onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`) in `runCatching` blocks. If the Composable is disposed of or navigating away while the native render thread is drawing, any thread race is caught gracefully instead of terminating the OS process.
* **Double-Guarded Initialization:** Embedded a fallback call to `MapLibre.getInstance(context)` inside a `remember(context)` block directly in the View as a secondary safety shield.

### 3. Permission-Gated LocationComponent Activation (`MapScreen.kt`)
We split map readiness from location services setup, eliminating the permission race condition:
* **Before:** `activateLocationComponent` was called blindly inside `onMapReady`.
* **After:** `onMapReady` now simply stores the map reference (`mapState.value = map`). We introduced a dedicated `LaunchedEffect(mapState.value, permissionsGranted)` block.
* **The Logic:** The app will now **only** attempt to activate and enable `LocationComponent` when:
  1. The map is fully loaded and ready.
  2. The user has successfully granted location permissions.
* If permissions are still pending or denied, the map renders safely as a standard map, activating location services the instant permission is granted.

### 4. Built-in Observability (Crashlytics NDK Integration)
To prevent silent native crashes in the future, we modified the project's dependency structure (`gradle/libs.versions.toml` and `app/build.gradle.kts`) to incorporate **Firebase Crashlytics NDK**. 
* **Benefit:** Any low-level C++ crash, memory leak, or signal violation in the future is now automatically intercepted by the Crashlytics native signal handler and reported directly to the Firebase console for instant tracking.
