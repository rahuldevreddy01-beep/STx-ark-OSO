package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.InstalledApp
import com.example.ui.StarkUiState
import com.example.ui.components.HudBracketCard
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCrimsonBorder
import com.example.ui.theme.HudCrimsonSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.HudCyanSubtle
import com.example.ui.theme.HudDivider
import com.example.ui.theme.HudEmerald
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSilver
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

@Composable
fun HealthSensorsPanel(
    state: StarkUiState,
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activityPermGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    val stepLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onPermissionGranted()
    }

    HudBracketCard(modifier = modifier.fillMaxSize(), borderColor = HudCyan, contentPadding = 16.dp) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("03 HEALTH & TELEMETRY", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudCyan)
                    Text("REAL HARDWARE SENSORS", style = MaterialTheme.typography.headlineSmall, color = HudIce)
                }
                if (!activityPermGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(HudCrimsonSubtle)
                            .border(1.dp, HudCrimson, RoundedCornerShape(4.dp))
                            .clickable { stepLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("ENABLE STEPS", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                    }
                }
            }

            // Live 3-axis Gyroscope Waveform Graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF03080F))
                    .border(1.dp, HudDivider, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                val samples = state.telemetry.gyroHistory
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (samples.size > 1) {
                        val path = Path()
                        val maxVal = (samples.maxOrNull() ?: 1f).coerceAtLeast(1.5f)
                        samples.forEachIndexed { idx, v ->
                            val x = (idx.toFloat() / (samples.size - 1)) * size.width
                            val y = size.height - ((v / maxVal).coerceIn(0f, 1f) * size.height * 0.85f) - 4f
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = HudCrimson,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    } else {
                        drawLine(
                            color = HudCyan.copy(alpha = 0.45f),
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
                Text(
                    text = "LIVE GYRO MAGNITUDE (RAD/S)",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = HudSlate,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompassMiniRadar(
                    azimuthDeg = state.telemetry.azimuthDeg,
                    modifier = Modifier.size(64.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SpecPairRow(
                        "PEDOMETER STEPS",
                        if (state.telemetry.stepsSensorAvailable) "${state.telemetry.stepsCount} STEPS" else "NO STEP HW",
                        HudCyan
                    )
                    SpecPairRow(
                        "COMPASS HEADING",
                        "${state.telemetry.cardinalDirection} ${state.telemetry.azimuthDeg}° (P:${state.telemetry.pitchDeg}° R:${state.telemetry.rollDeg}°)",
                        HudIce
                    )
                    SpecPairRow(
                        "GYROSCOPE X/Y/Z",
                        "${"%.2f".format(state.telemetry.gyroX)}, ${"%.2f".format(state.telemetry.gyroY)}, ${"%.2f".format(state.telemetry.gyroZ)}",
                        HudCrimson
                    )
                    SpecPairRow(
                        "ACCELEROMETER",
                        "${"%.1f".format(state.telemetry.accelX)}, ${"%.1f".format(state.telemetry.accelY)}, ${"%.1f".format(state.telemetry.accelZ)} m/s²",
                        HudSilver
                    )
                    SpecPairRow(
                        "LIGHT / THERMAL",
                        "${"%.0f".format(state.telemetry.ambientLux)} LUX · ${state.telemetry.batteryTempC}°C",
                        HudEmerald
                    )
                }
            }
        }
    }
}

@Composable
fun WifiNetworkPanel(
    state: StarkUiState,
    onOpenWifiPanel: () -> Unit,
    onOpenBluetoothPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    HudBracketCard(modifier = modifier.fillMaxSize(), borderColor = HudCyan, contentPadding = 16.dp) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("07 WIFI // RF DIAGNOSTICS", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudCyan)
                    Text(state.telemetry.wifiSsid, style = MaterialTheme.typography.headlineSmall, color = HudIce)
                }
                Box(
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                        .clickable(onClick = onOpenWifiPanel)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Wifi, "Open Wi-Fi panel", tint = HudCyan, modifier = Modifier.size(16.dp))
                        Text("OS WIFI", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                    }
                }
            }

            HorizontalDivider(color = HudDivider)

            SpecPairRow("TRANSPORT", state.telemetry.networkTransport, HudCyan)
            SpecPairRow("SIGNAL / RSSI", "${state.telemetry.wifiSignalPercent}% (${state.telemetry.wifiRssiDbm} dBm)", HudEmerald)
            SpecPairRow("THROUGHPUT / BAND", "${state.telemetry.wifiLinkSpeedMbps} Mbps · ${state.telemetry.wifiBandLabel}", HudIce)
            SpecPairRow("IPv4 ADDRESS", state.telemetry.ipv4Address, HudCyan)
            SpecPairRow("DNS SERVER", state.telemetry.dnsServer, HudSilver)
            SpecPairRow("BLUETOOTH", state.telemetry.bluetoothStateLabel, HudSilver)

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .clickable(onClick = onOpenWifiPanel),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SWITCH NETWORK", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCrimsonSubtle)
                        .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                        .clickable(onClick = onOpenBluetoothPanel),
                    contentAlignment = Alignment.Center
                ) {
                    Text("BLUETOOTH OS", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                }
            }
        }
    }
}

