package com.arielfaridja.ezrahi.data.repository

import com.arielfaridja.ezrahi.data.local.*
import com.arielfaridja.ezrahi.data.mapper.FieldReportMapper
import com.arielfaridja.ezrahi.domain.model.*
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EzrahiRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val dao: EzrahiDao
) : EzrahiRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun getEventUpdates(eventId: String): Flow<FieldEvent?> {
        // 1. Listen to Firestore and cache locally
        firestore.collection("events").document(eventId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val event = EventLocalEntity(
                        id = snapshot.id,
                        name = snapshot.getString("name") ?: "",
                        managerId = snapshot.getString("managerId") ?: "",
                        managerContact = snapshot.getString("managerContact") ?: "",
                        gpxRouteUrl = snapshot.getString("gpxRouteUrl"),
                        isLive = snapshot.getBoolean("isLive") ?: true
                    )
                    scope.launch { dao.insertEvent(event) }
                }
            }

        // 2. Emit from local Room database (Offline-First)
        return dao.observeEvent(eventId).map { local ->
            local?.let {
                FieldEvent(
                    id = it.id,
                    name = it.name,
                    managerId = it.managerId,
                    managerContact = it.managerContact,
                    gpxRouteUrl = it.gpxRouteUrl,
                    isLive = it.isLive
                )
            }
        }
    }

    override fun getParticipants(eventId: String): Flow<List<EventParticipant>> {
        firestore.collection("events").document(eventId).collection("participants")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        ParticipantLocalEntity(
                            userId = doc.id,
                            eventId = eventId,
                            fullName = doc.getString("fullName") ?: "",
                            phoneNumber = doc.getString("phoneNumber") ?: "",
                            role = doc.getString("role") ?: UserRole.MEMBER.name,
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            isOnline = doc.getBoolean("isOnline") ?: true,
                            lastSeenTimestamp = doc.getLong("lastSeenTimestamp") ?: System.currentTimeMillis()
                        )
                    }
                    scope.launch { dao.insertParticipants(list) }
                }
            }

        return dao.observeParticipants(eventId).map { list ->
            list.map {
                EventParticipant(
                    userId = it.userId,
                    fullName = it.fullName,
                    phoneNumber = it.phoneNumber,
                    role = runCatching { UserRole.valueOf(it.role) }.getOrDefault(UserRole.MEMBER),
                    currentLocation = GeoPoint(it.latitude, it.longitude, it.lastSeenTimestamp),
                    isOnline = it.isOnline,
                    lastSeenTimestamp = it.lastSeenTimestamp
                )
            }
        }
    }

    override fun getMessages(eventId: String): Flow<List<FieldMessage>> {
        firestore.collection("events").document(eventId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    snapshot.documentChanges.forEach { change ->
                        val doc = change.document
                        val msg = MessageLocalEntity(
                            id = doc.id,
                            eventId = eventId,
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            senderRole = doc.getString("senderRole") ?: UserRole.MEMBER.name,
                            targetRole = doc.getString("targetRole"),
                            messageText = doc.getString("messageText") ?: "",
                            isEmergency = doc.getBoolean("isEmergency") ?: false,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                        scope.launch { dao.insertMessage(msg) }
                    }
                }
            }

        return dao.observeMessages(eventId).map { list ->
            list.map {
                FieldMessage(
                    id = it.id,
                    eventId = it.eventId,
                    senderId = it.senderId,
                    senderName = it.senderName,
                    senderRole = runCatching { UserRole.valueOf(it.senderRole) }.getOrDefault(UserRole.MEMBER),
                    targetRole = it.targetRole?.let { roleStr -> runCatching { UserRole.valueOf(roleStr) }.getOrNull() },
                    messageText = it.messageText,
                    isEmergency = it.isEmergency,
                    timestamp = it.timestamp
                )
            }
        }
    }

    override suspend fun updateLocation(eventId: String, userId: String, location: GeoPoint): Result<Unit> = runCatching {
        val data = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "lastSeenTimestamp" to location.timestamp
        )
        firestore.collection("events").document(eventId)
            .collection("participants").document(userId)
            .update(data).await()
    }

    override suspend fun sendMessage(message: FieldMessage): Result<Unit> = runCatching {
        firestore.collection("events").document(message.eventId)
            .collection("messages").document(message.id.ifEmpty { firestore.collection("events").document().id })
            .set(message).await()
    }

    override suspend fun sendSOS(eventId: String, senderId: String, senderName: String, location: GeoPoint): Result<Unit> = runCatching {
        val sosMessage = FieldMessage(
            id = "SOS_${System.currentTimeMillis()}",
            eventId = eventId,
            senderId = senderId,
            senderName = senderName,
            senderRole = UserRole.MEMBER,
            targetRole = null,
            messageText = "🚨 EMERGENCY SOS! I need immediate assistance at (${location.latitude}, ${location.longitude})",
            isEmergency = true,
            timestamp = System.currentTimeMillis()
        )
        sendMessage(sosMessage).getOrThrow()
    }

    override fun getReports(actId: String): Flow<List<FieldReport>> {
        firestore.collection("Reports").whereEqualTo("ActId", actId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val reports = snapshot.documents.mapNotNull { doc ->
                        val report = FieldReportMapper.fromSnapshot(doc) ?: return@mapNotNull null
                        ReportLocalEntity(
                            id = report.id,
                            actId = report.actId,
                            reporterId = report.reporterId,
                            title = report.title,
                            description = report.description,
                            latitude = report.location.latitude,
                            longitude = report.location.longitude,
                            reportTime = report.reportTime,
                            status = report.status.value,
                            type = report.type.value
                        )
                    }
                    scope.launch { dao.insertReports(reports) }
                }
            }
        return dao.observeReports(actId).map { list ->
            list.map {
                FieldReport(
                    id = it.id,
                    actId = it.actId,
                    reporterId = it.reporterId,
                    title = it.title,
                    description = it.description,
                    location = GeoPoint(it.latitude, it.longitude),
                    reportTime = it.reportTime,
                    status = FieldReportStatus.getByValue(it.status),
                    type = FieldReportType.getByValue(it.type)
                )
            }
        }
    }

    override suspend fun addReport(report: FieldReport): Result<String> = runCatching {
        val docRef = firestore.collection("Reports").document(report.id.ifEmpty { firestore.collection("Reports").document().id })
        docRef.set(FieldReportMapper.toWriteMap(report)).await()
        docRef.id
    }
}
