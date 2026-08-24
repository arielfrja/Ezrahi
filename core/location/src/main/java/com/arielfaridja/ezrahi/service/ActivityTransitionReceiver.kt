package com.arielfaridja.ezrahi.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.arielfaridja.ezrahi.location.AdaptiveLocationEngine
import com.google.android.gms.location.ActivityTransitionResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ActivityTransitionReceiver : BroadcastReceiver() {

    @Inject lateinit var adaptiveEngine: AdaptiveLocationEngine

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != LocationTrackingService.ACTION_ACTIVITY_TRANSITION) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        result.transitionEvents.forEach { event ->
            adaptiveEngine.onActivityTransitionDetected(event.activityType)
        }
    }
}
