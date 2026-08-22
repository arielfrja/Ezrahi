package com.arielfaridja.ezrahi.util.logging

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global exception logger (spec todo-fix-6 v2, §5).
 * 
 * Refactored to route all exceptions (handled and uncaught) directly to
 * Firebase Crashlytics for console tracking and automatic deobfuscation.
 * No local disk caches, file dumps, or Firestore collection writes are used.
 */
@Singleton
class ExceptionLogger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    companion object {
        private const val TAG = "EzrahiLogger"
        private const val PREFS = "ezrahi_error_logger"
        private const val KEY_ANON_ID = "anon_id"
        private const val CRASH_DIR = "crash_reports"
    }

    private val loggerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionId = UUID.randomUUID().toString()
    private val breadcrumbs = ArrayDeque<String>()
    private val foregroundCount = AtomicInteger(0)

    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    private val userId: String
        get() {
            auth.currentUser?.uid?.let { return it }
            prefs.getString(KEY_ANON_ID, null)?.let { return it }
            val id = "anon-${UUID.randomUUID()}"
            prefs.edit().putString(KEY_ANON_ID, id).apply()
            return id
        }

    fun onActivityStarted() {
        foregroundCount.incrementAndGet()
    }

    fun onActivityStopped() {
        if (foregroundCount.get() > 0) foregroundCount.decrementAndGet()
    }

    fun addBreadcrumb(crumb: String) {
        synchronized(breadcrumbs) {
            if (breadcrumbs.size >= 8) breadcrumbs.removeFirst()
            val sanitized = "[${System.currentTimeMillis()}] ${PiiSanitizer.sanitize(crumb)}"
            breadcrumbs.addLast(sanitized)
            runCatching {
                FirebaseCrashlytics.getInstance().log(sanitized)
            }
        }
    }

    fun log(
        throwable: Throwable,
        errorType: ErrorType,
        eventId: String? = null,
        screen: String? = null,
        severity: Severity = Severity.ERROR
    ) {
        loggerScope.launch {
            try {
                val crashlytics = FirebaseCrashlytics.getInstance()
                
                // Configure context key-value pairs
                crashlytics.setUserId(userId)
                crashlytics.setCustomKey("session_id", sessionId)
                crashlytics.setCustomKey("error_type", errorType.name)
                crashlytics.setCustomKey("severity", severity.name)
                crashlytics.setCustomKey("event_id", eventId ?: "none")
                crashlytics.setCustomKey("screen", screen ?: "none")
                crashlytics.setCustomKey("in_foreground", foregroundCount.get() > 0)
                
                // Record the exception to the Firebase Console
                crashlytics.recordException(throwable)
                
                Log.e(TAG, "Exception recorded in Crashlytics: ${throwable.message} (Type: ${errorType.name})")
            } catch (e: Throwable) {
                Log.e(TAG, "Logger internal failure: ${e.message}")
            }
        }
    }

    fun flushPendingCrashDumps() {
        loggerScope.launch {
            try {
                // Clear any legacy on-disk reports to reclaim storage space
                val dir = File(context.filesDir, CRASH_DIR)
                if (dir.exists()) {
                    dir.deleteRecursively()
                    Log.i(TAG, "Cleared legacy disk crash reports folder")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to flush pending legacy dumps", e)
            }
        }
    }

    fun installCrashHandler() {
        // Crashlytics automatically sets itself as the default uncaught exception handler.
        // We set the base context properties here.
        runCatching {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setUserId(userId)
            crashlytics.setCustomKey("session_id", sessionId)
        }
    }
}
