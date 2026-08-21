package com.arielfaridja.ezrahi.core.network.transport

import com.arielfaridja.ezrahi.data.local.OutboxDao
import com.arielfaridja.ezrahi.data.local.OutboxRecord
import com.arielfaridja.ezrahi.domain.model.FieldMessage
import com.arielfaridja.ezrahi.domain.model.FieldReport
import com.arielfaridja.ezrahi.domain.model.TelemetryUpdate
import com.arielfaridja.ezrahi.domain.model.UserRole
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TacticalDispatchEngineTest {

    private val noopLogger = TransportErrorLogger { _, _, _ -> }

    private fun TestScope.engineScope() = CoroutineScope(StandardTestDispatcher(testScheduler))

    private fun sosMessage(eventId: String = "event-1") = FieldMessage(
        id = "SOS_1",
        eventId = eventId,
        senderId = "user-1",
        senderName = "Tester",
        senderRole = UserRole.MEMBER,
        messageText = "EMERGENCY SOS",
        isEmergency = true,
        timestamp = 1_000L
    )

    @Test
    fun telemetry_channel_drops_oldest_when_consumer_is_slow() = runTest {
        val adapter = BlockingTelemetryAdapter()
        val engine = TacticalDispatchEngine(
            adapters = setOf(adapter),
            outboxDao = FakeOutboxDao(),
            errorLogger = noopLogger,
            scope = engineScope()
        )

        repeat(5) { index ->
            engine.dispatchTelemetry("event-1", TelemetryUpdate(userId = "user-$index", eventId = "event-1"))
        }
        runCurrent()
        assertEquals(0, adapter.received.size)

        adapter.release.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, adapter.received.size)
        assertEquals("user-4", adapter.received.single().userId)
    }

    @Test
    fun sos_failure_enqueues_to_outbox_and_drains_after_recovery() = runTest {
        val dao = FakeOutboxDao()
        val adapter = FailingThenWorkingAdapter()
        val engine = TacticalDispatchEngine(
            adapters = setOf(adapter),
            outboxDao = dao,
            errorLogger = noopLogger,
            scope = engineScope()
        )

        engine.dispatchMessage("event-1", sosMessage())
        advanceUntilIdle()

        assertEquals(1, dao.records.size)
        assertEquals(1, dao.records[0].priority)
        assertEquals(OutboxPayloadType.EMERGENCY, dao.records[0].payloadType)
        assertEquals(0, adapter.sentMessages.size)

        adapter.available = true
        val sent = engine.flushOutbox()

        assertEquals(1, sent)
        assertTrue(dao.records.isEmpty())
        assertEquals(1, adapter.sentMessages.size)
        assertTrue(adapter.sentMessages[0].second.isEmergency)
    }

    @Test
    fun flushOutbox_bumps_retryCount_when_transport_still_offline() = runTest {
        val dao = FakeOutboxDao()
        val adapter = FailingThenWorkingAdapter()
        val engine = TacticalDispatchEngine(
            adapters = setOf(adapter),
            outboxDao = dao,
            errorLogger = noopLogger,
            scope = engineScope()
        )

        engine.dispatchMessage("event-1", sosMessage())
        advanceUntilIdle()
        assertEquals(0, dao.records[0].retryCount)

        engine.flushOutbox()
        assertEquals(0, adapter.sentMessages.size)
        assertEquals(1, dao.records[0].retryCount)
    }

    private class BlockingTelemetryAdapter : TacticalTransportAdapter {
        val release = CompletableDeferred<Unit>()
        val received = mutableListOf<TelemetryUpdate>()

        override val bearer: TransportBearer = TransportBearer.CELLULAR_FIREBASE
        override val capabilities: Flow<TransportCapabilities> =
            flowOf(TransportCapabilities(bearer, true, 1024, true, 100))

        override suspend fun sendTelemetry(eventId: String, telemetry: TelemetryUpdate): Boolean {
            release.await()
            received.add(telemetry)
            return true
        }

        override suspend fun sendEmergency(eventId: String, message: FieldMessage): Boolean = false
        override suspend fun sendReport(eventId: String, report: FieldReport): Boolean = false
        override suspend fun sendMessage(eventId: String, message: FieldMessage): Boolean = false
        override fun observeIncomingTelemetry(eventId: String): Flow<TelemetryUpdate> = emptyFlow()
        override fun observeIncomingEmergency(eventId: String): Flow<FieldMessage> = emptyFlow()
        override fun observeIncomingReports(eventId: String): Flow<FieldReport> = emptyFlow()
        override fun observeIncomingMessages(eventId: String): Flow<FieldMessage> = emptyFlow()
    }

    private class FailingThenWorkingAdapter : TacticalTransportAdapter {
        var available: Boolean = false
        val sentMessages = mutableListOf<Pair<String, FieldMessage>>()

        override val bearer: TransportBearer = TransportBearer.CELLULAR_FIREBASE
        override val capabilities: Flow<TransportCapabilities> =
            flowOf(TransportCapabilities(bearer, available, 1024, true, 100))

        override suspend fun sendTelemetry(eventId: String, telemetry: TelemetryUpdate): Boolean = available
        override suspend fun sendEmergency(eventId: String, message: FieldMessage): Boolean {
            if (!available) return false
            sentMessages.add(eventId to message)
            return true
        }

        override suspend fun sendReport(eventId: String, report: FieldReport): Boolean = available
        override suspend fun sendMessage(eventId: String, message: FieldMessage): Boolean {
            if (!available) return false
            sentMessages.add(eventId to message)
            return true
        }

        override fun observeIncomingTelemetry(eventId: String): Flow<TelemetryUpdate> = emptyFlow()
        override fun observeIncomingEmergency(eventId: String): Flow<FieldMessage> = emptyFlow()
        override fun observeIncomingReports(eventId: String): Flow<FieldReport> = emptyFlow()
        override fun observeIncomingMessages(eventId: String): Flow<FieldMessage> = emptyFlow()
    }

    private class FakeOutboxDao : OutboxDao {
        val records = mutableListOf<OutboxRecord>()
        private var nextId = 1L

        override suspend fun insert(record: OutboxRecord): Long {
            val id = nextId++
            records.add(record.copy(id = id))
            return id
        }

        override suspend fun getPending(limit: Int): List<OutboxRecord> =
            records.sortedWith(compareBy({ it.priority }, { it.createdAtTimestamp })).take(limit)

        override suspend fun delete(id: Long) {
            records.removeAll { it.id == id }
        }

        override suspend fun updateRetryCount(id: Long, retryCount: Int) {
            records.replaceAll { if (it.id == id) it.copy(retryCount = retryCount) else it }
        }

        override suspend fun countPending(): Int = records.size
    }
}