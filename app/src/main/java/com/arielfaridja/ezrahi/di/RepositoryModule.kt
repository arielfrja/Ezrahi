package com.arielfaridja.ezrahi.di

import com.arielfaridja.ezrahi.data.FirebaseDataRepo
import com.arielfaridja.ezrahi.data.IDataRepo
import com.arielfaridja.ezrahi.data.repository.EzrahiRepositoryImpl
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEzrahiRepository(
        impl: EzrahiRepositoryImpl
    ): EzrahiRepository

    @Binds
    @Singleton
    abstract fun bindDataRepo(
        impl: FirebaseDataRepo
    ): IDataRepo
}
