package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.HubModule
import com.example.model.InstalledApp
import com.example.model.StarkMode
import com.example.ui.StarkUiState
import com.example.ui.theme.HudEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.V3Cyan
import com.example.ui.theme.V3Dim
import com.example.ui.theme.V3Line
import com.example.ui.theme.V3LineBright
import com.example.ui.theme.V3Red
import com.example.ui.theme.V3RedBright
import com.example.ui.theme.V3RedDark
import com.example.ui.theme.V3Teal
import com.example.ui.theme.V3TealDark
import com.example.ui.theme.V3Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

val HOME_V3_PROTOCOLS = listOf("SLEEP", "STEALTH", "LIFE", "LAB", "FOCUS")

@Composable
fun rememberCutCornerShape(cut: Dp = 10.dp): Shape {
    val cutPx = with(LocalDensity.current) { cut.toPx() }
    return remember(cutPx) {
        GenericShape { size, _ ->
            val c = cutPx.coerceAtMost(minOf(size.width, size.height) * 0.45f)
            moveTo(c, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - c)
            lineTo(size.width - c, size.height)
            lineTo(0f, size.height)
            lineTo(0f, c)
            close()
        }
    }
}

@Composable
fun rememberHexTileShape(cut: Dp = 9.dp): Shape {
    val cutPx = with(LocalDensity.current) { cut.toPx() }
    return remember(cutPx) {
        GenericShape { size, _ ->
            val c = cutPx.coerceAtMost(minOf(size.width, size.height) * 0.4f)
            moveTo(c, 0f)
            lineTo(size.width - c, 0f)
            lineTo(size.width, c)
            lineTo(size.width, size.height - c)
            lineTo(size.width - c, size.height)
            lineTo(c, size.height)
            lineTo(0f, size.height - c)
            lineTo(0f, c)
            close()
        }
    }
}

/**
 * Full-bleed sci-fi HUD background matching HTML `bgfx` + `.scanbar` + radial gradients.
 */
@Composable
fun HomeV3Background(
    isLandscape: Boolean,
    dialCenterYFraction: Float = 0.54f,
    pauseAnimations: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val monoTf = remember(context) {
        runCatching { ResourcesCompat.getFont(context, R.font.jetbrains_mono) }.getOrNull() ?: Typeface.MONOSPACE
    }
    val infiniteTransition = rememberInfiniteTransition(label = "v3_scanbar")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = -0.15f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_y"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    if (w <= 2f || h <= 2f) {
                        return@drawWithCache onDrawBehind { }
                    }
                    val dcx = w
                    val dcy = h * dialCenterYFraction

                    val baseGrad = Brush.radialGradient(
                        colors = listOf(Color(0xFF0D1A2E), Color(0xFF060B14), Color.Black),
                        center = Offset(w * 0.5f, h * 0.4f),
                        radius = (maxOf(w, h) * 0.85f).coerceAtLeast(1f)
                    )
                    val blueGlow = Brush.radialGradient(
                        colors = listOf(Color(0x38123A6E), Color.Transparent),
                        center = Offset(dcx, dcy),
                        radius = (maxOf(w, h) * 0.55f).coerceAtLeast(1f)
                    )
                    val redGlow = Brush.radialGradient(
                        colors = listOf(V3Red.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(0f, h),
                        radius = (maxOf(w, h) * 0.7f).coerceAtLeast(1f)
                    )
                    val vignette = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.82f)
                        ),
                        center = Offset(w * 0.5f, h * 0.45f),
                        radius = (maxOf(w, h) * 0.72f).coerceAtLeast(1f)
                    )

                    // Precompute hex grid path
                    val hexPath = Path()
                    val stepX = 24.dp.toPx()
                    val stepY = 41.57.dp.toPx()
                    val halfX = stepX * 0.5f
                    val qY = stepY / 6f
                    var y = 0f
                    while (y < h + stepY) {
                        var x = 0f
                        while (x < w + stepX) {
                            hexPath.moveTo(x + halfX, y)
                            hexPath.lineTo(x + stepX, y + qY)
                            hexPath.lineTo(x + stepX, y + qY * 3f)
                            hexPath.lineTo(x + halfX, y + qY * 4f)
                            hexPath.lineTo(x, y + qY * 3f)
                            hexPath.lineTo(x, y + qY)
                            hexPath.close()
                            hexPath.moveTo(x + halfX, y + qY * 4f)
                            hexPath.lineTo(x + halfX, y + stepY)
                            x += stepX
                        }
                        y += stepY
                    }

                    // Corner PCB trace path
                    val tracePath = Path().apply {
                        if (!isLandscape) {
                            moveTo(8.dp.toPx(), h * 0.14f); lineTo(8.dp.toPx(), h * 0.31f); lineTo(22.dp.toPx(), h * 0.31f); lineTo(22.dp.toPx(), h * 0.39f)
                            moveTo(8.dp.toPx(), h * 0.76f); lineTo(8.dp.toPx(), h * 0.90f); lineTo(30.dp.toPx(), h * 0.90f)
                            moveTo(w - 8.dp.toPx(), 40.dp.toPx()); lineTo(w - 8.dp.toPx(), 90.dp.toPx()); lineTo(w - 28.dp.toPx(), 90.dp.toPx())
                            moveTo(60.dp.toPx(), h - 10.dp.toPx()); lineTo(160.dp.toPx(), h - 10.dp.toPx()); lineTo(160.dp.toPx(), h - 22.dp.toPx())
                        } else {
                            moveTo(8.dp.toPx(), 60.dp.toPx()); lineTo(8.dp.toPx(), 180.dp.toPx()); lineTo(20.dp.toPx(), 180.dp.toPx()); lineTo(20.dp.toPx(), 250.dp.toPx())
                            moveTo(w * 0.35f, h - 8.dp.toPx()); lineTo(w * 0.50f, h - 8.dp.toPx()); lineTo(w * 0.50f, h - 20.dp.toPx())
                        }
                    }

                    val ringRadii = listOf(210.dp.toPx(), 250.dp.toPx(), 320.dp.toPx(), 410.dp.toPx(), 520.dp.toPx())
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        typeface = monoTf
                        textSize = 6.5.dp.toPx()
                        color = android.graphics.Color.argb(56, 34, 211, 230)
                        textAlign = Paint.Align.CENTER
                        letterSpacing = 0.25f
                    }

                    onDrawBehind {
                        drawRect(baseGrad)
                        drawPath(hexPath, V3Teal.copy(alpha = 0.055f), style = Stroke(width = 0.8.dp.toPx()))
                        drawRect(blueGlow)
                        drawRect(redGlow)

                        ringRadii.forEachIndexed { i, rr ->
                            drawCircle(
                                color = V3Teal.copy(alpha = (0.09f - i * 0.012f).coerceAtLeast(0.02f)),
                                radius = rr,
                                center = Offset(dcx, dcy),
                                style = Stroke(
                                    width = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(
                                        if (i % 2 == 1) floatArrayOf(2.dp.toPx(), 7.dp.toPx())
                                        else floatArrayOf(1.dp.toPx(), 3.dp.toPx())
                                    )
                                )
                            )
                        }

                        var deg = 95f
                        while (deg < 270f) {
                            val rad = Math.toRadians(deg.toDouble())
                            drawLine(
                                color = V3Teal.copy(alpha = 0.035f),
                                start = Offset(dcx + (200.dp.toPx() * cos(rad)).toFloat(), dcy + (200.dp.toPx() * sin(rad)).toFloat()),
                                end = Offset(dcx + (560.dp.toPx() * cos(rad)).toFloat(), dcy + (560.dp.toPx() * sin(rad)).toFloat()),
                                strokeWidth = 1.dp.toPx()
                            )
                            deg += 12.5f
                        }

                        drawPath(tracePath, V3Teal.copy(alpha = 0.22f), style = Stroke(width = 1.dp.toPx()))
                        drawRect(vignette)

                        if (!isLandscape) {
                            drawIntoCanvas { c ->
                                val nc = c.nativeCanvas
                                nc.save()
                                nc.rotate(-90f, 8.dp.toPx(), h * 0.5f)
                                nc.drawText("ARC CORE · MK VI · 3 GW · NOMINAL", 8.dp.toPx(), h * 0.5f, textPaint)
                                nc.restore()
                            }
                        }
                    }
                }
        ) {}

        if (!pauseAnimations) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bandH = 120.dp.toPx()
                val topY = scanProgress * size.height
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, V3Teal.copy(alpha = 0.045f), Color.Transparent),
                        startY = topY,
                        endY = topY + bandH
                    ),
                    topLeft = Offset(0f, topY),
                    size = Size(size.width, bandH)
                )
            }
        }
    }
}

