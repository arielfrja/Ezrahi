# Event Map Layers — Hybrid Strategy (Specification v1)

> **STATUS: 🟡 PARTIAL IMPLEMENTATION APPROVED.**
>
> - **A1 (OpenFreeMap Liberty basemap) — approved by PO for immediate
>   implementation** as the first incremental step (§5.0). One-line change.
> - A2 (offline regions), A3 (event trails), §9 logging refactor — 🔵 spec only.
>
> Strategy chosen after deep-dive research into how the Israel Hiking Map (IHM)
> builds its vector map (`IsraelHikingMap/VectorMap`), and a build-vs-buy analysis.
> **Direction:** use off-the-shelf infrastructure now (OpenFreeMap basemap +
> MapLibre `OfflineManager` regions + Overpass-derived event trails synced via
> Firestore), keep the full custom tile-pipeline plan documented for later.

---

## 1. Background — research findings that shaped this spec

### 1.1 How IHM actually does it (origin analysis)

The IHM vector map is **not** a third-party product; it is:

| IHM source | What it is | Origin |
|---|---|---|
| `IHM` vector tiles (z0–14) | OpenMapTiles v3.x schema MVT tiles, self-hosted at `israelhiking.osm.org.il/vector/data/IHM` | OSM data (Geofabrik `israel-and-palestine` extract) processed through the OpenMapTiles schema + custom layers (`trail`, `barrier`, `ihm_lines`, `ihm_boundary`, `minefield`, `itm_grid`) derived from OSM tags (`network=rwn/lwn`, `osmc_symbol`, `way_colour`, `barrier=*`, admin boundaries). Generation pipeline is private (Mapeak infra). |
| `Contour` vector tiles | DEM-derived contour lines | Elevation model ("courtesy of Israel Hiking Map") |
| `TerrainRGB` | Terrain raster-dem z7–12 | Elevation model |
| Global styles | `global_topo.json` | JAXA AW3D30 terrarium DEM z0–11 |
| `Ortho.json` | Aerial raster z7–16 | Survey of Israel open-data orthophoto (`data-mapiil.opendata.arcgis.com`) |

**Key insight:** every visual concept we like (colored marked trails from route
relations, gate/cliff icons, ITM grid) is *derived from public OSM data and open
specs* — nothing requires IHM's code or servers, and nothing about it should be
copied. The same concepts can be re-implemented from first principles at any
scale we choose.

### 1.2 Why hybrid (PO decision)

Full country-wide custom pipelines are high-effort (CI, extractors, offline tile
serving). Ezrahi's actual product need is **event-scoped**: participants operate
in a known area for a limited time. Off-the-shelf components cover ~90% of value
at ~10% of effort:

- Basemap → **OpenFreeMap** (free, no API key, OpenMapTiles-schema planet tiles)
- Offline → **MapLibre Android built-in `OfflineManager`** region downloads
- Marked trails → **one Overpass query per event**, stored on the event document,
  rendered through the existing GeoJSON layer pattern

The custom pipeline remains documented (§10) with explicit activation triggers.

---

## 2. Requirements (from PO)

1. Real basemap replacing the MapLibre demotiles placeholder (`ONLINE_STYLE_URI`
   currently points at `https://demotiles.maplibre.org/style.json`).
2. The map must work well **offline** in the field.
3. Colored marked hiking/bike trails as an overlay — the IHM idea, implemented by
   us, scoped per event rather than whole-country.
4. Trail data must be **shared through the existing Firebase sync** so every
   participant receives it automatically (no per-device fetching).
5. No copying of IHM assets/code/styles — concepts only.
6. All failures logged via `ExceptionLogger` (global exception-logging rule).

## 3. Decisions log

| # | Decision | Choice | Rationale |
|---|---|---|---|
| D1 | Basemap provider | OpenFreeMap (Liberty style) | Free, keyless, fair-use production OK, OMT schema, standard style JSON |
| D2 | Offline mechanism | MapLibre `OfflineManager` regions | Built-in; replaces custom MBTiles serving entirely |
| D3 | Trails scope | Per-event bbox, fetched once at event save | Kilobytes vs gigabytes; matches event lifecycle |
| D4 | Trails sync | Firestore subcollection on the event doc | Auto-propagation to participants via existing listeners |
| D5 | Trail geometry storage | Flattened lon/lat double lists | Firestore forbids nested arrays |
| D6 | Rendering | Segment-based LineStrings (no relation ordering) | Visually identical for lines; massively simpler |
| D7 | Routing / contours / hillshade / PMTiles-web | Deferred | See §10 |

