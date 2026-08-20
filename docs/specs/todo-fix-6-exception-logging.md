# todo-fix-6 — Global Frontend Exception Logging (Specification v2)

> **STATUS: 🔵 SPEC ONLY — no development yet (PO: "don't develop it yet, just create a specification document").**
>
> **v2** incorporates the professional architectural review appended below (see [Appendix A — professional criticism](#professional-criticism)). All 6 flaws are resolved in this revision (§2); the review's own recommendations are adopted (two-tier pipeline, Room-free bookkeeping, PII sanitizer, keep-R8, pre-auth support).

## 1. Requirements (from PO)

1. Log **every stack trace** ("log every trace moves") and **ALL frontend exceptions**.
2. A **global handler** must log exceptions to Firebase (Firestore).
3. Every record must include:
   - Exception **message**
   - **Date and time** of occurrence
   - **Error ID — GUID, auto-generated**
4. Stored in **Firestore**.
5. Add the requirement to `AGENTS.md` (global rules) so every future session follows it. ✅ done (AGENTS.md updated).

## 2. Root cause this fixes (context)

The route bug today ("Route 'route' failed to load: Unexpected token (position:TEXT @1:2...)") was invisible — only written to Logcat. There was **no way to see frontend errors without the user manually reporting them**. This spec makes every exception visible in Firestore with full context.

### 2.1 Professional review — verdict & resolutions

| # | Flaw (Appendix A) | Severity | Resolution (this spec) |
|---|---|---|---|
| 1 | Pre-auth crashes rejected (rules require `auth != null`) — 0-day startup bugs lost | Critical | §6 — anonymous writes (`anon-` device id) with strict payload + size caps; App Check as defense (§10 Q3) |
| 2 | Firestore SDK write unreliable during JVM termination — crash reports dropped | Critical | §5.2 — fatal crashes write a synchronous atomic disk dump; flushed on next launch |
| 3 | Recursive crash if the logger itself uses Room (SQLite corruption, disk full) | Critical | §5.5 — logging bookkeeping is **Room-free** (LruCache + SharedPreferences) |
| 4 | Recomposition loop ⇒ runaway Firestore write costs at scale | High | §5.6 — token bucket (5/s) + hourly budget + circuit breaker |
| 5 | R8-disable advice bloats APK & removes optimizations | High | §5.7 — keep R8; ProGuard keep-rules + mapping-file workflow |
| 6 | PII leakage in messages/stack traces (phones, emails, coords, tokens) | High | §5.4 — PII sanitizer pipeline applied to every field |

## 3. Scope

Captured sources (all funneled into one `ExceptionLogger`):

| Source | Mechanism |
|---|---|
| Uncaught JVM/Android exceptions (fatal crash) | `Thread.setDefaultUncaughtExceptionHandler` (chained) — **disk dump only**, §5.2 |
| AndroidX/Compose runtime crashes | propagate to the thread handler → disk dump |
| Compose recomposition errors / UI-state exceptions | `UI_COMPOSE` type, via coroutine handler + recomposition guard |
| Coroutine exceptions in app scopes | `CoroutineExceptionHandler` on the app root scope + `viewModelScope` where needed |
| Caught exceptions in business logic (route parse/download, uploads, Firestore writes, listeners, SOS, location) | every `catch` / `runCatching.onFailure` routes through `ExceptionLogger.log()` |
| Pre-auth / startup crashes (`Application.onCreate`, DI init, login) | anonymous device id (App Installations) — §6 |
| Cancellation / expected flow-control (`CancellationException`, retried `IOException`) | **never logged** — noise (explicit exclusion) |

**Out of scope:** OS-level native crashes (requires Crashlytics NDK or a native handler — see §10 Q4).

## 4. Firestore data model

Collection: **`app_errors`** — document ID = the GUID.

| Field | Type | Notes |
|---|---|---|
| `id` | string (UUID v4, client-generated) | document id == this value |
| `errorType` | string | `CRASH` / `CAUGHT` / `ROUTE_PARSER` / `NETWORK` / `FIRESTORE_LISTENER` / `AUTH` / `LOCATION_SERVICE` / `UI_COMPOSE` / `UNKNOWN` |
| `severity` | string | `FATAL` (crash) / `ERROR` (caught) / `WARNING` (recoverable) |
| `message` | string | **sanitized** message (truncate 2 KB) |
| `cause` | string \| null | `exception.cause?.message`, sanitized (1 KB) |
| `stackTrace` | string | **sanitized** stack (8 KB, keep head + first 3 cause frames) |
| `errorHash` | string | `sha256(errorType + exception class + first sanitized frame)` — grouping/dedup key |
| `timestamp` | number (epoch millis) | sort key + rule anchor + TTL field |
| `timestampIso` | string | ISO-8601 UTC + local offset |
| `appVersion` | string | `BuildConfig.VERSION_NAME` |
| `buildCode` | number | `BuildConfig.VERSION_CODE` |
| `deviceModel` | string | `Build.MANUFACTURER + Build.MODEL` |
| `osVersion` | string | `Build.VERSION.RELEASE` |
| `osSdk` | number | `Build.VERSION.SDK_INT` |
| `userId` | string | auth uid **or** anonymous id `anon-<App Installations id>` (never email — PII) |
| `eventId` | string \| null | active event context (composite-index query key, §7) |
| `screen` | string \| null | last known destination/screen |
| `sessionId` | string | GUID per app process start — groups errors from one run |
| `threadName` | string | throwing thread; `main` = UI-thread crash |
| `isFatal` | boolean | true only for crash-path records (disk-dump flushed) |
| `inForeground` | boolean \| null | app foreground state at error time |
| `breadcrumbs` | array of string | sanitized ring buffer (max 8) — §5.9 |

### 4.1 Budget & guardrails

- Max payload ≈ **16 KB** (Firestore doc limit is 1 MiB — cheap writes).
- Truncation + sanitization in code; size caps enforced in rules (§6, double guard).

## 5. Architecture — two-tier resilient pipeline

```
                    TWO-TIER LOGGING PIPELINE (v2)

 TIER 1 — CAUGHT / HANDLED           TIER 2 — FATAL JVM/COMPOSE CRASH
 (GPX parse, listeners, network,     (Thread.UncaughtExceptionHandler)
  auth, location, UI_COMPOSE)
         │                                      │
         ▼                                      ▼
 PII Sanitizer + truncation            GlobalCrashHandler:
         │                              synchronous atomic JSON dump
         ▼                              (filesDir/crash_reports/crash_<guid>.json, fsync)
 In-memory token bucket + dedup               │
 (LruCache + SharedPreferences,        delegate to previous handler (OS kill)
  NO Room)                                   │
         │                              next launch: flushPendingCrashDumps()
         ▼                                      │
 Firestore SDK .set(record)  ◄──────────────────┘  (offline-safe, retries on reconnect)
```

### 5.1 Tier 1 — direct Firestore write (handled exceptions)

- Firestore SDK has **built-in offline persistence + automatic retry** → no custom queue.
- Write directly: `app_errors/{guid}.set(record)`; when offline the SDK queues locally and flushes on reconnect.
- No `pending_errors` table, no custom flusher, no ConnectivityManager wiring.

### 5.2 Tier 2 — fatal crash: synchronous disk dump + flush-on-launch

- The crash handler must **never do network I/O or Firestore calls** during JVM death — the async SDK executors and write-ahead log are torn down before the write completes (Flaw 2).
- Instead, synchronously write a **structured JSON** crash file (`crash_<guid>.json`, all record fields, already sanitized) to `filesDir/crash_reports/`, `flush()`+`fsync()`, then **always delegate** to the previous handler.
- `EzrahiApplication.onCreate` → `exceptionLogger.flushPendingCrashDumps()`: for each file → re-sanitize (idempotent) → `set()` → delete on success, keep on failure; purge files older than 30 days.

### 5.3 Crash-handler correctness (critical)

- `GlobalCrashHandler.uncaughtException()`: (1) dump to disk, (2) **delegate** to the previous handler (`defaultHandler?.uncaughtException(thread, throwable)`). Never swallow — a handler that returns without delegating leaves a zombie process.
- Budget: dump + fsync completes well under ~1 s (small file, no network).
- Idempotent install (guard `currentHandler is GlobalCrashHandler`).

### 5.4 PII sanitizer (Flaw 6)

Applied to **message, cause, stackTrace, breadcrumbs** — in code and again in `flushPendingCrashDumps` (idempotent):

| Pattern | Redacted to |
|---|---|
| Phone numbers `\+?\d[\d\s\-().]{8,}\d` | `[PHONE]` |
| Emails (standard regex) | `[EMAIL]` |
| Coordinates `(lat\|lng\|latitude\|longitude)[:=]\s*[-+]?\d*\.?\d+` (decimal + DMS variants) | `[COORDS]` |
| Google API keys `AIza[0-9A-Za-z_\-]{35}` | `[API_KEY]` |
| `Bearer \S+`; `(token\|password\|secret\|authorization\|apikey)[:=]\s*\S+` | `[SECRET]` |
| File/Content URIs `(content://\|file:///)[^\s"']+` | `[URI]` |
| `/storage/...`, `/data/data/...` path segments | `[PATH]` |

### 5.5 Room-free bookkeeping — no recursion (Flaw 3)

- **No Room anywhere in the logging path.** If SQLite is the crash source (disk full, migration failure), the logger must not touch the DB again.
- Rate-limit counter + dedup hashes: in-memory `LruCache<String, Long>` + `AtomicInteger`; the hourly counter is persisted to a single `SharedPreferences` key (atomic, crash-safe). **No Room migration / DB version bump.**

### 5.6 Token bucket + circuit breaker (Flaw 4)

- **Sustained:** 20 records/hour/device (window persisted). Past limit → one `RATE_LIMITED` marker per window, then drop.
- **Burst:** token bucket capped at **5 writes/second**; excess dropped, not written.
- **Circuit breaker:** >5 errors/sec sustained 30 s → mute logging for 5 min (write one `CIRCUIT_OPEN` marker). Prevents a recomposition loop across hundreds of devices from generating billable writes.
- **Dedup:** identical `errorHash` within 3 minutes → skipped (counts against hourly budget).
- Enforcement is **client-side only** (rules cannot count) — App Check recommended (§10 Q3).

### 5.7 Obfuscation / release builds — R8 stays ON (Flaw 5)

- **Do not disable minification.** ProGuard keep-rules:
  ```
  -keepattributes SourceFile,LineNumberTable
  -renamesourcefileattribute SourceFile
  -keepclassmembers class com.arielfaridja.ezrahi.domain.model.** { *; }
  -keepclassmembers class com.arielfaridja.ezrahi.data.local.** { *; }
  -keepnames class * extends java.lang.Throwable
  ```
- Line numbers/file names preserved for readable traces; archive `mapping.txt` per build for full de-obfuscation when needed.

### 5.8 Threading & performance

- All logging on `Dispatchers.IO`; never block the main thread.
- Tier 1 is fire-and-forget; logger-internal failures swallow to Logcat only (failsafe).
- Crash path is disk-only and synchronous (§5.3).

### 5.9 Breadcrumbs (context ring buffer)

- Process-lifetime buffer (max 8): `SCREEN:<name>`, `EVENT:<id>`, `UPLOAD:start/fail`, `ROUTE:<name>`, `LOCATION:start/stop`, `AUTH:<status>`, `UI:<screen>`.
- Sanitized before storage; never contains message text, URIs, tokens, coordinates.

### 5.10 Local mirror

- Every logged error also writes `E/EzrahiCrash: <GUID> <message>` to Logcat for on-device correlation with the Firestore record.

## 6. Firestore rules (v2 — pre-auth support, strict constraints)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /app_errors/{errorId} {
      allow create: if request.resource.id == errorId
        && request.resource.data.id == errorId
        && request.resource.data.timestamp is number
        && request.resource.data.timestamp <= request.time.toMillis() + 300000    // no future-dating > 5 min
        && request.resource.data.timestamp >= request.time.toMillis() - 86400000  // flush window: backdate ≤ 24 h
        && request.resource.data.message is string
        && request.resource.data.message.size() <= 2048
        && request.resource.data.stackTrace is string
        && request.resource.data.stackTrace.size() <= 8192
        && ( (request.auth != null && request.resource.data.userId == request.auth.uid)
          || (request.auth == null && request.resource.data.userId.startsWith('anon-')) );
      allow read: if false;            // console/gcloud only unless PO opts into admin views (§10 Q2)
      allow update, delete: if false;  // immutable records
    }
  }
}
```

Notes:
- **Flaw 1 fixed:** pre-auth crashes write with `userId = "anon-<App Installations id>"`; rule accepts them via `startsWith('anon-')` — startup/0-day crashes are captured.
- 24 h backdate window accommodates next-launch flushes of crash dumps written while offline.
- Spoofing guard: authenticated writers can only attribute errors to their own uid; anonymous writers are restricted to the `anon-` namespace.
- Immutable: no client update/delete.
- **TTL:** enabled on `timestamp`, **30 days** (review default — confirm in §10 Q6).

## 7. Scaling for multi-event operations

- **Composite index** for the operations console: `eventId` ASC + `timestamp` DESC (`firestore.indexes.json`).
- **Alerting (optional, future):** Cloud Function on `/app_errors` create → if error rate for an `eventId` > 10/min, notify the basecamp manager (channel TBD — §10 Q5).
- TTL 30 days keeps the collection bounded; `eventId` filter keeps queries cheap.

## 8. Naming & placement (Android)

- `app/util/logging/ExceptionLogger.kt` — `@Singleton`, thread-safe (single lock around bookkeeping).
- `app/util/logging/GlobalCrashHandler.kt` — chained handler + disk dump.
- `app/util/logging/PiiSanitizer.kt` — regex pipeline.
- `app/util/logging/ErrorRecord.kt` + `ErrorType.kt` — model + taxonomy.
- `EzrahiApplication` (`@HiltAndroidApp`) — install handler + `flushPendingCrashDumps()` in `onCreate`.
- **No Room changes** (Room-free logging). DI: `ExceptionLogger` @Inject into Application and all ViewModels.

## 9. Instrumentation inventory (route existing failures through the logger)

| Site | File (current) | Type |
|---|---|---|
| Route GPX download/parse failure | `EzrahiRepositoryImpl.getActiveRoutePoints` | `ROUTE_PARSER` |
| Route upload failure (incl. validation) | `EzrahiRepositoryImpl.uploadRoute` | `ROUTE_PARSER` |
| Firestore listener errors | `EzrahiRepositoryImpl` (all `logListenerError` sites) | `FIRESTORE_LISTENER` |
| Upload / delete / activate / permission failures | `EventManagementViewModel` `.onFailure` | `NETWORK` |
| SOS / report / role / rename failures | `*ViewModel` `.onFailure` | `CAUGHT` |
| Location service failures | `LocationTrackingService` catches | `LOCATION_SERVICE` |
| Compose recomposition guard | risky `LaunchedEffect`/state blocks | `UI_COMPOSE` |
| Startup/pre-auth (flush errors, DI) | `Application` / login flow | `AUTH` |
| Any future `catch` / `runCatching.onFailure` | all modules | `CAUGHT` (AGENTS.md rule) |

## 10. Open questions (remaining for PO)

1. **"log every trace moves"** — (a) every exception stack trace (this spec), or (b) also device **location tracking history** server-side? (b) is a separate, much larger feature.
2. Read access to `app_errors`: Firebase Console only, or in-app admin view?
3. **App Check** to harden anonymous writes (recommended) — enable now or later?
4. Add Crashlytics alongside the custom logger (also the only path to native/NDK crashes)?
5. Alerting channel for the >10/min Cloud Function (email / Telegram / none for now)?
6. **RESOLVED by review (confirm only):** TTL 30 days; keep R8 + ProGuard rules; token bucket 5/s + 20/h budget; message cap 2 KB.

## 11. Non-goals & considerations

- Crashlytics remains the industry standard (native/NDK, breadcrumbs, grouping) — can be layered on (Q4).
- Never log: passwords, tokens, API keys, full emails, message bodies, raw URIs, coordinates — enforced by §5.4.
- Israeli Privacy Protection Law 5741-1981 / GDPR: error data is personal-data-adjacent — sanitize + 30-day TTL + `userId`-only identity.
- `sendSOS` / location points must NOT enter `app_errors` (Q1b if location history is wanted).
- Payloads are never built from untrusted server content without escaping (no script injection into console views).

## 12. Testing & validation plan (v2)

1. Unit: `PiiSanitizer` — phone/email/coords/API-key/URI/token cases + idempotency.
2. Unit: token bucket — burst cap, hourly window reset, circuit breaker open/close, dedup window.
3. Instrumented: force crash (button) → disk file exists → relaunch → record lands in Firestore with all fields (Tier 2).
4. Pre-auth: logged-out crash → `anon-` record accepted by rules (Tier 2 + rules).
5. Offline: airplane mode → throw → reconnect → SDK flushes (Tier 1).
6. Recomposition loop test: 10 throws/s → circuit breaker mutes, single `CIRCUIT_OPEN` marker, zero excess writes.
7. Recursion safety: no Room in logging path (code-review gate); simulate disk-full/SQLite-corrupt scenario → logger still works.
8. Rules (TestRuleset ≥ 8 scenarios): authed valid ALLOW; pre-auth `anon-` ALLOW; oversized message/stack DENY; future (>5 min) DENY; backdated (>24 h) DENY; spoofed uid DENY; unauth non-anon DENY; read/update/delete DENY.
9. Release build: traces keep line numbers; `mapping.txt` archived per build.

## 13. Delivery plan (when approved)

1. `util/logging/` package: `ErrorType`, `ErrorRecord`, `PiiSanitizer`, `ExceptionLogger` (token bucket + dedup + SharedPreferences counter), `GlobalCrashHandler`, `EzrahiApplication` registration + flush.
2. ProGuard keep-rules + `mapping.txt` archiving.
3. Route the §9 inventory through the logger.
4. Firestore rules + composite index + TTL 30 days + deploy + TestRuleset run.
5. Build, stage APK, run §12 suite.
6. Commit (feature + docs; tracker row in `docs/summaries/refactor-fixes.md`).

----
## professional criticism


#  Executive Architectural Review & Scaling Blueprint
**Document Under Review:** `todo-fix-6-exception-logging.md` (Global Frontend Exception Logging)  
**Evaluated by:** VP of Product (PM), Principal Android Architect (EM), Mobile Security Lead (CISO)  
**Verdict:** **High-Potential Concept with 6 Critical Architectural & Scalability Flaws**

---

## 1. Executive Summary & Health Check

The requirement from the Product Owner (PO) is vital: **silent frontend failures (like the GPX route parsing bug) must never go unnoticed.** 

However, the current specification in `todo-fix-6-exception-logging.md` has fatal blind spots that will cause:
1. **Lost Pre-Login Crashes** (Security rules will reject any crash that happens before user authentication).
2. **Process Death / Deadlocks during Uncaught Crashes** (Firestore SDK cannot guarantee async disk/network flushing during JVM termination).
3. **Recursive Crash Loops** (Using Room DB inside the crash handler will trigger cascading crashes if the disk/database corrupts).
4. **Runaway Cloud Costs at Scale** (50 concurrent events with 500 devices in a recomposition loop could generate hundreds of thousands of billable Firestore writes per hour).

Below is the exhaustive critique of every weakness, followed by an **Enterprise-Grade Scaling Blueprint** that transforms this spec into a resilient, production-ready system.

---

## 2. The 6 Critical Flaws in the Current Spec

```
                           CURRENT SPEC VULNERABILITIES
 
  1. PRE-AUTH CRASH BLACK HOLE: Rule requires `auth != null` (Drops 0-day bugs)
  2. UNCAUGHT CRASH DEADLOCK: Firestore async SDK used during JVM shutdown   
  3. RECURSIVE ROOM CRASH: Logger calls Room  Room failure crashes Logger   
  4. RECOMPOSITION COST BOMB: Infinite loops on 500+ field devices = $$$$    
  5. R8 DISABLE ANTI-PATTERN: Disabling minification bloats APK & ruins app 
  6. PII & COMPLIANCE LEAKS: Stack traces & messages log raw phone/file data 
 
