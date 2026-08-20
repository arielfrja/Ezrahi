package com.arielfaridja.ezrahi.app.ui.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arielfaridja.ezrahi.R
import com.arielfaridja.ezrahi.app.util.LocationPermissionHelper
import com.arielfaridja.ezrahi.domain.model.FieldReportStatus
import com.arielfaridja.ezrahi.domain.model.FieldReportType
import com.arielfaridja.ezrahi.service.LocationTrackingService
import com.google.firebase.auth.FirebaseAuth
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private const val DEFAULT_ZOOM_LEVEL = 12.0
private const val DEFAULT_LATITUDE = 31.776551
private const val DEFAULT_LONGITUDE = 35.233808

private fun reportTypeToIcon(context: Context, type: FieldReportType): Drawable? {
    return when (type) {
        FieldReportType.MEDICAL -> ResourcesCompat.getDrawable(context.resources, R.drawable.report_medical, null)
        else -> ResourcesCompat.getDrawable(context.resources, R.drawable.report_canvas, null)
    }
}

private fun reportAlpha(status: FieldReportStatus): Float = when (status) {
    FieldReportStatus.HANDLED -> 0.5f
    FieldReportStatus.UNKNOWN -> 0.0f
    else -> 1.0f
}

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
        permissionLauncher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            val user = auth.currentUser
            if (user != null) {
                try {
                    val intent = Intent(context, LocationTrackingService::class.java).apply {
                        putExtra("EXTRA_EVENT_ID", eventId)
                        putExtra("EXTRA_USER_ID", user.uid)
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

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setCenter(GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
            controller.setZoom(DEFAULT_ZOOM_LEVEL)
            overlayManager.add(object : Overlay() {
                override fun onLongPress(event: MotionEvent?, mapView: MapView?): Boolean {
                    event?.let { ev ->
                        mapView?.let {
                            longPressLocation = it.projection.fromPixels(ev.x.toInt(), ev.y.toInt()) as? GeoPoint
                            showAddMarkerDialog = true
                        }
                    }
                    return true
                }
            })
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    val myLocationOverlay = remember(mapView) {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
            enableFollowLocation()
        }
    }

    val routePolyline = remember {
        Polyline().apply {
            setColor(0xFF1565C0.toInt())
            setWidth(8f)
        }
    }

    LaunchedEffect(state.routePoints, hasFittedRoute) {
        routePolyline.setPoints(state.routePoints.map { GeoPoint(it.latitude, it.longitude) })
        if (mapView.overlayManager.overlays().none { it == routePolyline }) {
            mapView.overlayManager.add(routePolyline)
        }
        if (state.routePoints.size > 1 && !hasFittedRoute) {
            hasFittedRoute = true
            val box = BoundingBox.fromGeoPoints(routePolyline.actualPoints)
            mapView.zoomToBoundingBox(box, true)
        }
        mapView.invalidate()
    }

    LaunchedEffect(state.participants, state.reports) {
        mapView.overlayManager.overlays()
            .filterIsInstance<Marker>()
            .forEach { mapView.overlayManager.remove(it) }
        state.reports.forEach { report ->
            Marker(mapView).apply {
                position = GeoPoint(report.location.latitude, report.location.longitude)
                title = report.title.ifEmpty { "Report" }
                snippet = report.description
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = reportTypeToIcon(context, report.type)
                alpha = reportAlpha(report.status)
                mapView.overlayManager.add(this)
            }
        }
        state.participants.forEach { participant ->
            participant.currentLocation?.let { loc ->
                Marker(mapView).apply {
                    position = GeoPoint(loc.latitude, loc.longitude)
                    title = "${participant.fullName} (${participant.role})"
                    snippet = "Last seen: ${participant.isOnline}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlayManager.add(this)
                }
            }
        }
        if (mapView.overlayManager.overlays().none { it == myLocationOverlay }) {
            mapView.overlayManager.add(myLocationOverlay)
        }
        mapView.invalidate()
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
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val loc = myLocationOverlay.myLocation
                    if (loc != null) {
                        myLocationOverlay.enableFollowLocation()
                        mapView.controller.animateTo(loc)
                        mapView.controller.zoomTo(20.0)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location")
            }
        }
    ) { padding ->
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize().padding(padding)
        )

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
    }
}