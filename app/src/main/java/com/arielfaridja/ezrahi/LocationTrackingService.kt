package com.arielfaridja.ezrahi

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.arielfaridja.ezrahi.UI.Main.MainActivity
import com.arielfaridja.ezrahi.data.DataRepoFactory
import com.arielfaridja.ezrahi.entities.User


private const val REFRESH_INTERVAL = 15000L

class LocationTrackingService : Service() {

    val dataRepo = DataRepoFactory.getInstance()
    lateinit var currentUser: User
    private var lastLocation: Location? = null
    private lateinit var locationManager: LocationManager
    private val handler = Handler(Looper.getMainLooper())

    private val notificationId = 1
    private val notificationChannelId = "location_tracking_service"
    private val notificationTitle = "Location Tracking Service"
    private val notificationText = "The location trackong service run in background"
    private val locationListener = LocationListener { location ->
        if (lastLocation == null || location.latitude != lastLocation!!.latitude || location.longitude != lastLocation!!.longitude) {
            // Save the location to the database
            lastLocation = location
            dataRepo.user_getCurrent().location.latitude = location.latitude
            dataRepo.user_getCurrent().location.longitude = location.longitude
            dataRepo.user_update(dataRepo.user_getCurrent())
        }
    }

    private fun createNotification(): Notification {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                notificationChannelId,
                notificationTitle,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopSelf = Intent(this, LocationTrackingService::class.java)
        stopSelf.action = ACTION_STOP
        val sPendingIntent = PendingIntent
            .getService(
                this, 0, stopSelf, PendingIntent.FLAG_IMMUTABLE
            ) // That you should change this part in your code

        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.ic_baseline_my_location_24)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "STOP", sPendingIntent)
        return notificationBuilder.build()
    }

    private val runnable = object : Runnable {
        override fun run() {
            resyncActivityUsers()
            handler.postDelayed(
                this,
                REFRESH_INTERVAL
            ) // Get users from the database every 15 seconds
        }
    }


    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            10000,
            5f,
            locationListener
        )
    }


    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        locationManager.removeUpdates(locationListener)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {
            ACTION_STOP -> {
                if (!MainActivity.getIsVisible()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    //NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
                }
            }
            else -> {
                startForeground(notificationId, createNotification())
                locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                try {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 1000, 0f, locationListener
                    )
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
                handler.post(runnable)
            }
        }
        return START_NOT_STICKY

    }

    private fun resyncActivityUsers() {
        dataRepo.user_getAllByCurrentActivity()
    }

    companion object {
        const val LOCATION_SERVICE_CHANNEL = "LocationService"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "ACTION_STOP"
    }
}