```

---

### Flaw 1: The Pre-Auth Crash Black Hole (Security Rule Bug)
* **The Problem:** The draft rule in �6 specifies:
  ```javascript
  allow create: if request.auth != null && request.resource.data.userId == request.auth.uid;
  ```
* **The Consequence:** The most severe crashes in Android occur during application startup (`Application.onCreate`, `StartupActivity`, dependency injection initialization, or before the user enters login credentials). Because `request.auth` is `null`, **Firestore will reject 100% of pre-authentication crashes**.
* **The Fix:** Allow unauthenticated error reports by separating the schema or permitting `userId == null` with strict payload constraints and anonymous device tokens (or Firebase App Check).

---

### Flaw 2: Firestore SDK Reliability During Process Termination
* **The Problem:** When an uncaught exception hits `Thread.UncaughtExceptionHandler`, Android allocates a very short time slice before tearing down threads, open sockets, and IPC binders.
* **The Consequence:** Calling Firestore SDK's `set()` relies on background executors and an embedded SQLite/LevelDB write-ahead log. When the OS terminates the process, the background write is cut off mid-flight, resulting in **dropped crash reports**.
* **The Fix:** 
  1. For **Uncaught/Fatal Crashes**: Write an immediate, atomic JSON dump to a dedicated crash directory in app-internal storage (`/data/data/.../files/crashes/crash_<guid>.json`). On the next app launch, a startup worker flushes all stored crash dumps to Firestore immediately.
  2. For **Caught/Handled Exceptions** (GPX parsing, Firestore listeners, network timeouts): Write directly via Firestore SDK since the app process is alive and healthy.

---

### Flaw 3: Recursive Crash Loop with Room Database
* **The Problem:** �5.1 and �7 propose using Room (`error_counters` table) to maintain rate limits and dedup hashes.
* **The Consequence:** If an exception is caused by SQLite corruption, full disk space (`SQLiteDiskIOException`), or a Room migration failure, the `catch` block calls `ExceptionLogger`, which attempts to call Room again. This causes a **recursive stack overflow / double crash**.
* **The Fix:** Decouple `ExceptionLogger` bookkeeping completely from Room. Use an in-memory `LruCache` / `AtomicInteger` array backed by a lightweight, atomic `SharedPreferences` or flat binary file.

---

### Flaw 4: Recomposition Loops & The Multi-Event Cost Bomb
* **The Problem:** In Jetpack Compose, a bug inside a Composable often triggers a continuous recomposition loop (throwing 60 exceptions per second). 
* **The Consequence:** If 50 events are active across 2,000 active devices, a bad UI state bug can generate **millions of Firestore document writes in a few minutes**, resulting in massive billing spikes and Firestore throttling.
* **The Fix:** Implement a **Token Bucket Rate Limiter with Exponential Backoff** per device and a circuit breaker that cuts off logging if error frequency exceeds 5 errors/second.

---

### Flaw 5: The Minification / R8 Bad Advice
* **The Problem:** �5.6 suggests considering "(a) Disable minification for stack fidelity".
* **The Consequence:** Disabling R8 minification in production increases APK size by 40�60%, leaves code unprotected against reverse engineering, and disables critical compiler optimizations.
* **The Fix:** Keep full R8 minification enabled, configure ProGuard rules to retain line numbers and file names (`-keepattributes SourceFile,LineNumberTable`), and maintain an automated build step that uploads the `mapping.txt` file (or keep class/method names in domain packages).

---

### Flaw 6: PII Leakage & Privacy Law Compliance
* **The Problem:** Stack traces and exception messages often capture user phone numbers, file paths, coordinates, or token parameters (e.g., `IllegalArgumentException: Invalid phone +972501234567`).
* **The Consequence:** Under the Israeli Privacy Protection Law (5741-1981) and GDPR, collecting unscrubbed log data containing personal identifiers in an operational error database violates privacy regulations.
* **The Fix:** Pass every message and stack trace through a **PII Sanitizer Pipeline** (Regex redactor for phone numbers, tokens, and GPS coordinates) before sending it over the wire.

---

# 3. Target Architecture: Two-Tier Resilient Logging System

```
                          TWO-TIER LOGGING PIPELINE
 
  SOURCE 1: Caught Exceptions           SOURCE 2: Fatal JVM / Compose Crashes 
  (GPX Parse, Listeners, Auth, Network) (UncaughtExceptionHandler)            
 
                                                               
                                                               
               
          PII Sanitizer & Truncation            Direct Atomic Disk Dump     
                 (/data/files/crashes/*.json)
                                               
                                                               (Next Startup)
                              
         In-Memory Token-Bucket & Dedup                      
         (LruCache / SharedPrefs - No    Flush Pending Crash Dumps
          Room Dependency)             
        
                        
                        
        
         Firestore SDK: `app_errors`   
         (Partitioned & Index-Ready)   
        
```

---

# 4. Refined Data Model for Multi-Event Scale

Collection: **`app_errors/{guid}`**

| Field | Type | Purpose / Indexing Strategy |
|---|---|---|
| `id` | `string` | Unique GUID (Document ID) |
| `errorType` | `string` | `CRASH`, `ROUTE_PARSER`, `NETWORK`, `FIRESTORE_LISTENER`, `AUTH`, `LOCATION_SERVICE`, `UI_COMPOSE` |
| `severity` | `string` | `FATAL`, `ERROR`, `WARNING` |
| `message` | `string` | Sanitized exception message (max 2 KB) |
| `stackTrace` | `string` | Sanitized stack trace (max 8 KB) |
| `errorHash` | `string` | `sha256(errorType + firstSanitizedStackFrame)` for instant server-side aggregation |
| `eventId` | `string?` | **Crucial for Multi-Event Scaling:** Filters errors affecting specific field events |
| `userId` | `string?` | UID or `"ANONYMOUS_PRE_AUTH"` |
| `appVersion` | `string` | `BuildConfig.VERSION_NAME` (e.g., `2.0.1`) |
| `buildCode` | `number` | `BuildConfig.VERSION_CODE` |
| `osVersion` | `number` | `Build.VERSION.SDK_INT` |
| `deviceModel` | `string` | `Build.MANUFACTURER + " " + Build.MODEL` |
| `timestamp` | `number` | Milliseconds epoch (for queries & TTL auto-delete) |
| `breadcrumbs` | `list<string>` | Ring buffer of last 8 user actions (Sanitized) |
| `isFatal` | `boolean` | `true` if process terminated; `false` if caught |

---

# 5. Production-Grade Implementation Specifications

### 5.1 The PII Sanitizer & In-Memory Token Bucket Logger

 **`app/src/main/java/com/arielfaridja/ezrahi/util/logging/ExceptionLogger.kt`**

```kotlin
package com.arielfaridja.ezrahi.util.logging

import android.content.Context
import android.os.Build
import android.util.Log
import android.util.LruCache
import com.arielfaridja.ezrahi.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExceptionLogger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val loggerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recentErrorHashes = LruCache<String, Long>(50) // Dedup cache
    private val hourlyCounter = AtomicInteger(0)
    private var lastCounterResetTime = System.currentTimeMillis()

    // Breadcrumbs ring buffer (max 8)
    private val breadcrumbs = ArrayDeque<String>(8)

    fun addBreadcrumb(crumb: String) {
        synchronized(breadcrumbs) {
            if (breadcrumbs.size >= 8) breadcrumbs.removeFirst()
            breadcrumbs.addLast("[${System.currentTimeMillis()}] $crumb")
        }
    }

    /**
     * Main entry point for caught business and infrastructure exceptions
     */
    fun logException(
        throwable: Throwable,
        errorType: String,
        eventId: String? = null,
        severity: String = "ERROR"
    ) {
        loggerScope.launch {
            try {
                val rawMessage = throwable.message ?: throwable.javaClass.simpleName
                val sanitizedMessage = sanitizePII(rawMessage).take(2048)
                val sanitizedStackTrace = sanitizePII(getStackTraceString(throwable)).take(8192)
                val topFrame = throwable.stackTrace.firstOrNull()?.toString() ?: "unknown"
                val errorHash = computeHash("$errorType:$topFrame")

                // 1. Rate-Limit & Dedup Check
                if (isDuplicateOrRateLimited(errorHash)) {
                    Log.w("EzrahiLogger", "Suppressed duplicate/rate-limited error: $errorHash")
                    return@launch
                }

                // 2. Build Error Record
                val errorId = UUID.randomUUID().toString()
                val record = hashMapOf(
                    "id" to errorId,
                    "errorType" to errorType,
                    "severity" to severity,
                    "message" to sanitizedMessage,
                    "stackTrace" to sanitizedStackTrace,
                    "errorHash" to errorHash,
                    "eventId" to eventId,
                    "userId" to (auth.currentUser?.uid ?: "PRE_AUTH"),
                    "appVersion" to BuildConfig.VERSION_NAME,
                    "buildCode" to BuildConfig.VERSION_CODE,
                    "osVersion" to Build.VERSION.SDK_INT,
                    "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                    "timestamp" to System.currentTimeMillis(),
                    "breadcrumbs" to synchronized(breadcrumbs) { breadcrumbs.toList() },
                    "isFatal" to false
                )

                // 3. Write directly to Firestore with offline caching enabled
                firestore.collection("app_errors").document(errorId)
                    .set(record, SetOptions.merge())
                    .addOnFailureListener { e ->
                        Log.e("EzrahiLogger", "Failed to send error to Firestore: ${e.message}")
                    }

            } catch (e: Exception) {
                // Failsafe: Never let the logger crash the app
                Log.e("EzrahiLogger", "Logger internal failure: ${e.message}")
            }
        }
    }

    /**
     * Flushes crash dumps saved to disk by the Uncaught Crash Handler
     */
    fun flushPendingCrashDumps() {
        loggerScope.launch {
            val crashDir = File(context.filesDir, "crash_reports")
            if (!crashDir.exists()) return@launch

            crashDir.listFiles()?.forEach { file ->
                try {
                    val content = file.readText()
                    val errorId = file.nameWithoutExtension.removePrefix("crash_")
                    
                    // Parse simple JSON and send to Firestore
                    val docData = hashMapOf(
                        "id" to errorId,
                        "errorType" to "CRASH",
                        "severity" to "FATAL",
                        "stackTrace" to content,
                        "timestamp" to file.lastModified(),
                        "isFatal" to true,
                        "userId" to (auth.currentUser?.uid ?: "UNKNOWN")
                    )

                    firestore.collection("app_errors").document(errorId).set(docData).await()
                    file.delete() // Cleanup on successful write
                } catch (e: Exception) {
                    Log.e("EzrahiLogger", "Failed to flush crash dump: ${file.name}", e)
                }
            }
        }
    }

    private fun isDuplicateOrRateLimited(hash: String): Boolean {
        val now = System.currentTimeMillis()
        
        // Reset hourly window
        if (now - lastCounterResetTime > 3600000L) {
            hourlyCounter.set(0)
            lastCounterResetTime = now
        }

        // Limit to 30 errors/device/hour
        if (hourlyCounter.incrementAndGet() > 30) return true

        // Dedup: suppress identical errors within a 3-minute window
        synchronized(recentErrorHashes) {
            val lastSeen = recentErrorHashes.get(hash)
            if (lastSeen != null && (now - lastSeen) < 180000L) {
                return true
            }
            recentErrorHashes.put(hash, now)
        }
        return false
    }

    private fun sanitizePII(text: String): String {
        return text
            .replace(Regex("""\+?\d{9,13}"""), "[PHONE_REDACTED]") // Redact phone numbers
            .replace(Regex("""[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+"""), "[EMAIL_REDACTED]") // Redact emails
            .replace(Regex("""\b(lat|lng|latitude|longitude)[:=]\s*[-+]?\d*\.\d+""", RegexOption.IGNORE_CASE), "[COORDS_REDACTED]")
            .replace(Regex("""(bearer|token|password|auth)[:=]\s*\S+""", RegexOption.IGNORE_CASE), "[SECRET_REDACTED]")
    }

    private fun getStackTraceString(t: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        t.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }

    private fun computeHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}
