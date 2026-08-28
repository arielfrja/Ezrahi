# ADR 0001 — Map Marker Interaction: SymbolLayer + Hit-Testing (not the Annotation Plugin)

- **Status:** Accepted
- **Date:** 2026-08-26
- **Deciders:** PO (Ariel Faridja) + Development Team
- **Scope:** All interactive point features on the MapLibre map (field reports, participants, future pins)
- **SDK:** `org.maplibre.gl:android-sdk:11.8.0` (`gradle/libs.versions.toml`)

---

## 1. Context

Ezrahi's map renders **live, high-churn point collections** — field reports and event
participants — that mutate continuously from Firestore snapshot flows:

- new reports appear mid-event (manager-created dynamic types, each with its own
  icon glyph + user-selected disc color, see `docs/specs/dynamic-report-types.md`);
- participants move with every GPS fix and change color with staleness state;
- markers must be **tappable** (tap a report → details dialog) without breaking the
  existing touch grammar (map tap = measure waypoint, map long-press = drop report).

MapLibre Native Android offers three ways to put interactive markers on a map:

| Option | Mechanism | Per-marker click listener? | State |
|---|---|---|---|
| A. **GeoJson source + `SymbolLayer`/`CircleLayer`** + click hit-testing | style layers | ❌ (via `queryRenderedFeatures` on the layer) | Current, recommended core-SDK path |
| B. **Annotation Plugin** (`org.maplibre.gl:android-plugin-annotation-v11`, `SymbolManager`/`CircleManager`) | manager-managed symbols | ✅ `SymbolManager.addSymbolClickListener` | Separate artifact, version-pinned per major SDK |
| C. **Legacy Google-Maps-style API** (`MapLibreMap.addMarker`, `Marker`) | built-in annotations | ✅ | **Deprecated since SDK 7.0.0** ("use MapLibre Annotation Plugin instead") |

The question: which option do we standardize on for *all* point features?

## 2. Decision

**Option A — GeoJson sources + style layers, with layer-scoped click hit-testing.**

Concretely:

- Every dynamic point collection is one `GeoJsonSource` + one or more style layers
  (`MapLayers.kt`): `ezrahi-reports-src/layer`, `ezrahi-participants-src/layer`,
  `ezrahi-measure-src/…`, `ezrahi-route-src/layer`.
- Per-feature variation (icon glyph, disc color, staleness tint, bearing rotation) is
  expressed as **GeoJSON feature properties** consumed by data-driven style expressions
  (e.g. `iconImage(Expression.get("icon"))`, `circleColor(Expression.get("color"))`).
- "Marker clicks" are implemented as a map click **hit-tested against one specific
  layer**: `map.projection.toScreenLocation(latLng)` →
  `map.queryRenderedFeatures(screenPoint, MapLibreConfig.REPORTS_LAYER)` → read the
  `reportId` feature property → resolve the report → show the details dialog
  (`MapScreen.kt`). Non-hits fall through to the next touch behavior (measure mode).
- Each report feature carries a `reportId` string property so the hit feature can be
  mapped back to the domain object (`FieldReport`) in one dictionary lookup.

## 3. Rationale

### 3.1 Live-fleet updates are O(1) for us with Option A, O(n) bookkeeping with Option B

Our collections change on every Firestore emission — often several times per second
during an active event (GPS cadence up to 1 Hz per participant).

- **Option A:** an update is **one call** — `source.setGeoJson(featureCollection)`.
  MapLibre's core performs the diffing/rendering natively. No client-side state to
  maintain; the source *is* the state.
- **Option B:** `SymbolManager` keeps a client-side list of `Symbol` objects. On every
  flow emission *we* would have to reconcile that list against the new data — matching
  symbols to reports by id, calling `symbol.iconImage = …`, `symbol.geometry = …`,
  `manager.update(symbol)`, `manager.delete(symbol)` for removals. The plugin performs
  **no reactive diffing for us**; we would be writing and maintaining a bespoke
  diff engine for zero user-visible benefit. (Internally the plugin just writes to a
  GeoJson source + layer anyway — it is a convenience wrapper, not a different
  rendering path.)

### 3.2 Data-driven styling matches our dynamic type system

The dynamic-report-types feature produces an **open-ended set of marker variants**
(any of 14 catalog glyphs × any user color × builtin/custom). Option A handles this
with one layer + a property: each feature carries `icon = "rtype_<typeId>"`, and
`updateReports()` stays 15 lines regardless of how many types exist.

