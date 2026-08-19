package com.arielfaridja.ezrahi.domain.repository

import com.arielfaridja.ezrahi.domain.model.*
import kotlinx.coroutines.flow.Flow

interface EzrahiRepository {
    fun getEventUpdates(eventId: String): Flow<FieldEvent?>
    fun getEvents(): Flow<List<FieldEvent>>
    fun getUserEvents(userId: String): Flow<List<FieldEvent>>
    fun getParticipants(eventId: String): Flow<List<EventParticipant>>
    fun getMessages(eventId: String): Flow<List<FieldMessage>>
    suspend fun updateLocation(eventId: String, userId: String, location: GeoPoint): Result<Unit>
    suspend fun sendMessage(message: FieldMessage): Result<Unit>
    suspend fun sendSOS(eventId: String, senderId: String, senderName: String, location: GeoPoint): Result<Unit>
    fun getReports(actId: String): Flow<List<FieldReport>>
    suspend fun addReport(report: FieldReport): Result<String>
    suspend fun registerUser(profile: UserProfile): Result<Unit>
}
