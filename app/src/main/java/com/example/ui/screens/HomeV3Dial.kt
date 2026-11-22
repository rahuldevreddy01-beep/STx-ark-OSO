package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.HubModule
import com.example.ui.theme.V3Red
import com.example.ui.theme.V3RedBright
import com.example.ui.theme.V3RedDark
import com.example.ui.theme.V3Teal
import com.example.ui.theme.V3TealDark
import com.example.ui.theme.V3Text
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Multi-layer glowing plasma filament orb matching Stark OS Home v3 HTML `orbFrame`.
 */
@Composable
fun PlasmaFilamentOrb(
    size: Dp,
    pauseAnimation: Boolean = false,
    speedMult: Float = 1f,
    bloomMult: Float = 1.1f,
    modifier: Modifier = Modifier
) {
    val isMini = size < 40.dp
    val scene = remember(isMini) { PlasmaOrbScene(isMini) }
    var timeSec by remember { mutableLongStateOf(0L) }

    LaunchedEffect(pauseAnimation, speedMult) {
        if (pauseAnimation) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last == 0L) last = now
                val dt = ((now - last) / 1e9f).coerceIn(0f, 0.05f)
                last = now
                scene.t += dt * speedMult.coerceIn(0.3f, 2.5f)
                timeSec = now
            }
        }
    }

    Canvas(modifier = modifier.size(size)) {
        @Suppress("UNUSED_VARIABLE")
        val tick = timeSec
        if (this.size.minDimension <= 2f) return@Canvas
        scene.draw(this, bloomMult.coerceIn(0.4f, 1.8f))
    }
}

private class PlasmaOrbScene(isMini: Boolean) {
    private data class Filament(
        val ax: Float,
        val ay: Float,
        val sp: Float,
        val w: Float,
        val color: Color,
        val ph: Float,
        val amp: Float
    )

    private data class Dust(
        val a: Float,
        val b: Float,
        val r: Float,
        val s: Float
    )

    var t = 0f

    private val filaments: List<Filament>
    private val dust: List<Dust>
    private val frontPath = Path()
    private val backPath = Path()

    init {
        val hues = listOf(
            Color(0xFF22D3E6),
            Color(0xFF47C4FF),
            Color(0xFFB469FF),
            Color(0xFFF06BFF),
            Color(0xFFFF59B4),
            Color(0xFF5599FF)
        )
        val rng = Random(42)
        val filCount = if (isMini) 5 else 13
        filaments = List(filCount) { i ->
            Filament(
                ax = rng.nextFloat() * 6.283f,
                ay = rng.nextFloat() * 6.283f,
                sp = (rng.nextFloat() * 0.6f + 0.4f) * if (rng.nextBoolean()) 1f else -1f,
                w = rng.nextFloat() * 0.5f + 0.5f,
                color = hues[i % hues.size],
                ph = rng.nextFloat() * 6.283f,
                amp = 0.18f + rng.nextFloat() * 0.25f
            )
        }
        val dustCount = if (isMini) 12 else 75
        dust = List(dustCount) {
            Dust(
                a = rng.nextFloat() * 6.283f,
                b = acos((rng.nextFloat() * 2f - 1f).coerceIn(-1f, 1f)),
                r = 0.8f + rng.nextFloat() * 0.28f,
                s = rng.nextFloat()
            )
        }
    }

