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
6. Builtin types (GENERAL/MEDICAL) can be **overridden per event** by the manager (name, icon) but can **never be deleted** (per PO).

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
| `builtin` | bool | `true` for the seeded GENERAL/MEDICAL docs — builtin docs can be **renamed/re-iconed by the manager** but can **never be deleted** |
| `createdAt` | timestamp | server timestamp |
| `createdBy` | string | user id |

Document ID = auto-id; also used as the report's `TypeId`.

Seeding: on **event creation**, the manager client writes `GENERAL` (`iconKey: "general"`) and `MEDICAL` (`iconKey: "medical"`) with `builtin = true`.

**Pre-existing events (created before this feature):**
- **Read path:** if `getReportTypes()` returns an empty list, the UI falls back to the two implicit builtins (GENERAL green / MEDICAL red from the catalog) so the Add-Report dialog always has options.
- **Write path:** before a manager's **first write** to an empty `report_types` subcollection (type editor opened or custom type added), the client seeds GENERAL/MEDICAL (`builtin = true`) idempotently, then applies the change.
- No batch migration; legacy reports keep decoding via the int fallback regardless.

## 5. Report ↔ type linkage & back-compat

`FieldReportMapper.toWriteMap` writes **both** fields:

- `"TypeId" to report.typeId` — document id of the type definition (new reports always set it).
- `"Type" to <int>` — legacy enum value kept for old clients/rules queries:
  - `0` if the linked definition is the builtin GENERAL doc, `1` for MEDICAL,
  - `-1` (UNKNOWN) for any custom type.

Read logic (`fromSnapshot`):

1. If `TypeId` present → resolve against the event's live type list; unknown/deleted ids render via §5.1.
2. Else → fall back to legacy `FieldReportType.getByValue(int)` exactly as today, exposed as an implicit builtin type.

### 5.1 Deletion resolution policy

Deleting a **non-builtin** type opens a **resolution dialog** asking what to do with its existing reports (per PO):

| Option | Effect |
|---|---|
| **1. Remove them all** | All reports of this type are permanently deleted |
| **2. Convert to General** | Reports are re-linked to the builtin GENERAL doc (`TypeId` rewritten; legacy `Type` set to `0`) |
| **3. Convert to another type…** | A combobox — **enabled only while this option is selected** — lists the event's remaining types; reports are re-linked to the chosen one |

Additional mechanics:

- A **"Remember my choice"** checkbox persists the selected action **per event** (in the event settings document). When set, subsequent non-builtin deletions apply the stored action automatically without prompting.
- The stored preference can be **changed or cleared at any time from the Event Overview screen** (§9.3).
- Option 1 performs batch deletes through the repository (chunked writes); it requires manager delete-permission on event reports — verify/extend existing rules (§8).
- **Defensive fallback unchanged:** any report whose `TypeId` no longer resolves (race condition, partial application) renders via the tombstone path — generic "Custom" label with a neutral icon; reports are never silently dropped.

## 6. Icon & color catalog

The catalog is a **deliberately small, curated set (~15 icons)** — a compact picker is better UX than scrolling hundreds of glyphs (per PO). Icons are **bundled vector assets addressed by stable string keys** — no resource IDs over the wire, no remote images. Each entry also carries a **default accent color** used by map markers, management counters and UI accents.

| # | key | Source | Glyph | Default accent |
|---|---|---|---|---|
| 1 | `general` | Material Symbols | `info` | green `#2E7D32` |
| 2 | `medical` | Font Awesome Free | `star-of-david` | red `#C62828` |
| 3 | `hazard` | Material Symbols | `warning` | amber `#F9A825` |
| 4 | `fire` | Material Symbols | `local_fire_department` | deep orange `#E64A19` |
| 5 | `water` | Material Symbols | `water_drop` | blue `#1565C0` |
| 6 | `tree` | Material Symbols | `forest` | dark green `#33691E` |
| 7 | `road` | Material Symbols | `road` | brown `#795548` |
| 8 | `trail` | Material Symbols | `hiking` | teal `#00796B` |
| 9 | `food` | Material Symbols | `restaurant` | orange `#EF6C00` |
| 10 | `meeting` | Material Symbols | `groups` | indigo `#3949AB` |
| 11 | `vehicle` | Material Symbols | `directions_car` | blue-grey `#455A64` |
| 12 | `weather` | Material Symbols | `cloud` | sky blue `#0288D1` |
| 13 | `lost_found` | Material Symbols | `person_search` | magenta `#AD1457` |
| 14 | `checkpoint` | Material Symbols | `flag` | violet `#5E35B1` |

The catalog ships with these **14 entries**; additions or removals are deliberate code releases (see rules below).

Style rule: all Material Symbols glyphs are pinned at export time to **FILL@1 · wght@400 · opsz@24 · ROUNDED** (filled + rounded look, per PO), converted once into bundled `ImageVector`s — consistent across picker, markers and counters. Accent colors are catalog defaults chosen for at-a-glance distinction; the PO may retune any hex without data migration.

```kotlin
object ReportIconCatalog {
    data class Entry(val drawableRes: Int, val colorHex: String)
    val entries: Map<String, Entry>  // key -> (drawable, accent color), per table above
}
```

Rules:

- **Icons come from established open-source libraries** (per PO) — no hand-drawn assets, no full icon-set dependency (keeps the APK lean):
  - **Material Symbols** (Apache 2.0) for the 13 standard glyphs in the table above.
  - **Font Awesome Free `star-of-david`** (solid, 6.7.x) for the medical icon — Material Symbols does *not* include a Star of David.
