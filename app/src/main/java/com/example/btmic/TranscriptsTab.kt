package com.example.btmic

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TranscriptsTab() {
    val ctx = LocalContext.current
    var files by remember { mutableStateOf(listOf<File>()) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick) {
        files = RecStore.list(ctx)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(12.dp))
        Text("Voice to Text", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(
            "Apni recordings ko bina internet ke text me badlo",
            color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))

        if (files.isEmpty()) {
            Glass { Text("Abhi koi recording nahi hai.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp) }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(files, key = { it.path }) { f ->
                    TranscriptItem(f)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun TranscriptItem(file: File) {
    val ctx = LocalContext.current
    val fmt = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    var transcript by remember { mutableStateOf(Transcriber.readTranscript(file)) }
    var busy by remember { mutableStateOf(false) }
    var lang by remember { mutableStateOf("hi") }

    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .padding(14.dp)
    ) {
        Text(fmt.format(Date(file.lastModified())), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        Row {
            LangChip("हिंदी", lang == "hi") { lang = "hi" }
            Spacer(Modifier.width(8.dp))
            LangChip("English", lang == "en-us") { lang = "en-us" }
        }
        Spacer(Modifier.height(10.dp))

        TextButton(onClick = {
            if (busy) return@TextButton
            busy = true
            Transcriber.transcribe(
                ctx, file, lang,
                onResult = { text ->
                    Transcriber.saveTranscript(file, text)
                    transcript = text
                    busy = false
                },
                onError = { err ->
                    Toast.makeText(ctx, err, Toast.LENGTH_LONG).show()
                    busy = false
                }
            )
        }) {
            Text(if (busy) "Transcribe ho raha hai..." else "Text me badlo", color = Cyan, fontWeight = FontWeight.Bold)
        }

        transcript?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }
    }
}

@Composable
fun LangChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Brush.linearGradient(listOf(Cyan, Violet)) else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