    fun draw(scope: androidx.compose.ui.graphics.drawscope.DrawScope, bloom: Float) {
        val s = scope.size.minDimension
        val cx = scope.size.width * 0.5f
        val cy = scope.size.height * 0.5f
        val r = s * 0.40f
        val center = Offset(cx, cy)

        // Outer halo
        scope.drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFF7850FF).copy(alpha = (0.28f * bloom).coerceAtMost(0.45f)),
                    0.5f to Color(0xFFFF3CB4).copy(alpha = (0.14f * bloom).coerceAtMost(0.28f)),
                    1.0f to Color.Transparent
                ),
                center = center,
                radius = (r * 1.25f).coerceAtLeast(1f)
            ),
            radius = r * 1.25f,
            center = center
        )

        // Inner plasma body
        val bodyRadius = r * (1f + sin(t * 2.2f) * 0.015f)
        scope.drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color.White,
                    0.15f to Color(0xFFFFDCFA).copy(alpha = 0.88f),
                    0.38f to Color(0xFFC878FF).copy(alpha = 0.48f),
                    0.70f to Color(0xFF3C78FF).copy(alpha = 0.26f),
                    1.00f to Color(0xFF1E2878).copy(alpha = 0.06f)
                ),
                center = Offset(cx - r * 0.10f, cy - r * 0.10f),
                radius = bodyRadius.coerceAtLeast(1f)
            ),
            radius = bodyRadius,
            center = center
        )

        // Wobbling 3D filaments
        val steps = 36
        for (f in filaments) {
            val rx = f.ax + t * 0.35f * f.sp
            val ry = f.ay + t * 0.22f * f.sp
            val crx = cos(rx); val srx = sin(rx)
            val cry = cos(ry); val sry = sin(ry)

            frontPath.reset()
            backPath.reset()
            var frontOpen = false
            var backOpen = false

            for (k in 0..steps) {
                val th = (k / steps.toFloat()) * 6.283185f
                val wob = 1f + f.amp * 0.18f * sin(th * 3f + t * 2f + f.ph)
                val x = cos(th) * wob
                val y = sin(th) * wob * (0.35f + f.amp)
                val z = sin(th * 2f + f.ph + t) * 0.25f

                val y2 = y * crx - z * srx
                val z2 = y * srx + z * crx
                val x3 = x * cry + z2 * sry
                val z3 = -x * sry + z2 * cry

                val px = cx + x3 * r * 0.98f
                val py = cy + y2 * r * 0.98f

                if (z3 > 0f) {
                    if (!frontOpen) { frontPath.moveTo(px, py); frontOpen = true } else frontPath.lineTo(px, py)
                    backOpen = false
                } else {
                    if (!backOpen) { backPath.moveTo(px, py); backOpen = true } else backPath.lineTo(px, py)
                    frontOpen = false
                }
            }

            // Back half (dimmer)
            scope.drawPath(
                path = backPath,
                color = f.color.copy(alpha = 0.10f),
                style = Stroke(width = (s / 24f * f.w).coerceAtLeast(1f), cap = StrokeCap.Round)
            )
            scope.drawPath(
                path = backPath,
                color = f.color.copy(alpha = 0.36f),
                style = Stroke(width = (s / 70f * f.w).coerceAtLeast(0.8f), cap = StrokeCap.Round)
            )

            // Front half (bright)
            scope.drawPath(
                path = frontPath,
                color = f.color.copy(alpha = 0.20f),
                style = Stroke(width = (s / 18f * f.w).coerceAtLeast(1.4f), cap = StrokeCap.Round)
            )
            scope.drawPath(
                path = frontPath,
                color = f.color.copy(alpha = 0.78f),
                style = Stroke(width = (s / 55f * f.w).coerceAtLeast(1f), cap = StrokeCap.Round)
            )
        }

        // Rim arcs
        val arcRect = Rect(cx - r, cy - r, cx + r, cy + r)
        scope.drawArc(
            color = Color(0xFFFF5AC8).copy(alpha = 0.38f),
            startAngle = 126f,
            sweepAngle = 115f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = Stroke(width = (s / 48f).coerceAtLeast(1f), cap = StrokeCap.Round)
        )
        scope.drawArc(
            color = Color(0xFF5AC8FF).copy(alpha = 0.45f),
            startAngle = -57f,
            sweepAngle = 148f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = Stroke(width = (s / 48f).coerceAtLeast(1f), cap = StrokeCap.Round)
        )

        // Sparkle dust
        for (p in dust) {
            val a = p.a + t * 0.25f
            val x = sin(p.b) * cos(a)
            val y = cos(p.b)
            val z = sin(p.b) * sin(a)
            val tw = 0.5f + 0.5f * sin(t * 4f + p.s * 30f)
            val dotR = (s / 130f * (1f + z * 0.5f)).coerceAtLeast(0.8f)
            val c = if (p.s < 0.5f) Color(0xFF96DCFF).copy(alpha = 0.55f * tw)
            else Color(0xFFFF82DC).copy(alpha = 0.55f * tw)
            scope.drawCircle(
                color = c,
                radius = dotR,
                center = Offset(cx + x * r * p.r, cy + y * r * p.r)
            )
        }

        // Hot core pulse
        val coreR = r * 0.32f * (1f + sin(t * 3f) * 0.06f)
        scope.drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.White.copy(alpha = 0.96f),
                    1.0f to Color.Transparent
                ),
                center = center,
                radius = coreR.coerceAtLeast(1f)
            ),
            radius = coreR,
            center = center
        )
    }
}

