package com.example.btmic

import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun neededCameraPermissions(): Array<String> = arrayOf(
    android.Manifest.permission.CAMERA,
    android.Manifest.permission.RECORD_AUDIO
)

fun currentTimePin(): String =
    SimpleDateFormat("HHmm", Locale.US).format(Date())

@Composable
fun CameraTab() {
    val ctx = LocalContext.current
    var running by remember { mutableStateOf(CamState.running) }
    var unlocked by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var files by remember { mutableStateOf(listOf<File>()) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick) { files = CamStore.list(ctx) }
    LaunchedEffect(Unit) {
        while (true) {
            running = CamState.running
            delay(400)
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            ctx.startForegroundService(Intent(ctx, CameraService::class.java).setAction("START"))
        } else {
            Toast.makeText(ctx, "Camera aur mic permission chahiye", Toast.LENGTH_LONG).show()
        }
    }

    fun toggleRec() {
        if (running) {
            ctx.startService(Intent(ctx, CameraService::class.java).setAction("STOP"))
            return
        }
        val missing = neededCameraPermissions().filter {
            ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permLauncher.launch(missing.toTypedArray())
            return
        }
        ctx.startForegroundService(Intent(ctx, CameraService::class.java).setAction("START"))
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Text("Camera", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text(
            "Background me video record karo, notification me hamesha saaf dikhega.",
            color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))

        Glass {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(12.dp).clip(CircleShape)
                        .background(if (running) Rose else Color.Gray)
                )
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        if (running) "Recording chal rahi hai" else "Recording band hai",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Jab tak chalu hai, notification bar me dikhta rahega",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background((if (running) Rose else Green).copy(alpha = 0.18f))
                        .border(1.dp, (if (running) Rose else Green).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .clickable { toggleRec() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        if (running) "Stop" else "Start",
                        color = if (running) Rose else Green, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Glass {
            ToggleRow(
                title = "Earbuds button se recording",
                sub = "Earbuds ka click/button dabake camera recording shuru-band karo",
                checked = CamState.earbudEnabled
            ) { on ->
                CamState.earbudEnabled = on
                CamState.save(ctx)
                if (on) {
                    ctx.startForegroundService(Intent(ctx, EarbudTriggerService::class.java))
                } else {
                    ctx.startService(Intent(ctx, EarbudTriggerService::class.java).setAction("STOP"))
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        if (!unlocked) {
            Glass {
                Text("Videos dekhne ke liye abhi ka time PIN ke roop me daalo", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Jaise abhi time 11:29 hai to PIN '1129' daalo", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it.filter { c -> c.isDigit() }.take(4) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = Keyboard
