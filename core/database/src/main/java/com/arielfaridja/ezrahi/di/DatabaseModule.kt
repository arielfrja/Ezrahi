package com.arielfaridja.ezrahi.di

import android.content.Context
import androidx.room.Room
import com.arielfaridja.ezrahi.data.local.EzrahiDao
import com.arielfaridja.ezrahi.data.local.EzrahiDatabase
import com.arielfaridja.ezrahi.data.local.OutboxDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EzrahiDatabase {
        return Room.databaseBuilder(
            context,
            EzrahiDatabase::class.java,
            "ezrahi_local_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideEzrahiDao(db: EzrahiDatabase): EzrahiDao = db.ezrahiDao()

    @Provides
    fun provideOutboxDao(db: EzrahiDatabase): OutboxDao = db.outboxDao()
}
