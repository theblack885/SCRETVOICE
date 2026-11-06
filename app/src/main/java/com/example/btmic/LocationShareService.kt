package com.example.btmic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class LocationShareService : Service(), LocationListener {

    private lateinit var lm: LocationManager
    private val client = OkHttpClient()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        if (LocationState.sharing) return START_NOT_STICKY

        val fine = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine) {
            stopSelf()
            return START_NOT_STICKY
        }

        val channelId = "loc_share"
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(channelId, "Location Sharing", NotificationManager.IMPORTANCE_LOW)
        )
        val stopPi = PendingIntent.getService(
            this, 4,
            Intent(this, LocationShareService::class.java).setAction("STOP"),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_media_pause), "Band karo", stopPi
        ).build()
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Location sharing chal rahi hai")
            .setContentText("Aapki live location share ho rahi hai")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .addAction(stopAction)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(301, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(301, notification)
        }

        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, this, Looper.getMainLooper())
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, this, Looper.getMainLooper())
        } catch (_: Exception) {
        }

        LocationState.sharing = true
        return START_STICKY
    }

    override fun onLocationChanged(location: Location) {
        sendLocation(location.latitude, location.longitude)
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    private fun sendLocation(lat: Double, lng: Double) {
        val host = LocationState.serverHost
        if (host.isBlank()) return
        val base = if (host.startsWith("http")) host else "https://$host"
        val url = "$base/location"
        val body = JSONObject().apply {
            put("room", LocationState.room)
            put("lat", lat)
            put("lng", lng)
        }.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url).post(body).build()
        Thread {
            try {
                client.newCall(req).execute().close()
            } catch (_: Exception) {
            }
        }.start()
    }

    override fun onDestroy() {
        try { lm.removeUpdates(this) } catch (_: Exception) {}
        LocationState.sharing = false
        super.onDestroy()
    }
}
