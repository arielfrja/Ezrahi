package com.arielfaridja.ezrahi.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EzrahiDao {

    // Events
    @Query("SELECT * FROM cached_events WHERE id = :eventId")
    fun observeEvent(eventId: String): Flow<EventLocalEntity?>

    @Query("SELECT * FROM cached_events")
    fun observeEvents(): Flow<List<EventLocalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventLocalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventLocalEntity>)

    // Participants
    @Query("SELECT * FROM cached_participants WHERE eventId = :eventId")
    fun observeParticipants(eventId: String): Flow<List<ParticipantLocalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ParticipantLocalEntity>)

    // Messages
    @Query("SELECT * FROM cached_messages WHERE eventId = :eventId ORDER BY timestamp ASC")
    fun observeMessages(eventId: String): Flow<List<MessageLocalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageLocalEntity)

    // Reports
    @Query("SELECT * FROM cached_reports WHERE actId = :actId ORDER BY reportTime ASC")
    fun observeReports(actId: String): Flow<List<ReportLocalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReports(reports: List<ReportLocalEntity>)

    // Routes
    @Query("SELECT * FROM cached_routes WHERE eventId = :eventId ORDER BY uploadedAt ASC")
    fun observeRoutes(eventId: String): Flow<List<RouteLocalEntity>>

    @Query("SELECT * FROM cached_routes WHERE id = :routeId")
    suspend fun getRoute(routeId: String): RouteLocalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<RouteLocalEntity>)

    @Query("UPDATE cached_routes SET pointsJson = :pointsJson WHERE id = :routeId")
    suspend fun updateRoutePoints(routeId: String, pointsJson: String)

    @Query("DELETE FROM cached_routes WHERE id = :routeId")
    suspend fun deleteRoute(routeId: String)

    @Query("DELETE FROM cached_routes WHERE eventId = :eventId AND id NOT IN (:ids)")
    suspend fun deleteRoutesNotIn(eventId: String, ids: List<String>)

    @Query("DELETE FROM cached_routes WHERE eventId = :eventId")
    suspend fun deleteRoutesForEvent(eventId: String)
}
