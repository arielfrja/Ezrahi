package com.arielfaridja.ezrahi.core.network.transport

import com.arielfaridja.ezrahi.domain.mesh.MeshTransceiver
import com.arielfaridja.ezrahi.domain.model.FieldMessage
import com.arielfaridja.ezrahi.domain.model.FieldReport
import com.arielfaridja.ezrahi.domain.model.TelemetryUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeshTransportAdapter @Inject constructor() : TacticalTransportAdapter {

    private var transceiver: MeshTransceiver? = null

    override val bearer: TransportBearer = TransportBearer.BLUETOOTH_LE

    override val capabilities: Flow<TransportCapabilities> = flowOf(
        TransportCapabilities(
            bearer = bearer,
            isAvailable = transceiver != null,
            maxPayloadBytes = 512,
            supportsStreaming = false,
            estimatedBandwidthKbps = 5
        )
    )

    fun bindTransceiver(transceiver: MeshTransceiver) {
        this.transceiver = transceiver
    }

    override suspend fun sendTelemetry(eventId: String, telemetry: TelemetryUpdate): Boolean =
        transceiver?.broadcastLocationPacket(telemetry.location)?.isSuccess ?: false

    override suspend fun sendEmergency(eventId: String, message: FieldMessage): Boolean =
        transceiver?.broadcastEmergencyPacket(message)?.isSuccess ?: false

    override suspend fun sendReport(eventId: String, report: FieldReport): Boolean = false

    override suspend fun sendMessage(eventId: String, message: FieldMessage): Boolean = false

    override fun observeIncomingTelemetry(eventId: String): Flow<TelemetryUpdate> = emptyFlow()

    override fun observeIncomingEmergency(eventId: String): Flow<FieldMessage> =
        transceiver?.observeIncomingPackets() ?: emptyFlow()

    override fun observeIncomingReports(eventId: String): Flow<FieldReport> = emptyFlow()

    override fun observeIncomingMessages(eventId: String): Flow<FieldMessage> =
        transceiver?.observeIncomingPackets() ?: emptyFlow()
}