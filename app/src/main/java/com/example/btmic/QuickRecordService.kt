package com.example.btmic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.IBinder

class QuickRecordService : Service() {

    @Volatile private var loop = false
    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        if (QuickRecordState.running) return START_NOT_STICKY

        val channelId = "quick_record"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Voice Recording", NotificationManager.IMPORTANCE_LOW)
        )
        val stopPi = PendingIntent.getService(
            this, 3,
            Intent(this, QuickRecordService::class.java).setAction("STOP"),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_media_pause), "Band karo", stopPi
        ).build()
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Voice recording chal rahi hai")
            .setContentText("Earbuds button se shuru hui")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .addAction(stopAction)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(102, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(102, notification)
        }

        QuickRecordState.running = true
        loop = true
        worker = Thread { runRecording() }.also { it.start() }
        return START_NOT_STICKY
    }

    private fun runRecording() {
        val rate = 44100
        val inMin = AudioRecord.getMinBufferSize(
            rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC, rate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, inMin * 2
        )
        val fx = mutableListOf<AudioEffect>()
        try {
            if (NoiseSuppressor.isAvailable())
                NoiseSuppressor.create(rec.audioSessionId)?.also { it.enabled = true; fx.add(it) }
            if (AutomaticGainControl.isAvailable())
                AutomaticGainControl.create(rec.audioSessionId)?.also { it.enabled = true; fx.add(it) }
            if (AcousticEchoCanceler.isAvailable())
                AcousticEchoCanceler.create(rec.audioSessionId)?.also { it.enabled = true; fx.add(it) }
        } catch (_: Exception) {}

        val file = RecStore.newFile(this)
        val wav = WavWriter(file, rate)
        val buf = ShortArray(inMin / 2)

        try {
            rec.startRecording()
            while (loop) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) wav.write(buf, n)
            }
        } catch (_: Exception) {
        } finally {
            try { wav.close() } catch (_: Exception) {}
            try { rec.stop() } catch (_: Exception) {}
            rec.release()
            fx.forEach { try { it.release() } catch (_: Exception) {} }
        }
    }

    override fun onDestroy() {
        loop = false
        worker?.join(1000)
        QuickRecordState.running = false
        super.onDestroy()
    }
}
