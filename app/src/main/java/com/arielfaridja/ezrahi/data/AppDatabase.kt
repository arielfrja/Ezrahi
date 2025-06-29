package com.arielfaridja.ezrahi.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.arielfaridja.ezrahi.entities.ActPermission
import com.arielfaridja.ezrahi.entities.ActUser

@Database(entities = [ActUser::class, ActPermission::class], version = 1)
@TypeConverters(TypesConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localDao(): LocalDao
}
