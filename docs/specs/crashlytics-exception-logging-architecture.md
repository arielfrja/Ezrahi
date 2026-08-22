# Specification: Console-Only Crashlytics Exception Logging Architecture

This document defines the production specification and operational details for the simplified, console-only **Firebase Crashlytics Logging Architecture** implemented in the Ezrahi application.

---

## 1. Architectural Strategy

### Context
Originally, Ezrahi was configured to route exceptions to a custom Firestore collection (`app_errors`) and write synchronous crash dumps to the device's local storage. This introduced significant resource overhead, bloated database write transactions, and failed to record native C++ crashes (such as in-map engine segmentation faults) which instantly terminate the JVM.

### The Decision
All exceptions—including fatal JVM crashes, C++ native signals, handled/caught business logic failures, and user journey warnings—are routed **exclusively to Firebase Crashlytics**. 
All raw technical log collections in Firestore are retired. Developers and coordinators will read and analyze all app diagnostics directly from the central **Firebase Console**.

---

## 2. Core Class: `ExceptionLogger.kt`

`ExceptionLogger` remains the central injection-ready API used throughout the Ezrahi codebase (`:core:common`), but is fully refactored to delegate directly to `FirebaseCrashlytics`.

### Data Flow Diagram

```
[ Code Try-Catch / Failure ]
             │
             ▼
    `ExceptionLogger.log()`
             │
             ▼
   [ Firebase Crashlytics ] ◄─────── [ Breadcrumbs / Local State ]
             │
             ▼
    [ Firebase Console ]
```

### API Signatures & Context Mapping

When `ExceptionLogger.log(throwable, errorType, eventId, screen, severity)` is invoked:
1. **Dynamic Metadata Injection:** It configures context-aware Crashlytics keys on the spot:
   - `session_id`: Unique session UUID generated on app launch.
   - `error_type`: High-level error categorization (`CAUGHT`, `CRASH`, etc.).
   - `severity`: Visual classification for quick sorting (`INFO`, `WARNING`, `ERROR`, `FATAL`).
   - `event_id`: The database event ID where the issue occurred.
   - `screen`: The screen view context (e.g. `map_screen`).
   - `in_foreground`: Whether the app was active in the foreground during the event.
2. **Crashlytics Breadcrumb Forwarding:** Converts standard logger breadcrumbs into native Crashlytics logs (`FirebaseCrashlytics.getInstance().log()`), preserving the exact historical sequence preceding the crash.
3. **Exception Recording:** Triggers `recordException(throwable)` to upload the trace to the Firebase Console asynchronously.

---

## 3. Benefits & Performance

1. **Zero Database Write Overhead:** Shifting logs out of Firestore eliminates thousands of database write transactions, preserving read/write quotas and drastically reducing cloud costs.
2. **Native Crash Catching:** Low-level faults inside the hardware-accelerated map engine (`libmaplibre.so`) are caught natively via native signals (`SIGSEGV`, `SIGABRT`) and uploaded automatically.
3. **Automatic Stack Deobfuscation:** Release build traces are automatically matched against ProGuard/R8 mapping files inside the Firebase Console, rendering raw memory pointers into readable Kotlin code instantly.
4. **Offline Resilience:** If the user is offline in a deep field rescue, Crashlytics securely buffers all reports in optimized local storage and flushes them to the console the instant connection is restored.
