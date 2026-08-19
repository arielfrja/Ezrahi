# todo-fix-5 — GPX Route Parser + Polyline Renderer (Story Analysis)

> **STATUS: ✅ DONE (committed, build42 green, APK staged).**
> PO decisions: Firebase Storage file storage; multiple routes + active switcher; auto-fit camera (free pan/zoom after); dynamic per-event upload permissions (manager toggles roles/members). In-app route *creation/editing* is explicitly deferred to a later iteration.

## 1. Requirement (from Hebrew spec "תוכנית פיתוח מפורטת" + audit F3)

**Task 4 — Route Management ("ניהול מסלולים")**:
1. **ניהול במובייל**: In the Event Management screen, add an "Upload Route / העלה מסלול" button.
2. **ניהול במובייל**: Launch a file picker to select a `.gpx` file and upload it to **Firebase Storage**.
3. **תצוגת מסלול**: Download the GPX file and parse it.
4. **תצוגת מסלול**: Create an osmdroid `Polyline` layer to render the route on the map.

Audit F3: *"Uploading, parsing, and rendering `.gpx` track files on top of the map to guide groups along the designated trail."* Event lifecycle (spec): the **manager** uploads the planned trail (GPX) during event setup; participants follow it.

## 2. Current State (facts, verified in code)

- `FieldEvent.gpxRouteUrl: String?` — already fetched from `events/{id}` doc, cached in Room (`EventLocalEntity.gpxRouteUrl`), **unused** anywhere.
- Map = **osmdroid** `MapView` (MapScreen.kt) with Marker overlays (reports + participants), long-press → add report dialog. No Polyline anywhere.
- `firebase-storage-ktx` dependency **already declared** in `app/build.gradle.kts` (never used).
- osmdroid 6.1.20 + osmbonuspack 6.9.0 available (osmbonuspack has `TrackPathOverlay` if wanted).
- Event Management screen (fix-3) has Details/Crew/Reports tabs; manager-only rename exists in Details tab — natural home for the upload button.
- No legacy GPX code in git history (nothing to restore).
- Firebase Storage release ruleset reference exists but the ruleset returns 404 (storage rules effectively need writing).

## 3. Design Decisions (needs PO approval)

### D1 — Where the GPX file lives
| Option | Pros | Cons |
|---|---|---|
| **A. Firebase Storage** (spec-faithful): picker → upload `gpx/{eventId}.gpx` → store download URL in `events/{id}.gpxRouteUrl` | Honors spec; handles large files; URL already flows through existing listener → Room cache | Needs Storage rules + download-URL handling (`gs://` not readable client-side — must use `getDownloadUrl()` or https URL) |
| **B. Firestore doc** (route content embedded in `events/{id}/gpx`) | Zero new services; works with existing rules + Room offline cache automatically | 1 MB doc limit (typical GPX 10–500 KB); less "file management" |

**Recommendation: A (Firebase Storage)** — it is the written requirement and the dependency is already there.

### D2 — One route or many per event
- Spec: singular "the planned trail"; audit: "upload **and switch** GPX routes".
- **Recommendation: ONE active route per event** (replace = re-upload; new upload overwrites `gpx/{eventId}.gpx` and updates the URL). Multi-route library can be a later iteration.

### D3 — Camera behavior on load
- **Recommendation:** when a route is parsed, auto-fit map bounds to the route (`zoomToBoundingBox` + margin); users can still zoom/pan freely.

### D4 — Offline behavior
- Spec promises partial offline operation. Room already caches `gpxRouteUrl`.
- **Recommendation:** cache the **raw parsed route points** (not the file) in Room so the polyline renders offline even if the file can't be re-downloaded.

### D5 — Parsing scope
- Parse `trk/trkseg/trkpt` (+ `rte/rtept` as fallback) via `XmlPullParser` (built-in, no new dependency). Ignore elevation/time (not needed for rendering).

## 4. Implementation Plan (after PO approval)

1. **Repository**: `uploadRoute(eventId, uri): Result<String>` (Storage upload → `getDownloadUrl()` → update event doc), `downloadRoute(url): Result<String>` (fetch raw GPX), `getRoutePoints(...)` flow — parse + cache in Room (new `RouteEntity` or column; DB v3→v4 destructive-safe).
2. **Rules**: Storage rules — `allow read: if request.auth != null; allow write: if <event manager>` (manager check needs custom claim or a lookup — Storage rules can't query Firestore; simplest: store upload under `gpx/{eventId}.gpx` and gate by a `_isManager` custom claim, or accept auth-only write with URL validated on the Firestore side). *(Open sub-question for security review.)*
3. **UI — Event Management / Details tab** (manager only): "Upload Route / העלה מסלול" button → SAF file picker (`.gpx`) → upload with progress → snackbar result; show current route indicator.
4. **UI — Map**: Polyline overlay (distinct color, e.g. blue, width ~8dp) drawn from cached/downloaded points; auto-fit on first load; refresh when `gpxRouteUrl` changes via the existing event listener.
5. **Test**: real `.gpx` file (generate one with a track around the test-event area), upload as manager, verify polyline + bounds fit; verify participant sees it without upload rights; verify offline cache.

## 5. Open Questions for PO
- Q1: Route storage — Firebase Storage (recommended) or Firestore-embedded?
- Q2: One active route per event (recommended) or multiple routes + switcher now?
- Q3: Auto-fit map to route on load — yes (recommended) or no?
- Q4: Storage write gating — strict manager-only (needs custom claims) vs auth-only (simpler; URL authority checked when reading)? *(Security question — recommended: auth-only upload to a per-event path + manager-gated URL write in Firestore, which is already enforced.)*