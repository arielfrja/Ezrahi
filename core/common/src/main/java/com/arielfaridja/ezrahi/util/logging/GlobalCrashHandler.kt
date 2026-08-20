package com.arielfaridja.ezrahi.util.logging

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Fatal-crash handler (spec §5.2/§5.3): synchronously writes a structured JSON
 * crash dump to `filesDir/crash_reports/crash_<guid>.json` with fsync, then
 * ALWAYS delegates to the previous handler so the OS terminates the process.
 * No network / Firestore / Room in the crash path.
 */
class GlobalCrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
    private val enrichment: () -> Map<String, Any?>
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val dir = File(context.filesDir, "crash_reports").apply { mkdirs() }
            val id = UUID.randomUUID().toString()
            val file = File(dir, "crash_$id.json")

            val base = ErrorRecord.build(
                context = context,
                throwable = throwable,
                errorType = ErrorType.CRASH,
                severity = Severity.FATAL,
                isFatal = true,
                threadName = thread.name,
                userId = "anon-pending",
                sessionId = "pending",
                eventId = null,
                screen = null,
                breadcrumbs = emptyList(),
                inForeground = null
            )
            val record = HashMap(base)
            record["id"] = id
            runCatching { enrichment() }.getOrDefault(emptyMap()).forEach { (k, v) -> record[k] = v }
            record["userId"] = record["userId"] ?: "anon-pending"
            record["sessionId"] = record["sessionId"] ?: "pending"

            val json = JSONObject(record).toString()
            FileOutputStream(file).use { fos ->
                fos.write(json.toByteArray())
                fos.flush()
                fos.fd.sync()
            }
            Log.e("EzrahiCrash", "Crash dump saved: ${file.absolutePath} ($id)")
        } catch (e: Throwable) {
            Log.e("EzrahiCrash", "Failed to write crash dump", e)
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        fun install(context: Context, enrichment: () -> Map<String, Any?> = { emptyMap() }) {
            val current = Thread.getDefaultUncaughtExceptionHandler()
            if (current !is GlobalCrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(GlobalCrashHandler(context.applicationContext, current, enrichment))
            }
        }
    }
}