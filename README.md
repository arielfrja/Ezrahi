# Ezrahi

Field-activity coordination app for guides and participants: live location sharing on an offline-first map, role-based messaging, field reports, quick dial, and event management.

**Stack:** Kotlin, Jetpack Compose (Material 3), Hilt, Room (offline-first cache), Firestore (live sync), osmdroid (map), Firebase Auth, Foreground Service (background location).

## Build

```sh
sh gradlew :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Exception logging (fix-6)

Every frontend exception and crash is logged to **Firestore** in the `app_errors`
collection (one document per error, document ID = GUID). Details and design:
`docs/specs/todo-fix-6-exception-logging.md`.

**Where errors land**

| Source | Where it goes | When |
|---|---|---|
| Handled exceptions (parse, uploads, listeners, network, auth, location) | Firestore `app_errors` (direct write, offline-safe) | Immediately |
| Fatal crashes | Disk dump `filesDir/crash_reports/crash_<guid>.json` → flushed to `app_errors` | On next launch |
| Logcat (local only) | `E/EzrahiLogger: <type> [<GUID>] <message>` | Immediately |

**What is in a record:** GUID, error type (CRASH / ROUTE_PARSER / NETWORK /
FIRESTORE_LISTENER / AUTH / LOCATION_SERVICE / UI_COMPOSE / CAUGHT), severity,
sanitized message + stack trace, timestamp, app version/code, device model,
OS version, user id (or `anon-<id>` when signed out), event id, screen,
session id, thread, fatal flag, foreground state, last 8 breadcrumbs.

**Reading errors:** Firebase Console → Firestore → `app_errors`
(reads are blocked from the app by security rules; query via console/gcloud).
Fatal records have `isFatal: true`.

**Guards (by design, see spec §5.5–§5.6):** PII sanitizer redacts phones,
emails, coordinates, API keys, tokens and URIs before anything leaves the
device; dedup (identical error ≤3 min), token bucket (≤5 writes/sec) and
hourly budget (≤20 records/hour/device) prevent floods — suppressed floods
are marked with `RATE_LIMITED` / `CIRCUIT_OPEN` records; records are
immutable and auto-deleted after 30 days (TTL).

## Project docs

All documentation lives under `docs/` (README excluded).

- `docs/roadmaps/` — migration roadmap + detailed development plan (Hebrew HTML)
- `docs/summaries/` — `refactor-fixes.md`: audit findings + progress tracker (current work series)
- `docs/planning/` — per-phase work plans (`todo.md`, `todo-1..7.md`)
- `docs/specs/` — analysis & specification documents for the fix series (`todo-fix-1..6.md`)
- `docs/DRAWER_MAP_TOUCH_CONFLICT.md` — resolved drawer/map gesture conflict

## Development sandbox (`.sandbox/`)

Any scripts, experiments, and throwaway code created during development or
testing (e.g. Firestore data scripts, parser playgrounds, build helpers) go
under `.sandbox/`. The directory itself is tracked (via `.sandbox/.gitkeep`)
so it always exists, but its **contents are git-ignored and never committed** —
if you need a scratch file, put it there; if it belongs in the app, it
belongs in the proper module under `app/src/main/java/`, not in the sandbox.

```sh
mkdir -p .sandbox   # create it when you need it; it is not tracked
```