@Composable
fun HomeV3StatusBar(
    batteryPercent: Int,
    isCharging: Boolean,
    wifiConnected: Boolean,
    pauseOrb: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PlasmaFilamentOrb(size = 15.dp, pauseAnimation = pauseOrb)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "STARK ",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 2.6.sp,
                    color = V3Text
                )
                Text(
                    text = "OS",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 2.6.sp,
                    color = V3Red
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "Network",
                tint = if (wifiConnected) Color(0xFFB9C3CC) else V3Dim,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isCharging) "$batteryPercent%⚡" else "$batteryPercent%",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.5.sp,
                letterSpacing = 1.sp,
                color = if (isCharging) HudEmerald else Color(0xFFB9C3CC)
            )
            Canvas(modifier = Modifier.size(width = 23.dp, height = 10.dp)) {
                val bodyW = size.width - 3.dp.toPx()
                drawRect(
                    color = Color(0xFFB9C3CC),
                    topLeft = Offset.Zero,
                    size = Size(bodyW, size.height),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawRect(
                    color = Color(0xFFB9C3CC),
                    topLeft = Offset(bodyW, size.height * 0.28f),
                    size = Size(2.5.dp.toPx(), size.height * 0.44f)
                )
                val pad = 1.8.dp.toPx()
                val frac = (batteryPercent / 100f).coerceIn(0.05f, 1f)
                drawRect(
                    color = if (isCharging) HudEmerald else V3Teal,
                    topLeft = Offset(pad, pad),
                    size = Size(((bodyW - pad * 2) * frac).coerceAtLeast(1f), size.height - pad * 2)
                )
            }
        }
    }
}

