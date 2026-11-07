package com.example.btmic

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

private const val SERVER_HOST = "traker-mi1p.onrender.com"

@Composable
fun LocationTab() {
    val ctx = LocalContext.current
    var sharing by remember { mutableStateOf(LocationState.sharing) }
    var room by remember { mutableStateOf(LocationState.room) }
    var mode by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            sharing = LocationState.sharing
            delay(500)
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            ctx.startForegroundService(Intent(ctx, LocationShareService::class.java))
        } else {
            Toast.makeText(ctx, "Location permission chahiye", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Cyan, Violet))),
                contentAlignment = Alignment.Center
            ) { PinIcon(Modifier.size(22.dp), Color.White) }
            Column(Modifier.padding(start = 12.dp)) {
                Text("Location Tracker", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Dono taraf se raazi hoke live location share karo",
                    color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        SegmentSwitch(
            options = listOf("Share karo", "Dekho"),
            selected = mode,
            onSelect = { mode = it }
        )

        Spacer(Modifier.height(16.dp))

        Glass {
            FieldLabel("Room code")
            OutlinedTextField(
                value = room,
                onValueChange = { v ->
                    val d = v.filter { it.isDigit() }.take(4)
                    room = d
                    LocationState.room = d
                    LocationState.save(ctx)
                },
                placeholder = { Text("dono taraf same, jaise 1234", color = Color.White.copy(alpha = 0.3f)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
        }

        Spacer(Modifier.height(16.dp))

        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "loc-mode"
        ) { m ->
            if (m == 0) {
                ShareCard(sharing = sharing) { on ->
                    if (on) {
                        val missing = arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ).filter {
                            ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
                        }
                        if (missing.isNotEmpty()) {
                            permLauncher.launch(missing.toTypedArray())
                        } else {
                            ctx.startForegroundService(Intent(ctx, LocationShareService::class.java))
                        }
                    } else {
                        ctx.startService(Intent(ctx, LocationShareService::class.java).setAction("STOP"))
                    }
                }
            } else {
                LocationMap(room = room)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun SegmentSwitch(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(22.dp))
            .padding(5.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (sel) Brush.linearGradient(listOf(Cyan, Violet))
                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onSelect(i) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (sel) Color.White else Color.White.copy(alpha = 0.55f),
                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
    focusedBorderColor = Cyan, unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
    cursorColor = Cyan
)

@Composable
fun ShareCard(sharing: Boolean, onToggle: (Boolean) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LocationOrb(active = sharing)
        Spacer(Modifier.height(18.dp))
        Text(
            if (sharing) "Location share ho rahi hai" else "Sharing band hai",
            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
        )
        Text(
            if (sharing) "Notification me hamesha dikhega. Kabhi bhi band kar sakte ho"
            else "Dabao to apni live location share karna shuru karo",
            color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp
        )
        Spacer(Modifier.height(18.dp))

        val pillModifier = if (sharing) {
            Modifier
                .background(Rose.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                .border(1.dp, Rose.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
        } else {
            Modifier.background(Brush.linearGradient(listOf(Cyan, Violet)), RoundedCornerShape(24.dp))
        }

        Box(
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .then(pillModifier)
                .clickable { onToggle(!sharing) }
                .padding(horizontal = 32.dp, vertical = 14.dp)
        ) {
            Text(
                if (sharing) "Band karo" else "Share karo",
                color = if (sharing) Rose else Color.White,
                fontSize = 15.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LocationOrb(active: Boolean) {
    val inf = rememberInfiniteTransition(label = "loc-pulse")
    val pulse by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "lp"
    )
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val base = size.minDimension * 0.26f
            if (active) {
                for (k in 0..2) {
                    val t = (pulse + k / 3f) % 1f
                    drawCircle(
                        Green.copy(alpha = (1f - t) * 0.45f),
                        radius = base + t * size.minDimension * 0.28f,
                        style = Stroke(3.dp.toPx())
                    )
                }
            } else {
                drawCircle(Color.White.copy(alpha = 0.05f), radius = base * 1.5f)
            }
        }
        Box(
            Modifier.size(78.dp).clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        if (active) listOf(Green, Cyan) else listOf(Color(0xFF2B2B4A), Color(0xFF1A1A30))
                    )
                ),
            contentAlignment = Alignment.Center
        ) { PinIcon(Modifier.size(34.dp), Color.White) }
    }
}

@Composable
fun PinIcon(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            color, radius = w * 0.1f,
            center = Offset(w * 0.5f, h * 0.38f)
        )
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            cubicTo(w * 0.82f, h * 0.05f, w * 0.9f, h * 0.3f, w * 0.72f, h * 0.55f)
            lineTo(w * 0.5f, h * 0.95f)
            lineTo(w * 0.28f, h * 0.55f)
            cubicTo(w * 0.1f, h * 0.3f, w * 0.18f, h * 0.05f, w * 0.5f, h * 0.05f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LocationMap(room: String) {
    val ctx = LocalContext.current
    var offlineMode by remember { mutableStateOf(false) }
    var offlineReady by remember { mutableStateOf(false) }

    LaunchedEffect(offlineMode) {
        if (offlineMode) {
            OfflineTileServer.start(ctx)
            offlineReady = OfflineTileServer.ready
        }
    }

    val base = "https://$SERVER_HOST"
    val tileUrl = if (offlineMode && offlineReady)
        "http://127.0.0.1:8744/{z}/{x}/{y}.png"
    else
        "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"

    val html = """
        <!DOCTYPE html>
        <html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>
          html,body{margin:0;background:#070712;font-family:-apple-system,Segoe UI,Roboto,sans-serif;height:100%;overflow:hidden}
          #map{height:100%;width:100%;filter:invert(1) hue-rotate(180deg) brightness(0.92) contrast(0.9) saturate(0.9)}
          #card{
            position:absolute;left:14px;right:14px;bottom:14px;
            background:rgba(19,15,43,0.85);backdrop-filter:blur(14px);-webkit-backdrop-filter:blur(14px);
            border:1px solid rgba(255,255,255,0.12);border-radius:22px;padding:16px 18px;color:#fff;
            box-shadow:0 10px 30px rgba(0,0,0,0.4);display:none;
          }
          #status{display:flex;align-items:center;gap:8px;margin-bottom:8px}
          #dot{width:9px;height:9px;border-radius:50%;background:#34E39B;box-shadow:0 0 10px #34E39B;animation:blink 1.6s infinite}
          @keyframes blink{0%,100%{opacity:1}50%{opacity:0.3}}
          #addr{font-size:14.5px;line-height:1.4;font-weight:600}
          #coords{font-size:12.5px;color:#4FD1FF;margin-top:8px;font-weight:600}
          #time{font-size:11.5px;color:rgba(255,255,255,0.45);margin-top:5px}
          .leaflet-control-attribution{font-size:9px;background:rgba(7,7,18,0.6)!important;color:#888!important}
          .leaflet-control-zoom a{background:rgba(19,15,43,0.85)!important;color:#fff!important;border:1px solid rgba(255,255,255,0.12)!important}
        </style>
        </head><body>
        <div id="map"></div>
        <div id="card">
          <div id="status"><div id="dot"></div><span style="font-size:11.5px;color:rgba(255,255,255,0.6);font-weight:700;letter-spacing:0.5px;text-transform:uppercase">Live</span></div>
          <div id="addr">Location dhundi ja rahi hai...</div>
          <div id="coords"></div>
          <div id="time"></div>
        </div>
        <script>
        var map = L.map('map', {zoomControl:true, attributionControl:true}).setView([20.5937,78.9629], 4);
        L.tileLayer('$tileUrl', { attribution: 'Map data', subdomains: 'abc', maxZoom: 19 }).addTo(map);

        var pulseIcon = L.divIcon({
            className: '',
            html: '<div style="width:18px;height:18px;border-radius:50%;background:#4FD1FF;border:3px solid #fff;box-shadow:0 0 16px #4FD1FF"></div>',
            iconSize: [18,18], iconAnchor: [9,9]
        });
        var marker = null, lastKey = "";

        function reverseGeocode(lat, lng) {
            var key = lat.toFixed(4) + "," + lng.toFixed(4);
            if (key === lastKey) return;
            lastKey = key;
            fetch('https://nominatim.openstreetmap.org/reverse?format=json&lat=' + lat + '&lon=' + lng)
                .then(function(r){ return r.json(); })
                .then(function(d){ document.getElementById('addr').innerText = d.display_name || "Pata nahi mil paya"; })
                .catch(function(){ document.getElementById('addr').innerText = "Pata load nahi ho paya"; });
        }

        function poll() {
            fetch('$base/location/$room').then(function(r){ return r.json(); }).then(function(d){
                if (d && d.lat) {
                    document.getElementById('card').style.display = 'block';
                    var pos = [d.lat, d.lng];
                    if (!marker) { marker = L.marker(pos, {icon: pulseIcon}).addTo(map); map.setView(pos, 16); }
                    else { marker.setLatLng(pos); }
                    reverseGeocode(d.lat, d.lng);
                    document.getElementById('coords').innerText = "GPS  " + d.lat.toFixed(6) + ", " + d.lng.toFixed(6);
                    document.getElementById('time').innerText = "Last update " + new Date(d.time).toLocaleTimeString();
                }
            }).catch(function(){});
        }
        poll();
        setInterval(poll, 5000);
        </script>
        </body></html>
    """.trimIndent()

    Column {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.End) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (offlineMode) Green.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f))
                    .clickable { offlineMode = !offlineMode }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    if (offlineMode) "Offline Map ON" else "Offline Map OFF",
                    color = if (offlineMode) Green else Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(500.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                update = { it.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null) },
                factory = { c ->
                    WebView(c).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    }
                }
            )
        }
    }
}