Option B would push that variation into per-`Symbol` property assignments, multiplying
the reconciliation code from §3.1 and splitting "what the marker looks like" between
style expressions and imperative symbol mutations.

### 3.3 Touch-event composition stays under our control

The map already layers three touch behaviors: tap (measure waypoint / report details),
long-press (drop report), and camera gestures. `SymbolManager` installs its own layers
and consumes events through its listeners **before** `addOnMapClickListener` sees them,
which forces event-priority untangling between three systems. With Option A there is
exactly **one** click pipeline; hit-testing order is explicit, readable code
(`MapScreen.onMapClick`).

### 3.4 Dependency hygiene

- The plugin is a **separate artifact whose major version must match the SDK**
  (`-v11` for 11.8.0) and has historically lagged SDK releases — a supply-chain and
  upgrade-friction surface we don't need.
- Option C is deprecated (since 7.0.0) and imitates the Google Maps API; adopting a
  deprecated API in new code is disqualifying.

### 3.5 Performance headroom

`queryRenderedFeatures` on a single named layer is a cheap native query (it inspects
only what is rendered under one screen point of one layer). The GeoJson-source path is
the architecture MapLibre documents for large/dynamic datasets; `SymbolManager` is
optimized for hundreds of pins, not continuous multi-Hz fleet updates.

## 4. Alternatives considered

| Alternative | Why rejected |
|---|---|
| **Annotation Plugin** (B) | Per-symbol listeners require client-side symbol-list reconciliation for every Firestore emission (§3.1); splits data-driven styling (§3.2); extra version-pinned dependency (§3.4); competing touch pipeline (§3.3). Revisit **only** if we need draggable annotations or per-annotation text editing. |
| **Legacy `addMarker` API** (C) | Deprecated since 7.0.0; Google-Maps-imitation API; not acceptable in new code. |
| **Compose `MapView` annotation composables** | No official Compose first-party API for MapLibre annotations; wrapping the plugin in Compose adds a layer on top of an already-rejected option. |

## 5. Consequences

**Positive**

- One uniform pattern for every point collection on the map (reports, participants,
  measure points) — one code shape to learn, test, and extend.
- Marker "click" semantics are explicit, layer-scoped, and cannot be hijacked by
  third-party event ordering.
- Adding a new interactive collection = new source + layer + one hit-test branch.
- Style image registration (`ensureReportTypeIcons`, `rtype_<typeId>` names) keeps
  marker bitmaps decoupled from feature data; color/icon edits re-register in place.

**Negative / trade-offs accepted**

- "Marker click" is implemented by us (~15 lines per interactive layer) instead of a
  library callback. Mitigated by the `reportId`-property convention documented here.
- If a future feature needs **draggable** map annotations, Option B (or a native
  gesture handler on a dedicated layer) will have to be introduced for that feature
  alone; this ADR should be superseded, not silently violated.

## 6. Implementation pointers

| Concern | Where |
|---|---|
| Sources/layers + `ensureReportTypeIcons` (overwrite semantics for live edits) | `core/map-engine/.../MapLayers.kt` |
| Layer/source name constants | `core/map-engine/.../MapLibreConfig.kt` |
| Report-marker bitmaps (catalog glyph on user-color disc) | `app/.../ui/reports/ReportIconCatalog.kt` `renderMarkerBitmap()` |
| Click hit-testing + details dialog | `app/.../ui/map/MapScreen.kt` (`onMapClick`, `selectedReport`) |
| `reportId` feature property | `MapLayers.updateReports()` |

## 7. References

- MapLibre Android API — `queryRenderedFeatures` (current overloads, point + layerIds):
  https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.maps/-map-libre-map/query-rendered-features.html
- Deprecated `getAnnotation`/marker API notice ("As of 7.0.0, use MapLibre Annotation Plugin"):
  https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.maps/-map-libre-map/get-annotation.html
- Annotation Plugin (per-symbol listeners; version-pinned `-vX` artifacts):
  https://github.com/maplibre/maplibre-plugins-android
- MapLibre annotations design proposal (history/direction of the two APIs):
  https://github.com/maplibre/maplibre-native/blob/main/design-proposals/2023-06-17-android-annotations.md
- Related spec: `docs/specs/dynamic-report-types.md` (§6 icon catalog, §9 UI)