```

---

### 5.2 The Fail-Safe Global Crash Handler

 **`app/src/main/java/com/arielfaridja/ezrahi/util/logging/GlobalCrashHandler.kt`**

```kotlin
package com.arielfaridja.ezrahi.util.logging

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.UUID

class GlobalCrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // 1. Synchronously dump stack trace to a local disk file (100% reliable during JVM death)
            val crashDir = File(context.filesDir, "crash_reports").apply { mkdirs() }
            val crashFile = File(crashDir, "crash_${UUID.randomUUID()}.txt")

            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            
            crashFile.writeText("""
                Thread: ${thread.name}
                Timestamp: ${System.currentTimeMillis()}
                Stack:
                ${sw}
            """.trimIndent())

            Log.e("EzrahiCrash", "Crash dump saved to ${crashFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("EzrahiCrash", "Failed to write local crash dump", e)
        } finally {
            // 2. ALWAYS delegate to system/OS handler so process terminates cleanly
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        fun install(context: Context) {
            val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (currentHandler !is GlobalCrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(GlobalCrashHandler(context, currentHandler))
            }
        }
    }
}
```

---

### 5.3 Registration in `EzrahiApp.kt`

 **`app/src/main/java/com/arielfaridja/ezrahi/EzrahiApp.kt`**

```kotlin
package com.arielfaridja.ezrahi

