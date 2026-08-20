package com.arielfaridja.ezrahi.util.logging

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds the Firestore `app_errors` document for a throwable (spec §4) and
 * shared helpers (stack formatting, dedup hash, version/ISO stamp, JSON→map).
 * Pure and dependency-light so it is safe to call from the crash path.
 */
object ErrorRecord {

    const val MAX_MESSAGE = 2048
    const val MAX_STACK = 8192
    const val MAX_CAUSE = 1024

    fun stackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }

    fun computeHash(errorType: ErrorType, throwable: Throwable): String {
        val top = throwable.stackTrace.firstOrNull()?.toString() ?: "unknown"
        val input = "${errorType.raw}|${throwable.javaClass.name}|$top"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }

    @Suppress("DEPRECATION")
    fun version(context: Context): Pair<String, Int> {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName.orEmpty() to info.versionCode
        } catch (e: Exception) {
            "unknown" to 0
        }
    }

    fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date())

    fun build(
        context: Context,
        throwable: Throwable,
        errorType: ErrorType,
        severity: Severity,
        isFatal: Boolean,
        threadName: String,
        userId: String,
        sessionId: String,
        eventId: String?,
        screen: String?,
        breadcrumbs: List<String>,
        inForeground: Boolean?
    ): Map<String, Any?> {
        val (appVersion, buildCode) = version(context)
        val rawMessage = throwable.message ?: throwable.javaClass.simpleName
        return mapOf(
            "id" to java.util.UUID.randomUUID().toString(),
            "errorType" to errorType.raw,
            "severity" to severity.raw,
            "message" to PiiSanitizer.sanitize(rawMessage).take(MAX_MESSAGE),
            "cause" to throwable.cause?.message?.let { PiiSanitizer.sanitize(it).take(MAX_CAUSE) },
            "stackTrace" to PiiSanitizer.sanitize(stackTrace(throwable)).take(MAX_STACK),
            "errorHash" to computeHash(errorType, throwable),
            "timestamp" to System.currentTimeMillis(),
            "timestampIso" to isoNow(),
            "appVersion" to appVersion,
            "buildCode" to buildCode,
            "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "osVersion" to Build.VERSION.RELEASE,
            "osSdk" to Build.VERSION.SDK_INT,
            "userId" to userId,
            "eventId" to eventId,
            "screen" to screen,
            "sessionId" to sessionId,
            "threadName" to threadName,
            "isFatal" to isFatal,
            "inForeground" to inForeground,
            "breadcrumbs" to breadcrumbs
        )
    }

    fun toMutableMap(json: JSONObject): MutableMap<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        json.keys().forEach { key ->
            val v = json.get(key)
            map[key] = if (v == JSONObject.NULL) null else v
        }
        return map
    }
}