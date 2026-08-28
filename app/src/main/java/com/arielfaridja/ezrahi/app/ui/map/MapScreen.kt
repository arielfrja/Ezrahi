package com.arielfaridja.ezrahi.app.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arielfaridja.ezrahi.MainActivity
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.app.util.Coordinates
import com.arielfaridja.ezrahi.app.util.CoordFormat
import com.arielfaridja.ezrahi.app.util.LocationPermissionHelper
import androidx.compose.ui.res.painterResource
import com.arielfaridja.ezrahi.app.ui.reports.ReportIconCatalog
import com.arielfaridja.ezrahi.core.mapengine.MapLayers
import com.arielfaridja.ezrahi.core.mapengine.MapLibreConfig
import com.arielfaridja.ezrahi.core.mapengine.MapLibreView
import com.arielfaridja.ezrahi.core.mapengine.REPORTS_LAYER
import com.arielfaridja.ezrahi.core.mapengine.OfflineTileManager
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.model.FieldReport
import com.arielfaridja.ezrahi.domain.model.FieldReportType
import com.arielfaridja.ezrahi.domain.model.GeoPoint
import com.arielfaridja.ezrahi.domain.model.ReportTypeDefinition
import com.arielfaridja.ezrahi.domain.model.StalenessConfig
import com.arielfaridja.ezrahi.domain.model.roleLabel
import com.arielfaridja.ezrahi.service.LocationTrackingService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.maps.MapLibreMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    eventId: String,
    onOpenDrawer: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var permissionsGranted by remember { mutableStateOf(false) }
    var showBackgroundExplanation by remember { mutableStateOf(false) }
    var showBatteryExplanation by remember { mutableStateOf(false) }
    var showAddMarkerDialog by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<ReportTip?>(null) }
    var mapBoxSize by remember { mutableStateOf(IntSize.Zero) }
    var cameraTick by remember { mutableStateOf(0) }
    var longPressLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var hasFittedRoute by remember { mutableStateOf(false) }
    var styleUri by remember { mutableStateOf<String?>(null) }
    val mapState = remember { mutableStateOf<MapLibreMap?>(null) }

    var coordFormat by remember { mutableStateOf(CoordFormat.ITM) }
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()
    var measureMode by remember { mutableStateOf(false) }
    val measurePoints = remember { mutableStateListOf<GeoPoint>() }

    val hudDisplay = HudDisplay(
        coordinateText = hudState.fix?.let {
            Coordinates.format(it.latitude, it.longitude, coordFormat)
        } ?: "--",
        accuracyMeters = hudState.fix?.accuracyMeters?.takeIf { a -> a > 0f },
        altitudeMeters = hudState.fix?.altitudeMeters?.takeIf { a -> a != 0.0 },
        online = hudState.online,
        pendingOutbox = hudState.pendingOutbox,
        batteryPercent = hudState.batteryPercent,
        strategyLabel = hudState.strategyLabel,
        lowPowerActive = hudState.lowPowerActive
    )

    lateinit var requestSecondaryPermissions: (Context) -> Unit
    lateinit var requestBatteryOptimizationExemption: (Context) -> Unit

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) requestBatteryOptimizationExemption(context)
    }

    val appSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        requestBatteryOptimizationExemption(context)
    }

    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
    }

    requestBatteryOptimizationExemption = { ctx ->
        if (!LocationPermissionHelper.isIgnoringBatteryOptimizations(ctx)) {
            showBatteryExplanation = true
        }
    }

    requestSecondaryPermissions = { ctx ->
        if (LocationPermissionHelper.backgroundLocationGranted(ctx)) {
            requestBatteryOptimizationExemption(ctx)
        } else if (LocationPermissionHelper.backgroundLocationCanBePrompted()) {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            showBackgroundExplanation = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
        if (permissionsGranted) {
            requestSecondaryPermissions(context)
        }
    }

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        permissionLauncher.launch(permissions.toTypedArray())
        styleUri = withContext(Dispatchers.IO) { OfflineTileManager.resolveStyleUri(context) }
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            val user = auth.currentUser
            if (user != null) {
                try {
                    val intent = android.content.Intent(context, LocationTrackingService::class.java).apply {
                        putExtra("EXTRA_EVENT_ID", eventId)
                        putExtra("EXTRA_USER_ID", user.uid)
                        putExtra(LocationTrackingService.EXTRA_CONTENT_ACTIVITY, MainActivity::class.java.name)
                    }
                    context.startForegroundService(intent)
                } catch (e: Exception) {
                    viewModel.logServiceStartFailure(e)
                }
            }
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    LaunchedEffect(mapState.value, state.participants, state.reports, state.reportTypes) {
        val map = mapState.value ?: return@LaunchedEffect
        map.style?.let { style ->
            MapLayers.updateParticipants(style, state.participants, state.event?.stalenessConfig ?: StalenessConfig())
            // Per-type marker bitmaps: one per type definition (icon glyph + user color)
            val typeBitmaps = state.reportTypes.associate { def ->
                "rtype_${def.id}" to ReportIconCatalog.renderMarkerBitmap(context, def.iconKey, def.colorHex)
            } + mapOf(
                "rtype_general" to ReportIconCatalog.renderMarkerBitmap(context, "general", "#2E7D32"),
                "rtype_medical" to ReportIconCatalog.renderMarkerBitmap(context, "medical", "#C62828")
            )
            MapLayers.ensureReportTypeIcons(style, typeBitmaps)
            val iconByTypeId = state.reportTypes.associate { it.id to "rtype_${it.id}" }
            MapLayers.updateReports(style, state.reports) { report ->
                report.typeId?.let { iconByTypeId[it] }
                    ?: if (report.type == FieldReportType.MEDICAL) "rtype_medical" else "rtype_general"
            }
        }
    }

    LaunchedEffect(mapState.value, state.routePoints) {
        val map = mapState.value ?: return@LaunchedEffect
        if (state.routePoints.size > 1 && !hasFittedRoute) {
            hasFittedRoute = true
            val bounds = LatLngBounds.Builder().includes(
                state.routePoints.map { LatLng(it.latitude, it.longitude) }
            ).build()
            map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 64))
        }
        map.style?.let { MapLayers.updateRoute(it, state.routePoints) }
    }

    LaunchedEffect(mapState.value, permissionsGranted) {
        val map = mapState.value ?: return@LaunchedEffect
        if (permissionsGranted) {
            val style = map.style ?: return@LaunchedEffect
            try {
                val locationComponent = map.locationComponent
                if (!locationComponent.isLocationComponentActivated) {
                    locationComponent.activateLocationComponent(
                        LocationComponentActivationOptions.builder(context, style)
                            .useDefaultLocationEngine(true)
                            .build()
                    )
                }
                locationComponent.isLocationComponentEnabled = true
            } catch (e: Exception) {
                viewModel.logServiceStartFailure(e)
            }
        }
    }

    LaunchedEffect(mapState.value, measureMode, measurePoints.size) {
        val map = mapState.value ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        MapLayers.ensureMeasureLayers(style)
        MapLayers.updateMeasure(style, measurePoints.toList())
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(state.event?.name ?: "Field Activity") },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
                if (state.activeRouteName != null) {
                    Text(
                        text = "Route: ${state.activeRouteName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                    )
                }
                TacticalHudBar(
                    hud = hudDisplay,
                    onCycleCoordinateFormat = { coordFormat = coordFormat.next() }
                )
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = {
                        measureMode = !measureMode
                        measurePoints.clear()
                    },
                    containerColor = if (measureMode) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Straighten, contentDescription = "Measure")
                }
                Spacer(Modifier.height(12.dp))
                FloatingActionButton(
                    onClick = {
                        val map = mapState.value
                        val loc = map?.locationComponent?.lastKnownLocation
                        if (map != null && loc != null) {
                            map.easeCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16.0))
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                }
            }
        }
    ) { padding ->
        if (styleUri == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(Modifier.fillMaxSize().padding(padding).onSizeChanged { mapBoxSize = it }) {
                MapLibreView(
                    styleUri = styleUri!!,
                    modifier = Modifier.fillMaxSize(),
                    onMapReady = { map ->
                        mapState.value = map
                        // Re-project the open tooltip's marker on every camera move
                        // (real-time following + hide/show when off-screen) — notes task A.
                        map.addOnCameraMoveListener { cameraTick++ }
                    },
                    onMapClick = { latLng ->
                        // Marker-click pattern (ADR docs/decisions/0001): layer-scoped
                        // hit-test instead of per-marker listeners; non-hits fall through.
                        val map = mapState.value
                        val hit = map?.let { m ->
                            runCatching {
                                val screen = m.projection.toScreenLocation(latLng)
                                val id = m.queryRenderedFeatures(screen, REPORTS_LAYER)
                                    .firstOrNull()
                                    ?.getProperty("reportId")?.asString
                                Pair(id, screen)
                            }.getOrNull()
                        }
                        val report = hit?.let { (id, screen) -> id?.let { state.reports.firstOrNull { it.id == id } } }
                        when {
                            report != null -> {
                                // Directly switch to the tapped marker's tooltip
                                // (single tap replaces the open one) — notes task 1.
                                selectedReport = ReportTip(report, GeoPoint(latLng.latitude, latLng.longitude))
                            }
                            selectedReport != null -> selectedReport = null
                            measureMode -> measurePoints.add(GeoPoint(latLng.latitude, latLng.longitude))
                        }
                    },
                    onLongClick = { latLng, _ ->
                        val geo = GeoPoint(latLng.latitude, latLng.longitude)
                        if (measureMode) {
                            measurePoints.add(geo)
                        } else {
                            longPressLocation = geo
                            showAddMarkerDialog = true
                        }
                    }
                )
                ReportTooltipPopup(
                    tip = selectedReport,
                    types = state.reportTypes,
                    participants = state.participants,
                    map = mapState.value,
                    parentSize = mapBoxSize,
                    cameraTick = cameraTick,
                    onDismiss = { selectedReport = null }
                )
            }
        }

        if (showBackgroundExplanation) {
            AlertDialog(
                onDismissRequest = { showBackgroundExplanation = false },
                title = { Text("Background Location / מיקום ברקע") },
                text = {
                    Text(
                        "To keep transmitting your location when the app is in the background, " +
                            "Ezrahi needs the \"Allow all the time\" location permission.\n\n" +
                            "Tap Continue to open Settings, then select \"Allow all the time\" under Location.\n\n" +
                            "כדי להמשיך לשדר את המיקום ברקע, יש להעניק הרשאת \"Allow all the time\"."
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showBackgroundExplanation = false
                        appSettingsLauncher.launch(LocationPermissionHelper.appSettingsIntent(context))
                    }) { Text("Continue / המשך") }
                },
                dismissButton = {
                    TextButton(onClick = { showBackgroundExplanation = false }) { Text("Not now / לא עכשיו") }
                }
            )
        }

        if (showBatteryExplanation) {
            AlertDialog(
                onDismissRequest = { showBatteryExplanation = false },
                title = { Text("Battery Optimization / חיסכון בסוללה") },
                text = {
                    Text(
                        "To keep location tracking reliable in the background, " +
                            "Ezrahi should be exempt from battery optimization.\n\n" +
                            "Tap Continue to allow it.\n\n" +
                            "כדי ששירות המיקום ימשיך לפעול ברקע, יש לבטל את חיסכון הסוללה עבור Ezrahi."
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showBatteryExplanation = false
                        batteryOptimizationLauncher.launch(LocationPermissionHelper.batteryOptimizationRequestIntent(context))
                    }) { Text("Continue / המשך") }
                },
                dismissButton = {
                    TextButton(onClick = { showBatteryExplanation = false }) { Text("Not now / לא עכשיו") }
                }
            )
        }

        if (showAddMarkerDialog) {
            var markerTitle by remember { mutableStateOf("") }
            var markerDescription by remember { mutableStateOf("") }
            val reportTypes by viewModel.reportTypes.collectAsStateWithLifecycle()
            val effectiveTypes = remember(reportTypes) {
                if (reportTypes.isEmpty()) {
                    listOf(
                        ReportTypeDefinition(name = "GENERAL", iconKey = "general", builtin = true),
                        ReportTypeDefinition(name = "MEDICAL", iconKey = "medical", builtin = true)
                    )
                } else reportTypes
            }
            var selectedType by remember(effectiveTypes) { mutableStateOf(effectiveTypes.first()) }
            var typeMenuExpanded by remember { mutableStateOf(false) }
            val selectedEntry = ReportIconCatalog.entry(selectedType.iconKey)
            AlertDialog(
                onDismissRequest = { showAddMarkerDialog = false },
                title = { Text("Add Marker / הוספת דיווח") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = markerTitle,
                            onValueChange = { markerTitle = it },
                            label = { Text("Title / כותרת") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = markerDescription,
                            onValueChange = { markerDescription = it },
                            label = { Text("Description / תיאור") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        ExposedDropdownMenuBox(
                            expanded = typeMenuExpanded,
                            onExpandedChange = { typeMenuExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedType.name.replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Report type") },
                                leadingIcon = {
                                    Icon(painterResource(selectedEntry.resId), contentDescription = null, tint = selectedEntry.accent)
                                },
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = typeMenuExpanded,
                                onDismissRequest = { typeMenuExpanded = false }
                            ) {
                                effectiveTypes.forEach { def ->
                                    val entry = ReportIconCatalog.entry(def.iconKey)
                                    DropdownMenuItem(
                                        text = { Text(def.name.replaceFirstChar { it.uppercase() }) },
                                        leadingIcon = {
                                            Icon(painterResource(entry.resId), contentDescription = null, tint = entry.accent)
                                        },
                                        onClick = {
                                            selectedType = def
                                            typeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        longPressLocation?.let { loc ->
                            viewModel.addReport(eventId, markerTitle, markerDescription, selectedType, loc.latitude, loc.longitude)
                        }
                        showAddMarkerDialog = false
                    }) { Text("Add / הוספה") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddMarkerDialog = false }) { Text("Cancel / ביטול") }
                }
            )
        }

        if (measureMode) {
            val segments = measurePoints.zipWithNext { a, b ->
                Coordinates.haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)
            }
            val totalMeters = segments.sum()
            val lastSegment = segments.lastOrNull()
            Box(Modifier.fillMaxSize()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 110.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = if (totalMeters >= 1000) "%.2f km".format(totalMeters / 1000)
                                else "%.0f m".format(totalMeters),
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (measurePoints.isNotEmpty()) {
                                Text(
                                    text = buildString {
                                        append("${measurePoints.size} pts")
                                        lastSegment?.let {
                                            append(" · last ")
                                            append(if (it >= 1000) "%.2f km".format(it / 1000) else "%.0f m".format(it))
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                        TextButton(onClick = { measurePoints.clear() }) { Text("Clear") }
                        TextButton(onClick = { measureMode = false; measurePoints.clear() }) { Text("Done") }
                    }
                }
            }
        }

    }
}

// Report tooltip anchored to marker position (ADR docs/decisions/0001)
@Immutable
data class ReportTip(val report: FieldReport, val latLng: GeoPoint)

@Composable
private fun ReportTooltipPopup(
    tip: ReportTip?,
    types: List<ReportTypeDefinition>,
    participants: List<EventParticipant>,
    map: MapLibreMap?,
    parentSize: IntSize,
    cameraTick: Int,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    var tipSize by remember { mutableStateOf(IntSize.Zero) }

    tip?.let { tip ->
        // Re-project the marker to its current screen position every camera move.
        val screen = map?.let { m ->
            runCatching {
                m.projection.toScreenLocation(
                    LatLng(tip.latLng.latitude, tip.latLng.longitude)
                )
            }.getOrNull()
        } ?: return@let

        val report = tip.report
        val def = types.firstOrNull { it.id == report.typeId }
        val entry = def?.let { ReportIconCatalog.entry(it.iconKey) }
            ?: ReportIconCatalog.entry(if (report.type == FieldReportType.MEDICAL) "medical" else "general")
        val accent = def?.let { d ->
            runCatching { Color(android.graphics.Color.parseColor(d.colorHex)) }.getOrDefault(entry.accent)
        } ?: entry.accent

        // While the marker is on-screen show the tooltip; otherwise render a small
        // directional triangle at the nearest screen edge pointing at the report.
        val onScreen = screen.x >= 0f && screen.y >= 0f &&
            screen.x <= parentSize.width.toFloat() && screen.y <= parentSize.height.toFloat()
        if (!onScreen) {
            ReportOffscreenIndicator(
                markerX = screen.x,
                markerY = screen.y,
                viewportW = parentSize.width,
                viewportH = parentSize.height,
                color = accent,
                onDismiss = onDismiss
            )
            return@let
        }

        val anchor = Offset(screen.x, screen.y)
        val gapPx = with(density) { 12.dp.toPx() }

        val tipWidth = tipSize.width
        val tipHeight = tipSize.height

        val left = (anchor.x - tipWidth / 2f).toInt()
            .coerceIn(0, (parentSize.width - tipWidth).coerceAtLeast(0))
        val top = (anchor.y - tipHeight - gapPx).toInt()
            .takeIf { it > 0 }
            ?: (anchor.y + gapPx).toInt()
                .coerceAtMost((parentSize.height - tipHeight).coerceAtLeast(0))

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(left, top),
                onDismissRequest = onDismiss,
                // focusable=false lets taps reach the map beneath, so a tap on another
                // marker switches the tooltip directly (handled in onMapClick) and a tap
                // on empty map closes it (also onMapClick) — notes task 1.
                // LTR is forced so the popup uses physical-pixel coordinates from the
                // map projection (otherwise RTL mirrors the horizontal axis).
                properties = PopupProperties(focusable = false, dismissOnClickOutside = false)
            ) {
            val typeLabel = def?.name ?: when (report.type) {
                FieldReportType.MEDICAL -> "Medical"
                FieldReportType.GENERAL -> "General"
                FieldReportType.UNKNOWN -> "Custom"
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .onSizeChanged { tipSize = it }
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(entry.resId),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            report.title.ifBlank { "Report" },
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        typeLabel.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                    if (report.description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            report.description,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    val reporter = participants.firstOrNull { it.userId == report.reporterId }
                    val reporterLabel = if (reporter != null) {
                        "${reporter.fullName.ifBlank { report.reporterId }} · ${roleLabel(reporter.role)}"
                    } else {
                        report.reporterId
                    }
                    Text(
                        "By $reporterLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        java.text.DateFormat.getDateTimeInstance().format(java.util.Date(report.reportTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun ReportOffscreenIndicator(
    markerX: Float,
    markerY: Float,
    viewportW: Int,
    viewportH: Int,
    color: Color,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val margin = with(density) { 14.dp.toPx() }
    val arrowSize = with(density) { 18.dp.toPx() }

    if (viewportW <= 0 || viewportH <= 0) return

    val cx = viewportW / 2f
    val cy = viewportH / 2f
    val dx = markerX - cx
    val dy = markerY - cy

    // Clamp the indicator to the nearest screen edge (with a small margin).
    val px = markerX.coerceIn(margin, (viewportW - margin).coerceAtLeast(margin))
    val py = markerY.coerceIn(margin, (viewportH - margin).coerceAtLeast(margin))

    // Triangle is drawn pointing "up" (-y); rotate it to aim at the marker.
    val degrees = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f

    val shadowColor = Color.Black.copy(alpha = 0.25f)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Popup(
            alignment = Alignment.TopStart,
            offset = IntOffset((px - arrowSize / 2f).toInt(), (py - arrowSize / 2f).toInt()),
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = false, dismissOnClickOutside = false)
        ) {
            Canvas(modifier = Modifier.size(with(density) { arrowSize.toDp() })) {
                val w = size.width
                val h = size.height
                val r = w * 0.22f
                val ax = w / 2f; val ay = 0f          // apex
                val bx = 0f;     val by = h           // bottom-left
                val cx = w;      val cy = h           // bottom-right

                fun unit(x1: Float, y1: Float, x2: Float, y2: Float): Pair<Float, Float> {
                    val d = kotlin.math.hypot(x2 - x1, y2 - y1).coerceAtLeast(1e-4f)
                    return ((x2 - x1) / d) to ((y2 - y1) / d)
                }
                // Corner A (apex): from edge CA into edge AB
                val (uAx, uAy) = unit(ax, ay, cx, cy)
                val (uBx, uBy) = unit(ax, ay, bx, by)
                val sA = Offset(ax + uAx * r, ay + uAy * r)
                val eA = Offset(ax + uBx * r, ay + uBy * r)
                // Corner B: from edge AB into edge BC
                val (u2x, u2y) = unit(bx, by, ax, ay)
                val (u3x, u3y) = unit(bx, by, cx, cy)
                val sB = Offset(bx + u2x * r, by + u2y * r)
                val eB = Offset(bx + u3x * r, by + u3y * r)
                // Corner C: from edge BC into edge CA
                val (u4x, u4y) = unit(cx, cy, bx, by)
                val (u5x, u5y) = unit(cx, cy, ax, ay)
                val sC = Offset(cx + u4x * r, cy + u4y * r)
                val eC = Offset(cx + u5x * r, cy + u5y * r)

                val triangle = Path().apply {
                    moveTo(eA.x, eA.y)
                    lineTo(sB.x, sB.y); quadraticBezierTo(bx, by, eB.x, eB.y)
                    lineTo(sC.x, sC.y); quadraticBezierTo(cx, cy, eC.x, eC.y)
                    lineTo(sA.x, sA.y); quadraticBezierTo(ax, ay, eA.x, eA.y)
                    close()
                }

                rotate(degrees) {
                    // Little elevated: soft drop shadow offset toward the base.
                    withTransform({ translate(left = 0f, top = 3f) }) {
                        drawPath(triangle, shadowColor, alpha = 0.35f)
                    }
                    drawPath(triangle, color)
                }
            }
        }
    }
}