## 4. Architecture overview

```
Event creation/edit (manager device)
        │
        ├─► [A3] Overpass API ──► TrailFeature list ──► events/{eventId}/trails/*
        │                                              (Firestore, one doc per relation)
        └─► [A2] bbox + zoom range ──► OfflineManager.createOfflineRegion()
                                               │
Participant devices                            ▼
        │                               MapLibre offline database
        ├─ listeners: events/*, trails/* ──► Room/local cache (offline-ready)
        │
        └─ MapScreen ──► MapLibreView(styleUri = OpenFreeMap Liberty)
                              ├─ basemap layers (from style JSON)
                              └─ ensureBaseLayers(): participants, reports,
                                 routes, measure, TRAILS casing+line
```

---

## 5. Component A1 — Online basemap

### 5.0 Implementation status & PO decision (first increment)

**Approved for immediate implementation (PO, current session).**

PO asked for "just the regular OSM map" as the simplest first step. Decision
matrix evaluated:

| Option | Look | Production OK? | Verdict |
|---|---|---|---|
| Literal OSM raster tiles (`tile.openstreetmap.org`) | classic osm.org look | ⚠️ No — OSMF tile policy discourages app usage; blocking risk | rejected |
| **OpenFreeMap Liberty style (vector)** | clean modern street map, same OSM data | ✅ free, keyless, app-appropriate | **chosen** |

Single point of change — everything funnels through one constant:

- `core/map-engine/src/main/java/com/arielfaridja/ezrahi/core/mapengine/MapLibreConfig.kt`
  (`ONLINE_STYLE_URI`, currently demotiles placeholder)
- Consumed automatically by `OfflineTileManager.resolveStyleUri()` fallback and
  `app/.../ui/map/MapScreen.kt` (`styleUri = resolveStyleUri(context)`) — no
  other call sites exist.

> **⚠️ Expectation setting — Liberty does NOT render marked trails.**
> Liberty draws generic footpaths/tracks only. Colored marked routes (Israel
> Trail stripes, regional purple, etc.) do not exist in its tile schema — that is
> precisely why IHM built a custom `trail` layer and why this spec defines
> component A3 (event trails overlay). Liberty = base map; colored trails arrive
> with A3.

Explicitly out of this increment: trails overlay (A3), offline regions (A2),
style-failure logging refactor (§9).

### 5.1 Change

`core/map-engine/.../MapLibreConfig.kt`:

```kotlin
const val ONLINE_STYLE_URI = "https://tiles.openfreemap.org/styles/liberty"
```

No other code change required — `MapLibreView` already loads arbitrary style URIs.

### 5.2 Requirements

- Style must load before `ensureBaseLayers(...)` runs (existing flow keeps this guarantee).
- Attribution: OpenFreeMap's style JSON ships OSM attribution; verify it renders
  in-app (MapLibre attribution widget). Add "© OpenStreetMap contributors" credit
  to the app's About screen if not surfaced.
- Fallback behavior unchanged: if style fetch fails, current failure path applies
  but must now be **logged** (see §9 — today it is Logcat-only, violating the
  exception-logging rule).

### 5.3 Acceptance (first increment)

- [x] `ONLINE_STYLE_URI` points at OpenFreeMap Liberty; demotiles no longer used.
      *(implemented; `:core:map-engine:compileDebugKotlin` BUILD SUCCESSFUL)*
- [ ] Map screen shows real Israel-region detail (roads, water, place names).
- [ ] Existing overlays unaffected: participants, reports, route, measure layers
      still render above the basemap (`ensureBaseLayers` runs post-style-load).
- [ ] Airplane mode after prior load still renders cached tiles.
- [ ] Attribution visible.

---

## 6. Component A2 — Offline regions (MapLibre OfflineManager)

### 6.1 Region definition