@Composable
fun HomeV3Hero(
    state: StarkUiState,
    missingPermCount: Int,
    voiceListening: Boolean,
    onOpenStxAi: () -> Unit,
    isCompactLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cutShape = rememberCutCornerShape(9.dp)
    val protoName = HOME_V3_PROTOCOLS.getOrElse(state.protocolIndex) { "LIFE" }
    val pulseTransition = rememberInfiniteTransition(label = "ai_dot_pulse")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dot_alpha"
    )

    val dateSub = remember(state.clock.dateText) {
        val parts = state.clock.dateText.split(" ")
        val dow = parts.firstOrNull() ?: "SAT"
        val dm = parts.drop(1).joinToString(" ").ifBlank {
            SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date()).uppercase()
        }
        dow to dm
    }
    val greetingFormatted = remember(state.clock.greeting, state.preferences.operatorCallsign) {
        val g = state.clock.greeting.lowercase().replaceFirstChar { it.uppercase() }
        val name = state.preferences.operatorCallsign.lowercase().replaceFirstChar { it.uppercase() }
        "$g, $name."
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "PROTOCOL ",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.5.sp,
                    letterSpacing = 1.6.sp,
                    color = V3Teal
                )
                Text(
                    text = "· $protoName",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.5.sp,
                    letterSpacing = 1.6.sp,
                    color = V3Text
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(cutShape)
                    .background(if (voiceListening) V3Red.copy(alpha = 0.14f) else V3Teal.copy(alpha = 0.08f))
                    .border(1.dp, if (voiceListening) V3Red else V3LineBright, cutShape)
                    .clickable(onClick = onOpenStxAi)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("portrait_stx_badge")
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer { alpha = dotAlpha }
                        .clip(CircleShape)
                        .background(if (voiceListening) V3Red else V3Cyan)
                )
                Text(
                    text = if (voiceListening) "LISTENING" else "STX AI ONLINE",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.5.sp,
                    letterSpacing = 1.4.sp,
                    color = if (voiceListening) V3Red else V3Cyan
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.clock.timeText,
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isCompactLandscape) 38.sp else 46.sp,
                    lineHeight = if (isCompactLandscape) 38.sp else 46.sp,
                    letterSpacing = 1.5.sp,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = ":${state.clock.secondsText}",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isCompactLandscape) 16.sp else 20.sp,
                    color = V3Teal,
                    modifier = Modifier.padding(start = 2.dp, bottom = 5.dp),
                    maxLines = 1
                )
            }

            Column(
                modifier = Modifier.padding(bottom = 5.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = dateSub.first,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.6.sp,
                    color = V3Teal
                )
                Text(
                    text = dateSub.second,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    color = Color(0xFF9AA6B1)
                )
                Text(
                    text = "${state.telemetry.batteryTempC.roundToInt()}°C ${state.telemetry.networkTransport}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 8.5.sp,
                    letterSpacing = 1.1.sp,
                    color = Color(0xFF9AA6B1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Tick-mark Day Progress Ruler
        val frac = state.clock.dayProgressFraction.coerceIn(0f, 1f)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(11.dp)
        ) {
            val tickStep = 8.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = V3Teal.copy(alpha = 0.35f),
                    start = Offset(x, 1.dp.toPx()),
                    end = Offset(x, size.height - 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
                x += tickStep
            }
            val baseLineY = size.height - 1.dp.toPx()
            drawLine(V3Teal.copy(alpha = 0.35f), Offset(0f, baseLineY), Offset(size.width, baseLineY), 1.dp.toPx())
            val fillW = size.width * frac
            drawRect(
                brush = Brush.horizontalGradient(listOf(V3TealDark, V3Teal), startX = 0f, endX = fillW.coerceAtLeast(1f)),
                topLeft = Offset(0f, baseLineY - 2.dp.toPx()),
                size = Size(fillW, 3.dp.toPx())
            )
            drawRect(
                color = Color.White,
                topLeft = Offset((fillW - 1.dp.toPx()).coerceIn(0f, size.width - 2.dp.toPx()), 0f),
                size = Size(2.dp.toPx(), size.height)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("00", fontFamily = JetBrainsMonoFontFamily, fontSize = 7.5.sp, letterSpacing = 1.4.sp, color = Color(0xFF5D6873))
            Text("DAY ${(frac * 100f).roundToInt()}%", fontFamily = JetBrainsMonoFontFamily, fontSize = 7.5.sp, letterSpacing = 1.4.sp, color = Color(0xFF5D6873))
            Text("24", fontFamily = JetBrainsMonoFontFamily, fontSize = 7.5.sp, letterSpacing = 1.4.sp, color = Color(0xFF5D6873))
        }

        Spacer(modifier = Modifier.height(if (isCompactLandscape) 3.dp else 5.dp))

        Text(
            text = greetingFormatted,
            fontFamily = RajdhaniFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (isCompactLandscape) 16.sp else 19.sp,
            letterSpacing = 0.8.sp,
            color = V3Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "ALL SYSTEMS NOMINAL · ",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 8.5.sp,
                letterSpacing = 1.4.sp,
                color = Color(0xFF7A8692)
            )
            Text(
                text = if (missingPermCount > 0) "$missingPermCount ALERTS" else "ARMED",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 8.5.sp,
                letterSpacing = 1.4.sp,
                color = if (missingPermCount > 0) V3Red else HudEmerald
            )
        }
    }
}

