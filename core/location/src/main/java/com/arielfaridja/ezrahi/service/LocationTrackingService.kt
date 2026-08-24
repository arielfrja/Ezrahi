package com.arielfaridja.ezrahi.service

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.arielfaridja.ezrahi.domain.model.GeoPoint
import com.arielfaridja.ezrahi.domain.repository.EzrahiRepository
import com.arielfaridja.ezrahi.location.AdaptiveLocationEngine
import com.arielfaridja.ezrahi.location.LocationProfileData
import com.arielfaridja.ezrahi.util.logging.ErrorType
import com.arielfaridja.ezrahi.util.logging.ExceptionLogger
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var repository: EzrahiRepository
    @Inject lateinit var logger: ExceptionLogger
    @Inject lateinit var adaptiveEngine: AdaptiveLocationEngine

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var eventId: String = ""
    private var userId: String = ""
    private var contentActivityName: String? = null

    private var batteryReceiver: BroadcastReceiver? = null
    private var activityReceiver: BroadcastReceiver? = null
    private var activityPendingIntent: PendingIntent? = null

    companion object {
        const val EXTRA_CONTENT_ACTIVITY = "EXTRA_CONTENT_ACTIVITY"
        const val EXTRA_EMERGENCY_SOS = "EXTRA_EMERGENCY_SOS"
        const val ACTION_ACTIVITY_TRANSITION = "com.arielfaridja.ezrahi.ACTION_ACTIVITY_TRANSITION"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    adaptiveEngine.onLocationReceived(loc)
                    if (eventId.isNotEmpty() && userId.isNotEmpty()) {
                        serviceScope.launch {
                            runCatching {
                                repository.updateLocation(
                                    eventId = eventId,
                                    userId = userId,
                                    location = GeoPoint(loc.latitude, loc.longitude, System.currentTimeMillis())
                                ).getOrThrow()
                            }.onFailure { e ->
                                logger.log(e, ErrorType.NETWORK, eventId, screen = "location_service")
                            }
                        }
                    }
                }
            }
        }

        registerBatteryMonitor()
        registerActivityRecognition()
        observeAdaptiveEngine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        eventId = intent?.getStringExtra("EXTRA_EVENT_ID") ?: ""
        userId = intent?.getStringExtra("EXTRA_USER_ID") ?: ""
        contentActivityName = intent?.getStringExtra(EXTRA_CONTENT_ACTIVITY)
        val sosActive = intent?.getBooleanExtra(EXTRA_EMERGENCY_SOS, false) ?: false

        adaptiveEngine.setEmergencySos(sosActive)

        try {
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    1001,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(1001, notification)
            }
        } catch (e: Exception) {
            logger.log(e, ErrorType.LOCATION_SERVICE, eventId, screen = "location_service")
        }

        return START_STICKY
    }

    private fun observeAdaptiveEngine() {
        serviceScope.launch {
            adaptiveEngine.effectiveConfig.collectLatest { config ->
                applyLocationConfig(config)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun applyLocationConfig(config: LocationProfileData) {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            val request = adaptiveEngine.buildLocationRequest(config)
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            logger.log(e, ErrorType.LOCATION_SERVICE, eventId, screen = "location_service")
        }
    }

    private fun registerBatteryMonitor() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
                adaptiveEngine.setBatteryPercent(batteryPct)
                val isLowPower = batteryPct < 15 && !isCharging
                adaptiveEngine.setLowPowerMode(isLowPower)
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    private fun registerActivityRecognition() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        runCatching {
            activityReceiver = ActivityTransitionReceiver().also {
                registerReceiver(it, IntentFilter(ACTION_ACTIVITY_TRANSITION))
            }
            val intent = Intent(this, ActivityTransitionReceiver::class.java)
                .setAction(ACTION_ACTIVITY_TRANSITION)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            activityPendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags)

            val request = ActivityTransitionRequest(
                listOf(
                    DetectedActivity.STILL,
                    DetectedActivity.WALKING,
                    DetectedActivity.RUNNING,
                    DetectedActivity.ON_FOOT,
                    DetectedActivity.IN_VEHICLE
                ).map { activityType ->
                    ActivityTransition.Builder()
                        .setActivityType(activityType)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()
                }
            )
            ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, activityPendingIntent!!)
                .addOnFailureListener { e ->
                    logger.log(e, ErrorType.LOCATION_SERVICE, eventId, screen = "location_service")
                }
        }.onFailure { e ->
            logger.log(e, ErrorType.LOCATION_SERVICE, eventId, screen = "location_service")
        }
    }

    private fun createNotification(): Notification {
        val contentIntent = contentActivityName?.let { name ->
            runCatching {
                PendingIntent.getActivity(
                    this, 0, Intent(this, Class.forName(name)),
                    PendingIntent.FLAG_IMMUTABLE
                )
            }.getOrNull()
        }

        val config = adaptiveEngine.effectiveConfig.value
        val subtitle = if (config.isLowPowerActive) {
            "Transmitting location (Low Power Battery Saver)"
        } else {
            "Transmitting location (${config.profile.name})"
        }

        val builder = NotificationCompat.Builder(this, "ezrahi_tracking_channel")
            .setContentTitle("Ezrahi Field Tracking Active")
            .setContentText(subtitle)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
        contentIntent?.let { builder.setContentIntent(it) }
        return builder.build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "ezrahi_tracking_channel",
            "Field Location Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryReceiver?.let {
            runCatching { unregisterReceiver(it) }
        }
        activityPendingIntent?.let { pi ->
            runCatching { ActivityRecognition.getClient(this).removeActivityTransitionUpdates(pi) }
        }
        activityReceiver?.let {
            runCatching { unregisterReceiver(it) }
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
