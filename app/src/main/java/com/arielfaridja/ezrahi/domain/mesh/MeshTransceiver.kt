package com.arielfaridja.ezrahi.domain.mesh

import com.arielfaridja.ezrahi.domain.model.FieldMessage
import com.arielfaridja.ezrahi.domain.model.GeoPoint
import kotlinx.coroutines.flow.Flow

interface MeshTransceiver {
    fun isDeviceConnected(): Flow<Boolean>
    suspend fun connectToNode(bluetoothAddress: String): Result<Unit>
    suspend fun broadcastLocationPacket(location: GeoPoint): Result<Unit>
    suspend fun broadcastEmergencyPacket(message: FieldMessage): Result<Unit>
    fun observeIncomingPackets(): Flow<FieldMessage>
}