| Parameter | Value | Notes |
|---|---|---|
| Geometry | Event location bbox + padding | padding ≈ 5 km or configurable per event |
| Zoom range | `8..15` | z8 regional context → z15 trail/path detail; z16+ excluded for size |
| Tile budget | raise `OfflineManager.setOfflineMapboxTileCountLimit(…)` to ≥ 30,000 | default 6,000 too low; verify actual count for typical bboxes during implementation |
| Storage | MapLibre internal offline database | no user-visible file management in v1 |

### 6.2 New component

`app/src/main/java/com/arielfaridja/ezrahi/app/map/OfflineRegionManager.kt`
(location may live in `core/map-engine` if UI-free logic dominates):

```kotlin
class OfflineRegionManager @Inject constructor(
    @ApplicationContext context: Context,
    private val logger: ExceptionLogger
) {
    fun downloadRegion(event: FieldEvent, onProgress: (Float) -> Unit, onDone: (Result<Unit>) -> Unit)
    fun listRegions(): List<OfflineRegion>
    fun deleteRegion(regionId: String)
}
```

Implementation notes:

- Use `org.maplibre.android.offline.OfflineManager` /
  `createOfflineRegion(...)` — **verify exact signature against the pinned SDK
  (11.8.x)** during implementation; the API was deprecated/reshaped across
  Mapbox→MapLibre versions and must match what the project compiles against.
- Metadata payload: serialize `{eventId, name, createdAt}` as JSON bytes so
  regions can be listed/deleted per event.
- Progress callbacks must hop to main thread before touching UI state.
- Every failure path → `ExceptionLogger.log(e, ErrorType.MAP_OFFLINE_REGION, eventId, screen = "map")`.

### 6.3 UI

- Event management screen: "Download offline map" action + progress bar.
- Settings screen: "Offline maps" section listing downloaded regions with size
  estimate and delete button.

### 6.4 Acceptance

- [ ] Download completes for a real event location; progress shown.
- [ ] Airplane mode: pan/zoom inside region renders fully (basemap + trails +
      participants from local cache).
- [ ] Outside region: graceful degradation (blank basemap, overlays still fine).
- [ ] Delete frees space; re-download works.

---

## 7. Component A3 — Event trails

### 7.1 Data acquisition (manager device, at event save)

**Provider:** Overpass API. Primary endpoint `https://overpass-api.de/api/interpreter`,
fallback `https://overpass.kumi.systems/api/interpreter`. Timeout 25 s.

**Query template** (`{{bbox}}` = `s,w,n,e` of padded event bbox):

```
[out:json][timeout:25];
relation["route"~"^(hiking|bicycle|mtb|foot)$"]({{bbox}});
out geom;
```

`out geom` returns each relation with member ways carrying inline geometry —
single request, no client-side assembly.

**Client-side reduction:** flatten each relation's member way geometries into
segment LineStrings carrying identical properties (D6 — no path ordering needed
for line rendering).

### 7.2 Color derivation (our own logic, from the public tag spec)

Per relation, resolve display color:

1. If `colour` tag present and parses as hex (`#rrggbb`) → use it.
2. Else if `osmc_symbol` present → foreground part before the first `:` mapped
   through a named-color table (`black red green yellow blue magenta brown grey/
   gray white orange purple violet pink` → our palette hexes).
3. Else fall back per network class:
   `iwn/nwn → #E65100`, `rwn → #6A1B9A`, `lwn → #2E7D32`, other/unparsed → `#455A64`.
4. Unparseable input never crashes — always lands on fallback (unit-tested).

### 7.3 Domain model

```kotlin
// core/model — FieldModels.kt
data class EventTrail(
    val id: String = "",          // OSM relation id, also Firestore doc id
    val eventId: String = "",
    val name: String = "",
    val ref: String = "",
    val network: String = "",     // iwn | nwn | rwn | lwn | ""
    val colorHex: String = "#455A64",
    val points: List<Double> = emptyList(), // flattened lon,lat pairs across segments;
                                            // segment boundaries marked by NaN? NO — see 7.4
)
```

### 7.4 Multi-segment encoding (resolved design)

One flattened array cannot represent disjoint segments without a separator.
**Decision:** store segments as parallel fields:

