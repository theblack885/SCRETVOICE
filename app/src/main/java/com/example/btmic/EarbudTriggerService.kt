package com.example.btmic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.core.content.ContextCompat

class EarbudTriggerService : Service() {

    private var mediaSession: MediaSessionCompat? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundNotification()
        setupMediaSession()
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "earbud_trigger"
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(channelId, "Earbud Shortcut", NotificationManager.IMPORTANCE_LOW)
        )
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Earbud shortcut chalu hai")
            .setContentText("Earbuds ka button dabake camera recording shuru/band karo")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(201, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(201, notification)
        }
    }

    private fun setupMediaSession() {
        if (mediaSession != null) return
        val session = MediaSessionCompat(this, "BtMicEarbud")
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .build()
        )
        session.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                val event = mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (event != null && event.action == KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_HEADSETHOOK,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            toggleCameraRecording()
                            return true
                        }
                    }
                }
                return super.onMediaButtonEvent(mediaButtonIntent)
            }
        })
        session.isActive = true
        mediaSession = session
    }

    private fun toggleCameraRecording() {
        if (CamState.running) {
            startService(Intent(this, CameraService::class.java).setAction("STOP"))
            return
        }
        val missing = neededCameraPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) return
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(Intent(this, CameraService::class.java).setAction("START"))
        } else {
            startService(Intent(this, CameraService::class.java).setAction("START"))
        }
    }

    override fun onDestroy() {
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