@Composable
fun CameraOpticalPanel(
    state: StarkUiState,
    onToggleTorch: () -> Unit,
    onFlipLens: () -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPerm = granted }

    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    HudBracketCard(modifier = modifier.fillMaxSize(), borderColor = HudCyan, contentPadding = 12.dp) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "05 OPTICAL HUD // ${if (state.cameraFacingBack) "REAR" else "FRONT"} · ${"%.1f".format(state.cameraZoom)}X",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = HudCyan,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (state.telemetry.flashlightOn) HudCrimsonSubtle else HudCyanSubtle)
                            .border(1.dp, if (state.telemetry.flashlightOn) HudCrimson else HudCyanBorder, RoundedCornerShape(4.dp))
                            .clickable(onClick = onToggleTorch)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.FlashlightOn, "Torch", tint = if (state.telemetry.flashlightOn) HudCrimson else HudCyan, modifier = Modifier.size(14.dp))
                            Text("TORCH", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudIce)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(HudCyanSubtle)
                            .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                            .clickable(onClick = onFlipLens)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Cameraswitch, "Flip camera", tint = HudCyan, modifier = Modifier.size(14.dp))
                            Text("FLIP", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudIce)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF03070C))
                    .border(1.dp, HudDivider, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPerm) {
                    val previewView = remember { PreviewView(context) }
                    DisposableEffect(state.cameraFacingBack, lifecycleOwner) {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        val executor = ContextCompat.getMainExecutor(context)
                        cameraProviderFuture.addListener({
                            runCatching {
                                val provider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val selector = if (state.cameraFacingBack) {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                }
                                provider.unbindAll()
                                boundCamera = provider.bindToLifecycle(lifecycleOwner, selector, preview)
                                boundCamera?.cameraControl?.setZoomRatio(state.cameraZoom)
                            }
                        }, executor)
                        onDispose {
                            if (cameraProviderFuture.isDone) {
                                runCatching { cameraProviderFuture.get().unbindAll() }
                            }
                        }
                    }

                    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

                    // Holographic reticle overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = size.minDimension * 0.22f
                        drawCircle(HudCyan.copy(alpha = 0.55f), radius = r, center = Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
                        drawLine(HudCrimson, Offset(cx - 14.dp.toPx(), cy), Offset(cx + 14.dp.toPx(), cy), 1.5.dp.toPx())
                        drawLine(HudCrimson, Offset(cx, cy - 14.dp.toPx()), Offset(cx, cy + 14.dp.toPx()), 1.5.dp.toPx())
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("CAMERA PERMISSION REQUIRED FOR LIVE HUD", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = HudSilver)
                        Box(
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(HudCyanSubtle)
                                .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                                .clickable { permLauncher.launch(Manifest.permission.CAMERA) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("GRANT CAMERA ACCESS", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = HudIce)
                        }
                    }
                }
            }

            if (hasCameraPerm) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("ZOOM", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudSlate)
                    Slider(
                        value = state.cameraZoom,
                        onValueChange = { z ->
                            onZoomChange(z)
                            runCatching { boundCamera?.cameraControl?.setZoomRatio(z) }
                        },
                        valueRange = 1f..5f,
                        colors = SliderDefaults.colors(thumbColor = HudCyan, activeTrackColor = HudCyan),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun VaultModulePanel(
    state: StarkUiState,
    onAddNote: (String, String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onStartTimer: (Int) -> Unit,
    onStopTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }

    HudBracketCard(modifier = modifier.fillMaxSize(), borderColor = HudCyan, contentPadding = 14.dp) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("04 VAULT // DATASTORE", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudCyan)
                    Text("NOTES & TACTICAL TIMER", style = MaterialTheme.typography.headlineSmall, color = HudIce)
                }
                Text(
                    text = if (state.vaultTimerRunning) "%02d:%02d".format(state.vaultTimerSeconds / 60, state.vaultTimerSeconds % 60) else "00:00",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (state.vaultTimerRunning) HudCrimson else HudSlate
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(60 to "1M", 300 to "5M", 600 to "10M").forEach { (sec, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(HudCyanSubtle)
                            .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                            .clickable { onStartTimer(sec) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCrimsonSubtle)
                        .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                        .clickable(onClick = onStopTimer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("STOP", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                }
            }

            HorizontalDivider(color = HudDivider)

            // Add Note Composer
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(0.38f)
                        .heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF040A12))
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (noteTitle.isEmpty()) Text("TITLE", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudSlate)
                    BasicTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        textStyle = TextStyle(fontFamily = RajdhaniFontFamily, fontSize = 13.sp, color = HudIce),
                        cursorBrush = SolidColor(HudCyan),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(0.62f)
                        .heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF040A12))
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (noteBody.isEmpty()) Text("ENCRYPTED NOTE BODY…", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudSlate)
                    BasicTextField(
                        value = noteBody,
                        onValueChange = { noteBody = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            onAddNote(noteTitle, noteBody)
                            noteTitle = ""
                            noteBody = ""
                        }),
                        textStyle = TextStyle(fontFamily = RajdhaniFontFamily, fontSize = 13.sp, color = HudIce),
                        cursorBrush = SolidColor(HudCyan),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                        .clickable {
                            onAddNote(noteTitle, noteBody)
                            noteTitle = ""
                            noteBody = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, "Save note", tint = HudCyan, modifier = Modifier.size(18.dp))
                }
            }

            state.preferences.vaultNotes.forEach { note ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x6606101B))
                        .border(1.dp, HudDivider, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${note.title} · ${note.timestampLabel}", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudCyan)
                        Text(note.body, style = MaterialTheme.typography.bodyMedium, color = HudIce)
                    }
                    IconButton(onClick = { onDeleteNote(note.id) }) {
                        Icon(Icons.Default.Delete, "Delete note", tint = HudCrimson, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SearchModulePanel(
    state: StarkUiState,
    onQueryChange: (String) -> Unit,
    onLaunchFullscreen: (InstalledApp) -> Unit,
    onWebSearch: (String) -> Unit,
    onAskStxAi: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val matches = remember(state.searchQuery, state.installedApps) {
        val q = state.searchQuery.trim().lowercase()
        if (q.isEmpty()) state.installedApps.take(8)
        else state.installedApps.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }.take(12)
    }

    HudBracketCard(modifier = modifier.fillMaxSize(), borderColor = HudCyan, contentPadding = 14.dp) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("06 SEARCH // INSTALLED APPS & WEB", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudCyan)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF040A12))
                    .border(1.dp, HudCyan, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, "Search", tint = HudCyan, modifier = Modifier.size(18.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (state.searchQuery.isEmpty()) {
                        Text(stringResource(R.string.search_apps_hint), fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = HudSlate)
                    }
                    BasicTextField(
                        value = state.searchQuery,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            val top = matches.firstOrNull()
                            if (top != null) onLaunchFullscreen(top)
                            else onWebSearch(state.searchQuery)
                        }),
                        textStyle = TextStyle(fontFamily = RajdhaniFontFamily, fontSize = 15.sp, color = HudIce),
                        cursorBrush = SolidColor(HudCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_module_input")
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .clickable { onWebSearch(state.searchQuery) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("WEB SEARCH", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCrimsonSubtle)
                        .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                        .clickable { onAskStxAi(state.searchQuery.ifBlank { "system brief" }) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("ASK STX AI", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                }
            }

            matches.forEach { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x6606101B))
                        .border(1.dp, HudDivider, RoundedCornerShape(4.dp))
                        .clickable { onLaunchFullscreen(app) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(app.label.uppercase(), fontFamily = OrbitronFontFamily, fontSize = 12.sp, color = HudIce)
                    Text(app.categoryTag, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudCyan)
                }
            }
        }
    }
}

