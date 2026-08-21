package com.arielfaridja.ezrahi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_outbox")
data class OutboxRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: String,
    val priority: Int,
    val payloadType: String,
    val payloadJson: String,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)