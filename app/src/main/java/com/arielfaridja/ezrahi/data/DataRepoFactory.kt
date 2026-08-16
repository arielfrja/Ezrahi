package com.arielfaridja.ezrahi.data

import android.content.Context

object DataRepoFactory {
    private var instance: IDataRepo? = null

    fun getInstance(context: Context): IDataRepo {
        if (instance == null) {
            instance = FirebaseDataRepo(context)
        }
        return instance!!
    }

    fun getInstance(): IDataRepo = instance
        ?: throw IllegalStateException("DataRepoFactory not initialized. Call getInstance(context) first.")
}
