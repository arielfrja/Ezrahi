package com.arielfaridja.ezrahi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EventLocalEntity::class, ParticipantLocalEntity::class, MessageLocalEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EzrahiDatabase : RoomDatabase() {
    abstract fun ezrahiDao(): EzrahiDao
}
