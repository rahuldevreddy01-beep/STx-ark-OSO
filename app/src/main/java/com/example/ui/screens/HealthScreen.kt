package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hardware.LauncherPermissionManager
import com.example.ui.StarkUiState
import com.example.ui.components.CarouselSpecRow
import com.example.ui.components.HolographicCardCarousel
import com.example.ui.components.HolographicCardHeader
import com.example.ui.components.HolographicSideCardFooter
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCrimsonBorder
import com.example.ui.theme.HudCrimsonSubtle
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
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

private data class HealthDeckCard(
    val id: String,
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
            HealthDeckCard(
                id = "steps",
                title = "PEDOMETER & ACCEL",
                subtitle = "01 KINETIC // MOTION SENSOR",
                badge = "${state.telemetry.stepsCount} STEPS"
            ),
            HealthDeckCard(
                id = "gyro",
                title = "3-AXIS GYROSCOPE",
                subtitle = "02 ANGULAR // LIVE WAVEFORM",
                badge = "RAD/S"
            ),
            HealthDeckCard(
                id = "compass",
                title = "MAGNETIC COMPASS",
                subtitle = "03 ORIENTATION // AZIMUTH",
                badge = "${state.telemetry.cardinalDirection} ${state.telemetry.azimuthDeg}°"
            ),
            HealthDeckCard(
                id = "power",
                title = "POWER & THERMAL",
                subtitle = "04 VITALS // CELL & LUX",
                badge = "${state.telemetry.batteryPercent}%",
                isCrimson = true
            )
        )
    }

    HolographicCardCarousel(
        items = cards,
        focusedIndex = focusedIndex,
        onSelectIndex = onSelectIndex,
        itemKey = { _, card -> card.id },
        isCrimsonAccent = { _, card, _ -> card.isCrimson },
        modifier = modifier
    ) { idx, card, isCenter, isCompactStage, isShortStage, relOffset ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isCenter) Modifier.testTag("carousel_card_${card.id}") else Modifier),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HolographicCardHeader(
                index = idx,
                totalCount = cards.size,
                moduleLabel = "HEALTH",
                title = card.title,
                subtitle = card.subtitle,
                badge = card.badge,
                isCenter = isCenter,
                isCompactStage = isCompactStage,
                crimsonBadge = card.isCrimson
            )

            if (isCenter) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (card.id) {
                        "steps" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("DAILY STEPS", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate)
                                    Text(
                                        text = if (state.telemetry.stepsSensorAvailable) "${state.telemetry.stepsCount}" else "NO STEP HW",
                                        fontFamily = OrbitronFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isCompactStage) 20.sp else 26.sp,
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )

                            val accelMag = sqrt(
                                state.telemetry.accelX * state.telemetry.accelX +
                                    state.telemetry.accelY * state.telemetry.accelY +
                                    state.telemetry.accelZ * state.telemetry.accelZ
                            )
                            CarouselSpecRow(
                                label = "ACCELEROMETER X/Y/Z",
                                value = "${"%.1f".format(state.telemetry.accelX)}, ${"%.1f".format(state.telemetry.accelY)}, ${"%.1f".format(state.telemetry.accelZ)} m/s²",
                                valueColor = HudSilver,
                                compact = isCompactStage
                            )
                            CarouselSpecRow(
                                label = "TOTAL G-MAGNITUDE",
                                value = "${"%.2f".format(accelMag)} m/s²",
                                valueColor = HudIce,
                                compact = isCompactStage
                            )
                        }

                        "gyro" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isShortStage) 64.dp else 96.dp)
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
                                    fontSize = 8.5.sp,
                                    color = HudSlate,
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                            }

                            val gyroMag = sqrt(
                                state.telemetry.gyroX * state.telemetry.gyroX +
                                    state.telemetry.gyroY * state.telemetry.gyroY +
                                    state.telemetry.gyroZ * state.telemetry.gyroZ
                            )
                            CarouselSpecRow(
                                label = "AXIS X / Y / Z",
                                value = "${"%.2f".format(state.telemetry.gyroX)}, ${"%.2f".format(state.telemetry.gyroY)}, ${"%.2f".format(state.telemetry.gyroZ)}",
                                valueColor = HudCrimson,
                                compact = isCompactStage
                            )
                            CarouselSpecRow(
                                label = "ROTATION RATE",
                                value = "${"%.3f".format(gyroMag)} rad/s",
                                valueColor = HudCyan,
                                compact = isCompactStage
                            )
                        }

                        "compass" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompassMiniRadar(
                                    azimuthDeg = state.telemetry.azimuthDeg,
                                    modifier = Modifier.size(if (isShortStage) 64.dp else 88.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${state.telemetry.cardinalDirection} ${state.telemetry.azimuthDeg}°",
                                        fontFamily = OrbitronFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isCompactStage) 18.sp else 24.sp,
                                        color = HudIce
                                    )
                                    Text(
                                        text = "MAGNETIC BEARING",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.sp,
                                        color = HudCyan
                                    )
                                }
                            }
                            CarouselSpecRow(
                                label = "PITCH ATTITUDE",
                                value = "${state.telemetry.pitchDeg}°",
                                valueColor = HudCyan,
                                compact = isCompactStage
                            )
                            CarouselSpecRow(
                                label = "ROLL ATTITUDE",
                                value = "${state.telemetry.rollDeg}°",
                                valueColor = HudCrimson,
                                compact = isCompactStage
                            )
                        }

                        "power" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("CORE CHARGE", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = HudSlate)
                                    Text(
                                        text = "${state.telemetry.batteryPercent}%",
                                        fontFamily = OrbitronFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isCompactStage) 20.sp else 26.sp,
                                        color = HudEmerald
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (state.telemetry.isCharging) HudCrimsonSubtle else Color(0x5506101B))
                                        .border(1.dp, if (state.telemetry.isCharging) HudCrimsonBorder else HudCyanBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = state.telemetry.chargingSource,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp,
                                        color = HudIce
                                    )
                                }
                            }

                            CarouselSpecRow(
                                label = "THERMAL / VOLT",
                                value = "${state.telemetry.batteryTempC}°C · ${state.telemetry.batteryVoltageMv} mV",
                                valueColor = HudCrimson,
                                compact = isCompactStage
                            )
                            CarouselSpecRow(
                                label = "CELL HEALTH",
                                value = state.telemetry.batteryHealth,
                                valueColor = HudEmerald,
                                compact = isCompactStage
                            )
                            CarouselSpecRow(
                                label = "AMBIENT LIGHT",
                                value = if (state.telemetry.lightSensorAvailable) "${"%.0f".format(state.telemetry.ambientLux)} LUX" else "NO LIGHT HW",
                                valueColor = HudCyan,
                                compact = isCompactStage
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
                HolographicSideCardFooter(relOffset = relOffset)
            }
        }
    }
}

@Composable
private fun CompassMiniRadar(
    azimuthDeg: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = Color(0xFF07131E), radius = r, center = c)
        drawCircle(color = HudCyan.copy(alpha = 0.55f), radius = r, center = c, style = Stroke(1.5f))
        drawCircle(color = HudCrimson.copy(alpha = 0.40f), radius = r * 0.58f, center = c, style = Stroke(1.2f))

        val rad = Math.toRadians((azimuthDeg - 90).toDouble())
        val tip = Offset(c.x + (r * 0.85f * cos(rad)).toFloat(), c.y + (r * 0.85f * sin(rad)).toFloat())
        drawLine(
            color = HudCrimson,
            start = c,
            end = tip,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = HudCyan, radius = 2.5.dp.toPx(), center = c)
    }
}