```kotlin
data class EventTrail(
    …,
    val segmentStartIndices: List<Int>, // index into `points` where each LineString starts
    val points: List<Double>            // lon,lat pairs, concatenated
)
```

Mapper rebuilds `List<LineString>` from `(points, segmentStartIndices)`.
Firestore-safe (flat arrays only), compact (no per-point objects).

### 7.5 Caps and guardrails

| Guardrail | Limit | Behavior when exceeded |
|---|---|---|
| Relations per event | 200 | keep priority iwn/nwn > rwn > lwn, then alphabetical; log warning |
| Points per trail doc | ~8,000 pairs (~64 doubles-list entries ≈ well under 1 MiB doc limit) | simplify/drop longest-tail segments; log warning |
| Total trails payload per event | soft target ≤ 2 MiB | tighten bbox padding instead of dropping data |
| Overpass failure | — | event saves successfully without trails; retry action offered in UI; error logged |

### 7.6 Repository & sync

- New methods on `EzrahiRepository`:
  - `suspend fun refreshTrails(eventId: String): Result<Int>` (manager path:
    Overpass fetch + replace subcollection atomically-enough: write new docs,
    delete stale ids)
  - `fun getTrailUpdates(eventId: String): Flow<List<EventTrail>>` (snapshot
    listener mirroring `getRoutes`)
- Participants receive trails automatically through the listener; data persists
  in the existing local-cache path → **trails work offline** once synced.
- Firestore path: `events/{eventId}/trails/{relationId}`.

### 7.7 Rendering (`MapLayers.kt` pattern)

New constants in `MapLibreConfig.kt`:

```kotlin
const val TRAILS_SRC = "ezrahi-trails-src"
const val TRAILS_CASING_LAYER = "ezrahi-trails-casing-layer"
const val TRAILS_LAYER = "ezrahi-trails-layer"
```

In `ensureBaseLayers(...)`:

- `GeoJsonSource(TRAILS_SRC)` (empty FeatureCollection initially)
- `LineLayer(TRAILS_CASING_LAYER)`: color `#FFFFFF`, width trailWidth+2,
  opacity 0.9
- `LineLayer(TRAILS_LAYER)`: `lineColor(get("color"))`, width interpolated by
  zoom (≈2 px @ z8 → 4 px @ z15), cap/join ROUND
- Both inserted **below `PARTICIPANTS_LAYER`** (same insertion anchor used by
  ROUTE/MEASURE layers), preserving operator-symbol priority.

New update function:

```kotlin
fun updateTrails(style: Style, trails: List<EventTrail>)
```

— maps each trail to N LineString features sharing `{color}`, sets GeoJsonSource.

`MapViewModel.loadEvent(...)` gains a `repository.getTrailUpdates(eventId)`
collector feeding `updateTrails`.

### 7.8 Acceptance

- [ ] Creating/updating an event near a known marked route (e.g., a section of
      the Israel Trail) populates `events/{id}/trails` with correct colors.
- [ ] Participant device sees trails without any action.
- [ ] Colors match the relations' `colour`/`osmc_symbol` values (spot-checked).
- [ ] Airplane mode: trails still render (local cache).
- [ ] Event with zero nearby relations: empty state, no errors.

---

## 8. Firestore data model & rules impact

Collection: **`events/{eventId}/trails/{relationId}`**

| Field | Type | Notes |
|---|---|---|
| `id` | string | OSM relation id |
| `eventId` | string | denormalized parent id |
| `name` / `ref` | string | may be empty |
| `network` | string | `iwn`/`nwn`/`rwn`/`lwn`/"" |
| `colorHex` | string | resolved per §7.2 |
| `points` | number[] | flat lon,lat pairs |
| `segmentStartIndices` | number[] | segment boundaries into `points` |

Rules implications:

- Subcollection inherits whatever `events/**` rules exist today. Required shape:
  read = event members; write = manager (same role model as `routes`). Verify
  existing `firestore.rules` cover wildcard subcollections or add an explicit
  `match /events/{eventId}/trails/{trailId}` block mirroring the `routes` block.
- Payload budget: largest realistic doc stays far below 1 MiB (§7.5).

---

## 9. Exception logging integration (mandatory)

Today several map paths fail silently (`MapLibreView` catches → `Log.e` only).
This spec closes those gaps per the global exception-logging rule:

