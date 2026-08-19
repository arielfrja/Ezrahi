package com.arielfaridja.ezrahi.app.util

import android.content.Context

object EventPrefs {
    private const val PREFS_NAME = "ezrahi_prefs"
    private const val KEY_LAST_EVENT_ID = "last_event_id"

    fun saveLastEventId(context: Context, eventId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_EVENT_ID, eventId)
            .apply()
    }

    fun getLastEventId(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_EVENT_ID, null)
}