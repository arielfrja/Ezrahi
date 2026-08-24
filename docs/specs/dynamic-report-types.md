# Dynamic Report Types (Specification)

> **STATUS: 🔵 SPEC ONLY — no development yet.**
>
> Replaces the hardcoded `FieldReportType` enum with per-event, user-defined report types.
> Revives the intent of the abandoned legacy `CustomReportType.kt` (found untracked in the working tree),
> redesigned for the modern architecture (Compose, Hilt, repository pattern, hardened Firestore rules).

## 1. Problem

Report categories are a closed Kotlin enum (`domain/model/FieldModels.kt:111`):

```kotlin
enum class FieldReportType(val value: Int) {
    GENERAL(0), MEDICAL(1), UNKNOWN(-1);
}
```

Consequences:

- Users can only file **Medical** or **General** reports (`MapScreen.kt:379` iterates `FieldReportType.entries`).
- Icons are hardcoded per enum value (`MapScreen.kt:50` `reportTypeToIcon`).
- Management statistics only know these two buckets (`EventManagementScreen.kt:473-474`).
- Event managers cannot define event-specific categories ("Fallen tree", "Water station", "Road closure", ...).

## 2. Requirements (from PO)

1. Report types must be **dynamic data, not code** — defined per event.
2. **Event managers** create / edit / delete types for their event; **participants** can use them.
3. Every type has a **name** and an **icon** chosen from a bundled icon catalog.
4. Existing reports and the legacy int encoding must keep working (**backward compatible**).
5. Deleting a type must not orphan existing reports.

## 3. Scope

| Area | Change |
|---|---|
| Domain model | new `ReportTypeDefinition` model; `FieldReport` gains `typeId` |
| Repository | CRUD + realtime flow for type definitions |
| Firestore | new subcollection + security-rules update (+ rule unit tests) |
| Map UI | Add-Report dialog reads live type list; markers resolve icons dynamically |
| Management UI | Reports tab: type editor (manager-only); counters per type |
| Mapper / back-compat | dual write/read of legacy `Type` int and new `TypeId` string |

**Out of scope:** remote/custom image uploads for icons (bundled catalog only); cross-event shared type libraries; re-coloring of existing map markers.

## 4. Firestore data model

Subcollection: **`events/{eventId}/report_types`** (matches the hardened rules pattern of `events/{eventId}/routes`).

| Field | Type | Notes |
|---|---|---|
| `name` | string | 1–32 chars, unique per event (case-insensitive, enforced client-side) |
| `iconKey` | string | key into the bundled icon catalog (§6), e.g. `"medical"`, `"tree"`, `"water"` |
| `builtin` | bool | `true` for the seeded GENERAL/MEDICAL docs — builtin docs cannot be deleted or renamed |
| `createdAt` | timestamp | server timestamp |
| `createdBy` | string | user id |

Document ID = auto-id; also used as the report's `TypeId`.

Seeding: on **event creation**, the manager client writes `GENERAL` (`iconKey: "general"`) and `MEDICAL` (`iconKey: "medical"`) with `builtin = true`.

## 5. Report ↔ type linkage & back-compat

`FieldReportMapper.toWriteMap` writes **both** fields:

- `"TypeId" to report.typeId` — document id of the type definition (new reports always set it).
- `"Type" to <int>` — legacy enum value kept for old clients/rules queries:
  - `0` if the linked definition is the builtin GENERAL doc, `1` for MEDICAL,
  - `-1` (UNKNOWN) for any custom type.

Read logic (`fromSnapshot`):

1. If `TypeId` present → resolve against the event's live type list; unknown/deleted ids render via §5.1.
2. Else → fall back to legacy `FieldReportType.getByValue(int)` exactly as today, exposed as an implicit builtin type.

### 5.1 Deleted-type policy

Deleting a non-builtin type that still has reports:

- Reports are **never modified or deleted**.
- A deleted definition leaves a **tombstone**: the UI renders the report under a generic label ("Custom") with a neutral icon, and shows the stored type name in the report detail text where available.
- Manager deletion confirm-dialog states the number of affected reports (client-side count).

## 6. Icon catalog

Icons are **bundled drawables addressed by stable string keys** — no resource IDs over the wire, no remote images.