| Path | ErrorType (new enum values in `ErrorType.kt`) | Screen |
|---|---|---|
| Style load failure / `ensureBaseLayers` failure | `MAP_STYLE` | `map` |
| Offline region download/list/delete failures | `MAP_OFFLINE_REGION` | `map` / `settings` |
| Overpass fetch/parse failures | `TRAILS_FETCH` | `event_management` |
| Trail mapper/geometry errors | `ROUTE_PARSER` (existing type fits) | `map` |

Rules:

- Never swallow: every `catch` / `runCatching.onFailure` in touched files routes
  through `ExceptionLogger.log(...)`.
- No PII beyond user id; Overpass URLs contain no PII (bbox coordinates are
  event data, permitted).
- Expected flow-control (e.g., benign "region already exists", Overpass 429 with
  successful retry) logs at `Severity.WARNING` at most, or not at all if retried
  successfully.

---

## 10. Phase B (documented, NOT built) — future custom pipeline

Full plan preserved verbatim in git history of this conversation and summarized
here; promote to its own spec + implementation when any trigger fires.

**Activation triggers**

1. Product needs whole-country offline packages (not just event regions).
2. Brand-owned cartography becomes a requirement (our palette everywhere).
3. Route planning moves to active development (pipeline feeds routing graphs).
4. OpenFreeMap sustainability/terms become unacceptable.

**Summary of the preserved plan**

- `tools/map-pipeline/` in-repo: Geofabrik `israel-and-palestine-latest.osm.pbf`
  fetch; stock Planetiler OpenMapTiles profile → `ezrahi-basemap.mbtiles`
  (z0–14); own pyosmium extractor for route relations (`route=hiking|bicycle|
  foot|mtb`, networks `iwn|nwn|rwn|lwn`, own `osmc_symbol` parser) → GeoJSON →
  tippecanoe → `ezrahi-trails.mbtiles`; original style JSON; GitHub Actions
  weekly builds publishing Releases.
- Forward-compat hooks (docs-only until triggered): emit `trail_members.json`
  (way-id → relation mapping); optional `--enrich-pbf` stamping synthetic
  `ezr:network` / `ezr:colour` tags on member ways so any future routing engine
  (GraphHopper hike profile preferred; online-first self-hosted deployment) can
  prefer marked trails via simple tag conditions.
- Later terrain: JAXA AW3D30 → vector contours + Terrarium/TerrainRGB hillshade.

---

## 11. Testing & validation checklist

Unit tests:

- [ ] `OsmcSymbolParserTest`: hex colour passthrough; named osmc foregrounds;
      malformed symbols → fallback; all named colors covered.
- [ ] `TrailGeometryMapperTest`: `(points, segmentStartIndices)` ↔ List<LineString>
      round-trip; single-segment and multi-segment cases.
- [ ] `TrailReductionTest`: caps enforce priority order (iwn kept over lwn when
      over limit).

Integration/on-device:

- [ ] A1 acceptance (§5.3).
- [ ] A2 acceptance (§6.4).
- [ ] A3 acceptance (§7.8).
- [ ] Regression: participants/reports/route/measure layers unaffected; SOS flow
      intact; no new crashes in Crashlytics after a field session.

## 12. Risks & mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| OpenFreeMap availability/sustainability changes | Basemap outage online | Offline regions already mitigate field use; swap `ONLINE_STYLE_URI` is a one-line change to any provider |
| Overpass rate limits / downtime at event-save time | Trails missing | Fallback endpoint; non-blocking save; manual retry action |
| OfflineManager API drift across MapLibre versions | Build/runtime surprises | Pin verification task §6.2 before implementing UI |
| Oversized trails payloads near dense trail networks (Jerusalem Hills) | Slow sync, storage bloat | Caps in §7.5; bbox padding tuning |
| OfflineManager quota default | Downloads abort mid-way | Raise tile-count limit explicitly (§6.1) |

## 13. Out of scope (v1)

Contours/hillshade/terrain 3D · whole-country MBTiles/PMTiles · PMTiles web app ·
routing engine & navigation · per-participant offline region customization ·
trail shields/labels along path · Waymarked-style interactive trail selection.
