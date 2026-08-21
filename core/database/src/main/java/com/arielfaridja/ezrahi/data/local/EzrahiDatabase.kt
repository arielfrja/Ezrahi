package com.arielfaridja.ezrahi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EventLocalEntity::class, ParticipantLocalEntity::class, MessageLocalEntity::class, ReportLocalEntity::class, RouteLocalEntity::class, OutboxRecord::class],
    version = 6,
    exportSchema = false
)
abstract class EzrahiDatabase : RoomDatabase() {
    abstract fun ezrahiDao(): EzrahiDao
    abstract fun outboxDao(): OutboxDao
}
