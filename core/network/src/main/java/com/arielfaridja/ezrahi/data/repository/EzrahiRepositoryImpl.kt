package com.arielfaridja.ezrahi.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arielfaridja.ezrahi.data.local.*
import com.arielfaridja.ezrahi.data.mapper.FieldReportMapper
import com.arielfaridja.ezrahi.data.parser.GpxParser
import com.arielfaridja.ezrahi.domain.model.*
import com.arielfaridja.ezrahi.domain.model.defaultRoleOptions
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.arielfaridja.ezrahi.util.logging.ErrorType
import com.arielfaridja.ezrahi.util.logging.ExceptionLogger
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EzrahiRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val dao: EzrahiDao,
    private val logger: ExceptionLogger
) : EzrahiRepository {

    companion object {
        private const val TAG = "EzrahiRepo"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _routeErrorEvents = MutableSharedFlow<String>()
    override val routeErrorEvents: SharedFlow<String> = _routeErrorEvents.asSharedFlow()

    private fun logListenerError(query: String, error: Exception) {
        Log.w(TAG, "listener '$query' failed: ${error.message} (serving cached data)", error)
        logger.log(error, ErrorType.FIRESTORE_LISTENER, screen = query)
    }

    override fun getEvents(): Flow<List<FieldEvent>> = callbackFlow {
        val registration = firestore.collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logListenerError("events", error)
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
                            isLive = doc.getBoolean("isLive") ?: true,
                            routeAllowedRolesJson = listToJson(doc.get("routeAllowedRoles") as? List<*>),
                            routeAllowedUidsJson = listToJson(doc.get("routeAllowedUids") as? List<*>),
                            stalenessConfigJson = stalenessConfigToJson(mapToStalenessConfig(doc.get("stalenessConfig") as? Map<*, *>))
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
                            isLive = doc.getBoolean("isLive") ?: true,
                            routeAllowedRoles = (doc.get("routeAllowedRoles") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            routeAllowedUids = (doc.get("routeAllowedUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            stalenessConfig = mapToStalenessConfig(doc.get("stalenessConfig") as? Map<*, *>)
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
                    logListenerError("events/$eventId", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val event = EventLocalEntity(
                        id = snapshot.id,
                        name = snapshot.getString("name") ?: "",
                        managerId = snapshot.getString("managerId") ?: "",
                        managerContact = snapshot.getString("managerContact") ?: "",
                        gpxRouteUrl = snapshot.getString("gpxRouteUrl"),
                        isLive = snapshot.getBoolean("isLive") ?: true,
                        routeAllowedRolesJson = listToJson(snapshot.get("routeAllowedRoles") as? List<*>),
                        routeAllowedUidsJson = listToJson(snapshot.get("routeAllowedUids") as? List<*>),
                        stalenessConfigJson = stalenessConfigToJson(mapToStalenessConfig(snapshot.get("stalenessConfig") as? Map<*, *>))
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
                    logListenerError("events/$eventId/participants", error)
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
                            lastSeenTimestamp = doc.getLong("lastSeenTimestamp") ?: System.currentTimeMillis(),
                            messengersJson = (doc.get("messengers") as? Map<*, *>)
                                ?.let { org.json.JSONObject(it).toString() }
                                ?: "{}",
                            manualStateOverride = doc.getString("manualStateOverride")
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
                    logListenerError("events/$eventId/messages", error)
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

    override fun getRoutes(eventId: String): Flow<List<RouteInfo>> = callbackFlow {
        val routesRef = firestore.collection("events").document(eventId).collection("routes")
        val registration = routesRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logListenerError("events/$eventId/routes", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val existing = snapshot.documents.filter { it.exists() }
                    if (existing.size == 1 && existing[0].getBoolean("isActive") != true) {
                        routesRef.document(existing[0].id).update("isActive", true)
                    }
                    val serverIds = existing.map { it.id }
                    scope.launch {
                        if (serverIds.isEmpty()) {
                            dao.deleteRoutesForEvent(eventId)
                        } else {
                            dao.deleteRoutesNotIn(eventId, serverIds)
                        }
                        existing.forEach { doc ->
                            val cached = dao.getRoute(doc.id)
                            dao.insertRoutes(
                                listOf(
                                    RouteLocalEntity(
                                        id = doc.id,
                                        eventId = eventId,
                                        name = doc.getString("name") ?: "",
                                        gpxRouteUrl = doc.getString("gpxRouteUrl") ?: "",
                                        storagePath = doc.getString("storagePath") ?: "",
                                        uploadedBy = doc.getString("uploadedBy") ?: "",
                                        uploadedAt = doc.getLong("uploadedAt") ?: System.currentTimeMillis(),
                                        isActive = doc.getBoolean("isActive") ?: false,
                                        pointsJson = cached?.pointsJson
                                    )
                                )
                            )
                        }
                    }
                }
            }

        val job = launch {
            dao.observeRoutes(eventId).collect { list ->
                trySend(list.map { it.toRouteInfo() })
            }
        }
        awaitClose {
            registration.remove()
            job.cancel()
        }
    }

    override fun getActiveRoutePoints(eventId: String): Flow<List<GeoPoint>> = callbackFlow {
        val registration = firestore.collection("events").document(eventId).collection("routes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logListenerError("events/$eventId/routes (active)", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val existing = snapshot.documents.filter { it.exists() }
                    val serverIds = existing.map { it.id }
                    scope.launch {
                        if (serverIds.isEmpty()) {
                            dao.deleteRoutesForEvent(eventId)
                        } else {
                            dao.deleteRoutesNotIn(eventId, serverIds)
                        }
                        existing.forEach { doc ->
                            val cached = dao.getRoute(doc.id)
                            dao.insertRoutes(
                                listOf(
                                    RouteLocalEntity(
                                        id = doc.id,
                                        eventId = eventId,
                                        name = doc.getString("name") ?: "",
                                        gpxRouteUrl = doc.getString("gpxRouteUrl") ?: "",
                                        storagePath = doc.getString("storagePath") ?: "",
                                        uploadedBy = doc.getString("uploadedBy") ?: "",
                                        uploadedAt = doc.getLong("uploadedAt") ?: System.currentTimeMillis(),
                                        isActive = doc.getBoolean("isActive") ?: false,
                                        pointsJson = cached?.pointsJson
                                    )
                                )
                            )
                        }
                    }
                }
            }

        val job = launch {
            var lastFetchedRouteId: String? = null
            dao.observeRoutes(eventId).collect { routes ->
                val active = if (routes.size == 1) routes.first() else routes.firstOrNull { it.isActive }
                if (active == null) {
                    lastFetchedRouteId = null
                    trySend(emptyList())
                    return@collect
                }
                val cachedPoints = active.pointsJson?.let { parsePointsJson(it) }
                if (cachedPoints != null) trySend(cachedPoints)
                if (active.id != lastFetchedRouteId || cachedPoints == null) {
                    lastFetchedRouteId = active.id
                    if (active.gpxRouteUrl.isNotBlank()) {
                        scope.launch {
                            runCatching {
                                val xml = downloadGpx(active.gpxRouteUrl)
                                val points = GpxParser.parse(xml)
                                if (points.isNotEmpty()) {
                                    dao.updateRoutePoints(active.id, toPointsJson(points))
                                    trySend(points)
                                } else {
                                    _routeErrorEvents.emit("Route '${active.name}': GPX contains no track points")
                                }
                            }.onFailure { error ->
                                Log.w(TAG, "route download/parse failed for '${active.name}': ${error.message}")
                                logger.log(error, ErrorType.ROUTE_PARSER, eventId, screen = "map")
                                _routeErrorEvents.emit("Route '${active.name}' failed to load: ${error.message}")
                            }
                        }
                    } else {
                        _routeErrorEvents.emit("Route '${active.name}' has no download URL")
                    }
                }
            }
        }
        awaitClose {
            registration.remove()
            job.cancel()
        }
    }

    override suspend fun uploadRoute(eventId: String, uid: String, uri: Uri, fileName: String): Result<Unit> = runCatching {
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "route.gpx" }
        val xml = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalArgumentException("Cannot read the selected file")
        val parsedPoints = GpxParser.parse(xml)
        if (parsedPoints.isEmpty()) {
            throw IllegalArgumentException("Selected file is not a valid GPX track (no track points found)")
        }
        val path = "gpx/$eventId/$uid/$safeName"
        val ref = storage.reference.child(path)
        val downloadUrl = ref.putFile(uri).await().storage.downloadUrl.await()
        val routesRef = firestore.collection("events").document(eventId).collection("routes")
        val isFirst = routesRef.get().await().documents.isEmpty()
        val routeId = routesRef.document().id
        routesRef.document(routeId).set(
            mapOf(
                "name" to safeName.removeSuffix(".gpx"),
                "gpxRouteUrl" to downloadUrl.toString(),
                "storagePath" to path,
                "uploadedBy" to uid,
                "uploadedAt" to System.currentTimeMillis(),
                "isActive" to isFirst
            )
        ).await()
        Unit
    }.onFailure { error ->
        Log.w(TAG, "uploadRoute(event=$eventId) failed", error)
        logger.log(error, ErrorType.ROUTE_PARSER, eventId, screen = "management")
    }

    override suspend fun setActiveRoute(eventId: String, routeId: String): Result<Unit> = runCatching {
        val refs = firestore.collection("events").document(eventId).collection("routes")
        val batch = firestore.batch()
        refs.get().await().documents.forEach { doc ->
            batch.update(refs.document(doc.id), "isActive", doc.id == routeId)
        }
        batch.commit().await()
    }

    override suspend fun deleteRoute(eventId: String, routeId: String): Result<Unit> = runCatching {
        val docRef = firestore.collection("events").document(eventId).collection("routes").document(routeId)
        val path = docRef.get().await().getString("storagePath")
        if (path != null) {
            runCatching { storage.reference.child(path).delete().await() }
        }
        docRef.delete().await()
    }

    override suspend fun updateRoutePermissions(eventId: String, allowedRoles: List<String>, allowedUids: List<String>): Result<Unit> = runCatching {
        firestore.collection("events").document(eventId).update(
            mapOf(
                "routeAllowedRoles" to allowedRoles,
                "routeAllowedUids" to allowedUids
            )
        ).await()
    }

    private suspend fun downloadGpx(url: String): String = withContext(Dispatchers.IO) {
        URL(url).openConnection().apply {
            setRequestProperty("User-Agent", "Ezrahi")
            connectTimeout = 15_000
            readTimeout = 15_000
        }.getInputStream().bufferedReader().use { it.readText() }
    }

    private fun toPointsJson(points: List<GeoPoint>): String {
        val arr = JSONArray()
        points.forEach { arr.put(JSONArray(listOf(it.latitude, it.longitude))) }
        return arr.toString()
    }

    private fun parsePointsJson(json: String): List<GeoPoint>? = runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).map { idx ->
            val pair = arr.getJSONArray(idx)
            GeoPoint(latitude = pair.getDouble(0), longitude = pair.getDouble(1))
        }
    }.getOrNull()

    private fun listToJson(list: List<*>?): String {
        if (list == null) return "[]"
        val arr = JSONArray()
        list.forEach { if (it is String) arr.put(it) }
        return arr.toString()
    }

    private fun jsonToList(json: String): List<String> {
        val arr = JSONArray(json)
        return (0 until arr.length()).mapNotNull { idx ->
            arr.optString(idx).takeIf { it.isNotBlank() }
        }
    }

    private fun stalenessConfigToJson(c: StalenessConfig): String = org.json.JSONObject().apply {
        put("staleThresholdMinutes", c.staleThresholdMinutes)
        put("disconnectedThresholdMinutes", c.disconnectedThresholdMinutes)
        put("expiredThresholdMinutes", c.expiredThresholdMinutes)
    }.toString()

    private fun jsonToStalenessConfig(json: String): StalenessConfig = runCatching {
        val obj = org.json.JSONObject(json)
        StalenessConfig(
            staleThresholdMinutes = obj.optInt("staleThresholdMinutes", 5),
            disconnectedThresholdMinutes = obj.optInt("disconnectedThresholdMinutes", 15),
            expiredThresholdMinutes = obj.optInt("expiredThresholdMinutes", 30)
        )
    }.getOrDefault(StalenessConfig())

    private fun stalenessConfigToMap(c: StalenessConfig): Map<String, Int> = mapOf(
        "staleThresholdMinutes" to c.staleThresholdMinutes,
        "disconnectedThresholdMinutes" to c.disconnectedThresholdMinutes,
        "expiredThresholdMinutes" to c.expiredThresholdMinutes
    )

    private fun mapToStalenessConfig(map: Map<*, *>?): StalenessConfig {
        if (map == null) return StalenessConfig()
        return StalenessConfig(
            staleThresholdMinutes = (map["staleThresholdMinutes"] as? Number)?.toInt() ?: 5,
            disconnectedThresholdMinutes = (map["disconnectedThresholdMinutes"] as? Number)?.toInt() ?: 15,
            expiredThresholdMinutes = (map["expiredThresholdMinutes"] as? Number)?.toInt() ?: 30
        )
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

    override fun getRoleOptions(): Flow<List<RoleOption>> = callbackFlow {
        val docRef = firestore.collection("settings").document("roles")
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                logListenerError("settings/roles", error)
                trySend(defaultRoleOptions())
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val raw = snapshot.get("options")
                val list = (raw as? List<*>)?.mapNotNull { item ->
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    val name = map["name"] as? String ?: return@mapNotNull null
                    val label = map["label"] as? String ?: name
                    val isStaff = (map["isStaff"] as? Boolean) ?: true
                    RoleOption(name, label, isStaff)
                }
                if (!list.isNullOrEmpty()) trySend(list) else trySend(defaultRoleOptions())
            } else {
                trySend(defaultRoleOptions())
            }
        }
        awaitClose { registration.remove() }
    }

    override fun getMessengerOptions(): Flow<List<MessengerOption>> = callbackFlow {
        val docRef = firestore.collection("settings").document("messengers")
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                logListenerError("settings/messengers", error)
                trySend(defaultMessengerOptions())
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val raw = snapshot.get("options")
                val list = (raw as? List<*>)?.mapNotNull { item ->
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    val id = map["id"] as? String ?: return@mapNotNull null
                    val label = map["label"] as? String ?: id
                    val template = map["urlTemplate"] as? String ?: return@mapNotNull null
                    MessengerOption(id, label, template)
                }
                if (!list.isNullOrEmpty()) trySend(list) else trySend(defaultMessengerOptions())
            } else {
                trySend(defaultMessengerOptions())
            }
        }
        awaitClose { registration.remove() }
    }

    override fun getMyMessengers(eventId: String, userId: String): Flow<Map<String, String>> = callbackFlow {
        val docRef = firestore.collection("events").document(eventId)
            .collection("participants").document(userId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                logListenerError("events/$eventId/participants/$userId (my messengers)", error)
                return@addSnapshotListener
            }
            val messengers = (snapshot?.get("messengers") as? Map<*, *>)
                ?.filterKeys { it is String }
                ?.mapKeys { it.key as String }
                ?.mapValues { it.value as? String ?: "" }
                ?.filterValues { it.isNotBlank() }
                ?: emptyMap()
            trySend(messengers)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun updateMyMessengers(eventId: String, userId: String, messengers: Map<String, String>): Result<Unit> = runCatching {
        firestore.collection("events").document(eventId)
            .collection("participants").document(userId)
            .update("messengers", messengers.filterValues { it.isNotBlank() }).await()
    }

    override fun getDirectMessages(eventId: String, myUserId: String, otherUserId: String): Flow<List<FieldMessage>> = callbackFlow {
        val pairId = directPairId(myUserId, otherUserId)
        val registration = firestore.collection("events").document(eventId)
            .collection("direct").document(pairId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logListenerError("events/$eventId/direct/$pairId/messages", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        FieldMessage(
                            id = doc.id,
                            eventId = eventId,
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            senderRole = runCatching { UserRole.valueOf(doc.getString("senderRole") ?: "") }
                                .getOrDefault(UserRole.MEMBER),
                            messageText = doc.getString("messageText") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    }
                    trySend(list.sortedBy { it.timestamp })
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getDirectLastMessage(eventId: String, myUserId: String, otherUserId: String): String? = runCatching {
        val pairId = directPairId(myUserId, otherUserId)
        firestore.collection("events").document(eventId)
            .collection("direct").document(pairId).collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
            .get().await().documents.firstOrNull()?.getString("messageText")
    }.getOrNull()

    override suspend fun sendDirectMessage(
        eventId: String,
        myUserId: String,
        myName: String,
        otherUserId: String,
        text: String
    ): Result<Unit> = runCatching {
        val pairId = directPairId(myUserId, otherUserId)
        val messageId = firestore.collection("events").document().id
        firestore.collection("events").document(eventId)
            .collection("direct").document(pairId).collection("messages")
            .document(messageId).set(
                mapOf(
                    "senderId" to myUserId,
                    "senderName" to myName,
                    "senderRole" to "MEMBER",
                    "messageText" to text.trim(),
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()
            Unit
    }.onFailure { error ->
        Log.w(TAG, "sendDirectMessage(event=$eventId, other=$otherUserId) failed", error)
        logger.log(error, ErrorType.NETWORK, eventId, screen = "direct")
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
                    logListenerError("Reports?ActId=$actId", error)
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
                            type = report.type.value,
                            typeId = report.typeId
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

    override suspend fun updateStalenessConfig(eventId: String, config: StalenessConfig): Result<Unit> = runCatching {
        firestore.collection("events").document(eventId)
            .update("stalenessConfig", stalenessConfigToMap(config)).await()
        Unit
    }.onFailure { error ->
        Log.w(TAG, "updateStalenessConfig(event=$eventId) failed", error)
        logger.log(error, ErrorType.CAUGHT, eventId, screen = "management")
    }

    override suspend fun updateParticipantManualState(
        eventId: String,
        userId: String,
        override: EntityLivenessState?,
        config: StalenessConfig
    ): Result<Unit> = runCatching {
        val data = if (override == null) {
            mapOf("manualStateOverride" to null)
        } else {
            // Seed the staleness timer so the chosen liveness is fresh, and clear
            // any sticky pin so the state decays naturally (not permanent).
            mapOf(
                "lastSeenTimestamp" to config.seedTimestampFor(override),
                "manualStateOverride" to null
            )
        }
        firestore.collection("events").document(eventId)
            .collection("participants").document(userId)
            .update(data).await()
        Unit
    }.onFailure { error ->
        Log.w(TAG, "updateParticipantManualState(event=$eventId, user=$userId) failed", error)
        logger.log(error, ErrorType.CAUGHT, eventId, screen = "management")
    }

    override fun getReportTypes(eventId: String): Flow<List<ReportTypeDefinition>> = callbackFlow {
        val registration = firestore.collection("events").document(eventId)
            .collection("report_types")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logListenerError("events/$eventId/report_types", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val types = snapshot.documents.mapNotNull { doc ->
                        val name = doc.getString("name") ?: return@mapNotNull null
                        ReportTypeDefinition(
                            id = doc.id,
                            name = name,
                            iconKey = doc.getString("iconKey") ?: "general",
                            colorHex = doc.getString("colorHex") ?: if (name.equals("MEDICAL", true)) "#C62828" else "#2E7D32",
                            builtin = doc.getBoolean("builtin") ?: false
                        )
                    }.sortedWith(compareByDescending<ReportTypeDefinition> { it.builtin }.thenBy { it.name.lowercase() })
                    trySend(types)
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun ensureReportTypesSeeded(eventId: String): Result<Unit> = runCatching {
        val ref = firestore.collection("events").document(eventId).collection("report_types")
        val existing = ref.get().await()
        val haveNames = existing.documents.mapNotNull { it.getString("name")?.lowercase() }.toSet()
        val seeds = listOf(
            Triple("GENERAL", "general", "#2E7D32"),
            Triple("MEDICAL", "medical", "#C62828")
        ).filter { (name, _, _) -> name.lowercase() !in haveNames }
        if (seeds.isEmpty()) return@runCatching
        val batch = firestore.batch()
        seeds.forEach { (name, iconKey, colorHex) ->
            batch.set(ref.document(), mapOf(
                "name" to name,
                "iconKey" to iconKey,
                "colorHex" to colorHex,
                "builtin" to true,
                "createdAt" to com.google.firebase.Timestamp.now()
            ))
        }
        batch.commit().await()
    }.onFailure { error ->
        Log.w(TAG, "ensureReportTypesSeeded(event=$eventId) failed", error)
        logger.log(error, ErrorType.CAUGHT, eventId, screen = "management")
    }

    override suspend fun addReportType(eventId: String, name: String, iconKey: String, colorHex: String): Result<String> = runCatching {
        // Try seeding best-effort (ignore seeding error if it fails so user creation still proceeds)
        ensureReportTypesSeeded(eventId)
        val ref = firestore.collection("events").document(eventId)
            .collection("report_types").document()
        ref.set(mapOf(
            "name" to name.trim(),
            "iconKey" to iconKey,
            "colorHex" to colorHex,
            "builtin" to false,
            "createdAt" to com.google.firebase.Timestamp.now()
        )).await()
        ref.id
    }.onFailure { error ->
        Log.w(TAG, "addReportType(event=$eventId, name=$name) failed", error)
        logger.log(error, ErrorType.CAUGHT, eventId, screen = "management")
    }

    override suspend fun updateReportType(eventId: String, typeId: String, name: String, iconKey: String, colorHex: String): Result<Unit> = runCatching {
        firestore.collection("events").document(eventId)
            .collection("report_types").document(typeId)
            .update(mapOf("name" to name.trim(), "iconKey" to iconKey, "colorHex" to colorHex)).await()
        Unit
    }.onFailure { error ->
        Log.w(TAG, "updateReportType(event=$eventId, type=$typeId) failed", error)
        logger.log(error, ErrorType.CAUGHT, eventId, screen = "management")
    }

    override suspend fun deleteReportType(eventId: String, typeId: String, resolution: DeletionResolution?): Result<Unit> = runCatching {
        val typesRef = firestore.collection("events").document(eventId).collection("report_types")
        val typeDoc = typesRef.document(typeId).get().await()
        if (!typeDoc.exists()) throw IllegalArgumentException("Report type $typeId does not exist")
        if (typeDoc.getBoolean("builtin") == true) {
            throw IllegalArgumentException("Builtin report types cannot be deleted")
        }

        val affected = firestore.collection("Reports")
            .whereEqualTo("ActId", eventId)
            .whereEqualTo("TypeId", typeId)
            .get().await()
            .documents

        when (resolution) {
            is DeletionResolution.RemoveReports -> {
                affected.chunked(450).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
            }
            is DeletionResolution.ConvertToGeneral, is DeletionResolution.ConvertTo -> {
                val targetTypeId = when (resolution) {
                    is DeletionResolution.ConvertToGeneral -> {
                        val generalDoc = typesRef.whereEqualTo("name", "GENERAL").limit(1).get().await().documents.firstOrNull()
                            ?: throw IllegalStateException("Builtin GENERAL type not found for event $eventId")
                        generalDoc.id
                    }
                    is DeletionResolution.ConvertTo -> resolution.targetTypeId
                    else -> throw IllegalStateException()
                }
                val targetDef = typesRef.document(targetTypeId).get().await()
                val legacyInt = legacyIntFor(targetDef)
                affected.chunked(450).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { batch.update(it.reference, mapOf("TypeId" to targetTypeId, "Type" to legacyInt)) }
                    batch.commit().await()
                }
            }
            null -> Unit
        }

        typesRef.document(typeId).delete().await()
        Unit
    }.onFailure { error ->
        Log.w(TAG, "deleteReportType(event=$eventId, type=$typeId) failed", error)
        logger.log(error, ErrorType.CAUGHT, eventId, screen = "management")
    }

    override fun getDeletionPreference(eventId: String): Flow<DeletionResolution?> = callbackFlow {
        val registration = firestore.collection("events").document(eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logListenerError("events/$eventId (deletion preference)", error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.getString("reportTypeDeletionAction").toResolution())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun setDeletionPreference(eventId: String, resolution: DeletionResolution?): Result<Unit> = runCatching {
        val docRef = firestore.collection("events").document(eventId)
        if (resolution == null) {
            docRef.update("reportTypeDeletionAction", com.google.firebase.firestore.FieldValue.delete()).await()
        } else {
            docRef.update("reportTypeDeletionAction", resolution.toFieldValue()).await()
        }
        Unit
    }.onFailure { error ->
        Log.w(TAG, "setDeletionPreference(event=$eventId) failed", error)
        logger.log(error, ErrorType.CAUGHT, eventId, screen = "management")
    }

    private fun legacyIntFor(typeDoc: com.google.firebase.firestore.DocumentSnapshot): Int {
        if (typeDoc.getBoolean("builtin") != true) return -1
        return when (typeDoc.getString("name")?.lowercase()) {
            "general" -> 0
            "medical" -> 1
            else -> -1
        }
    }

    private val PREF_REMOVE = "REMOVE"
    private val PREF_GENERAL = "CONVERT_GENERAL"
    private val PREF_TYPE_PREFIX = "CONVERT_TYPE:"

    private fun DeletionResolution.toFieldValue(): String = when (this) {
        is DeletionResolution.RemoveReports -> PREF_REMOVE
        is DeletionResolution.ConvertToGeneral -> PREF_GENERAL
        is DeletionResolution.ConvertTo -> "$PREF_TYPE_PREFIX$targetTypeId"
    }

    private fun String?.toResolution(): DeletionResolution? = when {
        this == PREF_REMOVE -> DeletionResolution.RemoveReports
        this == PREF_GENERAL -> DeletionResolution.ConvertToGeneral
        this != null && startsWith(PREF_TYPE_PREFIX) -> DeletionResolution.ConvertTo(removePrefix(PREF_TYPE_PREFIX))
        else -> null
    }

    private fun EventLocalEntity.toFieldEvent() = FieldEvent(
        id = id,
        name = name,
        managerId = managerId,
        managerContact = managerContact,
        gpxRouteUrl = gpxRouteUrl,
        isLive = isLive,
        routeAllowedRoles = jsonToList(routeAllowedRolesJson),
        routeAllowedUids = jsonToList(routeAllowedUidsJson),
        stalenessConfig = jsonToStalenessConfig(stalenessConfigJson)
    )

    private fun RouteLocalEntity.toRouteInfo() = RouteInfo(
        id = id,
        eventId = eventId,
        name = name,
        gpxRouteUrl = gpxRouteUrl,
        storagePath = storagePath,
        uploadedBy = uploadedBy,
        uploadedAt = uploadedAt,
        isActive = isActive
    )

    private fun ParticipantLocalEntity.toEventParticipant() = EventParticipant(
        userId = userId,
        fullName = fullName,
        phoneNumber = phoneNumber,
        role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.MEMBER),
        currentLocation = GeoPoint(latitude, longitude, lastSeenTimestamp),
            isOnline = isOnline,
            lastSeenTimestamp = lastSeenTimestamp,
            manualStateOverride = manualStateOverride?.let { runCatching { EntityLivenessState.valueOf(it) }.getOrNull() },
            messengers = runCatching {
            val obj = org.json.JSONObject(messengersJson)
            obj.keys().asSequence().associateWith { obj.getString(it) }
        }.getOrDefault(emptyMap())
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

    private fun defaultMessengerOptions(): List<MessengerOption> = listOf(
        MessengerOption("whatsapp", "WhatsApp", "https://wa.me/{handle}"),
        MessengerOption("telegram", "Telegram", "https://t.me/{handle}")
    )

    private fun directPairId(a: String, b: String) = listOf(a, b).sorted().joinToString("_")

    private fun ReportLocalEntity.toFieldReport() = FieldReport(
        id = id,
        actId = actId,
        reporterId = reporterId,
        title = title,
        description = description,
        location = GeoPoint(latitude, longitude),
        reportTime = reportTime,
        status = FieldReportStatus.getByValue(status),
        type = FieldReportType.getByValue(type),
        typeId = typeId
    )
}
