package com.arielfaridja.ezrahi.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.arielfaridja.ezrahi.entities.ActUser

@Database(entities = [ActUser::class], version = 0)
@TypeConverters(TypesConverter::class)
abstract class AppDatabase : RoomDatabase() {
    // Room will generate the implementation
}
