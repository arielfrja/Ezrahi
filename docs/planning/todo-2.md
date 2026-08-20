# Todo — Phase 2: Dependency Injection with Dagger Hilt

> Source: `../roadmaps/modernization-roadmap.md` "💉 Phase 2" (Days 8–11) + checklist items 2.1–2.3.
> Phase 1 is **complete** (`7ab4cf8`): FieldReport domain models + mappers + modern repo report methods.

## Objective
Replace manual `DataRepoFactory.getInstance()` calls with automated Hilt DI, and make the modern Compose screens (Phase 5 UI) fully injectable.

## Current State (verified)
- **Hilt is already scaffolded** (leftover from pre-restore modern work) — much of the roadmap checklist is DONE:
  - ✅ Task 2.1: `@HiltAndroidApp` on `EzrahiApp.kt` (also calls `CrashLogger.install`).
  - ✅ Task 2.2: manifest registers `android:name=".EzrahiApp"`.
  - ✅ Task 2.3: `di/FirebaseModule.kt` (Auth, Firestore, Storage) — exactly matches roadmap sample.
  - ✅ Bonus: `di/RepositoryModule.kt` (`@Binds` → `EzrahiRepository`), `di/DatabaseModule.kt` (Room `EzrahiDatabase` + `EzrahiDao`), `data/repository/EzrahiRepositoryImpl.kt` (`@Inject` + `@Singleton`), modern `MainActivity` (`@AndroidEntryPoint`), modern `MapViewModel` (`@HiltViewModel`), modern `service/LocationTrackingService` (`@AndroidEntryPoint` + `@Inject repository`).
  - ✅ Hilt deps + KSP plugin already in `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`; **build is green**.
- **What still uses `DataRepoFactory` (10 files, all legacy UI/data):** `StartupActivity`, `LoginViewModel`, `MainActivityViewModel`, `MainActivity`, `SignupActivityViewModel`, legacy `LocationTrackingService`, `SettingsViewModel`, legacy `MapViewModel`, `ActivityOverviewViewModel`, `DataRepoFactory` itself.
- **Naming collision / duplicates to resolve:**
  - Two `LocationTrackingService` classes: legacy `com.arielfaridja.ezrahi.LocationTrackingService` (plain `Service`, uses `DataRepoFactory`) AND modern `com.arielfaridja.ezrahi.service.LocationTrackingService` (`@AndroidEntryPoint`, `@Inject repository`). BOTH registered in manifest (`exported` differs).
  - Two `MainActivity` classes: legacy `UI.Main.MainActivity` (launcher, `AppCompatActivity`) AND modern `com.arielfaridja.ezrahi.MainActivity` (Compose, `ComponentActivity`, `exported=false`, "wired in Phase 5").
- Legacy ViewModels use plain constructors (`LoginViewModel()`, `SettingsViewModel(user, activity)`, `MainActivityViewModel()`) — some need args, so migrating them to `@HiltViewModel` requires a factory/assisted pattern.

---

## Task 2.1: Application class — DONE (verify only)
- [x] `EzrahiApp.kt` annotated `@HiltAndroidApp`, extends `android.app.Application`, calls `CrashLogger.install(this)`.

## Task 2.2: Register application in manifest — DONE (verify only)
- [x] `android:name=".EzrahiApp"` present on `<application>`.

## Task 2.3: Firebase Hilt module — DONE (verify only)
- [x] `di/FirebaseModule.kt` provides `FirebaseAuth`, `FirebaseFirestore`, `FirebaseStorage` as singletons.

## Task 2.4: Bind the legacy `IDataRepo`/`FirebaseDataRepo` via Hilt
- [x] Add `@Inject constructor(@ApplicationContext context: Context)` to `FirebaseDataRepo` (it currently takes `Context`; needs the qualifier). → **done**: `@Singleton class FirebaseDataRepo @Inject constructor(@ApplicationContext val context: Context)`.
- [x] Add `@Binds` in `di/RepositoryModule.kt` (or a new `di/DataRepoModule.kt`): `abstract fun bindDataRepo(impl: FirebaseDataRepo): IDataRepo`. → **done**.
- [x] Decide strategy for `DataRepoFactory` (see Constraints): either keep as a thin Hilt-backed bridge or delete after consumers migrate. → **done**: `DataRepoFactory` is now an `@EntryPoint` facade (`DataRepoEntryPoint`) pulling `IDataRepo` from the Hilt singleton graph; legacy `getInstance()` API preserved (no consumer changes needed).