- **Medical icon (per PO):** the FA `star-of-david` glyph rendered **red** — *not* the red cross (matches Israeli convention / Magen David Adom and avoids Red Cross emblem restrictions). The SVG is converted once into a bundled `ImageVector`/vector drawable at build time; no network fetching.
- **License note:** Font Awesome Free icons are **CC BY 4.0** — attribution must be added to the app's third-party licenses/about screen ("Icons by Font Awesome — CC BY 4.0"). Material Symbols require no attribution beyond the existing Apache notice.
- **Colors live in the catalog, not per-document**: every type sharing an `iconKey` shares its accent color (v1 keeps it simple; a per-type `colorHex` override can be added later without any data migration).
- Adding or removing a catalog entry is a deliberate code change (small, reviewable); `iconKey` values already stored remain valid.
- Unknown `iconKey` (e.g., removed from catalog in a future release) falls back to the general entry (green).
- Marker rendering resolves `typeId -> definition.iconKey -> (drawable, color)` through one helper used by both Map and Management screens.

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

// resolution = RemoveReports | ConvertToGeneral | ConvertTo(typeId)  (§5.1)
suspend fun deleteReportType(eventId: String, typeId: String, resolution: DeletionResolution?): Result<Unit>

// remembered deletion preference (null = "ask every time")
fun getDeletionPreference(eventId: String): Flow<DeletionResolution?>
suspend fun setDeletionPreference(eventId: String, resolution: DeletionResolution?): Result<Unit>
```

Implementation notes:

- Follows existing listener discipline: `callbackFlow` + `awaitClose`, errors routed through `logListenerError("events/$eventId/report_types", ...)`.
- Uniqueness of names checked client-side against the live list before write (Firestore has no case-insensitive unique constraint).

## 8. Security rules

Extend Firestore rules (same shape as routes):

- **read:** any authenticated participant of the event.
- **create/update/delete:** manager only (`request.auth.uid` has manager participant doc).
- Builtin docs are **undeletable** but their display properties may be **overridden** by the manager (rename, change `iconKey`); rules enforce: `resource.data.builtin == true` → delete denied, update allowed only if `builtin` flag itself remains `true` and payload validation passes.
- Payload validation: `name` string 1–32 chars, `iconKey` ∈ catalog allow-list duplicated in rules (or regex `[a-z_]{2,24}`).
- **Report deletion (§5.1 option 1):** confirm the existing rules allow **manager-only delete** on event reports; extend if the current hardened rules only cover create/update.

Add rule unit tests alongside the existing suite (currently 8/8 passing).

## 9. UI

### 9.1 Add-Report dialog (MapScreen)

- Type picker becomes a lazy list/grid fed by `getReportTypes(eventId)` instead of `FieldReportType.entries`.
- Selected icon preview next to the title field.
- Submitting passes `typeId`; mapper derives the legacy int (§5).

### 9.2 Management screen — Reports tab

- Managers see a "Manage types" entry point → bottom sheet listing definitions with edit/remove actions (edit enabled for all; remove hidden/disabled for builtin).
- **Remove action** opens the deletion resolution dialog (§5.1): three options (remove all / convert to General / convert to chosen type via a combobox enabled only for that option) + "Remember my choice" checkbox, and shows the number of affected reports (client-side count).
- Counters row generalized: count per definition id, ordered by frequency; deleted types collapse into "Other".
- Color coding currently tied to MEDICAL switches to the catalog accent color of each definition (§6: GENERAL green, MEDICAL red, etc.).

### 9.3 Event Overview screen — deletion preference

- Shows the event's current remembered deletion action (§5.1) with controls to **change or clear it** ("Ask every time" = cleared).
- Visible to managers only; changes write straight to the event settings document.

## 10. Testing

| Layer | Test |
|---|---|
| Mapper | legacy doc (int only) → implicit builtin; new doc → resolves by `TypeId`; unknown id → tombstone rendering |
| Repository | fake Firestore: seeding, CRUD, listener error path via `logListenerError`; deletion resolution (remove-all chunked deletes / convert re-links `TypeId` + legacy int); remembered-preference read/write/clear |
| Rules | manager can write; participant read-only; builtin undeletable but rename/re-icon update allowed (`builtin` flag itself immutable); payload validation rejects bad `name`/`iconKey`; manager-only report delete |
| UI (Compose) | dialog lists live types incl. late-added one; deletion dialog: combobox enabled only for option 3, remember-checkbox persists, overview screen can change/clear preference; management sheet hides destructive actions for builtin; unknown `iconKey` renders general icon + green accent |

## 11. Rollout / migration

1. Ship app version writing dual fields (`TypeId` + legacy `Type`) — old clients unaffected (they ignore `TypeId`).
2. Deploy rules before enabling the editor UI (feature-flagged behind a boolean in Settings until rules are live).
3. No batch migration required: legacy reports decode via fallback indefinitely; pre-existing events fall back to implicit builtins and are lazily seeded on the manager's first write (§4); optional future cleanup may backfill `TypeId` for reports whose int maps to a builtin doc.

## Appendix A — Relation to abandoned legacy work

The untracked `entities/CustomReportType.kt` + `dialog_add_report_type.xml` expressed the same idea against the deleted legacy stack (XML views, `iconResId: Int`, mutable `var`s, global collection). Nothing is reusable verbatim:

| Legacy approach | This spec |
|---|---|
| Global `ReportTypes`-style storage implied | per-event subcollection, rules-scoped |
| Android `iconResId` int persisted | stable string `iconKey` into bundled catalog |
| XML `Spinner` dialog | Compose bottom sheet / selector |
| Mutable data class | immutable model, repository-managed lifecycle |
