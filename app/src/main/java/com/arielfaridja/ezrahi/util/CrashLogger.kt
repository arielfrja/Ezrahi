package com.arielfaridja.ezrahi.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {

    private const val TAG = "CrashLogger"
    private const val DIR_NAME = "ezrahi"
    private var installed = false

    private lateinit var appContext: Context

    @Synchronized
    fun install(context: Context) {
        if (installed) return
        installed = true
        appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                persist(appContext, thread, throwable)
            } catch (t: Throwable) {
                Log.e(TAG, "failed to persist crash", t)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun log(tag: String, throwable: Throwable) {
        if (!installed) return
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            pw.println("=== LOG [$tag] ${timestamp()} ===")
            pw.println()
            throwable.printStackTrace(pw)
            pw.flush()
            persistText(sw.toString())
        } catch (t: Throwable) {
            Log.e(TAG, "failed to persist log", t)
        }
    }

    /** Append a timestamped step marker to Download/ezrahi/ezrahi_events.txt */
    fun logEvent(message: String) {
        if (!installed) return
        try {
            val line = "[${timestamp()}] $message\n"
            val resolver = appContext.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            val args = arrayOf("ezrahi_events.txt")
            var uri: Uri? = null
            resolver.query(collection, arrayOf(MediaStore.Downloads._ID), selection, args, null)?.use { c ->
                if (c.moveToFirst()) uri = ContentUris.withAppendedId(collection, c.getLong(0))
            }
            if (uri == null) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "ezrahi_events.txt")
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DIR_NAME")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                uri = resolver.insert(collection, values)
                    ?: throw IllegalStateException("no uri from MediaStore")
                resolver.openOutputStream(uri)?.use { it.write(line.toByteArray()) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                resolver.openOutputStream(uri, "wa")?.use { it.write(line.toByteArray()) }
            }
            Log.i(TAG, "event logged: $message")
        } catch (t: Throwable) {
            Log.e(TAG, "failed to log event", t)
        }
    }

    private fun persist(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        pw.println("=== CRASH ${timestamp()} ===")
        pw.println("Thread: ${thread.name}")
        pw.println("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.HARDWARE})")
        pw.println("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        pw.println("App version: ${packageVersion(context)}")
        pw.println()
        throwable.printStackTrace(pw)
        pw.flush()
        persistText(sw.toString())
    }

    private fun persistText(content: String) {
        val filename = "ezrahi_crash_${dateStamp()}.txt"
        var writtenTo = false
        try {
            writtenTo = writeToRootDir(filename, content)
        } catch (e: Exception) {
            Log.e(TAG, "root dir write failed", e)
        }
        try {
            if (!writtenTo) {
                writtenTo = writeViaMediaStore(appContext, filename, content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "media write failed", e)
        }
        try {
            if (!writtenTo) {
                writtenTo = writeToAppDir(appContext, filename, content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "app dir write failed", e)
        }
        Log.i(TAG, "crash persisted: $filename (public=$writtenTo)")
    }

    private fun writeToRootDir(filename: String, content: String): Boolean {
        val root = Environment.getExternalStorageDirectory()
        val dir = File(root, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, filename)
        file.writeText(content)
        Log.i(TAG, "crash log written: ${file.absolutePath}")
        return true
    }

    private fun writeViaMediaStore(context: Context, filename: String, content: String): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DIR_NAME")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("no uri from MediaStore")
        resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        Log.i(TAG, "crash log written via MediaStore (Download/$DIR_NAME): $filename")
        return true
    }

    private fun writeToAppDir(context: Context, filename: String, content: String): Boolean {
        val dir = context.getExternalFilesDir(null) ?: return false
        val file = File(dir, filename)
        file.writeText(content)
        Log.i(TAG, "crash log written to app dir: ${file.absolutePath}")
        return true
    }

    private fun packageVersion(context: Context): String {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${info.versionCode})"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())

    private fun dateStamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
}