import android.app.Application
import com.arielfaridja.ezrahi.util.logging.ExceptionLogger
import com.arielfaridja.ezrahi.util.logging.GlobalCrashHandler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EzrahiApp : Application() {

    @Inject lateinit var exceptionLogger: ExceptionLogger

    override fun onCreate() {
        super.onCreate()
        
        // 1. Install crash handler for fatal JVM crashes
        GlobalCrashHandler.install(this)

        // 2. Flush any pending crashes from previous app launches
        exceptionLogger.flushPendingCrashDumps()
    }
}
```

---

### 5.4 Production Firestore Rules (`firestore.rules`)

This updated rule set allows authenticated users to log errors, permits unauthenticated pre-login errors under strict size constraints, and prevents reading or modifying errors from client apps.

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    match /app_errors/{errorId} {
      // 1. Allow creation from both authenticated and pre-auth states
      allow create: if request.resource.id == errorId
        && request.resource.data.id == errorId
        && request.resource.data.timestamp is number
        // Guard against future-dating or severe backdating
        && request.resource.data.timestamp <= request.time.toMillis() + 300000
        && request.resource.data.timestamp >= request.time.toMillis() - 86400000
        // Payload size limits to prevent database spam
        && request.resource.data.message is string
        && request.resource.data.message.size() <= 2048
        && request.resource.data.stackTrace is string
        && request.resource.data.stackTrace.size() <= 8192
        && (
             (request.auth != null && request.resource.data.userId == request.auth.uid) ||
             (request.auth == null && request.resource.data.userId in ["PRE_AUTH", "UNKNOWN"])
           );

      // 2. Strict lockdown: No client reads, updates, or deletes
      allow read: if false;
      allow update, delete: if false;
    }
  }
}
```

