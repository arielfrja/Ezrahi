# todo-fix-6 — Global Frontend Exception Logging (Specification)

> **STATUS: 🔵 SPEC ONLY — no development yet (PO: "don't develop it yet, just create a specification document").**

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

## 3. Scope

Captured sources (all funneled into one `ExceptionLogger`):

| Source | Mechanism |
|---|---|
| Uncaught JVM/Android exceptions (crash) | `Thread.setDefaultUncaughtExceptionHandler` — **chain the previous handler** |
| AndroidX/Compose runtime crashes | surface through the default thread handler (Compose exceptions propagate to the thread that dispatched them) |
| Coroutine exceptions in app scopes | `CoroutineExceptionHandler` on the app's root scope + `viewModelScope` custom handler where needed |
| Caught exceptions in business logic (route download/parse, uploads, Firestore writes, listeners) | every `catch` / `runCatching.onFailure` routes through `ExceptionLogger.log()` — no more silent Logcat-only failures |
| Cancellation / expected flow-control exceptions (`CancellationException`, `IOException` on retried ops) | **never logged** — noise (explicit exclusion) |

**Out of scope:** OS-level native crashes (requires Crashlytics NDK or a native handler — see §9 Q5).

## 4. Firestore data model

Collection: **`app_errors`** — document ID = the GUID.

| Field | Type | Notes |
|---|---|---|
| `id` | string (UUID v4, client-generated) | document id == this value |
| `errorType` | string | taxonomy: `CRASH` / `CAUGHT` / `ROUTE` / `NETWORK` / `LISTENER` / `AUTH` / `UNKNOWN` |
| `message` | string | exception message (truncate to 4 KB) |
| `cause` | string \| null | `exception.cause?.message` (root-cause hint, truncate 1 KB) |
| `stackTrace` | string | full stack trace (truncate to 8 KB, keep head + first 3 cause frames) |
| `firstFrame` | string | first stack frame — dedup/grouping key input |
| `errorHash` | string | `sha1(message + firstFrame + top cause frame)` — grouping/dedup key |
| `timestamp` | number (epoch millis) | rule-validation anchor + sort key |
| `timestampIso` | string | ISO-8601 UTC + local offset for humans |
| `appVersion` | string | `BuildConfig.VERSION_NAME` |
| `buildCode` | number | `BuildConfig.VERSION_CODE` |
| `deviceModel` | string | `Build.MANUFACTURER + Build.MODEL` |
| `osVersion` | string | `Build.VERSION.RELEASE` + `SDK_INT` |
| `userId` | string \| null | FirebaseAuth uid if signed in (never email — PII) |
| `eventId` | string \| null | active event context if available |
| `screen` | string \| null | last known destination/screen name |
| `sessionId` | string | GUID per app process start — groups errors from one run |
| `threadName` | string | name of the throwing thread; `main` = UI-thread crash |
| `isFatal` | boolean | uncaught crash vs handled exception |
| `inForeground` | boolean \| null | app foreground state at time of error |
| `breadcrumbs` | array of string | ring buffer (max 8) of recent context events — see §5.4 |

### 4.1 Budget & guardrails

- Max payload per doc ≈ **16 KB** (Firestore doc limit is 1 MiB — we stay tiny and cheap).
- Truncation is applied in code **and** enforced in rules (double guard, §6).

## 5. Architecture (design, not yet implemented)

```
Application.onCreate
  ├─ create sessionId (process-scoped UUID)
  ├─ register GlobalCrashHandler (CHAINED default handler — see §5.2)
  └─ appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler → logger)

ExceptionLogger (@Singleton, @Inject)
  ├─ log(throwable, errorType, eventId?, screen?, fatal = false)
  ├─ builds ErrorRecord (GUID, timestamps, device/app info, breadcrumbs snapshot)
  ├─ dedup check (errorHash window) + rate-limit check (§5.5)
  └─ write via Firestore SDK (offline-safe by default) — §5.1
```

### 5.1 Delivery — no custom queue needed

Firestore SDK has **built-in offline persistence and write retry**. Delivery strategy:

1. `app_errors/{guid}.set(record)` directly — the SDK queues the write locally when offline and flushes automatically on reconnect.
2. No Room `pending_errors` table, no custom flusher, no ConnectivityManager wiring → less code, fewer failure modes.
3. Room is used **only** for bookkeeping that must survive restart: hourly rate-limit counter + recent `errorHash`/timestamp window for dedup.

### 5.2 Crash-handler correctness (critical)

- `Thread.setDefaultUncaughtExceptionHandler(handler)` where `handler`:
  1. logs synchronously to Firestore (with a short timeout budget — see 5.3),
  2. **always delegates to the previous handler** (`previousHandler.uncaughtException(thread, throwable)`) so the OS still terminates the process and shows the system crash UI.
- **Never** swallow the crash: a handler that returns without delegating leaves a zombie process.
- Firestore writes during a crash are best-effort: if the app dies before the write lands, the SDK's offline cache persists it and retries on next launch.

### 5.3 Threading & performance

- All logging runs on `Dispatchers.IO`; never on the main thread.
- Crash-path budget: attempt the write, then delegate to the OS handler within ~1 s (timebox the log call; don't block the death sequence).
- Logging is fire-and-forget — failures inside the logger are swallowed (Logcat only).

### 5.4 Breadcrumbs (context ring buffer)

- A process-lifetime ring buffer (max 8) records lightweight events: `SCREEN:<name>`, `EVENT:<id>`, `UPLOAD:start/fail`, `ROUTE:<name>`, `LOCATION:start/stop`, `AUTH:<status>`.
- Flushed into every error record — turns a bare stack into "what was the user doing".
- Never contains: message text, URIs, tokens, coordinates.

### 5.5 Dedup & rate limiting

- **Dedup:** identical `errorHash` within 5 minutes → skipped (counts against the hourly limit).
- **Rate limit:** max **20 records/hour/device** — past that, write one `RATE_LIMITED` marker per window, then drop.
- Enforcement is **client-side only** (Firestore rules cannot count). Documented as a known limit — a malicious client can spam the collection; mitigations in §6 notes.

### 5.6 Release builds & obfuscation (must-read)

- With R8/minify enabled, stack traces are obfuscated (`a.b.c.d(...)`). Options:
  - (a) **Disable minification for stack fidelity** — acceptable for v1, keeps traces readable; or
  - (b) keep minify + upload the **mapping file** and de-obfuscate with `retrace`/`retype` when reading errors (workflow overhead);
  - (c) keep `-keepattributes SourceFile,LineNumberTable` + `-renamesourcefileattribute SourceFile` to preserve line numbers (cheap, recommended alongside either).
- Decision needed in §9 (Q6).

### 5.7 Local mirror

- Every logged error is also written to Logcat with its GUID: `E/EzrahiCrash: <GUID> <message>` — lets a dev on the device correlate the Firestore record to local logs.

## 6. Firestore rules (draft for the spec)

```
match /app_errors/{errorId} {
    allow create: if request.auth != null
        && request.resource.id == errorId
        && request.resource.data.id == errorId
        && request.resource.data.timestamp <= request.time + 300000    // not backdated > 5 min
        && request.resource.data.timestamp >= request.time - 600000   // not future-dated
        && request.resource.data.message is string
        && request.resource.data.message.size() <= 4096
        && request.resource.data.stackTrace.size() <= 8192
        && request.resource.data.userId == request.auth.uid;          // cannot spoof another user
    allow read: if false;   // console/gcloud only unless PO wants in-app admin views (Q2)
    allow update, delete: if false;  // immutable error records
}
```

Notes:
- `userId == request.auth.uid` prevents writing errors attributed to other users.
- Reads default to **false**; query via Firebase Console / gcloud. Toggle per Q2.
- Writes are immutable: no update/delete from clients.
- **TTL/retention:** set an `expireAt` timestamp field and enable Firestore TTL when retention is chosen (Q4).
- Known limit: per-doc size caps only — no server-side rate limit (see §5.5).

## 7. Naming & placement (Android)

- `app/util/ExceptionLogger.kt` — `@Singleton`, thread-safe (single Mutex around bookkeeping).
- `app/util/GlobalCrashHandler.kt` — chained uncaught-handler registration.
- `app/util/ErrorRecord.kt` + `app/util/ErrorType.kt` — model + taxonomy enums.
- `EzrahiApplication` (`@HiltAndroidApp`) — register handler + sessionId in `onCreate`.
- Room: `ErrorCounterEntity` table `error_counters` (rate-limit + dedup state), DB version +1.
- DI: `ExceptionLogger` injected into Application and available to all ViewModels.

## 8. Instrumentation inventory (route existing failures through the logger)

Concrete call sites that today swallow/`Log.w` silently — each must call `logger.log(...)` instead:

| Site | File (current) | Type |
|---|---|---|
| Route GPX download/parse failure | `EzrahiRepositoryImpl.getActiveRoutePoints` | `ROUTE` |
| Route upload failure (incl. validation) | `EzrahiRepositoryImpl.uploadRoute` | `ROUTE` |
| Firestore listener errors | `EzrahiRepositoryImpl` (all `logListenerError` sites) | `LISTENER` |
| Upload / delete / activate / permission failures | `EventManagementViewModel` (`.onFailure`) | `ROUTE` / `NETWORK` |
| SOS / report / role / rename failures | `*ViewModel` `.onFailure` blocks | `CAUGHT` |
| Location service failures | `LocationTrackingService` catches | `CAUGHT` |
| Any future `catch` / `runCatching.onFailure` in app code | all modules | `CAUGHT` (AGENTS.md rule) |

## 9. Open questions (for PO)

1. **"log every trace moves"** — clarify: (a) every exception's stack trace (covered here), or (b) also **device location tracking history** (every location point logged server-side)? (b) is a separate, much larger feature — separate spec.
2. Read access to `app_errors`: Firebase Console only, or in-app visibility for managers/admins?
3. Include network/API operation failures as first-class loggable events (yes per §8), or crashes only?
4. Retention: keep forever / 30 / 90 days (TTL)?
5. Add Crashlytics alongside the custom logger, or custom only? (also the only path to native/NDK crash capture)
6. Release builds: disable minification for readable traces (a), or keep minify + mapping-file workflow (b/c)?

## 10. Non-goals & considerations

- **Crashlytics** remains the industry standard (free tier, native + NDK + breadcrumbs, automatic grouping). PO requested custom Firestore logging — honored; Crashlytics can be layered on later (Q5).
- Never log: passwords, tokens, API keys, full emails, message content, raw URIs, coordinates.
- PII minimization: `userId` only; Israeli privacy context (Privacy Protection Law 5741-1981) — treat error data as personal-data-adjacent.
- `sendSOS` / location data must NOT enter `app_errors` (separate domain; Q1b if location history is wanted).
- Error payloads must never be built from untrusted server content without escaping (no script injection into console views).

## 11. Testing & validation plan (when approved)

1. Unit: `ExceptionLogger` — GUID format, truncation, dedup window, rate-limit marker, `errorHash` stability.
2. Instrumented: force a crash (throw in a button handler) → verify record in Firestore with all fields.
3. Offline test: airplane mode → throw → land → verify write lands after reconnect (SDK offline persistence).
4. Dedup: throw the same error twice within 5 min → exactly one record.
5. Rate limit: throw > 20 in an hour → marker record + drops.
6. Rules: TestRuleset scenarios — valid create ALLOW; oversized message/stack DENY; future/backdated timestamp DENY; spoofed `userId` DENY; unauthenticated DENY; read DENY; update/delete DENY (≥ 8 scenarios).
7. Release build: confirm traces readable (or mapping workflow in place per Q6).

## 12. Delivery plan (when approved)

1. `ErrorType` + `ErrorRecord` + `ExceptionLogger` (dedup/rate-limit/Room counters) + `GlobalCrashHandler` + Application registration.
2. Route the §8 inventory through the logger.
3. Firestore rules for `app_errors` + deploy + TestRuleset run (≥ 8 scenarios).
4. Build, stage APK, run §11 tests (crash injection, offline flush, dedup, rate-limit).
5. Commit (feature + docs; tracker row in `docs/summaries/refactor-fixes.md`).