@Composable
fun BriefModulePanel(
    state: StarkUiState,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    HudBracketCard(modifier = modifier.fillMaxSize(), borderColor = HudCyan, contentPadding = 16.dp) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("01 BRIEFING // LIVE EXECUTIVE SUMMARY", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = HudCyan)
            Text("${state.clock.greeting}, ${state.preferences.operatorCallsign}", style = MaterialTheme.typography.headlineSmall, color = HudIce)

            HorizontalDivider(color = HudDivider)

            SpecPairRow("POWER CELL", "${state.telemetry.batteryPercent}% (${state.telemetry.chargingSource}) · ${state.telemetry.batteryTempC}°C", HudEmerald)
            SpecPairRow("MEMORY", "${state.telemetry.ramUsedMb} / ${state.telemetry.ramTotalMb} MB (${state.telemetry.ramUsedPercent}%)", HudCyan)
            SpecPairRow("STORAGE", "${"%.1f".format(state.telemetry.storageFreeGb)} GB FREE (${state.telemetry.storageUsedPercent}% USED)", HudSilver)
            SpecPairRow("NETWORK", "${state.telemetry.networkTransport} · ${state.telemetry.wifiSsid}", HudCyan)
            SpecPairRow("SENSORS", "${state.telemetry.stepsCount} STEPS · ${state.telemetry.cardinalDirection} ${state.telemetry.azimuthDeg}°", HudIce)
            SpecPairRow("INSTALLED APPS", "${state.installedApps.size} PACKAGES · ${state.preferences.pinnedPackages.size} PINNED", HudSilver)

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .clickable(onClick = onOpenApps),
                    contentAlignment = Alignment.Center
                ) {
                    Text("OPEN APPS", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCrimsonSubtle)
                        .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                        .clickable(onClick = onOpenSettings),
                    contentAlignment = Alignment.Center
                ) {
                    Text("OS SETTINGS", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = HudIce)
                }
            }
        }
    }
}
