package com.arielfaridja.ezrahi.data

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

object DataRepoFactory {
    private var instance: IDataRepo? = null

    fun getInstance(context: Context): IDataRepo {
        if (instance == null) {
            instance = EntryPointAccessors.fromApplication(context, DataRepoEntryPoint::class.java).dataRepo()
        }
        return instance!!
    }

    fun getInstance(): IDataRepo = instance
        ?: throw IllegalStateException("DataRepoFactory not initialized. Call getInstance(context) first.")
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DataRepoEntryPoint {
    fun dataRepo(): IDataRepo
}