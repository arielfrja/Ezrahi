package com.arielfaridja.ezrahi.domain.repository

import com.arielfaridja.ezrahi.domain.model.*
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface EzrahiRepository {
    fun getEventUpdates(eventId: String): Flow<FieldEvent?>
    fun getEvents(): Flow<List<FieldEvent>>
    fun getUserEvents(userId: String): Flow<List<FieldEvent>>
    fun getParticipants(eventId: String): Flow<List<EventParticipant>>
    fun getMessages(eventId: String): Flow<List<FieldMessage>>
    fun getRoutes(eventId: String): Flow<List<RouteInfo>>
    fun getActiveRoutePoints(eventId: String): Flow<List<GeoPoint>>
    val routeErrorEvents: SharedFlow<String>
    suspend fun uploadRoute(eventId: String, uid: String, uri: Uri, fileName: String): Result<Unit>
    suspend fun setActiveRoute(eventId: String, routeId: String): Result<Unit>
    suspend fun deleteRoute(eventId: String, routeId: String): Result<Unit>
    suspend fun updateRoutePermissions(eventId: String, allowedRoles: List<String>, allowedUids: List<String>): Result<Unit>
    suspend fun updateLocation(eventId: String, userId: String, location: GeoPoint): Result<Unit>
    suspend fun updateParticipantRole(eventId: String, userId: String, role: UserRole): Result<Unit>
    suspend fun updateEventName(eventId: String, name: String): Result<Unit>
    fun getRoleOptions(): Flow<List<RoleOption>>
    fun getMessengerOptions(): Flow<List<MessengerOption>>
    fun getMyMessengers(eventId: String, userId: String): Flow<Map<String, String>>
    suspend fun updateMyMessengers(eventId: String, userId: String, messengers: Map<String, String>): Result<Unit>
    fun getDirectMessages(eventId: String, myUserId: String, otherUserId: String): Flow<List<FieldMessage>>
    suspend fun getDirectLastMessage(eventId: String, myUserId: String, otherUserId: String): String?
    suspend fun sendDirectMessage(eventId: String, myUserId: String, myName: String, otherUserId: String, text: String): Result<Unit>
    suspend fun sendMessage(message: FieldMessage): Result<Unit>
    suspend fun sendSOS(eventId: String, senderId: String, senderName: String, location: GeoPoint): Result<Unit>
    fun getReports(actId: String): Flow<List<FieldReport>>
    suspend fun addReport(report: FieldReport): Result<String>
    suspend fun registerUser(profile: UserProfile): Result<Unit>
}
