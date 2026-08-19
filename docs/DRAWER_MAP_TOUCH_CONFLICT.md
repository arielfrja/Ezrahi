# Touch Conflict: Material3 `ModalNavigationDrawer` vs. osmdroid `MapView` (one-finger pan)

Status: **RESOLVED** — see §10 (applied 2026-08-19).
Created: 2026-08-19
Reporter: Ezrahi project (Phase 5 — single-activity Compose migration)

---

## 1. TL;DR

In a Jetpack Compose single-activity app, a Material3 `ModalNavigationDrawer`
wraps a `NavHost`. One screen hosts an osmdroid `MapView` through
`AndroidView`. The MapView cannot be panned with one finger while
`ModalNavigationDrawer.gesturesEnabled = true` (default). Setting
`gesturesEnabled = false` fixes the map, but **breaks the drawer's
scrim-tap-to-close** (and Material3 1.2.1 has no drawer `BackHandler` at all).
We need a way to have **both**: a fully functional drawer (open via hamburger,
close via scrim tap / back press / swipe) **and** a MapView that receives
one-finger drag gestures.

---

## 2. Environment

| Item | Value |
|---|---|
| Device | Motorola Moto G84 5G, Android 15 (API 35) |
| Language / framework | Kotlin 2.0.0, Jetpack Compose, single-activity |
| Compose BOM | `2024.08.00` → compose-ui **1.7.2**, material3 **1.2.1** |
| activity-compose | 1.9.1 |
| navigation-compose | 2.7.7 |
| osmdroid | **6.1.20** |
| OSMBonusPack | 6.9.0 |
| AGP | 8.5.2 |

---

## 3. Layout / architecture

```
MainActivity (ComponentActivity)
└── setContent { EzrahiTheme { Surface { EzrahiNavApp() } } }

EzrahiNavApp()
└── ModalNavigationDrawer(          // Material3 1.2.1
        drawerState = drawerState,
        gesturesEnabled = <see below>,   // default true
        drawerContent = { ModalDrawerSheet { ...NavigationDrawerItem... } }
    ) {
        NavHost(startDestination = "auth") {
            composable("events")          { EventPickerScreen(...) }
            composable("map/{eventId}")   { MapScreen(...) }   // ← the problem
            composable("messages/{eventId}") { ... }
            ...
        }
    }

MapScreen(eventId, onOpenDrawer)
└── Scaffold(topBar = TopAppBar(hamburger → onOpenDrawer))
└── AndroidView(
        factory = { osmdroid MapView },          // TileSourceFactory.MAPNIK
        modifier = Modifier.fillMaxSize().padding(padding)
                   .pointerInteropFilter { ... } // one attempted fix, see §6.3
    )
```

- The MapView is created with `setMultiTouchControls(true)` and a custom
  `Overlay.onLongPress` handler (long-press → "Add marker" dialog).
- The drawer is opened programmatically: hamburger → `scope.launch { drawerState.open() }`.
- A custom `BackHandler(enabled = drawerState.isOpen) { drawerState.close() }`
  exists in `EzrahiNavApp` (added by us, see §6.2).

---

## 4. Problem statement

With `ModalNavigationDrawer.gesturesEnabled = true` (the default), the map
screen cannot be panned with one finger:

- One-finger drag does **not** move the map (the drag appears to be swallowed).
- Long-press also stops working (it was reported together with the drag).
- Two-finger pinch-zoom works (diagonal drag; horizontal detector doesn't
  claim it).
- The drawer works correctly: opens via hamburger, closes via scrim tap,
  closes via back press (our explicit `BackHandler`), items navigate.

We verified the horizontal-drag theft is caused by the drawer's
`anchoredDraggable`: Material3 1.2.1's `ModalNavigationDrawer` puts

```kotlin
Modifier
    .fillMaxSize()
    .anchoredDraggable(
        state = drawerState.anchoredDraggableState,
        orientation = Orientation.Horizontal,
        enabled = gesturesEnabled,
        reverseDirection = isRtl
    )
```

on the **outer Box that wraps the entire content** (`NavigationDrawer.kt`,
`ModalNavigationDrawer`). The detector is active even while the drawer is
`Closed` (it is anchored at `Closed` and still participates in gesture
detection), so horizontal drags anywhere on screen — including on the map —
are consumed by the drawer's drag gesture detector before/while the embedded
View handles them.