@Composable
fun HomeV3VitalsRow(
    state: StarkUiState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SegmentedVitalColumn(
            label = "PWR",
            value = "${state.telemetry.batteryPercent}%",
            fraction = state.telemetry.batteryPercent / 100f,
            fillColor = V3Teal,
            modifier = Modifier.weight(1f)
        )
        SegmentedVitalColumn(
            label = "MEM",
            value = "${state.telemetry.ramUsedPercent}%",
            fraction = state.telemetry.ramUsedPercent / 100f,
            fillColor = V3Teal,
            modifier = Modifier.weight(1f)
        )
        SegmentedVitalColumn(
            label = "STOR",
            value = "${"%.1f".format(state.telemetry.storageFreeGb)} GB",
            fraction = state.telemetry.storageUsedPercent / 100f,
            fillColor = V3Red,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentedVitalColumn(
    label: String,
    value: String,
    fraction: Float,
    fillColor: Color,
    modifier: Modifier = Modifier
) {
    val filledCount = (fraction.coerceIn(0f, 1f) * 12f).roundToInt().coerceIn(0, 12)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 1.2.sp, color = Color(0xFF66727E))
            Text(value, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 0.8.sp, color = Color(0xFFDFE6EC))
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        ) {
            val total = 12
            val gap = 2.dp.toPx()
            val segW = ((size.width - gap * (total - 1)) / total).coerceAtLeast(2f)
            val skew = 2.2.dp.toPx()
            val p = Path()
            for (i in 0 until total) {
                val x0 = i * (segW + gap)
                p.reset()
                p.moveTo(x0 + skew, 0f)
                p.lineTo(x0 + segW + skew, 0f)
                p.lineTo(x0 + segW, size.height)
                p.lineTo(x0, size.height)
                p.close()
                drawPath(p, if (i < filledCount) fillColor else Color(0xFF11161C))
            }
        }
    }
}

