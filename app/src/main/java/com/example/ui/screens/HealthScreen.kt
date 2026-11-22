package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hardware.LauncherPermissionManager
import com.example.ui.StarkUiState
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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class HealthPage(
    val id: String,
    val code: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val isCrimson: Boolean = false
)

@Composable
fun HealthScreen(
    state: StarkUiState,
    focusedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onBack: () -> Unit = {},
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var activityPermGranted by remember {
        mutableStateOf(LauncherPermissionManager.hasActivityRecognitionPermission(context))
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowGranted = LauncherPermissionManager.hasActivityRecognitionPermission(context)
                val changed = !activityPermGranted && nowGranted
                activityPermGranted = nowGranted
                if (changed) onPermissionGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val stepLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val effective = granted || LauncherPermissionManager.hasActivityRecognitionPermission(context)
        activityPermGranted = effective
        if (effective) onPermissionGranted()
    }

    val cards = remember(
        state.telemetry.stepsCount,
        state.telemetry.cardinalDirection,
        state.telemetry.azimuthDeg,
        state.telemetry.batteryPercent
    ) {
        listOf(
            HealthPage("steps", "01", "PEDOMETER & ACCEL", "KINETIC MOTION SENSOR", "${state.telemetry.stepsCount} STEPS"),
            HealthPage("gyro", "02", "3-AXIS GYROSCOPE", "ANGULAR WAVEFORM", "RAD/S"),
            HealthPage("compass", "03", "MAGNETIC COMPASS", "AZIMUTH BEARING", "${state.telemetry.cardinalDirection} ${state.telemetry.azimuthDeg}°"),
            HealthPage("power", "04", "POWER & THERMAL", "BATTERY CELL & LUX", "${state.telemetry.batteryPercent}%", true)
        )
    }

    val safeIndex = focusedIndex.coerceIn(0, cards.lastIndex)
    val pagerState = rememberPagerState(initialPage = safeIndex, pageCount = { cards.size })
    LaunchedEffect(safeIndex) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != safeIndex) {
            pagerState.scrollToPage(safeIndex)
        }
    }
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != safeIndex) {
            onSelectIndex(pagerState.settledPage)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xD8050A12))
                .border(1.dp, HudCyanBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(HudCrimsonSubtle)
                    .border(1.dp, HudCrimsonBorder, RoundedCornerShape(4.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("half_dial_center_back"),
                contentAlignment = Alignment.Center
            ) {
                Text("< BACK", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = HudIce)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("HEALTH & SENSORS", fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HudIce)
                Text("${cards[safeIndex].code} // ${cards[safeIndex].subtitle}", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = HudCyan)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .clickable { onSelectIndex((safeIndex - 1 + cards.size) % cards.size) }
                        .testTag("carousel_prev_button"),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous health card", tint = HudCyan, modifier = Modifier.size(16.dp)) }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HudCyanSubtle)
                        .border(1.dp, HudCyanBorder, RoundedCornerShape(4.dp))
                        .clickable { onSelectIndex((safeIndex + 1) % cards.size) }
                        .testTag("carousel_next_button"),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next health card", tint = HudCyan, modifier = Modifier.size(16.dp)) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            cards.forEachIndexed { idx, c ->
                val sel = idx == safeIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (sel) HudCyanSubtle else Color(0x8806101B))
                        .border(1.dp, if (sel) HudCyan else HudDivider, RoundedCornerShape(4.dp))
                        .clickable { onSelectIndex(idx) }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${c.code} ${c.id.uppercase()}", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = if (sel) HudIce else HudSilver, maxLines = 1)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            key = { cards[it].id },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { page ->
            val card = cards[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xEB040911))
                    .border(1.2.dp, if (card.isCrimson) HudCrimsonBorder else HudCyanBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp)
                    .then(if (page == pagerState.currentPage) Modifier.testTag("carousel_card_${card.id}") else Modifier),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(card.title, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HudIce)
                        Text("${card.code} // ${card.subtitle}", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.5.sp, color = HudCyan)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (card.isCrimson) HudCrimsonSubtle else HudCyanSubtle)
                            .border(0.8.dp, if (card.isCrimson) HudCrimsonBorder else HudCyanBorder, RoundedCornerShape(3.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(card.badge, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = HudIce)
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(HudCyan.copy(alpha = 0.22f)))

                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (card.id) {
                        "steps" -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("DAILY STEPS", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate)
                                    Text(
                                        text = if (state.telemetry.stepsSensorAvailable) "${state.telemetry.stepsCount}" else "NO STEP HW",
                                        fontFamily = OrbitronFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        color = HudCyan
                                    )
                                }
                                if (!activityPermGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    Box(
                                        modifier = Modifier
                                            .heightIn(min = 40.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(HudCrimsonSubtle)
                                            .border(1.dp, HudCrimson, RoundedCornerShape(4.dp))
                                            .clickable { stepLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("health_enable_steps_btn"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("ENABLE STEPS", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = HudIce)
                                    }
                                }
                            }
                            LinearProgressIndicator(
                                progress = { (state.telemetry.stepsCount / 10000f).coerceIn(0f, 1f) },
                                color = HudCyan,
                                trackColor = Color(0xFF0A1624),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                            val accelMag = sqrt(state.telemetry.accelX * state.telemetry.accelX + state.telemetry.accelY * state.telemetry.accelY + state.telemetry.accelZ * state.telemetry.accelZ)
                            HealthSpecRow("ACCELEROMETER X/Y/Z", "${"%.1f".format(state.telemetry.accelX)}, ${"%.1f".format(state.telemetry.accelY)}, ${"%.1f".format(state.telemetry.accelZ)} m/s²", HudSilver)
                            HealthSpecRow("TOTAL G-MAGNITUDE", "${"%.2f".format(accelMag)} m/s²", HudIce)
                        }

                        "gyro" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
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
                                        samples.forEachIndexed { i, v ->
                                            val x = (i.toFloat() / (samples.size - 1)) * size.width
                                            val y = size.height - ((v / maxVal).coerceIn(0f, 1f) * size.height * 0.85f) - 4f
                                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                        }
                                        drawPath(path = path, color = HudCrimson, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                                    } else {
                                        drawLine(HudCyan.copy(alpha = 0.45f), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1.5.dp.toPx())
                                    }
                                }
                                Text("LIVE GYRO MAGNITUDE (RAD/S)", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.5.sp, color = HudSlate, modifier = Modifier.align(Alignment.TopStart))
                            }
                            val gyroMag = sqrt(state.telemetry.gyroX * state.telemetry.gyroX + state.telemetry.gyroY * state.telemetry.gyroY + state.telemetry.gyroZ * state.telemetry.gyroZ)
                            HealthSpecRow("AXIS X / Y / Z", "${"%.2f".format(state.telemetry.gyroX)}, ${"%.2f".format(state.telemetry.gyroY)}, ${"%.2f".format(state.telemetry.gyroZ)}", HudCrimson)
                            HealthSpecRow("ROTATION RATE", "${"%.3f".format(gyroMag)} rad/s", HudCyan)
                        }

                        "compass" -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Canvas(modifier = Modifier.size(88.dp)) {
                                    val r = size.minDimension / 2f
                                    val c = Offset(size.width / 2f, size.height / 2f)
                                    drawCircle(Color(0xFF07131E), radius = r, center = c)
                                    drawCircle(HudCyan.copy(alpha = 0.55f), radius = r, center = c, style = Stroke(1.5f))
                                    drawCircle(HudCrimson.copy(alpha = 0.40f), radius = r * 0.58f, center = c, style = Stroke(1.2f))
                                    val rad = Math.toRadians((state.telemetry.azimuthDeg - 90).toDouble())
                                    val tip = Offset(c.x + (r * 0.85f * cos(rad)).toFloat(), c.y + (r * 0.85f * sin(rad)).toFloat())
                                    drawLine(HudCrimson, c, tip, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                                    drawCircle(HudCyan, radius = 2.5.dp.toPx(), center = c)
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${state.telemetry.cardinalDirection} ${state.telemetry.azimuthDeg}°", fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = HudIce)
                                    Text("MAGNETIC BEARING", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudCyan)
                                }
                            }
                            HealthSpecRow("PITCH ATTITUDE", "${state.telemetry.pitchDeg}°", HudCyan)
                            HealthSpecRow("ROLL ATTITUDE", "${state.telemetry.rollDeg}°", HudCrimson)
                        }

                        "power" -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("CORE CHARGE", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate)
                                    Text("${state.telemetry.batteryPercent}%", fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = HudEmerald)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (state.telemetry.isCharging) HudCrimsonSubtle else Color(0x5506101B))
                                        .border(1.dp, if (state.telemetry.isCharging) HudCrimsonBorder else HudCyanBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(state.telemetry.chargingSource, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = HudIce)
                                }
                            }
                            HealthSpecRow("THERMAL / VOLT", "${state.telemetry.batteryTempC}°C · ${state.telemetry.batteryVoltageMv} mV", HudCrimson)
                            HealthSpecRow("CELL HEALTH", state.telemetry.batteryHealth, HudEmerald)
                            HealthSpecRow("AMBIENT LIGHT", if (state.telemetry.lightSensorAvailable) "${"%.0f".format(state.telemetry.ambientLux)} LUX" else "NO LIGHT HW", HudCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthSpecRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate, maxLines = 1)
        Text(value, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
    }
}
