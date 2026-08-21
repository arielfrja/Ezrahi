package com.arielfaridja.ezrahi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OutboxDao {

    @Insert
    suspend fun insert(record: OutboxRecord): Long

    @Query("SELECT * FROM network_outbox ORDER BY priority ASC, createdAtTimestamp ASC LIMIT :limit")
    suspend fun getPending(limit: Int): List<OutboxRecord>

    @Query("DELETE FROM network_outbox WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE network_outbox SET retryCount = :retryCount WHERE id = :id")
    suspend fun updateRetryCount(id: Long, retryCount: Int)

    @Query("SELECT COUNT(*) FROM network_outbox")
    suspend fun countPending(): Int
}