@Composable
fun HomeV3TogglesGrid(
    state: StarkUiState,
    columns: Int,
    onToggleWifi: () -> Unit,
    onToggleBluetooth: () -> Unit,
    onToggleFlash: () -> Unit,
    onToggleMute: () -> Unit,
    onSync: () -> Unit,
    onToggleLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple("WI-FI", Icons.Default.Wifi, state.telemetry.wifiConnected to onToggleWifi),
        Triple("BT", Icons.Default.Bluetooth, state.telemetry.bluetoothEnabled to onToggleBluetooth),
        Triple("TORCH", Icons.Default.FlashlightOn, state.telemetry.flashlightOn to onToggleFlash),
        Triple("MUTE", Icons.AutoMirrored.Filled.VolumeOff, state.telemetry.audioMuted to onToggleMute),
        Triple("SYNC", Icons.Default.Sync, true to onSync),
        Triple("LOCK", Icons.Default.Lock, state.telemetry.touchLocked to onToggleLock)
    )
    val hexShape = rememberHexTileShape(8.dp)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        items.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                rowItems.forEach { (label, icon, pair) ->
                    val (active, action) = pair
                    val borderBrush = if (active) {
                        Brush.linearGradient(listOf(V3Cyan, V3Teal.copy(alpha = 0.35f)))
                    } else {
                        Brush.linearGradient(listOf(V3Teal.copy(alpha = 0.55f), V3Teal.copy(alpha = 0.14f)))
                    }
                    val fillBrush = if (active) {
                        Brush.radialGradient(listOf(V3Teal.copy(alpha = 0.30f), Color(0xFF04070B)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFF0B0F14), Color(0xFF040608)))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (columns == 3) Modifier.height(36.dp)
                                else Modifier.height(50.dp)
                            )
                            .clip(hexShape)
                            .background(borderBrush)
                            .clickable(onClick = action)
                            .padding(1.2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(hexShape)
                                .background(fillBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            if (columns == 3) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(icon, label, tint = if (active) V3Cyan else Color(0xFF7B8894), modifier = Modifier.size(13.dp))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(label, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 7.sp, lineHeight = 8.sp, letterSpacing = 0.6.sp, color = Color(0xFFC7D0D8), maxLines = 1)
                                        Text(if (active) "ON" else "OFF", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 6.5.sp, lineHeight = 7.5.sp, color = if (active) V3Cyan else V3Red)
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Icon(icon, label, tint = if (active) V3Cyan else Color(0xFF7B8894), modifier = Modifier.size(13.dp))
                                    Text(label, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 6.5.sp, lineHeight = 8.sp, letterSpacing = 0.5.sp, color = Color(0xFFC7D0D8), maxLines = 1)
                                    Text(if (active) "ON" else "OFF", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 6.sp, lineHeight = 7.5.sp, color = if (active) V3Cyan else V3Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeV3ActionsStrip(
    isFullscreen: Boolean,
    onSelectMode: (StarkMode) -> Unit,
    onFoldRotate: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onLockDeck: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cutShape = rememberCutCornerShape(7.dp)
    val acts = listOf(
        Triple("HUB", Icons.Default.Hub) { onSelectMode(StarkMode.HUB) },
        Triple("DESK", Icons.Default.GridView) { onSelectMode(StarkMode.HALF_DIAL) },
        Triple("ROTATE", Icons.Default.ScreenRotation) { onFoldRotate() },
        Triple(if (isFullscreen) "EXIT" else "FULL", if (isFullscreen) Icons.Default.CloseFullscreen else Icons.Default.CropFree) { onToggleFullscreen() },
        Triple("DECK", Icons.Default.Lock) { onLockDeck() }
    )
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        acts.forEach { (label, icon, action) ->
            val isDeck = label == "DECK"
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(cutShape)
                    .background(
                        Brush.verticalGradient(
                            if (isDeck) listOf(V3Red.copy(alpha = 0.16f), V3RedDark.copy(alpha = 0.08f))
                            else listOf(V3Teal.copy(alpha = 0.10f), V3Teal.copy(alpha = 0.03f))
                        )
                    )
                    .border(1.dp, if (isDeck) V3Red.copy(alpha = 0.45f) else V3Line, cutShape)
                    .clickable(onClick = action)
                    .padding(horizontal = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
            ) {
                Icon(icon, label, tint = if (isDeck) V3Red else Color(0xFFB4BEC7), modifier = Modifier.size(11.dp))
                Text(label, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 7.2.sp, letterSpacing = 0.6.sp, color = Color(0xFFB4BEC7), maxLines = 1)
            }
        }
    }
}

@Composable
fun HomeV3ProtocolSelector(
    protocolIndex: Int,
    onSelectProtocol: (Int) -> Unit,
    horizontalRow: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cutShape = rememberCutCornerShape(8.dp)
    val safeIdx = protocolIndex.coerceIn(0, HOME_V3_PROTOCOLS.lastIndex)
    val activeProto = HOME_V3_PROTOCOLS[safeIdx]

    if (horizontalRow) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("PROTOCOL", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 7.5.sp, letterSpacing = 1.8.sp, color = Color(0xFF5D6873))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(cutShape)
                    .background(Brush.horizontalGradient(listOf(V3Teal.copy(alpha = 0.09f), Color.Transparent)))
                    .border(1.dp, V3Line, cutShape),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = 28.dp)
                        .clickable { onSelectProtocol(safeIdx - 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev protocol", tint = V3Teal, modifier = Modifier.size(16.dp))
                }
                Text(activeProto, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, letterSpacing = 2.4.sp, color = Color.White)
                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = 28.dp)
                        .clickable { onSelectProtocol(safeIdx + 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next protocol", tint = V3Teal, modifier = Modifier.size(16.dp))
                }
            }
            Row(modifier = Modifier.width(84.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HOME_V3_PROTOCOLS.forEachIndexed { idx, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clickable { onSelectProtocol(idx) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(if (idx == safeIdx) V3Red else Color(0xFF1B1F24))
                        )
                    }
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("PROTOCOL", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 7.5.sp, letterSpacing = 2.2.sp, color = Color(0xFF5D6873))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(cutShape)
                    .background(Brush.horizontalGradient(listOf(V3Teal.copy(alpha = 0.09f), Color.Transparent)))
                    .border(1.dp, V3Line, cutShape),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 30.dp)
                        .clickable { onSelectProtocol(safeIdx - 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev protocol", tint = V3Teal, modifier = Modifier.size(16.dp))
                }
                Text(activeProto, fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.8.sp, color = Color.White)
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 30.dp)
                        .clickable { onSelectProtocol(safeIdx + 1) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next protocol", tint = V3Teal, modifier = Modifier.size(16.dp))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                HOME_V3_PROTOCOLS.forEachIndexed { idx, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clickable { onSelectProtocol(idx) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(if (idx == safeIdx) V3Red else Color(0xFF1B1F24))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeV3FocusPanel(
    state: StarkUiState,
    focusedModule: HubModule,
    missingPermCount: Int,
    onEngage: (HubModule) -> Unit,
    onStartTimer: (Int) -> Unit,
    onStopTimer: () -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    isLandscape: Boolean = false,
    engageTestTag: String = "portrait_target_lock_banner",
    modifier: Modifier = Modifier
) {
    val cutShape = rememberCutCornerShape(10.dp)
    val hexShape = rememberHexTileShape(6.dp)
    val hexBtnShape = rememberHexTileShape(6.dp)

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cutShape)
                .background(
                    Brush.linearGradient(
                        listOf(V3Teal.copy(alpha = 0.08f), Color(0xD9040609))
                    )
                )
                .border(1.dp, V3Line, cutShape)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: 04 | VAULT / NOTES & TIMER
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = focusedModule.code,
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = V3Teal
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = focusedModule.title,
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        letterSpacing = 1.6.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = focusedModule.subtitle,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 7.5.sp,
                        letterSpacing = 1.5.sp,
                        color = V3TealDark,
                        maxLines = 1
                    )
                }
            }

            // Live Preview Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isLandscape) Modifier.weight(1f, fill = false) else Modifier)
            ) {
                when (focusedModule) {
                    HubModule.BRIEF -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        FocusPreviewRow(
                            icon = Icons.Default.WarningAmber,
                            text = if (missingPermCount > 0) "$missingPermCount permissions missing" else "All permissions armed",
                            tint = if (missingPermCount > 0) V3Red else HudEmerald
                        )
                        FocusPreviewRow(Icons.Default.GridView, "${state.installedApps.size} apps · ${state.preferences.pinnedPackages.size} pinned", V3Cyan)
                        FocusPreviewRow(Icons.Default.Sync, "Core ${state.telemetry.batteryTempC}°C · ${state.telemetry.uptimeFormatted}", V3Cyan)
                    }

                    HubModule.APPS -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val previewApps = state.topRecentApps.take(if (isLandscape) 6 else 4)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            previewApps.forEach { app ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(hexShape)
                                        .background(Brush.linearGradient(listOf(V3Teal.copy(alpha = 0.34f), V3Teal.copy(alpha = 0.16f))))
                                        .clickable { onLaunchApp(app) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (app.icon != null) {
                                        Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(22.dp))
                                    } else {
                                        Text(app.label.take(2).uppercase(), fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                        Text(
                            text = "${state.installedApps.size} INSTALLED · ${state.preferences.pinnedPackages.size} PINNED",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 7.5.sp,
                            letterSpacing = 1.4.sp,
                            color = Color(0xFF5D6873)
                        )
                    }

                    HubModule.HEALTH -> Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("${state.telemetry.stepsCount}", fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 30.sp, color = Color.White)
                            Text("/ 8,000 STEPS", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, color = V3Dim, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        SegmentedVitalColumn("ACTIVITY", "${((state.telemetry.stepsCount / 8000f) * 100f).roundToInt()}%", state.telemetry.stepsCount / 8000f, V3Red)
                        Text(
                            text = "HDG ${state.telemetry.cardinalDirection} ${state.telemetry.azimuthDeg}° · ${state.telemetry.ambientLux.roundToInt()} LUX",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 7.5.sp,
                            letterSpacing = 1.2.sp,
                            color = Color(0xFF5D6873)
                        )
                    }

                    HubModule.VAULT -> {
                        val notes = state.preferences.vaultNotes.take(2)
                        val timerText = if (state.vaultTimerRunning) {
                            "%02d:%02d".format(state.vaultTimerSeconds / 60, state.vaultTimerSeconds % 60)
                        } else "25:00"

                        if (isLandscape) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    VaultNotePreviewList(notes)
                                }
                                VaultTimerMiniCard(
                                    timerText = timerText,
                                    running = state.vaultTimerRunning,
                                    onToggle = { if (state.vaultTimerRunning) onStopTimer() else onStartTimer(1500) },
                                    vertical = true,
                                    modifier = Modifier.width(106.dp)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                VaultNotePreviewList(notes)
                                VaultTimerMiniCard(
                                    timerText = timerText,
                                    running = state.vaultTimerRunning,
                                    onToggle = { if (state.vaultTimerRunning) onStopTimer() else onStartTimer(1500) },
                                    vertical = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    HubModule.CAMERA -> Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isLandscape) 86.dp else 66.dp)
                                .border(1.dp, V3Teal.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CenterFocusStrong, "Viewfinder", tint = V3Cyan, modifier = Modifier.size(22.dp))
                        }
                        Text(
                            text = "${if (state.cameraFacingBack) "REAR" else "FRONT"} · ${"%.1f".format(state.cameraZoom)}× · TORCH ${if (state.telemetry.flashlightOn) "ON" else "OFF"}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 7.5.sp,
                            letterSpacing = 1.2.sp,
                            color = Color(0xFF5D6873)
                        )
                    }

                    HubModule.SEARCH -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .border(1.dp, V3LineBright)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Search, "Search", tint = V3Dim, modifier = Modifier.size(12.dp))
                            Text("Search apps or run cmd…", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.5.sp, color = V3Dim)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf("chrome", "wifi on", "timer 25").forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, V3Line)
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(tag, fontFamily = JetBrainsMonoFontFamily, fontSize = 7.5.sp, color = V3Teal)
                                }
                            }
                        }
                    }

                    HubModule.WIFI -> Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(state.telemetry.wifiSsid, fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${state.telemetry.wifiBandLabel} · ${state.telemetry.wifiLinkSpeedMbps} MBPS", fontFamily = JetBrainsMonoFontFamily, fontSize = 7.5.sp, color = Color(0xFF5D6873))
                            }
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                val bars = (state.telemetry.wifiSignalPercent / 25).coerceIn(0, 4)
                                listOf(6.dp, 11.dp, 16.dp, 21.dp).forEachIndexed { idx, h ->
                                    Box(modifier = Modifier.size(width = 5.dp, height = h).background(if (idx < bars) V3Cyan else Color(0xFF1B232B)))
                                }
                            }
                        }
                        FocusPreviewRow(Icons.Default.Bluetooth, "Bluetooth · ${state.telemetry.bluetoothStateLabel}", V3Cyan)
                    }

                    HubModule.SETTINGS -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        FocusPreviewRow(Icons.Default.Hub, "Home launcher role", V3Cyan)
                        FocusPreviewRow(Icons.Default.CropFree, "Immersive mode ${if (state.preferences.fullscreenImmersive) "✓" else "OFF"}", V3Cyan)
                        FocusPreviewRow(Icons.Default.AutoAwesome, "Orb studio ›", V3Cyan)
                    }

                    HubModule.STX_AI -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "“Hey STX, what's next today?",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "TAP ENGAGE OR THE REACTOR TO TALK",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 7.5.sp,
                            letterSpacing = 1.4.sp,
                            color = Color(0xFF5D6873)
                        )
                    }
                }
            }

            // Red ENGAGE > button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(cutShape)
                    .background(Brush.horizontalGradient(listOf(V3RedDark, V3Red)))
                    .clickable { onEngage(focusedModule) }
                    .testTag(engageTestTag),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ENGAGE",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 2.4.sp,
                    color = Color.White
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Engage", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        // Right-edge glowing cyan connector dot
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(6.dp)
                .background(V3Teal)
        )
    }
}