---

### 5.5 ProGuard / R8 Rules for Stack Trace Fidelity

Ensure line numbers and file names are preserved in production without disabling code shrinking and optimizations.

 **`app/proguard-rules.pro`**

```proguard
# 1. Keep source file names and line numbers for stack trace readability
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 2. Keep Domain Models and Entities for safe deserialization
-keepclassmembers class com.arielfaridja.ezrahi.domain.model.** { *; }
-keepclassmembers class com.arielfaridja.ezrahi.data.local.** { *; }

# 3. Ensure Exception classes retain their original names
-keepnames class * extends java.lang.Throwable
```

---

# 6. Scaling to Manage Multiple Simultaneous Field Events

When managing 50+ concurrent events across youth movements, use these three operational patterns:

```
                  MULTI-EVENT AGGREGATION ARCHITECTURE
 
  Field Event A (Desert Hike)  `app_errors` [eventId: "event_A"]        
  Field Event B (Galilee Camp)  `app_errors` [eventId: "event_B"]        
 
                                        
                                        
             
              Composite Firestore Index:                        
              `eventId` (Asc) + `timestamp` (Desc)              
             
                                        
                                        
             
              Manager Web Dashboard / Cloud Function Trigger:   
              Alerts Basecamp Manager if Event Error Rate > 10/m
             
```

