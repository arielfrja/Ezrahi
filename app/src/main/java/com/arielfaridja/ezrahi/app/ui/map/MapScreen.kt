package com.arielfaridja.ezrahi.app.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arielfaridja.ezrahi.MainActivity
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.app.util.Coordinates
import com.arielfaridja.ezrahi.app.util.CoordFormat
import com.arielfaridja.ezrahi.app.util.LocationPermissionHelper
import com.arielfaridja.ezrahi.core.mapengine.MapLayers
import com.arielfaridja.ezrahi.core.mapengine.MapLibreConfig
import com.arielfaridja.ezrahi.core.mapengine.MapLibreView
import com.arielfaridja.ezrahi.core.mapengine.OfflineTileManager
import com.arielfaridja.ezrahi.domain.model.FieldReportType
import com.arielfaridja.ezrahi.domain.model.GeoPoint
import com.arielfaridja.ezrahi.domain.model.StalenessConfig
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

    LaunchedEffect(mapState.value, state.participants, state.reports) {
        val map = mapState.value ?: return@LaunchedEffect
        map.style?.let { style ->
            MapLayers.updateParticipants(style, state.participants, state.event?.stalenessConfig ?: StalenessConfig())
            MapLayers.updateReports(style, state.reports)
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
            MapLibreView(
                styleUri = styleUri!!,
                modifier = Modifier.fillMaxSize().padding(padding),
                onMapReady = { map ->
                    mapState.value = map
                },
                onMapClick = { latLng ->
                    if (measureMode) {
                        measurePoints.add(GeoPoint(latLng.latitude, latLng.longitude))
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
            var reportType by remember { mutableStateOf(FieldReportType.GENERAL) }
            AlertDialog(
                onDismissRequest = { showAddMarkerDialog = false },
                title = { Text("Add Marker / הוספת דיווח") },
                text = {
                    LazyColumn {
                        item {
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FieldReportType.entries.forEach { type ->
                                    FilterChip(
                                        selected = reportType == type,
                                        onClick = { reportType = type },
                                        label = { Text(if (type == FieldReportType.MEDICAL) "Medical" else "General") }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        longPressLocation?.let { loc ->
                            viewModel.addReport(eventId, markerTitle, markerDescription, reportType, loc.latitude, loc.longitude)
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
