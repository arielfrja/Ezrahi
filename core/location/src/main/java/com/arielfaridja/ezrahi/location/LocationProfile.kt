package com.arielfaridja.ezrahi.location

import com.google.android.gms.location.Priority

enum class LocationProfile(
    val intervalMillis: Long,
    val minUpdateIntervalMillis: Long,
    val minDistanceMeters: Float,
    val priority: Int
) {
    STATIONARY(
        intervalMillis = 60_000L,        // 1 min
        minUpdateIntervalMillis = 30_000L,
        minDistanceMeters = 25f,
        priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
    ),
    FOOT_PATROL(
        intervalMillis = 5_000L,         // 5 sec
        minUpdateIntervalMillis = 2_500L,
        minDistanceMeters = 5f,
        priority = Priority.PRIORITY_HIGH_ACCURACY
    ),
    VEHICLE(
        intervalMillis = 2_000L,         // 2 sec
        minUpdateIntervalMillis = 1_000L,
        minDistanceMeters = 10f,
        priority = Priority.PRIORITY_HIGH_ACCURACY
    ),
    EMERGENCY_SOS(
        intervalMillis = 1_000L,         // 1 sec
        minUpdateIntervalMillis = 500L,
        minDistanceMeters = 1f,
        priority = Priority.PRIORITY_HIGH_ACCURACY
    );

    fun toProfileData(isLowPower: Boolean): LocationProfileData {
        if (!isLowPower || this == EMERGENCY_SOS) {
            return LocationProfileData(
                profile = this,
                intervalMillis = intervalMillis,
                minUpdateIntervalMillis = minUpdateIntervalMillis,
                minDistanceMeters = minDistanceMeters,
                priority = priority,
                isLowPowerActive = false
            )
        }
        return when (this) {
            STATIONARY -> LocationProfileData(
                profile = this,
                intervalMillis = 120_000L, // 2 min
                minUpdateIntervalMillis = 60_000L,
                minDistanceMeters = 50f,
                priority = Priority.PRIORITY_LOW_POWER,
                isLowPowerActive = true
            )
            FOOT_PATROL -> LocationProfileData(
                profile = this,
                intervalMillis = 15_000L,  // 15 sec
                minUpdateIntervalMillis = 7_500L,
                minDistanceMeters = 15f,
                priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                isLowPowerActive = true
            )
            VEHICLE -> LocationProfileData(
                profile = this,
                intervalMillis = 5_000L,   // 5 sec
                minUpdateIntervalMillis = 2_500L,
                minDistanceMeters = 20f,
                priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                isLowPowerActive = true
            )
            EMERGENCY_SOS -> LocationProfileData(
                profile = this,
                intervalMillis = 1_000L,
                minUpdateIntervalMillis = 500L,
                minDistanceMeters = 1f,
                priority = Priority.PRIORITY_HIGH_ACCURACY,
                isLowPowerActive = false
            )
        }
    }
}

data class LocationProfileData(
    val profile: LocationProfile,
    val intervalMillis: Long,
    val minUpdateIntervalMillis: Long,
    val minDistanceMeters: Float,
    val priority: Int,
    val isLowPowerActive: Boolean
)
