# Todo — Current Phase

## Step 0: Full Legacy Restore (base for everything)
- [x] Switch base to `refactor/modernization-v2` (commit `8b0e586`; backup at `backup/modernization-v2`)
- [x] Restore all 59 legacy files from `master` (UI package, legacy data/entities, services, res)
- [x] Merge AndroidManifest: legacy launcher + both location services, keep `Theme.Ezrahi` + cleartext, drop Maps API key
- [x] Fix `ic_menu_white_24dp.xml` (use `@color/cardview_dark_background`)
- [x] Extend version catalog + KTS build with legacy deps; Room unified at 2.7.2 via KSP only
- [x] Relocate modern `ui` package → `com.arielfaridja.ezrahi.app.ui` (case-insensitive FS collision with legacy `UI`)
- [x] `sh gradlew assembleDebug` → **BUILD SUCCESSFUL** (APK: `app/build/outputs/apk/debug/app-debug.apk`)
- [x] Commit `b54adb5`

## Phase 0: Project Setup & Gradle Modernization
- [x] Version catalog (`gradle/libs.versions.toml`) present
- [x] Gradle KTS (`build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts`)
- [x] Build config clean: aapt2 override, compileSdk 34, Java 17, AGP 8.5.2, Kotlin 2.0.0
- [x] Build passes with legacy + modern code coexisting
- [x] **USER TEST GATE** — splash → login → OSM map opens; long-press report dialog works + marker appears (fixed snapshot listener ADDED/MODIFIED); activity overview opens; settings opens
- [x] Fix issues found during user test (crash NPE, Firestore rules PERMISSION_DENIED, report markers not appearing)
- [ ] Confirm remaining: my-location button, LocationTrackingService notification, signup flow

## Next
- [ ] After user test passes → create `todo-1.md` for Phase 1 (Domain Entities & Cleaning Technical Debt)