The legacy (pre-Compose) version of this app used a classic `DrawerLayout`,
which only intercepts **edge swipes**, so the map always worked.

---

## 5. Behavior matrix (verified on device)

| Configuration | Map one-finger pan | Map long-press | Drawer open (hamburger) | Drawer close: scrim tap | Drawer close: back press | Drawer close: drawer item |
|---|---|---|---|---|---|---|
| `gesturesEnabled = true` (default) | **BROKEN** | broken | works | works | works (our `BackHandler`) | works |
| `gesturesEnabled = false` | works | works | works | **BROKEN** (scrim eats the tap, does nothing) | works (our `BackHandler`) | works |

---

## 6. Steps already tried

### 6.1 `gesturesEnabled = false` (drawer gesture disabled)
Result: map works perfectly (pan, long-press, zoom), but the scrim no longer
closes the drawer on tap. Material3 1.2.1 source (`NavigationDrawer.kt`,
`ModalNavigationDrawer`):

```kotlin
Scrim(
    open = drawerState.isOpen,
    onClose = {
        if (
            gesturesEnabled &&                                        // ← gated!
            drawerState.anchoredDraggableState.confirmValueChange(DrawerValue.Closed)
        ) {
            scope.launch { drawerState.close() }
        }
    },
    ...
)
```

So with `gesturesEnabled = false`, tapping the scrim is a no-op (the scrim's
`detectTapGestures` still consumes the tap — see `Scrim` composable in the
same file — so the touch never reaches the content below either). Only closing
via a drawer item or back press remains. Not acceptable UX.

### 6.2 Explicit `BackHandler` for the drawer
Added in `EzrahiNavApp`:

```kotlin
BackHandler(enabled = drawerState.isOpen) {
    scope.launch { drawerState.close() }
}
```

Result: back press closes the drawer reliably on all screens. This covers the
"close via back" requirement regardless of Material3 version (1.2.1 has no
drawer `BackHandler` internally — verified in source).

### 6.3 `Modifier.pointerInteropFilter` on the `AndroidView` (current attempt)
Wrapped the map's `AndroidView`:

```kotlin
AndroidView(
    factory = { mapView },
    modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .pointerInteropFilter { event ->
            mapView.dispatchTouchEvent(event)
        }
)
```

Theory: the interop filter consumes the events at the View-interop layer,
before the drawer's `anchoredDraggable` (a Compose ancestor detector) can
claim them, and forwards them directly to the osmdroid view.

Result: **map pan is still broken** with `gesturesEnabled = true`. The drawer
behaves correctly. So the interop-filter consumption does not stop the
ancestor `anchoredDraggable` from winning the drag — or the events never reach
the interop filter (consumed earlier in the pointer dispatch). Note: compose-ui
1.7.2's `AndroidViewHolder` already registers its own internal
`pointerInteropFilter` that forwards to `view.dispatchTouchEvent(...)`
(`AndroidViewHolder.android.kt`, `layoutNode` core modifier), so our extra
filter is stacked *outside* that one.

### 6.4 Material3 1.3.0 — checked, not a fix
Inspected `material3-android-1.3.0` sources: the scrim `onClose` is **still**
gated on `gesturesEnabled` in 1.3.0 (`NavigationDrawer.kt` line ~347), so an
upgrade does not solve the scrim-tap issue. (1.3.0 does add a predictive back
handler, which our explicit `BackHandler` already covers.)

---

## 7. Questions for the expert

1. What is the canonical way to embed an osmdroid (or any legacy View)
   `MapView` inside a Material3 `ModalNavigationDrawer` so that the map gets
   one-finger drag gestures **and** the drawer keeps scrim-tap close, back
   close, and (optionally) swipe-to-open?
2. Why does `pointerInteropFilter` on the `AndroidView` modifier not prevent
   the ancestor `anchoredDraggable` from stealing the drag? Is there an
   ordering issue (initial pass vs. main pass) between the anchored draggable
   and the interop consumption, and if so, what consumption/claim API should
   the map use instead?
3. Is there a supported way to make `ModalNavigationDrawer` swipe-to-open
   only from the screen **edge** (like the legacy `DrawerLayout` edge-swipe
   behavior), so full-width horizontal drags on the map are never contested?
4. Would wrapping the `MapView` in a nested-scroll container (or providing a
   `NestedScrollConnection`) help, given osmdroid does not implement nested
   scrolling itself?
