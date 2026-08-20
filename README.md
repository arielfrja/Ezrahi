# Ezrahi

Field-activity coordination app for guides and participants: live location sharing on an offline-first map, role-based messaging, field reports, quick dial, and event management.

**Stack:** Kotlin, Jetpack Compose (Material 3), Hilt, Room (offline-first cache), Firestore (live sync), osmdroid (map), Firebase Auth, Foreground Service (background location).

## Build

```sh
sh gradlew :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

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