@Composable
private fun FocusPreviewRow(icon: ImageVector, text: String, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(12.dp))
        Text(
            text = text,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 8.5.sp,
            color = Color(0xFFBCC6CF),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VaultNotePreviewList(notes: List<com.example.model.VaultNote>) {
    if (notes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(V3Teal.copy(alpha = 0.05f))
                .padding(horizontal = 7.dp, vertical = 5.dp)
        ) {
            Text("NO SAVED NOTES · TAP ENGAGE", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = V3Dim)
        }
    } else {
        notes.forEachIndexed { idx, note ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(V3Teal.copy(alpha = 0.06f))
            ) {
                Box(modifier = Modifier.width(2.5.dp).height(30.dp).background(if (idx == 0) V3Cyan else V3Teal))
                Column(modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)) {
                    Text(note.title, fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = V3Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${note.body} · ${note.timestampLabel}", fontFamily = JetBrainsMonoFontFamily, fontSize = 7.5.sp, color = V3Dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun VaultTimerMiniCard(
    timerText: String,
    running: Boolean,
    onToggle: () -> Unit,
    vertical: Boolean,
    modifier: Modifier = Modifier
) {
    val content: @Composable () -> Unit = {
        Text(timerText, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = Color.White)
        Box(
            modifier = Modifier
                .border(1.dp, if (running) V3Red else V3Cyan)
                .clickable(onClick = onToggle)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (running) "STOP" else "START",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                letterSpacing = 1.6.sp,
                color = if (running) V3Red else V3Cyan
            )
        }
    }
    if (vertical) {
        Column(
            modifier = modifier
                .border(1.dp, V3LineBright)
                .background(V3Teal.copy(alpha = 0.05f))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) { content() }
    } else {
        Row(
            modifier = modifier
                .border(1.dp, V3LineBright)
                .background(V3Teal.copy(alpha = 0.05f))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) { content() }
    }
}

@Composable
fun HomeV3ChipsRow(
    missingPermCount: Int,
    onOpenPermissions: () -> Unit,
    onOpenOrbStudio: () -> Unit,
    horizontal: Boolean,
    modifier: Modifier = Modifier
) {
    val cutShape = rememberCutCornerShape(7.dp)
    val chipsContent: @Composable (Modifier) -> Unit = { itemMod ->
        Row(
            modifier = itemMod
                .height(26.dp)
                .clip(cutShape)
                .background(if (missingPermCount > 0) V3Red.copy(alpha = 0.12f) else V3Teal.copy(alpha = 0.06f))
                .border(1.dp, if (missingPermCount > 0) V3Red.copy(alpha = 0.55f) else V3Line, cutShape)
                .clickable(onClick = onOpenPermissions)
                .padding(horizontal = 8.dp)
                .testTag("portrait_permissions_btn"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.WarningAmber, "Permissions", tint = if (missingPermCount > 0) Color(0xFFFF7D8B) else HudEmerald, modifier = Modifier.size(11.dp))
            Text(
                text = if (missingPermCount > 0) "PERMS · $missingPermCount FIX" else "PERMS · ARMED",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                letterSpacing = 1.1.sp,
                color = if (missingPermCount > 0) Color(0xFFFF7D8B) else HudEmerald,
                maxLines = 1
            )
        }
        Row(
            modifier = itemMod
                .height(26.dp)
                .clip(cutShape)
                .background(V3Teal.copy(alpha = 0.05f))
                .border(1.dp, V3Line, cutShape)
                .clickable(onClick = onOpenOrbStudio)
                .padding(horizontal = 8.dp)
                .testTag("portrait_orb_studio_btn"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.AutoAwesome, "Orb studio", tint = V3Teal, modifier = Modifier.size(11.dp))
            Text("ORB STUDIO", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 8.sp, letterSpacing = 1.1.sp, color = Color(0xFFC3CCD4), maxLines = 1)
        }
    }

    if (horizontal) {
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            chipsContent(Modifier.weight(1f))
        }
    } else {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            chipsContent(Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun HomeV3CommandBar(
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    voiceListening: Boolean,
    voicePartial: String,
    onVoiceCommand: () -> Unit,
    onSendCommand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cutShape = rememberCutCornerShape(10.dp)
    val hexShape = rememberHexTileShape(7.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(cutShape)
            .background(Brush.horizontalGradient(listOf(V3Teal.copy(alpha = 0.08f), Color(0x99040609))))
            .border(1.dp, if (voiceListening) V3Red else V3Line, cutShape)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (commandText.isEmpty()) {
                Text(
                    text = if (voiceListening && voicePartial.isNotBlank()) voicePartial else stringResource(R.string.ask_stx_ai_hint),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.sp,
                    color = if (voiceListening) V3Red else Color(0xFF5F6A75),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = commandText,
                onValueChange = onCommandTextChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendCommand() }),
                textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = Color.White),
                cursorBrush = SolidColor(V3Cyan),
                modifier = Modifier.fillMaxWidth().testTag("portrait_command_input")
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(hexShape)
                .background(if (voiceListening) V3Red.copy(alpha = 0.42f) else V3Red.copy(alpha = 0.22f))
                .clickable(onClick = onVoiceCommand)
                .testTag("portrait_voice_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Mic, stringResource(R.string.cd_voice_command), tint = Color(0xFFFF6B7C), modifier = Modifier.size(15.dp))
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(hexShape)
                .background(V3Teal.copy(alpha = 0.18f))
                .clickable(onClick = onSendCommand),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = V3Cyan, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun HomeV3Dock(
    installedApps: List<InstalledApp>,
    isLandscape: Boolean,
    onOpenAllAppsDrawer: () -> Unit,
    onOpenModule: (HubModule) -> Unit,
    onLaunchApp: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val hexShape = rememberHexTileShape(9.dp)
    val pinned = remember(installedApps) { installedApps.filter { it.isPinnedToDock }.take(4) }
    val phoneApp = remember(installedApps) {
        installedApps.firstOrNull {
            val l = it.label.lowercase()
            val p = it.packageName.lowercase()
            l.contains("phone") || l.contains("dialer") || p.contains("dialer")
        }
    }
    val msgApp = remember(installedApps) {
        installedApps.firstOrNull {
            val l = it.label.lowercase()
            val p = it.packageName.lowercase()
            l.contains("message") || l.contains("sms") || p.contains("messaging")
        }
    }

    var swipeY by remember { mutableFloatStateOf(0f) }

    data class DockSlot(
        val label: String,
        val icon: ImageVector,
        val app: InstalledApp? = null,
        val isAllApps: Boolean = false,
        val tag: String? = null,
        val onClick: () -> Unit
    )

    val slot0 = pinned.getOrNull(0)?.let { app ->
        DockSlot(app.label.uppercase().take(7), Icons.Default.Apps, app = app) { onLaunchApp(app) }
    } ?: DockSlot("PHONE", Icons.Default.Call, app = phoneApp) {
        if (phoneApp != null) onLaunchApp(phoneApp) else onOpenAllAppsDrawer()
    }

    val slot1 = pinned.getOrNull(1)?.let { app ->
        DockSlot(app.label.uppercase().take(7), Icons.Default.Apps, app = app) { onLaunchApp(app) }
    } ?: DockSlot("MSG", Icons.Default.ChatBubbleOutline, app = msgApp) {
        if (msgApp != null) onLaunchApp(msgApp) else onOpenModule(HubModule.SEARCH)
    }

    val slot2 = DockSlot("ALL APPS", Icons.Default.GridView, isAllApps = true, tag = "portrait_dock_all_apps_btn") {
        onOpenAllAppsDrawer()
    }

    val slot3 = pinned.getOrNull(2)?.let { app ->
        DockSlot(app.label.uppercase().take(7), Icons.Default.Apps, app = app) { onLaunchApp(app) }
    } ?: DockSlot("CAMERA", Icons.Default.CameraAlt) { onOpenModule(HubModule.CAMERA) }

    val slot4 = pinned.getOrNull(3)?.let { app ->
        DockSlot(app.label.uppercase().take(7), Icons.Default.Apps, app = app) { onLaunchApp(app) }
    } ?: DockSlot("SEARCH", Icons.Default.Search) { onOpenModule(HubModule.SEARCH) }

    val slots = listOf(slot0, slot1, slot2, slot3, slot4)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { swipeY = 0f },
                    onDragCancel = { swipeY = 0f }
                ) { _, dragAmount ->
                    swipeY += dragAmount
                    if (swipeY < -50f) {
                        swipeY = 0f
                        onOpenAllAppsDrawer()
                    }
                }
            }
            .testTag("portrait_bottom_dock"),
        horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        slots.forEach { slot ->
            val weight = if (!isLandscape && slot.isAllApps) 1.25f else 1f
            val borderBrush = if (slot.isAllApps) {
                Brush.linearGradient(listOf(V3Teal, V3Red))
            } else {
                Brush.linearGradient(listOf(V3Teal.copy(alpha = 0.55f), V3Teal.copy(alpha = 0.12f)))
            }
            val fillBrush = if (slot.isAllApps) {
                Brush.radialGradient(listOf(V3Teal.copy(alpha = 0.25f), Color(0xFF060504)))
            } else {
                Brush.verticalGradient(listOf(Color(0xFF0B0F14), Color(0xFF040608)))
            }

            Box(
                modifier = Modifier
                    .weight(weight)
                    .then(
                        if (isLandscape) Modifier.height(34.dp)
                        else Modifier.aspectRatio(1.40f)
                    )
                    .clip(hexShape)
                    .background(borderBrush)
                    .clickable(onClick = slot.onClick)
                    .then(if (slot.tag != null) Modifier.testTag(slot.tag) else Modifier)
                    .padding(1.2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(hexShape)
                        .background(fillBrush),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLandscape) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (slot.app?.icon != null) {
                                Image(bitmap = slot.app.icon, contentDescription = slot.label, modifier = Modifier.size(14.dp))
                            } else {
                                Icon(slot.icon, slot.label, tint = if (slot.isAllApps) V3Teal else Color(0xFFC7D0D8), modifier = Modifier.size(13.dp))
                            }
                            Text(
                                text = slot.label,
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 6.5.sp,
                                color = if (slot.isAllApps) Color.White else Color(0xFFC7D0D8),
                                maxLines = 1
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (slot.app?.icon != null) {
                                Image(bitmap = slot.app.icon, contentDescription = slot.label, modifier = Modifier.size(17.dp))
                            } else {
                                Icon(
                                    slot.icon,
                                    slot.label,
                                    tint = if (slot.isAllApps) V3Teal else Color(0xFF8B99A6),
                                    modifier = Modifier.size(if (slot.isAllApps) 18.dp else 15.dp)
                                )
                            }
                            Text(
                                text = slot.label,
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 6.5.sp,
                                letterSpacing = 0.6.sp,
                                color = if (slot.isAllApps) Color.White else Color(0xFFC7D0D8),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