/**
 * Right-edge clipped ball-bearing rotary module dial matching Stark OS Home v3 HTML `Dial`.
 */
@Composable
fun HomeV3RightEdgeDial(
    modules: List<HubModule>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onActivateModule: (HubModule) -> Unit,
    onOrbClick: () -> Unit,
    speedMult: Float,
    bloomMult: Float,
    pauseAnimations: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val count = modules.size.coerceAtLeast(1)
    val stepDeg = 360f / count
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val currentSelected by rememberUpdatedState(selectedIndex.coerceIn(0, count - 1))
    val currentOnSelect by rememberUpdatedState(onSelectIndex)
    val currentOnActivate by rememberUpdatedState(onActivateModule)

    val bearingAngle = remember(count) { Animatable(-selectedIndex.coerceIn(0, count - 1) * stepDeg) }
    var isDragging by remember { mutableStateOf(false) }
    var lastSyncedIndex by remember(count) { mutableIntStateOf(selectedIndex.coerceIn(0, count - 1)) }
    var motionJob by remember(count) { mutableStateOf<Job?>(null) }

    LaunchedEffect(selectedIndex, isDragging, count) {
        if (!isDragging) {
            val safeIdx = selectedIndex.coerceIn(0, count - 1)
            val cur = bearingAngle.value
            val desired = -safeIdx * stepDeg
            val deltaMod = (((desired - cur) % 360f + 540f) % 360f) - 180f
            if (abs(deltaMod) > 0.5f) {
                bearingAngle.animateTo(cur + deltaMod, spring(dampingRatio = 0.82f, stiffness = 360f))
            }
        }
    }

    val orbitronTf = remember(context) {
        runCatching { ResourcesCompat.getFont(context, R.font.orbitron) }.getOrNull() ?: Typeface.DEFAULT_BOLD
    }
    val monoTf = remember(context) {
        runCatching { ResourcesCompat.getFont(context, R.font.jetbrains_mono) }.getOrNull() ?: Typeface.MONOSPACE
    }

    val labelPaint = remember(orbitronTf) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(orbitronTf, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }
    val numPaint = remember(monoTf) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(monoTf, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }
    val wedgePath = remember { Path() }
    val focusPath = remember { Path() }

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        if (widthPx <= 10f || heightPx <= 10f) return@BoxWithConstraints

        val rPx = min(widthPx * 0.89f, heightPx * 0.44f).coerceAtLeast(80f)
        val cx = widthPx
        val cy = heightPx * 0.5f
        val r1 = rPx * 0.47f
        val r2 = rPx - with(density) { 9.dp.toPx() }
        val rBall = rPx * 0.40f
        val rRaceIn = rPx * 0.36f
        val rRaceOut = rPx * 0.445f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(count, widthPx, heightPx) {
                    detectTapGestures { pos ->
                        val dx = pos.x - cx
                        val dy = pos.y - cy
                        val dist = hypot(dx, dy)
                        if (dist <= rRaceIn) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOrbClick()
                        } else if (dist in rRaceIn..(rPx + 24f)) {
                            val tapDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            val rel = ((tapDeg - 180f - bearingAngle.value) % 360f + 360f) % 360f
                            val tapped = (((rel + stepDeg * 0.5f) / stepDeg).toInt()) % count
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (tapped == currentSelected) {
                                currentOnActivate(modules[tapped])
                            } else {
                                currentOnSelect(tapped)
                            }
                        }
                    }
                }
                .pointerInput(count, widthPx, heightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val start = awaitTouchSlopOrCancellation(down.id) { c, _ -> c.consume() }
                            ?: return@awaitEachGesture
                        val distAtStart = hypot(start.position.x - cx, start.position.y - cy)
                        if (distAtStart > rPx + 24.dp.toPx()) return@awaitEachGesture

                        motionJob?.cancel()
                        isDragging = true
                        var dragAngle = bearingAngle.value
                        var lastDeg = Math.toDegrees(
                            atan2((start.position.y - cy).toDouble(), (start.position.x - cx).toDouble())
                        ).toFloat()
                        var angVel = 0f
                        var lastTimeNs = start.uptimeMillis * 1_000_000L
                        val velTracker = VelocityTracker()

                        drag(start.id) { ch ->
                            ch.consume()
                            velTracker.addPosition(ch.uptimeMillis, ch.position)
                            val curDeg = Math.toDegrees(
                                atan2((ch.position.y - cy).toDouble(), (ch.position.x - cx).toDouble())
                            ).toFloat()
                            var delta = curDeg - lastDeg
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f
                            val nowNs = ch.uptimeMillis * 1_000_000L
                            val dtSec = ((nowNs - lastTimeNs) / 1e9f).coerceIn(0.005f, 0.08f)
                            lastTimeNs = nowNs
                            lastDeg = curDeg
                            dragAngle += delta
                            angVel = angVel * 0.55f + (delta / dtSec) * 0.45f

                            val slot = ((-dragAngle / stepDeg).roundToInt() % count + count) % count
                            if (slot != lastSyncedIndex) {
                                lastSyncedIndex = slot
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnSelect(slot)
                            }
                            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                bearingAngle.snapTo(dragAngle)
                            }
                        }

                        isDragging = false
                        val clampedVel = angVel.coerceIn(-900f, 900f)
                        motionJob = scope.launch {
                            var angle = bearingAngle.value
                            var vel = clampedVel
                            var lastFrame = 0L
                            // Coast like a ball bearing then snap smoothly to nearest wedge
                            while (abs(vel) > 35f) {
                                withFrameNanos { now ->
                                    if (lastFrame == 0L) lastFrame = now
                                    val dt = ((now - lastFrame) / 1e9f).coerceIn(0.001f, 0.033f)
                                    lastFrame = now
                                    angle += vel * dt
                                    vel *= exp(-dt * 2.2f)
                                    val slot = ((-angle / stepDeg).roundToInt() % count + count) % count
                                    if (slot != lastSyncedIndex) {
                                        lastSyncedIndex = slot
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentOnSelect(slot)
                                    }
                                }
                                bearingAngle.snapTo(angle)
                            }
                            val nearest = (-angle / stepDeg).roundToInt()
                            val finalSlot = ((nearest % count) + count) % count
                            if (finalSlot != lastSyncedIndex) {
                                lastSyncedIndex = finalSlot
                                currentOnSelect(finalSlot)
                            }
                            bearingAngle.animateTo(
                                -nearest * stepDeg,
                                spring(dampingRatio = 0.82f, stiffness = 360f)
                            )
                        }
                    }
                }
        ) {
            val angle = bearingAngle.value
            val center = Offset(cx, cy)

            // Outer counter-rotating ring lA
            val outerDashR = rPx + 13.dp.toPx()
            rotate(degrees = -angle * 0.6f, pivot = center) {
                drawCircle(
                    color = V3Teal.copy(alpha = 0.45f),
                    radius = outerDashR,
                    center = center,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 6.dp.toPx(), 2.dp.toPx(), 6.dp.toPx()))
                    )
                )
                listOf(30f, 110f, 200f, 290f).forEach { deg ->
                    val rad = Math.toRadians(deg.toDouble())
                    drawCircle(
                        color = V3Red,
                        radius = 3.dp.toPx(),
                        center = Offset(cx + (outerDashR * cos(rad)).toFloat(), cy + (outerDashR * sin(rad)).toFloat())
                    )
                }
                val arcR = rPx + 19.dp.toPx()
                drawArc(
                    color = V3Red,
                    startAngle = 140f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(cx - arcR, cy - arcR),
                    size = Size(arcR * 2f, arcR * 2f),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Housing disk
            drawCircle(
                color = Color(0xFF030405),
                radius = rPx + 5.dp.toPx(),
                center = center
            )
            drawCircle(
                color = V3TealDark,
                radius = rPx + 5.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Rotating bezel ticks (race)
            rotate(degrees = angle, pivot = center) {
                for (a in 0 until 360 step 3) {
                    val isMajor = a % 15 == 0
                    val innerTickR = rPx - (if (isMajor) 7.dp.toPx() else 3.dp.toPx())
                    val rad = Math.toRadians(a.toDouble())
                    drawLine(
                        color = V3Teal.copy(alpha = if (isMajor) 0.72f else 0.42f),
                        start = Offset(cx + (innerTickR * cos(rad)).toFloat(), cy + (innerTickR * sin(rad)).toFloat()),
                        end = Offset(cx + (rPx * cos(rad)).toFloat(), cy + (rPx * sin(rad)).toFloat()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // 9 Module wedges
            val focusFillBrush = Brush.horizontalGradient(
                colors = listOf(V3RedBright, V3RedDark),
                startX = cx - r2,
                endX = cx - r1
            )
            val gapDeg = 1.8f

            for (i in 0 until count) {
                val cDeg = 180f + i * stepDeg + angle
                val startDeg = cDeg - stepDeg * 0.5f + gapDeg * 0.5f
                val sweep = stepDeg - gapDeg
                val isFocus = i == selectedIndex
                val neighborDist = min(
                    ((i - selectedIndex) % count + count) % count,
                    ((selectedIndex - i) % count + count) % count
                )
                val isNeighbor = neighborDist == 1

                buildArcWedgePath(wedgePath, cx, cy, r1, r2, startDeg, sweep)

                if (isFocus) {
                    drawPath(wedgePath, focusFillBrush)
                    drawPath(wedgePath, V3Teal, style = Stroke(width = 1.4.dp.toPx()))
                } else {
                    drawPath(wedgePath, Color(0xE6090C10))
                    drawPath(
                        wedgePath,
                        if (isNeighbor) V3Teal.copy(alpha = 0.42f) else V3Teal.copy(alpha = 0.22f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Draw code number & module title rotated along the wedge spoke
                val radCenter = Math.toRadians(cDeg.toDouble())
                val textR = r2 - 22.dp.toPx()
                val tx = cx + (textR * cos(radCenter)).toFloat()
                val ty = cy + (textR * sin(radCenter)).toFloat()
                val mod = modules[i]

                drawIntoCanvas { canvas ->
                    val nc = canvas.nativeCanvas
                    nc.save()
                    nc.translate(tx, ty)
                    nc.rotate(cDeg - 180f)

                    numPaint.textSize = 7.dp.toPx()
                    numPaint.color = if (isFocus) android.graphics.Color.parseColor("#22D3E6")
                    else android.graphics.Color.parseColor("#14879A")
                    nc.drawText(mod.code, 0f, -6.dp.toPx(), numPaint)

                    labelPaint.textSize = 8.2.dp.toPx()
                    labelPaint.color = when {
                        isFocus -> android.graphics.Color.WHITE
                        isNeighbor -> android.graphics.Color.parseColor("#DFE6EC")
                        else -> android.graphics.Color.parseColor("#C3CCD4")
                    }
                    nc.drawText(mod.title, 0f, 5.dp.toPx(), labelPaint)
                    nc.restore()
                }
            }

            // Outer & inner bearing race rings
            drawCircle(
                color = V3TealDark,
                radius = rRaceOut,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = Color(0xE605070A),
                radius = rRaceOut - 1.dp.toPx(),
                center = center
            )

            // 16 Metallic ball bearings rotating at angle / 2
            val ballRadius = (rPx * 0.026f).coerceAtLeast(3.dp.toPx())
            for (i in 0 until 16) {
                val bDeg = i * 22.5f + angle * 0.5f
                val rad = Math.toRadians(bDeg.toDouble())
                val bx = cx + (rBall * cos(rad)).toFloat()
                val by = cy + (rBall * sin(rad)).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White,
                            0.45f to Color(0xFFB9C6D2),
                            1.00f to Color(0xFF2D3742)
                        ),
                        center = Offset(bx - ballRadius * 0.3f, by - ballRadius * 0.3f),
                        radius = (ballRadius * 1.4f).coerceAtLeast(1f)
                    ),
                    radius = ballRadius,
                    center = Offset(bx, by)
                )
            }

            // Inner glowing race ring
            drawCircle(
                color = V3Teal,
                radius = rRaceIn,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner counter-rotating arcs lB
            val innerArcR = rRaceIn - 7.dp.toPx()
            rotate(degrees = angle * 1.8f, pivot = center) {
                drawArc(
                    color = V3Teal,
                    startAngle = 200f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(cx - innerArcR, cy - innerArcR),
                    size = Size(innerArcR * 2f, innerArcR * 2f),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = V3Red,
                    startAngle = 20f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(cx - innerArcR, cy - innerArcR),
                    size = Size(innerArcR * 2f, innerArcR * 2f),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Red focus window around 180° left-pointing slot
            buildArcWedgePath(focusPath, cx, cy, r1 - 3.dp.toPx(), r2 + 3.dp.toPx(), 180f - stepDeg * 0.5f, stepDeg)
            drawPath(focusPath, V3Red, style = Stroke(width = 1.6.dp.toPx()))

            // Cyan pointer notch & dashed connector line to Focus Panel
            val tx = cx - rPx - 4.dp.toPx()
            val notchPath = Path().apply {
                moveTo(tx - 9.dp.toPx(), cy)
                lineTo(tx + 1.dp.toPx(), cy - 6.dp.toPx())
                lineTo(tx + 1.dp.toPx(), cy + 6.dp.toPx())
                close()
            }
            drawPath(notchPath, V3Teal)
            drawLine(
                color = V3Teal.copy(alpha = 0.8f),
                start = Offset(0f, cy),
                end = Offset((tx - 10.dp.toPx()).coerceAtLeast(0f), cy),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx()))
            )
        }

        // Center clickable plasma orb positioned on the right edge (cx, cy)
        val orbSizeDp = with(density) { (rPx * 0.66f).toDp().coerceAtLeast(64.dp) }
        val orbSizePx = with(density) { orbSizeDp.toPx() }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (cx - orbSizePx * 0.5f).roundToInt(),
                        y = (cy - orbSizePx * 0.5f).roundToInt()
                    )
                }
                .size(orbSizeDp)
                .clip(CircleShape)
                .clickable(onClick = onOrbClick)
                .testTag("dial_center_orb_trigger"),
            contentAlignment = Alignment.Center
        ) {
            PlasmaFilamentOrb(
                size = orbSizeDp,
                pauseAnimation = pauseAnimations,
                speedMult = speedMult,
                bloomMult = bloomMult,
                modifier = Modifier.testTag("stx_html_orb_view")
            )
        }
    }
}

private fun buildArcWedgePath(
    path: Path,
    cx: Float,
    cy: Float,
    r1: Float,
    r2: Float,
    startDeg: Float,
    sweepDeg: Float
) {
    path.reset()
    val a0 = Math.toRadians(startDeg.toDouble())
    val a1 = Math.toRadians((startDeg + sweepDeg).toDouble())
    path.moveTo((cx + r1 * cos(a0)).toFloat(), (cy + r1 * sin(a0)).toFloat())
    path.lineTo((cx + r2 * cos(a0)).toFloat(), (cy + r2 * sin(a0)).toFloat())
    path.arcTo(Rect(cx - r2, cy - r2, cx + r2, cy + r2), startDeg, sweepDeg, false)
    path.lineTo((cx + r1 * cos(a1)).toFloat(), (cy + r1 * sin(a1)).toFloat())
    path.arcTo(Rect(cx - r1, cy - r1, cx + r1, cy + r1), startDeg + sweepDeg, -sweepDeg, false)
    path.close()
}
