package com.arielfaridja.ezrahi.app.ui.map

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arielfaridja.ezrahi.service.LocationTrackingService
import com.google.firebase.auth.FirebaseAuth
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private const val DEFAULT_ZOOM_LEVEL = 12.0
private const val DEFAULT_LATITUDE = 31.776551
private const val DEFAULT_LONGITUDE = 35.233808

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    eventId: String,
    onNavigateToMessages: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var permissionsGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
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
                    e.printStackTrace()
                }
            }
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setCenter(GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
            controller.setZoom(DEFAULT_ZOOM_LEVEL)
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

    LaunchedEffect(state.participants) {
        mapView.overlayManager.overlays()
            .filterIsInstance<Marker>()
            .forEach { mapView.overlayManager.remove(it) }
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
        topBar = {
            TopAppBar(
                title = { Text(state.event?.name ?: "Field Activity") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToMessages) {
                        Text("Messages")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val center = mapView.mapCenter
                    viewModel.triggerSOS(eventId, center.latitude, center.longitude)
                },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "SOS")
                    Spacer(Modifier.width(8.dp))
                    Text("SOS / מצוקה")
                }
            }
        }
    ) { padding ->
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}