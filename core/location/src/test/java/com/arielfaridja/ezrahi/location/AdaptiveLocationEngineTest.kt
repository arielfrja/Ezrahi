package com.arielfaridja.ezrahi.location

import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdaptiveLocationEngineTest {

    private lateinit var engine: AdaptiveLocationEngine

    @Before
    fun setUp() {
        engine = AdaptiveLocationEngine()
    }

    @Test
    fun `test speed resolution boundaries`() {
        assertEquals(LocationProfile.STATIONARY, engine.resolveProfileFromSpeed(0.0f))
        assertEquals(LocationProfile.STATIONARY, engine.resolveProfileFromSpeed(0.49f))
        assertEquals(LocationProfile.FOOT_PATROL, engine.resolveProfileFromSpeed(0.5f))
        assertEquals(LocationProfile.FOOT_PATROL, engine.resolveProfileFromSpeed(1.8f))
        assertEquals(LocationProfile.FOOT_PATROL, engine.resolveProfileFromSpeed(2.5f))
        assertEquals(LocationProfile.VEHICLE, engine.resolveProfileFromSpeed(2.51f))
        assertEquals(LocationProfile.VEHICLE, engine.resolveProfileFromSpeed(15.0f))
    }

    @Test
    fun `test profile to low power transformations`() {
        val stationaryNormal = LocationProfile.STATIONARY.toProfileData(isLowPower = false)
        assertEquals(60_000L, stationaryNormal.intervalMillis)
        assertEquals(25f, stationaryNormal.minDistanceMeters)
        assertFalse(stationaryNormal.isLowPowerActive)

        val stationaryLow = LocationProfile.STATIONARY.toProfileData(isLowPower = true)
        assertEquals(120_000L, stationaryLow.intervalMillis)
        assertEquals(50f, stationaryLow.minDistanceMeters)
        assertEquals(Priority.PRIORITY_LOW_POWER, stationaryLow.priority)
        assertTrue(stationaryLow.isLowPowerActive)

        val footNormal = LocationProfile.FOOT_PATROL.toProfileData(isLowPower = false)
        assertEquals(5_000L, footNormal.intervalMillis)
        assertEquals(5f, footNormal.minDistanceMeters)

        val footLow = LocationProfile.FOOT_PATROL.toProfileData(isLowPower = true)
        assertEquals(15_000L, footLow.intervalMillis)
        assertEquals(15f, footLow.minDistanceMeters)
        assertEquals(Priority.PRIORITY_BALANCED_POWER_ACCURACY, footLow.priority)
        assertTrue(footLow.isLowPowerActive)

        val vehicleNormal = LocationProfile.VEHICLE.toProfileData(isLowPower = false)
        assertEquals(2_000L, vehicleNormal.intervalMillis)
        assertEquals(10f, vehicleNormal.minDistanceMeters)

        val vehicleLow = LocationProfile.VEHICLE.toProfileData(isLowPower = true)
        assertEquals(5_000L, vehicleLow.intervalMillis)
        assertEquals(20f, vehicleLow.minDistanceMeters)
        assertTrue(vehicleLow.isLowPowerActive)
    }

    @Test
    fun `test activity recognition state transitions`() {
        engine.onActivityTransitionDetected(DetectedActivity.WALKING)
        assertEquals(LocationProfile.FOOT_PATROL, engine.currentProfile.value)

        engine.onActivityTransitionDetected(DetectedActivity.IN_VEHICLE)
        assertEquals(LocationProfile.VEHICLE, engine.currentProfile.value)

        engine.onActivityTransitionDetected(DetectedActivity.STILL)
        assertEquals(LocationProfile.STATIONARY, engine.currentProfile.value)
    }

    @Test
    fun `test emergency sos overrides low power mode`() {
        engine.setLowPowerMode(true)
        assertTrue(engine.effectiveConfig.value.isLowPowerActive)

        engine.setEmergencySos(true)
        assertEquals(LocationProfile.EMERGENCY_SOS, engine.effectiveConfig.value.profile)
        assertEquals(1_000L, engine.effectiveConfig.value.intervalMillis)
        assertEquals(1f, engine.effectiveConfig.value.minDistanceMeters)
        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, engine.effectiveConfig.value.priority)

        // Emergency SOS should NEVER throttle in low power
        assertFalse(engine.effectiveConfig.value.isLowPowerActive)

        engine.setEmergencySos(false)
        assertTrue(engine.effectiveConfig.value.isLowPowerActive)
    }
}