## Task 2.5: Migrate legacy `DataRepoFactory` consumers toward injection
- [x] **Decision needed:** legacy UI (19 files) will be **replaced by Compose in Phase 5**. Migrating all 10 legacy consumers to `@HiltViewModel`/`@AndroidEntryPoint` now is throwaway work. Recommended: migrate only what Phase 5 reuses; defer the rest. → **Decision: defer**. `DataRepoFactory` now routes through Hilt, so legacy consumers get the same singleton without being rewritten.
- [ ] If migrated now: convert `LoginViewModel`, `MainActivityViewModel`, `SignupActivityViewModel`, `SettingsViewModel`, `MapViewModel`, `ActivityOverviewViewModel` to `@HiltViewModel` with `@Inject constructor` (note: `SettingsViewModel`/`ActivityOverviewViewModel` take runtime args → use `SavedStateHandle` or assisted injection). → **DEFERRED to Phase 5**.
- [ ] If migrated now: add `@AndroidEntryPoint` to `StartupActivity`, legacy `MainActivity`, `LoginActivity`, `SignupActivity`, legacy `LocationTrackingService`, fragments. → **DEFERRED to Phase 5**.

## Task 2.6: Resolve duplicate classes
- [ ] `LocationTrackingService`: unify on the modern `service/` version (has `@AndroidEntryPoint` + `@Inject`); remove/alias legacy one **only when legacy UI is gone (Phase 5)** — legacy MainActivity currently starts it. → **deferred to Phase 5**.
- [ ] `MainActivity`: keep legacy launcher until Phase 5; modern one is already `@AndroidEntryPoint` and stays unexported. → **deferred to Phase 5**.

## Constraints / Notes
- **Keep build green + app running.** Legacy UI is the live launcher and drives tests; deleting/replacing it is Phase 5 work. Phase 2 should mostly *enable* DI for the modern path and the data layer, not rewrite the legacy UI.
- `DataRepoFactory` is a manual singleton (`getInstance()`). If we Hilt-bind `FirebaseDataRepo`, we must ensure **only one instance** exists (Hilt singleton). Options:
  - (a) Make `DataRepoFactory` an `@EntryPoint` facade that pulls from the Hilt graph (minimal legacy churn), or
  - (b) leave `DataRepoFactory` as-is and only migrate consumers in Phase 5.
  - Recommend (a) for the data layer + defer ViewModel/Activity migration.
- `FirebaseDataRepo` builds a Room DB named `"database"` (legacy) while modern `DatabaseModule` uses `"ezrahi_local_db"` — two separate Room instances will coexist; unify in Phase 3.
- **Runtime smoke test required** for any DI change: DI graph errors surface at runtime (not compile time) — test app launch after each Hilt change.

## Definition of Done (Phase 2)
- [x] `FirebaseDataRepo` Hilt-injectable (`@Inject constructor(@ApplicationContext ...)`) and bound as `IDataRepo`.
- [x] Modern path fully injectable (already true: repo, ViewModel, service, activity).
- [x] `DataRepoFactory` either Hilt-backed or explicitly scheduled for removal (no dangling plan). → Hilt-backed `@EntryPoint` facade; removal scheduled with legacy UI (Phase 5).
- [x] `sh gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [x] App launches + user smoke-tests legacy flow (login → map → report → overview → settings) after the change. → **passed**.

## Next
- [ ] Create `todo-3.md` for Phase 3 (Offline-First Room data layer) — note Phase 3 items 3.1–3.4 are **already scaffolded** (Room entities, DAO, repo impl, bindings exist); verify + extend for reports/messages/events.
