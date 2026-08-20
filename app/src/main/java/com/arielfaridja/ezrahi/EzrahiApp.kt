package com.arielfaridja.ezrahi

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.arielfaridja.ezrahi.util.logging.ExceptionLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EzrahiApp : Application() {

    @Inject lateinit var exceptionLogger: ExceptionLogger

    override fun onCreate() {
        super.onCreate()
        exceptionLogger.installCrashHandler()
        exceptionLogger.flushPendingCrashDumps()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) = exceptionLogger.onActivityStarted()
            override fun onActivityStopped(activity: Activity) = exceptionLogger.onActivityStopped()
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}