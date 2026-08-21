package com.arielfaridja.ezrahi.core.network

import com.arielfaridja.ezrahi.domain.model.EntityLivenessState
import com.arielfaridja.ezrahi.domain.model.EventParticipant
import com.arielfaridja.ezrahi.domain.model.StalenessConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class StalenessEngineTest {

    private val config = StalenessConfig(staleThresholdMinutes = 5, disconnectedThresholdMinutes = 15, expiredThresholdMinutes = 30)
    private val now = 1_000_000L

    @Test
    fun compute_activeWithinStaleThreshold() {
        assertEquals(EntityLivenessState.ACTIVE, EntityLivenessState.compute(now - 4 * 60_000L, config, now))
    }

    @Test
    fun compute_staleBetweenStaleAndDisconnected() {
        assertEquals(EntityLivenessState.STALE, EntityLivenessState.compute(now - 10 * 60_000L, config, now))
    }

    @Test
    fun compute_disconnectedBetweenDisconnectedAndExpired() {
        assertEquals(EntityLivenessState.DISCONNECTED, EntityLivenessState.compute(now - 20 * 60_000L, config, now))
    }

    @Test
    fun compute_expiredBeyondExpiredThreshold() {
        assertEquals(EntityLivenessState.EXPIRED, EntityLivenessState.compute(now - 40 * 60_000L, config, now))
    }

    @Test
    fun effectiveState_manualOverrideTakesPrecedence() {
        val participant = EventParticipant(
            userId = "u1",
            lastSeenTimestamp = now - 40 * 60_000L,
            manualStateOverride = EntityLivenessState.ACTIVE
        )
        assertEquals(EntityLivenessState.ACTIVE, participant.effectiveState(config, now))
    }

    @Test
    fun effectiveState_fallsBackToComputedWhenNoOverride() {
        val participant = EventParticipant(userId = "u1", lastSeenTimestamp = now - 40 * 60_000L)
        assertEquals(EntityLivenessState.EXPIRED, participant.effectiveState(config, now))
    }

    @Test
    fun effectiveState_clearOverrideReturnsToComputed() {
        val participant = EventParticipant(
            userId = "u1",
            lastSeenTimestamp = now - 2 * 60_000L,
            manualStateOverride = null
        )
        assertEquals(EntityLivenessState.ACTIVE, participant.effectiveState(config, now))
    }

    @Test
    fun seedTimestampFor_producesActive() {
        val seed = config.seedTimestampFor(EntityLivenessState.ACTIVE, now)
        assertEquals(EntityLivenessState.ACTIVE, EntityLivenessState.compute(seed, config, now))
    }

    @Test
    fun seedTimestampFor_producesStale() {
        val seed = config.seedTimestampFor(EntityLivenessState.STALE, now)
        assertEquals(EntityLivenessState.STALE, EntityLivenessState.compute(seed, config, now))
    }

    @Test
    fun seedTimestampFor_producesDisconnected() {
        val seed = config.seedTimestampFor(EntityLivenessState.DISCONNECTED, now)
        assertEquals(EntityLivenessState.DISCONNECTED, EntityLivenessState.compute(seed, config, now))
    }

    @Test
    fun seedTimestampFor_producesExpired() {
        val seed = config.seedTimestampFor(EntityLivenessState.EXPIRED, now)
        assertEquals(EntityLivenessState.EXPIRED, EntityLivenessState.compute(seed, config, now))
    }
}
