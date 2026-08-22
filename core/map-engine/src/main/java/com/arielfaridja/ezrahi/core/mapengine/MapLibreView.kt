package com.arielfaridja.ezrahi.core.mapengine

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@Composable
fun MapLibreView(
    styleUri: String,
    modifier: Modifier = Modifier,
    onMapReady: (MapLibreMap) -> Unit = {},
    onLongClick: (LatLng) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    remember(context) { MapLibre.getInstance(context) }

    val mapView = remember {
        MapView(context).also { mv ->
            runCatching { mv.onCreate(null) }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            runCatching {
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> {}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { mapView.onDestroy() }
        }
    }

    LaunchedEffect(mapView, styleUri) {
        runCatching {
            mapView.getMapAsync { map ->
                try {
                    map.setStyle(Style.Builder().fromUri(styleUri)) { style ->
                        runCatching {
                            MapLayers.ensureBaseLayers(style, context)
                            onMapReady(map)
                        }.onFailure { e ->
                            Log.e("MapLibreView", "ensureBaseLayers failed", e)
                        }
                    }
                    map.addOnMapLongClickListener { latLng ->
                        onLongClick(latLng)
                        true
                    }
                } catch (e: Throwable) {
                    Log.e("MapLibreView", "onMapReady setup failed", e)
                }
            }
        }.onFailure { e ->
            Log.e("MapLibreView", "getMapAsync failed", e)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView }
    )
}
