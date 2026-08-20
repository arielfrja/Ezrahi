package com.arielfaridja.ezrahi.util.logging

import android.content.Context
import android.os.Build
import android.util.Log
import android.util.LruCache
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global exception logger (spec todo-fix-6 v2, §5).
 *
 * Tier 1 (handled exceptions): PII-sanitize → throttle (token bucket + hourly
 * budget + circuit breaker + dedup) → direct Firestore write (offline-safe).
 * Tier 2 (fatal crashes): GlobalCrashHandler writes a disk dump; this class
 * flushes it on next launch. Logging bookkeeping is Room-free (§5.5).
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
        private const val KEY_HOUR_START = "hour_window_start"
        private const val KEY_HOUR_COUNT = "hour_count"
        private const val KEY_ANON_ID = "anon_id"
        private const val HOUR_LIMIT = 20
        private const val DEDUP_WINDOW_MS = 180_000L
        private const val BURST_PER_SECOND = 5
        private const val CIRCUIT_VIOLATIONS = 3
        private const val CIRCUIT_WINDOW_MS = 30_000L
        private const val CIRCUIT_MUTE_MS = 300_000L
        private const val MAX_BREADCRUMBS = 8
        private const val CRASH_DIR = "crash_reports"
        private const val PURGE_AGE_MS = 30L * 24 * 60 * 60 * 1000
    }

    private val loggerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionId = UUID.randomUUID().toString()
    private val breadcrumbs = ArrayDeque<String>()
    private val dedupCache = LruCache<String, Long>(50)
    private val recentWriteTimes = ArrayDeque<Long>()
    private val foregroundCount = AtomicInteger(0)

    @Volatile private var circuitOpenUntil = 0L
    @Volatile private var consecutiveBurstViolations = 0
    @Volatile private var lastBurstViolationAt = 0L
    @Volatile private var rateMarkerWritten = false

    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    private val userId: String
        get() {
            auth.currentUser?.uid?.let { return it }
            prefs.getString(KEY_ANON_ID, null)?.let { return it }
            val id = "anon-${UUID.randomUUID()}"
            prefs.edit().putString(KEY_ANON_ID, id).apply()
            return id
        }

    // ------------------------------------------------------------------
    // Lifecycle / context
    // ------------------------------------------------------------------

    fun onActivityStarted() {
        foregroundCount.incrementAndGet()
    }

    fun onActivityStopped() {
        if (foregroundCount.get() > 0) foregroundCount.decrementAndGet()
    }

    fun addBreadcrumb(crumb: String) {
        synchronized(breadcrumbs) {
            if (breadcrumbs.size >= MAX_BREADCRUMBS) breadcrumbs.removeFirst()
            breadcrumbs.addLast("[${System.currentTimeMillis()}] ${PiiSanitizer.sanitize(crumb)}")
        }
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    fun log(
        throwable: Throwable,
        errorType: ErrorType,
        eventId: String? = null,
        screen: String? = null,
        severity: Severity = Severity.ERROR
    ) {
        loggerScope.launch {
            try {
                if (!passesThrottle(throwable, errorType)) return@launch
                val record = ErrorRecord.build(
                    context = context,
                    throwable = throwable,
                    errorType = errorType,
                    severity = severity,
                    isFatal = false,
                    threadName = Thread.currentThread().name,
                    userId = userId,
                    sessionId = sessionId,
                    eventId = eventId,
                    screen = screen,
                    breadcrumbs = snapshotBreadcrumbs(),
                    inForeground = foregroundCount.get() > 0
                )
                write(record)
            } catch (e: Throwable) {
                Log.e(TAG, "Logger internal failure: ${e.message}")
            }
        }
    }

    fun flushPendingCrashDumps() {
        loggerScope.launch {
            try {
                val dir = File(context.filesDir, CRASH_DIR)
                if (!dir.exists()) return@launch
                val files = dir.listFiles()
                    ?.filter { it.name.startsWith("crash_") && it.name.endsWith(".json") }
                    ?: emptyList()
                val now = System.currentTimeMillis()
                for (file in files) {
                    try {
                        if (now - file.lastModified() > PURGE_AGE_MS) {
                            file.delete()
                            continue
                        }
                        val record = ErrorRecord.toMutableMap(JSONObject(file.readText()))
                        auth.currentUser?.uid?.let { record["userId"] = it }
                        record["isFatal"] = true
                        val id = record["id"] as? String ?: continue
                        firestore.collection("app_errors").document(id).set(record).await()
                        file.delete()
                        Log.i(TAG, "Flushed crash dump $id")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to flush crash dump ${file.name}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "flushPendingCrashDumps failed", e)
            }
        }
    }

    fun installCrashHandler() {
        GlobalCrashHandler.install(context) {
            runCatching {
                mapOf(
                    "sessionId" to sessionId,
                    "userId" to userId,
                    "breadcrumbs" to snapshotBreadcrumbs(),
                    "inForeground" to (foregroundCount.get() > 0)
                )
            }.getOrDefault(emptyMap())
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private fun passesThrottle(throwable: Throwable, errorType: ErrorType): Boolean {
        val now = System.currentTimeMillis()

        if (now < circuitOpenUntil) return false

        synchronized(recentWriteTimes) {
            recentWriteTimes.removeAll { now - it > 1000 }
            if (recentWriteTimes.size >= BURST_PER_SECOND) {
                if (now - lastBurstViolationAt > CIRCUIT_WINDOW_MS) consecutiveBurstViolations = 0
                consecutiveBurstViolations++
                lastBurstViolationAt = now
                if (consecutiveBurstViolations >= CIRCUIT_VIOLATIONS) {
                    circuitOpenUntil = now + CIRCUIT_MUTE_MS
                    consecutiveBurstViolations = 0
                    writeMarker("CIRCUIT_OPEN", "Logging muted ${CIRCUIT_MUTE_MS / 1000}s after >${BURST_PER_SECOND} errors/sec")
                }
                return false
            }
            recentWriteTimes.addLast(now)
        }

        val hash = ErrorRecord.computeHash(errorType, throwable)
        synchronized(dedupCache) {
            val lastSeen = dedupCache.get(hash)
            if (lastSeen != null && now - lastSeen < DEDUP_WINDOW_MS) return false
            dedupCache.put(hash, now)
        }

        var windowStart = prefs.getLong(KEY_HOUR_START, 0L)
        var count = prefs.getInt(KEY_HOUR_COUNT, 0)
        if (now - windowStart > 3_600_000L) {
            windowStart = now
            count = 0
            rateMarkerWritten = false
        }
        if (count >= HOUR_LIMIT) {
            if (!rateMarkerWritten) {
                rateMarkerWritten = true
                writeMarker("RATE_LIMITED", "Hourly limit reached ($HOUR_LIMIT/hr); further errors suppressed this hour")
            }
            return false
        }
        prefs.edit().putLong(KEY_HOUR_START, windowStart).putInt(KEY_HOUR_COUNT, count + 1).apply()
        return true
    }

    private fun snapshotBreadcrumbs(): List<String> = synchronized(breadcrumbs) { breadcrumbs.toList() }

    private fun write(record: Map<String, Any?>) {
        val id = record["id"] as? String ?: return
        firestore.collection("app_errors").document(id).set(record)
            .addOnFailureListener { e -> Log.e(TAG, "Failed to write error $id: ${e.message}") }
        Log.e(TAG, "${record["errorType"]} [$id] ${record["message"]}")
    }

    private fun writeMarker(kind: String, message: String) {
        try {
            val now = System.currentTimeMillis()
            val record = mapOf(
                "id" to UUID.randomUUID().toString(),
                "errorType" to "CAUGHT",
                "severity" to "WARNING",
                "message" to "$kind: $message",
                "stackTrace" to kind,
                "errorHash" to "marker:$kind",
                "timestamp" to now,
                "timestampIso" to ErrorRecord.isoNow(),
                "appVersion" to ErrorRecord.version(context).first,
                "buildCode" to ErrorRecord.version(context).second,
                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "osVersion" to Build.VERSION.RELEASE,
                "osSdk" to Build.VERSION.SDK_INT,
                "userId" to userId,
                "eventId" to null,
                "screen" to null,
                "sessionId" to sessionId,
                "threadName" to Thread.currentThread().name,
                "isFatal" to false,
                "inForeground" to (foregroundCount.get() > 0),
                "breadcrumbs" to snapshotBreadcrumbs()
            )
            write(record)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write marker $kind", e)
        }
    }
}