package com.example.btmic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CamStore {
    fun dir(ctx: Context): File = File(ctx.filesDir, "videos").also { it.mkdirs() }

    fun newFile(ctx: Context): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir(ctx), "vid_$stamp.mp4")
    }

    fun list(ctx: Context): List<File> =
        dir(ctx).listFiles { f -> f.extension == "mp4" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
}

class CameraService : LifecycleService() {

    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            "START" -> {
                startForegroundNotification()
                setupCameraAndRecord()
            }
            "STOP" -> stopRecordingAndService()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "camera_rec"
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(channelId, "Video Recording", NotificationManager.IMPORTANCE_LOW)
        )
        val stopPi = PendingIntent.getService(
            this, 2,
            Intent(this, CameraService::class.java).setAction("STOP"),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_media_pause), "Band karo", stopPi
        ).build()
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Video recording chal raha hai")
            .setContentText("BT Mic camera se record kar raha hai")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .addAction(stopAction)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else {
            startForeground(101, notification)
        }
        CamState.running = true
    }

    private fun setupCameraAndRecord() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, videoCapture)
                startVideoRecording()
            } catch (e: Exception) {
                stopSelf()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startVideoRecording() {
        val capture = videoCapture ?: return
        val file = CamStore.newFile(this)
        val output = FileOutputOptions.Builder(file).build()
        activeRecording = capture.output
            .prepareRecording(this, output)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(this)) { event ->
                if (event is VideoRecordEvent.Finalize && event.hasError()) {
                    activeRecording = null
                    stopSelf()
                }
            }
    }

    private fun stopRecordingAndService() {
        activeRecording?.stop()
        activeRecording = null
        CamState.running = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        activeRecording?.stop()
        CamState.running = false
    }
}
