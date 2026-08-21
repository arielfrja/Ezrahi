package com.arielfaridja.ezrahi.core.network.transport

import com.arielfaridja.ezrahi.data.mapper.FieldReportMapper
import com.arielfaridja.ezrahi.domain.model.FieldMessage
import com.arielfaridja.ezrahi.domain.model.FieldReport
import com.arielfaridja.ezrahi.domain.model.GeoPoint
import com.arielfaridja.ezrahi.domain.model.TelemetryUpdate
import com.arielfaridja.ezrahi.domain.model.UserRole
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTransportAdapter @Inject constructor(
    private val firestore: FirebaseFirestore
) : TacticalTransportAdapter {

    override val bearer: TransportBearer = TransportBearer.CELLULAR_FIREBASE

    override val capabilities: Flow<TransportCapabilities> = flowOf(
        TransportCapabilities(
            bearer = bearer,
            isAvailable = true,
            maxPayloadBytes = 1_048_576,
            supportsStreaming = true,
            estimatedBandwidthKbps = 1_024
        )
    )

    override suspend fun sendTelemetry(eventId: String, telemetry: TelemetryUpdate): Boolean = runCatching {
        firestore.collection("events").document(eventId)
            .collection("participants").document(telemetry.userId)
            .update(
                mapOf(
                    "latitude" to telemetry.location.latitude,
                    "longitude" to telemetry.location.longitude,
                    "lastSeenTimestamp" to telemetry.location.timestamp
                )
            ).await()
        true
    }.getOrDefault(false)

    override suspend fun sendEmergency(eventId: String, message: FieldMessage): Boolean =
        sendMessage(eventId, message.copy(isEmergency = true))

    override suspend fun sendReport(eventId: String, report: FieldReport): Boolean = runCatching {
        val docId = report.id.ifEmpty { firestore.collection("Reports").document().id }
        firestore.collection("Reports").document(docId)
            .set(FieldReportMapper.toWriteMap(report)).await()
        true
    }.getOrDefault(false)

    override suspend fun sendMessage(eventId: String, message: FieldMessage): Boolean = runCatching {
        val docId = message.id.ifEmpty { firestore.collection("events").document().id }
        firestore.collection("events").document(eventId)
            .collection("messages").document(docId)
            .set(
                mapOf(
                    "id" to docId,
                    "eventId" to eventId,
                    "senderId" to message.senderId,
                    "senderName" to message.senderName,
                    "senderRole" to message.senderRole.name,
                    "targetRole" to message.targetRole?.name,
                    "messageText" to message.messageText,
                    "isEmergency" to message.isEmergency,
                    "timestamp" to message.timestamp
                )
            ).await()
        true
    }.getOrDefault(false)

    override fun observeIncomingMessages(eventId: String): Flow<FieldMessage> = callbackFlow {
        val registration = firestore.collection("events").document(eventId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    snapshot.documentChanges.forEach { change ->
                        if (change.type == DocumentChange.Type.ADDED || change.type == DocumentChange.Type.MODIFIED) {
                            val doc = change.document
                            trySend(
                                FieldMessage(
                                    id = doc.id,
                                    eventId = eventId,
                                    senderId = doc.getString("senderId") ?: "",
                                    senderName = doc.getString("senderName") ?: "",
                                    senderRole = runCatching { UserRole.valueOf(doc.getString("senderRole") ?: "") }
                                        .getOrDefault(UserRole.MEMBER),
                                    targetRole = doc.getString("targetRole")?.let { role ->
                                        runCatching { UserRole.valueOf(role) }.getOrNull()
                                    },
                                    messageText = doc.getString("messageText") ?: "",
                                    isEmergency = doc.getBoolean("isEmergency") ?: false,
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    override fun observeIncomingEmergency(eventId: String): Flow<FieldMessage> = callbackFlow {
        val registration = firestore.collection("events").document(eventId).collection("messages")
            .whereEqualTo("isEmergency", true)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    snapshot.documentChanges.forEach { change ->
                        if (change.type == DocumentChange.Type.ADDED || change.type == DocumentChange.Type.MODIFIED) {
                            val doc = change.document
                            trySend(
                                FieldMessage(
                                    id = doc.id,
                                    eventId = eventId,
                                    senderId = doc.getString("senderId") ?: "",
                                    senderName = doc.getString("senderName") ?: "",
                                    senderRole = runCatching { UserRole.valueOf(doc.getString("senderRole") ?: "") }
                                        .getOrDefault(UserRole.MEMBER),
                                    targetRole = doc.getString("targetRole")?.let { role ->
                                        runCatching { UserRole.valueOf(role) }.getOrNull()
                                    },
                                    messageText = doc.getString("messageText") ?: "",
                                    isEmergency = true,
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    override fun observeIncomingReports(eventId: String): Flow<FieldReport> = callbackFlow {
        val registration = firestore.collection("Reports")
            .whereEqualTo("ActId", eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    snapshot.documentChanges.forEach { change ->
                        if (change.type == DocumentChange.Type.ADDED || change.type == DocumentChange.Type.MODIFIED) {
                            FieldReportMapper.fromSnapshot(change.document)?.let { trySend(it) }
                        }
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    override fun observeIncomingTelemetry(eventId: String): Flow<TelemetryUpdate> = callbackFlow {
        val registration = firestore.collection("events").document(eventId).collection("participants")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    snapshot.documentChanges.forEach { change ->
                        if (change.type == DocumentChange.Type.ADDED || change.type == DocumentChange.Type.MODIFIED) {
                            val doc = change.document
                            trySend(
                                TelemetryUpdate(
                                    userId = doc.id,
                                    eventId = eventId,
                                    location = GeoPoint(
                                        latitude = doc.getDouble("latitude") ?: 0.0,
                                        longitude = doc.getDouble("longitude") ?: 0.0,
                                        timestamp = doc.getLong("lastSeenTimestamp") ?: System.currentTimeMillis()
                                    )
                                )
                            )
                        }
                    }
                }
            }
        awaitClose { registration.remove() }
    }
}