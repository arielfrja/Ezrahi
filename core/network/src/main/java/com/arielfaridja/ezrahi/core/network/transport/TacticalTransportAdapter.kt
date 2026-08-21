package com.arielfaridja.ezrahi.core.network.transport

import com.arielfaridja.ezrahi.domain.model.FieldMessage
import com.arielfaridja.ezrahi.domain.model.FieldReport
import com.arielfaridja.ezrahi.domain.model.TelemetryUpdate
import kotlinx.coroutines.flow.Flow

enum class TransportBearer {
    CELLULAR_FIREBASE,
    LORA_MESH,
    BLUETOOTH_LE,
    LOCAL_WEBSOCKET
}

data class TransportCapabilities(
    val bearer: TransportBearer,
    val isAvailable: Boolean,
    val maxPayloadBytes: Int,
    val supportsStreaming: Boolean,
    val estimatedBandwidthKbps: Int
)

interface TacticalTransportAdapter {
    val bearer: TransportBearer
    val capabilities: Flow<TransportCapabilities>

    suspend fun sendTelemetry(eventId: String, telemetry: TelemetryUpdate): Boolean
    suspend fun sendEmergency(eventId: String, message: FieldMessage): Boolean
    suspend fun sendReport(eventId: String, report: FieldReport): Boolean
    suspend fun sendMessage(eventId: String, message: FieldMessage): Boolean

    fun observeIncomingTelemetry(eventId: String): Flow<TelemetryUpdate>
    fun observeIncomingEmergency(eventId: String): Flow<FieldMessage>
    fun observeIncomingReports(eventId: String): Flow<FieldReport>
    fun observeIncomingMessages(eventId: String): Flow<FieldMessage>
}