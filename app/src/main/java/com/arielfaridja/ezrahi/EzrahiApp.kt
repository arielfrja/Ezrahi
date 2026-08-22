package com.arielfaridja.ezrahi

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.arielfaridja.ezrahi.util.logging.ExceptionLogger
import com.arielfaridja.ezrahi.work.OutboxSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre
import javax.inject.Inject

@HiltAndroidApp
class EzrahiApp : Application(), Configuration.Provider {

    @Inject lateinit var exceptionLogger: ExceptionLogger
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var outboxSyncScheduler: OutboxSyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        exceptionLogger.installCrashHandler()
        exceptionLogger.flushPendingCrashDumps()
        outboxSyncScheduler.schedule()
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