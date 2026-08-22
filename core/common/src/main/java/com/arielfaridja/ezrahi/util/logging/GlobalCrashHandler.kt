package com.arielfaridja.ezrahi.util.logging

import android.content.Context

/**
 * Legacy crash handler wrapper. 
 * Delegated directly to default handlers (such as Firebase Crashlytics)
 * for central monitoring, removing local filesystem overhead.
 */
class GlobalCrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        defaultHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        fun install(context: Context, enrichment: () -> Map<String, Any?> = { emptyMap() }) {
            val current = Thread.getDefaultUncaughtExceptionHandler()
            if (current !is GlobalCrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(GlobalCrashHandler(context.applicationContext, current))
            }
        }
    }
}