5. Is the recommended workaround to replace `ModalNavigationDrawer` with a
   custom drawer implementation (own `Box` + offset + edge `pointerInput`
   detector + own scrim), or is there a first-party pattern for
   "gesturesEnabled = false but scrim still closes"?

---

## 8. Relevant Material3 1.2.1 source excerpts

`androidx/compose/material3/NavigationDrawer.kt` — `ModalNavigationDrawer`:

```kotlin
Box(
    modifier
        .fillMaxSize()
        .anchoredDraggable(
            state = drawerState.anchoredDraggableState,
            orientation = Orientation.Horizontal,
            enabled = gesturesEnabled,      // full-width drag detector, always active
            reverseDirection = isRtl
        )
) {
    Box { content() }
    Scrim(
        open = drawerState.isOpen,
        onClose = {
            if (
                gesturesEnabled &&          // ← scrim close gated on gesturesEnabled
                drawerState.anchoredDraggableState.confirmValueChange(DrawerValue.Closed)
            ) {
                scope.launch { drawerState.close() }
            }
        },
        ...
    )
    Box(Modifier.offset { ... }) { drawerContent() }
}
```

`Scrim` (same file):

```kotlin
private fun Scrim(open, onClose, fraction, color) {
    val dismissDrawer = if (open) {
        Modifier
            .pointerInput(onClose) { detectTapGestures { onClose() } }  // consumes taps even when onClose is a no-op
            .semantics(mergeDescendants = true) { ... }
    } else { Modifier }
    Canvas(Modifier.fillMaxSize().then(dismissDrawer)) { drawRect(color, alpha = fraction()) }
}
```

compose-ui 1.7.2 `AndroidViewHolder` core modifier (always applied):

```kotlin
val coreModifier = Modifier
    .nestedScroll(NoOpScrollConnection, dispatcher)
    .semantics(true) {}
    .pointerInteropFilter(this)   // forwards to view.dispatchTouchEvent
    ...
```

---

## 9. Files in this repository (for context)

- `app/src/main/java/com/arielfaridja/ezrahi/MainActivity.kt` — `EzrahiNavApp`,
  drawer + NavHost + explicit `BackHandler`.
- `app/src/main/java/com/arielfaridja/ezrahi/app/ui/map/MapScreen.kt` — map
  screen with the osmdroid `AndroidView` and the `pointerInteropFilter`
  attempt.
- `app/src/main/java/com/arielfaridja/ezrahi/UI/Main/MainActivity.kt` — legacy
  View-based version (drawer via `DrawerLayout` + navigation component) where
  the map always worked.

## 10. RESOLUTION (applied)

**Fix:** make drawer gestures **state-dependent** instead of static, and drop the
`pointerInteropFilter` on the map's `AndroidView`.

`MainActivity.kt` (`EzrahiNavApp`):

```kotlin
ModalNavigationDrawer(
    drawerState = drawerState,
    gesturesEnabled = drawerState.isOpen || drawerState.targetValue == DrawerValue.Open,
    ...
)
```

`MapScreen.kt`: reverted `AndroidView` to a plain integration (no
`pointerInteropFilter`).

**Why it works:**
- Drawer **closed** → `gesturesEnabled = false` → the drawer's
  `anchoredDraggable` is disabled → the map owns the whole screen
  (pan / pinch / long-press verified working).
- Drawer **open** → `gesturesEnabled = true` → the scrim's `onClose` passes the
  `if (gesturesEnabled)` gate → **scrim tap closes the drawer** (and
  drag-to-close works).
- Drawer opening stays programmatic (hamburger → `drawerState.open()`), which
  does not depend on `gesturesEnabled`.
- Back press close is covered by the explicit `BackHandler`.

Verified behavior matrix (all rows **WORKS**): map pan, map long-press, map
pinch-zoom, hamburger open, scrim-tap close, back-press close, drawer swipe
close.

- `app/src/main/java/com/arielfaridja/ezrahi/MainActivity.kt` — `EzrahiNavApp`,
  drawer + NavHost + explicit `BackHandler`.
- `app/src/main/java/com/arielfaridja/ezrahi/app/ui/map/MapScreen.kt` — map
  screen with the osmdroid `AndroidView` and the `pointerInteropFilter`
  attempt.
- `app/src/main/java/com/arielfaridja/ezrahi/UI/Main/MainActivity.kt` — legacy
  View-based version (drawer via `DrawerLayout` + navigation component) where
  the map always worked.