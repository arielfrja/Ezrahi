package com.arielfaridja.ezrahi.data.repository

import com.arielfaridja.ezrahi.data.local.*
import com.arielfaridja.ezrahi.data.mapper.FieldReportMapper
import com.arielfaridja.ezrahi.domain.model.*
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    override fun getEvents(): Flow<List<FieldEvent>> = callbackFlow {
        val registration = firestore.collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        if (!doc.exists()) return@mapNotNull null
                        EventLocalEntity(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            managerId = doc.getString("managerId") ?: "",
                            managerContact = doc.getString("managerContact") ?: "",
                            gpxRouteUrl = doc.getString("gpxRouteUrl"),
                            isLive = doc.getBoolean("isLive") ?: true
                        )
                    }
                    scope.launch { dao.insertEvents(list) }
                }
            }
        val job = launch {
            dao.observeEvents().collect { trySend(it.map { it.toFieldEvent() }) }
        }
        awaitClose {
            registration.remove()
            job.cancel()
        }
    }

    override fun getUserEvents(userId: String): Flow<List<FieldEvent>> = callbackFlow {
        val eventsById = mutableMapOf<String, FieldEvent>()
        var managedIds = emptySet<String>()
        var participantIds = emptySet<String>()
        val pendingFetch = mutableSetOf<String>()

        fun emitMerged() {
            val ids = managedIds + participantIds
            trySend(ids.mapNotNull { eventsById[it] }.sortedBy { it.name })
        }

        fun fetchEvents(ids: Set<String>) {
            val missing = ids.filter { it !in eventsById && it !in pendingFetch }
            if (missing.isEmpty()) return
            pendingFetch.addAll(missing)
            firestore.collection("events")
                .whereIn(FieldPath.documentId(), missing)
                .get()
                .addOnSuccessListener { snap ->
                    pendingFetch.removeAll(missing)
                    snap.documents.forEach { doc ->
                        eventsById[doc.id] = FieldEvent(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            managerId = doc.getString("managerId") ?: "",
                            managerContact = doc.getString("managerContact") ?: "",
                            gpxRouteUrl = doc.getString("gpxRouteUrl"),
                            isLive = doc.getBoolean("isLive") ?: true
                        )
                    }
                    emitMerged()
                }
                .addOnFailureListener { pendingFetch.removeAll(missing) }
        }

        val managedListener = firestore.collection("events")
            .whereEqualTo("managerId", userId)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    managedIds = snap.documents.mapNotNull { it.id }.toSet()
                    fetchEvents(managedIds)
                    emitMerged()
                }
            }

        val participantListener = firestore.collectionGroup("participants")
            .whereEqualTo(FieldPath.documentId(), "participants/$userId")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    participantIds = snap.documents.mapNotNull { it.reference.parent.parent?.id }.toSet()
                    fetchEvents(participantIds)
                    emitMerged()
                }
            }

        awaitClose {
            managedListener.remove()
            participantListener.remove()
        }
    }

    override fun getEventUpdates(eventId: String): Flow<FieldEvent?> = callbackFlow {
        // 1. Listen to Firestore and cache locally
        val registration = firestore.collection("events").document(eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
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
        val job = launch {
            dao.observeEvent(eventId).collect { local ->
                trySend(local?.toFieldEvent())
            }
        }
        awaitClose {
            registration.remove()
            job.cancel()
        }
    }

    override fun getParticipants(eventId: String): Flow<List<EventParticipant>> = callbackFlow {
        val registration = firestore.collection("events").document(eventId).collection("participants")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
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

        val job = launch {
            dao.observeParticipants(eventId).collect { list ->
                trySend(list.map { it.toEventParticipant() })
            }
        }
        awaitClose {
            registration.remove()
            job.cancel()
        }
    }

    override fun getMessages(eventId: String): Flow<List<FieldMessage>> = callbackFlow {
        val registration = firestore.collection("events").document(eventId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
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

        val job = launch {
            dao.observeMessages(eventId).collect { list ->
                trySend(list.map { it.toFieldMessage() })
            }
        }
        awaitClose {
            registration.remove()
            job.cancel()
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

    override suspend fun updateParticipantRole(eventId: String, userId: String, role: UserRole): Result<Unit> = runCatching {
        firestore.collection("events").document(eventId)
            .collection("participants").document(userId)
            .update("role", role.name).await()
    }

    override suspend fun updateEventName(eventId: String, name: String): Result<Unit> = runCatching {
        firestore.collection("events").document(eventId)
            .update("name", name).await()
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

    override fun getReports(actId: String): Flow<List<FieldReport>> = callbackFlow {
        val registration = firestore.collection("Reports").whereEqualTo("ActId", actId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
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

        val job = launch {
            dao.observeReports(actId).collect { list ->
                trySend(list.map { it.toFieldReport() })
            }
        }
        awaitClose {
            registration.remove()
            job.cancel()
        }
    }

    override suspend fun addReport(report: FieldReport): Result<String> = runCatching {
        val docRef = firestore.collection("Reports").document(report.id.ifEmpty { firestore.collection("Reports").document().id })
        docRef.set(FieldReportMapper.toWriteMap(report)).await()
        docRef.id
    }

    override suspend fun registerUser(profile: UserProfile): Result<Unit> = runCatching {
        val data = mapOf(
            "Email" to profile.email,
            "FirstName" to profile.firstName,
            "LastName" to profile.lastName,
            "Phone" to profile.phoneNumber,
            "LastUpdate" to com.google.firebase.Timestamp.now()
        )
        firestore.collection("Users").document(profile.id).set(data).await()
    }

    private fun EventLocalEntity.toFieldEvent() = FieldEvent(
        id = id,
        name = name,
        managerId = managerId,
        managerContact = managerContact,
        gpxRouteUrl = gpxRouteUrl,
        isLive = isLive
    )

    private fun ParticipantLocalEntity.toEventParticipant() = EventParticipant(
        userId = userId,
        fullName = fullName,
        phoneNumber = phoneNumber,
        role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.MEMBER),
        currentLocation = GeoPoint(latitude, longitude, lastSeenTimestamp),
        isOnline = isOnline,
        lastSeenTimestamp = lastSeenTimestamp
    )

    private fun MessageLocalEntity.toFieldMessage() = FieldMessage(
        id = id,
        eventId = eventId,
        senderId = senderId,
        senderName = senderName,
        senderRole = runCatching { UserRole.valueOf(senderRole) }.getOrDefault(UserRole.MEMBER),
        targetRole = targetRole?.let { roleStr -> runCatching { UserRole.valueOf(roleStr) }.getOrNull() },
        messageText = messageText,
        isEmergency = isEmergency,
        timestamp = timestamp
    )

    private fun ReportLocalEntity.toFieldReport() = FieldReport(
        id = id,
        actId = actId,
        reporterId = reporterId,
        title = title,
        description = description,
        location = GeoPoint(latitude, longitude),
        reportTime = reportTime,
        status = FieldReportStatus.getByValue(status),
        type = FieldReportType.getByValue(type)
    )
}
