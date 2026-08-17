package com.arielfaridja.ezrahi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_events")
data class EventLocalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val managerId: String,
    val managerContact: String,
    val gpxRouteUrl: String?,
    val isLive: Boolean
)

@Entity(tableName = "cached_participants")
data class ParticipantLocalEntity(
    @PrimaryKey val userId: String,
    val eventId: String,
    val fullName: String,
    val phoneNumber: String,
    val role: String,
    val latitude: Double,
    val longitude: Double,
    val isOnline: Boolean,
    val lastSeenTimestamp: Long
)

@Entity(tableName = "cached_messages")
data class MessageLocalEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val targetRole: String?,
    val messageText: String,
    val isEmergency: Boolean,
    val timestamp: Long
)

@Entity(tableName = "cached_reports")
data class ReportLocalEntity(
    @PrimaryKey val id: String,
    val actId: String,
    val reporterId: String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val reportTime: Long,
    val status: Int,
    val type: Int
)
