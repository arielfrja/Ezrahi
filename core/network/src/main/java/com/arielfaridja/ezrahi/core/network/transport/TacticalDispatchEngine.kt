package com.arielfaridja.ezrahi.core.network.transport

import com.arielfaridja.ezrahi.data.local.OutboxDao
import com.arielfaridja.ezrahi.data.local.OutboxRecord
import com.arielfaridja.ezrahi.domain.model.FieldMessage
import com.arielfaridja.ezrahi.domain.model.FieldReport
import com.arielfaridja.ezrahi.domain.model.FieldReportStatus
import com.arielfaridja.ezrahi.domain.model.FieldReportType
import com.arielfaridja.ezrahi.domain.model.GeoPoint
import com.arielfaridja.ezrahi.domain.model.TelemetryUpdate
import com.arielfaridja.ezrahi.domain.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.jvm.JvmSuppressWildcards
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

object OutboxPayloadType {
    const val EMERGENCY = "EMERGENCY"
    const val MESSAGE = "MESSAGE"
    const val REPORT = "REPORT"
}

@Singleton
class TacticalDispatchEngine @Inject constructor(
    private val adapters: @JvmSuppressWildcards Set<TacticalTransportAdapter>,
    private val outboxDao: OutboxDao,
    private val errorLogger: TransportErrorLogger,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    companion object {
        private const val MAX_RETRY_COUNT = 15
    }

    private val orderedAdapters: List<TacticalTransportAdapter> = adapters.sortedBy { it.bearer.ordinal }

    internal val telemetryChannel = Channel<TelemetryUpdate>(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    internal val messageChannel = Channel<FieldMessage>(capacity = Channel.UNLIMITED)
    internal val reportChannel = Channel<FieldReport>(capacity = Channel.UNLIMITED)

    init {
        scope.launch { runConsumers() }
    }

    fun dispatchTelemetry(eventId: String, telemetry: TelemetryUpdate) {
        telemetryChannel.trySend(telemetry)
    }

    fun dispatchMessage(eventId: String, message: FieldMessage) {
        messageChannel.trySend(message)
    }

    fun dispatchReport(eventId: String, report: FieldReport) {
        reportChannel.trySend(report)
    }

    suspend fun flushOutbox(limit: Int = 100): Int {
        val pending = outboxDao.getPending(limit)
        var sent = 0
        for (record in pending) {
            if (record.retryCount >= MAX_RETRY_COUNT) {
                outboxDao.delete(record.id)
                continue
            }
            if (deliver(record)) {
                outboxDao.delete(record.id)
                sent++
            } else {
                outboxDao.updateRetryCount(record.id, record.retryCount + 1)
            }
        }
        return sent
    }

    suspend fun pendingCount(): Int = outboxDao.countPending()

    private suspend fun runConsumers() = coroutineScope {
        launch { consumeTelemetry() }
        launch { consumeMessages() }
        launch { consumeReports() }
    }

    private suspend fun consumeTelemetry() {
        for (telemetry in telemetryChannel) {
            orderedAdapters.any { adapter ->
                runCatching { adapter.sendTelemetry(telemetry.eventId, telemetry) }
                    .onFailure { errorLogger.logError(it, telemetry.eventId, "telemetry") }
                    .getOrDefault(false)
            }
        }
    }

    private suspend fun consumeMessages() {
        for (message in messageChannel) {
            val delivered = orderedAdapters.any { adapter ->
                runCatching { adapter.sendMessage(message.eventId, message) }
                    .onFailure { errorLogger.logError(it, message.eventId, "message") }
                    .getOrDefault(false)
            }
            if (!delivered) enqueue(message)
        }
    }

    private suspend fun consumeReports() {
        for (report in reportChannel) {
            val delivered = orderedAdapters.any { adapter ->
                runCatching { adapter.sendReport(report.actId, report) }
                    .onFailure { errorLogger.logError(it, report.actId, "report") }
                    .getOrDefault(false)
            }
            if (!delivered) enqueue(report)
        }
    }

    private suspend fun enqueue(message: FieldMessage) {
        runCatching {
            outboxDao.insert(
                OutboxRecord(
                    eventId = message.eventId,
                    priority = if (message.isEmergency) 1 else 3,
                    payloadType = if (message.isEmergency) OutboxPayloadType.EMERGENCY else OutboxPayloadType.MESSAGE,
                    payloadJson = encodeMessage(message)
                )
            )
        }.onFailure { errorLogger.logError(it, message.eventId, "outbox") }
    }

    private suspend fun enqueue(report: FieldReport) {
        runCatching {
            outboxDao.insert(
                OutboxRecord(
                    eventId = report.actId,
                    priority = 2,
                    payloadType = OutboxPayloadType.REPORT,
                    payloadJson = encodeReport(report)
                )
            )
        }.onFailure { errorLogger.logError(it, report.actId, "outbox") }
    }

    private suspend fun deliver(record: OutboxRecord): Boolean = when (record.payloadType) {
        OutboxPayloadType.EMERGENCY -> {
            val message = decodeMessage(record.payloadJson) ?: return false
            orderedAdapters.any { adapter ->
                runCatching { adapter.sendEmergency(record.eventId, message) }
                    .onFailure { errorLogger.logError(it, record.eventId, "outbox-emergency") }
                    .getOrDefault(false)
            }
        }
        OutboxPayloadType.MESSAGE -> {
            val message = decodeMessage(record.payloadJson) ?: return false
            orderedAdapters.any { adapter ->
                runCatching { adapter.sendMessage(record.eventId, message) }
                    .onFailure { errorLogger.logError(it, record.eventId, "outbox-message") }
                    .getOrDefault(false)
            }
        }
        OutboxPayloadType.REPORT -> {
            val report = decodeReport(record.payloadJson) ?: return false
            orderedAdapters.any { adapter ->
                runCatching { adapter.sendReport(record.eventId, report) }
                    .onFailure { errorLogger.logError(it, record.eventId, "outbox-report") }
                    .getOrDefault(false)
            }
        }
        else -> false
    }

    private fun encodeMessage(message: FieldMessage): String = JSONObject().apply {
        put("id", message.id)
        put("eventId", message.eventId)
        put("senderId", message.senderId)
        put("senderName", message.senderName)
        put("senderRole", message.senderRole.name)
        val targetRole = message.targetRole
        if (targetRole != null) put("targetRole", targetRole.name) else put("targetRole", JSONObject.NULL)
        put("messageText", message.messageText)
        put("isEmergency", message.isEmergency)
        put("timestamp", message.timestamp)
    }.toString()

    private fun decodeMessage(json: String): FieldMessage? = runCatching {
        val obj = JSONObject(json)
        FieldMessage(
            id = obj.optString("id"),
            eventId = obj.optString("eventId"),
            senderId = obj.optString("senderId"),
            senderName = obj.optString("senderName"),
            senderRole = runCatching { UserRole.valueOf(obj.optString("senderRole")) }
                .getOrDefault(UserRole.MEMBER),
            targetRole = if (obj.isNull("targetRole")) null
            else runCatching { UserRole.valueOf(obj.optString("targetRole")) }.getOrNull(),
            messageText = obj.optString("messageText"),
            isEmergency = obj.optBoolean("isEmergency", false),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
        )
    }.onFailure { errorLogger.logError(it, null, "outbox-decode") }.getOrNull()

    private fun encodeReport(report: FieldReport): String = JSONObject().apply {
        put("id", report.id)
        put("actId", report.actId)
        put("reporterId", report.reporterId)
        put("title", report.title)
        put("description", report.description)
        put("latitude", report.location.latitude)
        put("longitude", report.location.longitude)
        put("reportTime", report.reportTime)
        put("status", report.status.value)
        put("type", report.type.value)
    }.toString()

    private fun decodeReport(json: String): FieldReport? = runCatching {
        val obj = JSONObject(json)
        FieldReport(
            id = obj.optString("id"),
            actId = obj.optString("actId"),
            reporterId = obj.optString("reporterId"),
            title = obj.optString("title"),
            description = obj.optString("description"),
            location = GeoPoint(
                latitude = obj.optDouble("latitude", 0.0),
                longitude = obj.optDouble("longitude", 0.0)
            ),
            reportTime = obj.optLong("reportTime", System.currentTimeMillis()),
            status = FieldReportStatus.getByValue(obj.optInt("status", -1)),
            type = FieldReportType.getByValue(obj.optInt("type", -1))
        )
    }.onFailure { errorLogger.logError(it, null, "outbox-decode") }.getOrNull()
}