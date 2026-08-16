package com.arielfaridja.ezrahi

import android.app.Application
import com.arielfaridja.ezrahi.util.CrashLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EzrahiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}
