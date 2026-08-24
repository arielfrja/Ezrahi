package com.arielfaridja.ezrahi.location

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double,
    val accuracyMeters: Float,
    val speedMps: Float,
    val timestamp: Long
)

@Singleton
class AdaptiveLocationEngine @Inject constructor(
    @ApplicationContext private val context: Context?
) {
    constructor() : this(null)

    companion object {
        private const val TAG = "AdaptiveLocationEngine"
        const val SPEED_STATIONARY_MAX_MPS = 0.5f
        const val SPEED_FOOT_PATROL_MAX_MPS = 2.5f
    }

    private val _currentProfile = MutableStateFlow(LocationProfile.STATIONARY)
    val currentProfile: StateFlow<LocationProfile> = _currentProfile.asStateFlow()

    private val _isLowPower = MutableStateFlow(false)
    val isLowPower: StateFlow<Boolean> = _isLowPower.asStateFlow()

    private val _isEmergencySos = MutableStateFlow(false)
    val isEmergencySos: StateFlow<Boolean> = _isEmergencySos.asStateFlow()

    private val _effectiveConfig = MutableStateFlow(
        LocationProfile.STATIONARY.toProfileData(isLowPower = false)
    )
    val effectiveConfig: StateFlow<LocationProfileData> = _effectiveConfig.asStateFlow()

    private val _lastFix = MutableStateFlow<GpsFix?>(null)
    val lastFix: StateFlow<GpsFix?> = _lastFix.asStateFlow()

    private val _batteryPercent = MutableStateFlow<Int?>(null)
    val batteryPercent: StateFlow<Int?> = _batteryPercent.asStateFlow()

    fun setEmergencySos(active: Boolean) {
        _isEmergencySos.value = active
        reevaluateConfig()
    }

    fun setLowPowerMode(lowPower: Boolean) {
        _isLowPower.value = lowPower
        reevaluateConfig()
    }

    fun setBatteryPercent(percent: Int) {
        _batteryPercent.value = percent
    }

    fun onActivityTransitionDetected(detectedActivityType: Int) {
        val newProfile = when (detectedActivityType) {
            DetectedActivity.STILL -> LocationProfile.STATIONARY
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.ON_FOOT -> LocationProfile.FOOT_PATROL
            DetectedActivity.IN_VEHICLE -> LocationProfile.VEHICLE
            else -> return
        }
        if (_currentProfile.value != newProfile) {
            Log.d(TAG, "Activity transition detected -> Profile: $newProfile")
            _currentProfile.value = newProfile
            reevaluateConfig()
        }
    }

    fun onLocationReceived(location: Location) {
        val previous = _lastFix.value
        val current = GpsFix(
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMeters = if (location.hasAltitude()) location.altitude else 0.0,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else 0f,
            speedMps = if (location.hasSpeed()) location.speed else 0f,
            timestamp = location.time
        )
        _lastFix.value = current

        if (_isEmergencySos.value) return

        val speedMps = when {
            location.hasSpeed() -> location.speed
            previous != null -> displacementSpeedMps(previous, current)
            else -> 0f
        }
        val inferredProfile = resolveProfileFromSpeed(speedMps)
        if (_currentProfile.value != inferredProfile) {
            Log.d(TAG, "Inferred speed $speedMps m/s -> Profile: $inferredProfile")
            _currentProfile.value = inferredProfile
            reevaluateConfig()
        }
    }

    private fun displacementSpeedMps(prev: GpsFix, cur: GpsFix): Float {
        val dtSeconds = (cur.timestamp - prev.timestamp) / 1000.0
        if (dtSeconds < 1.0 || dtSeconds > 120.0) return 0f
        val dLat = Math.toRadians(cur.latitude - prev.latitude)
        val dLon = Math.toRadians(cur.longitude - prev.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(prev.latitude)) * cos(Math.toRadians(cur.latitude)) *
            sin(dLon / 2) * sin(dLon / 2)
        val meters = 2 * 6371000.0 * Math.atan2(sqrt(a), sqrt(1 - a))
        return (meters / dtSeconds).toFloat()
    }

    fun resolveProfileFromSpeed(speedMps: Float): LocationProfile {
        return when {
            speedMps < SPEED_STATIONARY_MAX_MPS -> LocationProfile.STATIONARY
            speedMps <= SPEED_FOOT_PATROL_MAX_MPS -> LocationProfile.FOOT_PATROL
            else -> LocationProfile.VEHICLE
        }
    }

    private fun reevaluateConfig() {
        val baseProfile = if (_isEmergencySos.value) {
            LocationProfile.EMERGENCY_SOS
        } else {
            _currentProfile.value
        }
        val configData = baseProfile.toProfileData(_isLowPower.value)
        _effectiveConfig.value = configData
    }

    fun buildLocationRequest(config: LocationProfileData): LocationRequest {
        return LocationRequest.Builder(config.priority, config.intervalMillis)
            .setMinUpdateIntervalMillis(config.minUpdateIntervalMillis)
            .setMinUpdateDistanceMeters(config.minDistanceMeters)
            .build()
    }
}