1. **Firestore Composite Indexes:**
   Add a composite index in `firestore.indexes.json`:
   ```json
   {
     "collectionGroup": "app_errors",
     "queryScope": "COLLECTION",
     "fields": [
       { "fieldPath": "eventId", "order": "ASCENDING" },
       { "fieldPath": "timestamp", "order": "DESCENDING" }
     ]
   }
   ```
   *Benefit:* Allows the central operations console to query all errors for a specific trip in real time without scanning the entire collection.

2. **Automated TTL (Time-To-Live):**
   Enable Firestore TTL on the `timestamp` field set to **30 days**. This automatically purges old logs, preventing database bloat and maintaining compliance.

---

# 7. Action Plan for Development

You are now ready to instruct your AI agent or implement the feature yourself following this sequence:

1. **Create Package:** Add `app/src/main/java/com/arielfaridja/ezrahi/util/logging/`.
2. **Implement `ExceptionLogger.kt`:** Include PII sanitization, in-memory token bucket rate limiting, and disk flush logic (�5.1).
3. **Implement `GlobalCrashHandler.kt`:** Include the fail-safe atomic local file dumper (�5.2).
4. **Register in `EzrahiApp.kt`:** Install the handler in `onCreate` (�5.3).
5. **Update Call Sites:** Route all `catch` and `.onFailure` blocks (such as GPX parsing and Firestore listeners) through `exceptionLogger.logException(...)`.
6. **Deploy `firestore.rules`:** Apply the authenticated and pre-auth rules (�5.4).
7. **Verify:**
   * Test 1: Throw a test exception in a ViewModel $\rightarrow$ verify record in Firestore.
   * Test 2: Trigger a crash with a button click $\rightarrow$ re-launch app $\rightarrow$ verify flushed crash dump in Firestore.
   * Test 3: Enable Airplane mode, trigger an error, reconnect $\rightarrow$ verify write automatically flushes.