```kotlin
object ReportIconCatalog {
    val icons: Map<String, Int>  // key -> R.drawable.*
    // seeded: general, medical
    // candidates: tree, water, road, fire, animal, hazard, info, meeting ...
}
```

Rules:

- Adding a catalog entry is a code change (small, reviewable); `iconKey` values already stored remain valid.
- Unknown `iconKey` (e.g. removed from catalog in a future release) falls back to the general icon.
- Marker rendering resolves `typeId -> definition.iconKey -> drawable` through one helper used by both Map and Management screens.

## 7. Domain & repository API

`domain/model/FieldModels.kt`:

```kotlin
data class ReportTypeDefinition(
    val id: String = "",
    val name: String = "",
    val iconKey: String = "general",
    val builtin: Boolean = false,
)
```

- `FieldReport` gains `val typeId: String? = null` (null ⇔ legacy report).
- `FieldReportType` enum stays **temporarily** for legacy decoding; new code must not branch on it except in the mapper fallback.

`domain/repository/EzrahiRepository.kt` additions:

```kotlin
fun getReportTypes(eventId: String): Flow<List<ReportTypeDefinition>>
suspend fun addReportType(eventId: String, name: String, iconKey: String): Result<String>
suspend fun updateReportType(eventId: String, typeId: String, name: String, iconKey: String): Result<Unit>
suspend fun deleteReportType(eventId: String, typeId: String): Result<Unit>   // builtin -> failure
```

Implementation notes:

- Follows existing listener discipline: `callbackFlow` + `awaitClose`, errors routed through `logListenerError("events/$eventId/report_types", ...)`.
- Uniqueness of names checked client-side against the live list before write (Firestore has no case-insensitive unique constraint).

## 8. Security rules

Extend Firestore rules (same shape as routes):

- **read:** any authenticated participant of the event.
- **create/update/delete:** manager only (`request.auth.uid` has manager participant doc).
- Builtin docs additionally immutable + undeletable (checked in rules via `resource.data.builtin == true`).
- Payload validation: `name` string 1–32 chars, `iconKey` ∈ catalog allow-list duplicated in rules (or regex `[a-z_]{2,24}`).

Add rule unit tests alongside the existing suite (currently 8/8 passing).

## 9. UI

### 9.1 Add-Report dialog (MapScreen)

- Type picker becomes a lazy list/grid fed by `getReportTypes(eventId)` instead of `FieldReportType.entries`.
- Selected icon preview next to the title field.
- Submitting passes `typeId`; mapper derives the legacy int (§5).

### 9.2 Management screen — Reports tab

- Managers see a "Manage types" entry point → bottom sheet listing definitions with edit/remove actions (remove hidden/disabled for builtin).
- Counters row generalized: count per definition id, ordered by frequency; deleted types collapse into "Other".
- Color coding currently tied to MEDICAL switches to a per-definition accent derived from the icon catalog entry.

## 10. Testing

| Layer | Test |
|---|---|
| Mapper | legacy doc (int only) → implicit builtin; new doc → resolves by `TypeId`; unknown id → tombstone rendering |
| Repository | fake Firestore: seeding, CRUD, listener error path via `logListenerError` |
| Rules | manager can write; participant read-only; builtin immutable/undeletable; payload validation rejects bad `name`/`iconKey` |
| UI (Compose) | dialog lists live types incl. late-added one; management sheet hides destructive actions for builtin |

## 11. Rollout / migration

1. Ship app version writing dual fields (`TypeId` + legacy `Type`) — old clients unaffected (they ignore `TypeId`).
2. Deploy rules before enabling the editor UI (feature-flagged behind a boolean in Settings until rules are live).
3. No batch migration required: legacy reports decode via fallback indefinitely; optional future cleanup may backfill `TypeId` for reports whose int maps to a builtin doc.

## Appendix A — Relation to abandoned legacy work

The untracked `entities/CustomReportType.kt` + `dialog_add_report_type.xml` expressed the same idea against the deleted legacy stack (XML views, `iconResId: Int`, mutable `var`s, global collection). Nothing is reusable verbatim:

| Legacy approach | This spec |
|---|---|
| Global `ReportTypes`-style storage implied | per-event subcollection, rules-scoped |
| Android `iconResId` int persisted | stable string `iconKey` into bundled catalog |
| XML `Spinner` dialog | Compose bottom sheet / selector |
| Mutable data class | immutable model, repository-managed lifecycle |
