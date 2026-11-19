package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DialWedgeItem
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanSoft
import com.example.ui.theme.HudElectricBlue
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSilver
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

enum class DialSide {
    LEFT,
    RIGHT
}

@Composable
fun ArcReactorFullDial(
    items: List<DialWedgeItem>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onActivateIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
) {
    if (items.isEmpty()) return
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val count = items.size
    val stepDeg = 360f / count
    val currentSelectedIdx by rememberUpdatedState(selectedIndex.coerceIn(0, count - 1))
    val currentOnSelect by rememberUpdatedState(onSelectIndex)
    val currentOnActivate by rememberUpdatedState(onActivateIndex)

    val bearingAngle = remember(count) { Animatable(-selectedIndex.coerceIn(0, count - 1) * stepDeg) }
    val detentPulse = remember { Animatable(1f) }
    var isDragging by remember { mutableStateOf(false) }
    var lastSyncedIndex by remember(count) { mutableIntStateOf(selectedIndex.coerceIn(0, count - 1)) }
    var activeMotionJob by remember(count) { mutableStateOf<Job?>(null) }

    val infiniteOrb = rememberInfiniteTransition(label = "dial_sweep_orb")
    val sweepOrbitDeg by infiniteOrb.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "outer_sweep_deg"
    )
    val radarSweepDeg by infiniteOrb.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6400, easing = LinearEasing), RepeatMode.Restart),
        label = "inner_radar_deg"
    )
    val dashedEffect = remember { PathEffect.dashPathEffect(floatArrayOf(14f, 6f, 3f, 6f), 0f) }

    LaunchedEffect(selectedIndex) {
        detentPulse.snapTo(0f)
        detentPulse.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 360)
        )
    }

    LaunchedEffect(selectedIndex, isDragging, count) {
        if (!isDragging) {
            val safeIdx = selectedIndex.coerceIn(0, count - 1)
            val current = bearingAngle.value
            val currentSlot = ((-current / stepDeg).roundToInt() % count + count) % count
            if (safeIdx != currentSlot || safeIdx != lastSyncedIndex) {
                lastSyncedIndex = safeIdx
                val desiredBase = -safeIdx * stepDeg
                val deltaMod = (((desiredBase - current) % 360f + 540f) % 360f) - 180f
                bearingAngle.animateTo(
                    targetValue = current + deltaMod,
                    animationSpec = spring(
                        dampingRatio = 0.80f,
                        stiffness = 340f
                    )
                )
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val diameterPx = min(widthPx, heightPx).coerceAtLeast(160f)
        val radiusPx = diameterPx * 0.48f
        val cx = widthPx / 2f
        val cy = heightPx / 2f

        val wedgeInnerR = radiusPx * 0.42f
        val wedgeOuterR = radiusPx * 0.84f
        val activeOuterR = radiusPx * 0.90f
        val reusablePath = remember { Path() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(count, widthPx, heightPx) {
                    detectTapGestures { pos ->
                        val dx = pos.x - cx
                        val dy = pos.y - cy
                        val dist = hypot(dx, dy)
                        if (dist <= wedgeInnerR) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnActivate(currentSelectedIdx)
                            return@detectTapGestures
                        }
                        if (dist in wedgeInnerR..(activeOuterR * 1.12f)) {
                            val deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            val rel = ((deg - (-90f + bearingAngle.value)) % 360f + 360f) % 360f
                            val tappedIdx = (((rel + stepDeg / 2f) / stepDeg).toInt()) % count
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnSelect(tappedIdx)
                            currentOnActivate(tappedIdx)
                        }
                    }
                }
                .pointerInput(count, widthPx, heightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val dragStart = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                            change.consume()
                        } ?: return@awaitEachGesture

                        activeMotionJob?.cancel()
                        isDragging = true
                        var angularVelocity = 0f
                        var dragAngle = bearingAngle.value.let { if (it.isFinite()) it else 0f }
                        val initialAngle = Math.toDegrees(
                            atan2((dragStart.position.y - cy).toDouble(), (dragStart.position.x - cx).toDouble())
                        ).toFloat()
                        var lastPointerAngle = if (initialAngle.isFinite()) initialAngle else 0f

                        val completed = drag(dragStart.id) { change ->
                            change.consume()
                            val curAngle = Math.toDegrees(
                                atan2((change.position.y - cy).toDouble(), (change.position.x - cx).toDouble())
                            ).toFloat()
                            if (!curAngle.isFinite()) return@drag
                            var delta = curAngle - lastPointerAngle
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f
                            if (!delta.isFinite()) return@drag
                            lastPointerAngle = curAngle
                            angularVelocity = angularVelocity * 0.58f + delta * 0.42f
                            dragAngle += delta
                            if (!dragAngle.isFinite()) return@drag

                            val detentSlot = (-dragAngle / stepDeg).roundToInt()
                            val normalizedIdx = ((detentSlot % count) + count) % count
                            if (normalizedIdx != lastSyncedIndex) {
                                lastSyncedIndex = normalizedIdx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnSelect(normalizedIdx)
                            }
                            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                bearingAngle.snapTo(dragAngle)
                            }
                        }

                        isDragging = false
                        if (completed) {
                            val projected = dragAngle + (angularVelocity * 3.1f)
                                .coerceIn(-stepDeg * 2.2f, stepDeg * 2.2f)
                            val safeProjected = if (projected.isFinite()) projected else -currentSelectedIdx * stepDeg
                            val nearestDetentSlot = (-safeProjected / stepDeg).roundToInt()
                            val targetAngle = -nearestDetentSlot * stepDeg
                            val normalizedIdx = ((nearestDetentSlot % count) + count) % count
                            if (normalizedIdx != lastSyncedIndex) {
                                lastSyncedIndex = normalizedIdx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnSelect(normalizedIdx)
                            }
                            activeMotionJob?.cancel()
                            activeMotionJob = scope.launch {
                                bearingAngle.animateTo(
                                    targetValue = targetAngle,
                                    animationSpec = spring(
                                        dampingRatio = 0.80f,
                                        stiffness = 320f
                                    )
                                )
                            }
                        } else {
                            activeMotionJob?.cancel()
                            activeMotionJob = scope.launch {
                                val cur = bearingAngle.value
                                val safeCur = if (cur.isFinite()) cur else -currentSelectedIdx * stepDeg
                                val nearestDetentSlot = (-safeCur / stepDeg).roundToInt()
                                bearingAngle.animateTo(
                                    targetValue = -nearestDetentSlot * stepDeg,
                                    animationSpec = spring(
                                        dampingRatio = 0.86f,
                                        stiffness = 360f
                                    )
                                )
                            }
                        }
                    }
                }
        ) {
            if (size.width <= 0f || size.height <= 0f || size.minDimension <= 0f) return@Canvas
            val currentAngle = bearingAngle.value
            val tickOuter = radiusPx * 0.99f
            val tickInnerMinor = radiusPx * 0.955f
            val tickInnerMajor = radiusPx * 0.922f
            val tickPhase = currentAngle * 0.25f

            // Ambient radial halo + Iron Man 2 holographic radar sweep cone behind dial
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        HudElectricBlue.copy(alpha = if (isDragging) 0.16f else 0.09f),
                        HudCrimson.copy(alpha = if (isDragging) 0.13f else 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = (radiusPx * 1.05f).coerceAtLeast(1f)
                ),
                center = Offset(cx, cy),
                radius = radiusPx * 1.05f
            )
            withTransform({
                rotate(degrees = radarSweepDeg, pivot = Offset(cx, cy))
            }) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.72f to Color.Transparent,
                        0.92f to HudCyan.copy(alpha = if (isDragging) 0.14f else 0.08f),
                        0.985f to HudIce.copy(alpha = if (isDragging) 0.18f else 0.10f),
                        1.0f to Color.Transparent,
                        center = Offset(cx, cy)
                    ),
                    center = Offset(cx, cy),
                    radius = wedgeOuterR * 1.02f
                )
            }

            // Detent lock magnetic shockwave ripple on module switch
            val pulseFrac = detentPulse.value
            if (pulseFrac in 0.01f..0.98f) {
                val pulseR = wedgeInnerR + (radiusPx * 1.06f - wedgeInnerR) * pulseFrac
                val pulseAlpha = (1f - pulseFrac) * 0.55f
                drawCircle(
                    color = HudCyan.copy(alpha = pulseAlpha),
                    radius = pulseR,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.2.dp.toPx() * (1f - pulseFrac * 0.5f))
                )
                drawCircle(
                    color = HudCrimson.copy(alpha = pulseAlpha * 0.65f),
                    radius = (pulseR * 0.92f).coerceAtLeast(1f),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 72 precision ticks with alternating Red / Amber / Electric Blue / Cyan beacon pips
            for (t in 0 until 72) {
                val a = Math.toRadians((t * 5.0) - 90.0 + tickPhase)
                val major = t % 6 == 0
                val crimsonTick = t % 12 == 0
                val amberSub = major && !crimsonTick
                val rIn = if (major) tickInnerMajor else tickInnerMinor
                drawLine(
                    color = when {
                        crimsonTick -> HudCrimson.copy(alpha = 0.92f)
                        amberSub -> HudAmber.copy(alpha = 0.78f)
                        major -> HudCyan.copy(alpha = 0.82f)
                        t % 2 == 0 -> HudElectricBlue.copy(alpha = 0.36f)
                        else -> HudCrimson.copy(alpha = 0.24f)
                    },
                    start = Offset(cx + (rIn * cos(a)).toFloat(), cy + (rIn * sin(a)).toFloat()),
                    end = Offset(cx + (tickOuter * cos(a)).toFloat(), cy + (tickOuter * sin(a)).toFloat()),
                    strokeWidth = if (major) 2.2f else 1.1f
                )
                if (crimsonTick) {
                    val dotR = radiusPx * 1.005f
                    drawCircle(
                        color = HudCrimson,
                        radius = 2.dp.toPx(),
                        center = Offset(cx + (dotR * cos(a)).toFloat(), cy + (dotR * sin(a)).toFloat())
                    )
                }
            }

            // Kinetic outer dashed gyro ring + glowing Crimson & Cyan laser sweep arcs
            val gyroR = radiusPx * 0.945f
            val gyroTopLeft = Offset(cx - gyroR, cy - gyroR)
            val gyroSize = Size(gyroR * 2f, gyroR * 2f)
            drawArc(
                color = HudElectricBlue.copy(alpha = 0.36f),
                startAngle = -sweepOrbitDeg,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = gyroTopLeft,
                size = gyroSize,
                style = Stroke(width = 1.6.dp.toPx(), pathEffect = dashedEffect)
            )
            // Counter-arc cyan holographic stabilizer
            drawArc(
                color = HudCyan.copy(alpha = if (isDragging) 0.55f else 0.32f),
                startAngle = -sweepOrbitDeg * 1.25f - 45f,
                sweepAngle = 64f,
                useCenter = false,
                topLeft = gyroTopLeft,
                size = gyroSize,
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
            )
            // Sweeping glowing crimson perimeter laser arc
            val crimsonSweepStart = 128f + currentAngle * 0.35f + sweepOrbitDeg * 0.25f
            drawArc(
                color = HudCrimson.copy(alpha = if (isDragging) 0.44f else 0.24f),
                startAngle = crimsonSweepStart - 6f,
                sweepAngle = if (isDragging) 126f else 104f,
                useCenter = false,
                topLeft = gyroTopLeft,
                size = gyroSize,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = HudCrimson.copy(alpha = 0.94f),
                startAngle = crimsonSweepStart,
                sweepAngle = if (isDragging) 114f else 92f,
                useCenter = false,
                topLeft = gyroTopLeft,
                size = gyroSize,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Dual calibration rings just outside wedges
            drawCircle(
                color = HudElectricBlue.copy(alpha = 0.38f),
                radius = radiusPx * 0.908f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.1.dp.toPx())
            )
            drawCircle(
                color = HudCrimson.copy(alpha = 0.30f),
                radius = radiusPx * 0.888f,
                center = Offset(cx, cy),
                style = Stroke(width = 0.9.dp.toPx())
            )

            // Module annular wedges
            val gapDeg = if (count > 7) 3.4f else 4.2f
            for (i in 0 until count) {
                val centerAngle = -90f + i * stepDeg + currentAngle
                val isSelected = i == selectedIndex
                val item = items[i]
                val outerR = if (isSelected) activeOuterR else wedgeOuterR
                fillAnnularWedgePath(
                    path = reusablePath,
                    cx = cx,
                    cy = cy,
                    innerR = wedgeInnerR,
                    outerR = outerR,
                    startAngleDeg = centerAngle - (stepDeg / 2f) + (gapDeg / 2f),
                    sweepDeg = stepDeg - gapDeg
                )

                val fillColor = when {
                    isSelected && item.isAccentCrimson -> HudCrimson.copy(alpha = 0.32f)
                    isSelected -> HudElectricBlue.copy(alpha = 0.30f)
                    else -> Color(0x88060E1A)
                }
                val strokeColor = when {
                    isSelected && item.isAccentCrimson -> HudCrimson
                    isSelected -> HudIce
                    item.isAccentCrimson -> HudCrimson.copy(alpha = 0.52f)
                    else -> HudCyan.copy(alpha = 0.36f)
                }

                if (isSelected) {
                    drawPath(
                        path = reusablePath,
                        color = (if (item.isAccentCrimson) HudCrimson else HudCyan).copy(alpha = 0.28f),
                        style = Stroke(width = 6.dp.toPx())
                    )
                }
                drawPath(path = reusablePath, color = fillColor)
                drawPath(
                    path = reusablePath,
                    color = strokeColor,
                    style = Stroke(width = if (isSelected) 2.dp.toPx() else 1.dp.toPx())
                )
            }

            // Stark 12-o'clock target-lock reticle bracket + crimson crown
            val reticleR = activeOuterR + 4.dp.toPx()
            drawArc(
                color = HudCyan.copy(alpha = 0.72f),
                startAngle = -108f,
                sweepAngle = 36f,
                useCenter = false,
                topLeft = Offset(cx - reticleR, cy - reticleR),
                size = Size(reticleR * 2f, reticleR * 2f),
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
            )
            val tipY = cy - activeOuterR - 3.dp.toPx()
            drawCircle(
                color = HudCrimson.copy(alpha = 0.38f),
                radius = 7.dp.toPx(),
                center = Offset(cx, tipY)
            )
            reusablePath.reset()
            reusablePath.moveTo(cx, tipY + 6.dp.toPx())
            reusablePath.lineTo(cx - 5.5.dp.toPx(), tipY - 3.dp.toPx())
            reusablePath.lineTo(cx + 5.5.dp.toPx(), tipY - 3.dp.toPx())
            reusablePath.close()
            drawPath(reusablePath, color = HudCrimson)
        }

        val coreDiameterDp = with(density) { (wedgeInnerR * 1.88f).toDp() }
        ArcCoreSpinnerOverlay(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                currentOnActivate(currentSelectedIdx)
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    val corePx = coreDiameterDp.roundToPx()
                    IntOffset((cx - corePx / 2f).roundToInt(), (cy - corePx / 2f).roundToInt())
                }
                .size(coreDiameterDp)
                .testTag("dial_center_orb_trigger")
        )

        // Labels read bearingAngle inside GPU graphicsLayer{} (zero layout passes during 60/120fps drag)
        val labelBoxW = (radiusPx * 0.48f / density.density).dp.coerceIn(38.dp, 62.dp)
        val labelBoxH = 24.dp
        val labelHalfWPx = with(density) { labelBoxW.toPx() / 2f }
        val labelHalfHPx = with(density) { labelBoxH.toPx() / 2f }
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val labelR = if (isSelected) {
                (wedgeInnerR + activeOuterR) * 0.50f
            } else {
                (wedgeInnerR + wedgeOuterR) * 0.50f
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(width = labelBoxW, height = labelBoxH)
                    .graphicsLayer {
                        val angleDeg = -90f + index * stepDeg + bearingAngle.value
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val lx = cx + (labelR * cos(angleRad)).toFloat()
                        val ly = cy + (labelR * sin(angleRad)).toFloat()
                        translationX = lx - labelHalfWPx
                        translationY = ly - labelHalfHPx
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.label,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = if (compactMode) 7.5.sp else 8.5.sp,
                    lineHeight = 9.sp,
                    color = if (isSelected) HudIce else HudSilver,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = item.badge,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = if (compactMode) 6.5.sp else 7.sp,
                    lineHeight = 8.sp,
                    color = if (isSelected) HudCyan else HudSlate,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * High-FPS symmetrical Half-Dial for either LEFT or RIGHT side of the cockpit.
 * Reads continuous animation state ONLY in Draw (Canvas) and Layout (offset{}) phases,
 * eliminating per-frame Compose recompositions for smooth 60/120fps bearing rotation.
 */
@Composable
fun ArcReactorSideHalfDial(
    side: DialSide,
    items: List<DialWedgeItem>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onActivateIndex: (Int) -> Unit,
    onCenterActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    centerLabel: String = "< BACK",
    centerSubLabel: String? = null,
    centerAccentCrimson: Boolean = false,
    centerButtonTestTag: String = "half_dial_center_back",
    showOuterTicks: Boolean = false,
    showCoreSpinner: Boolean = false,
    accentPrimary: Color = HudCyan,
    accentSecondary: Color = HudCrimson,
    halfOffScreen: Boolean = false,
    activateOnlyWhenFocused: Boolean = false
) {
    if (items.isEmpty()) return
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val count = items.size
    val maxSlots = min(if (showOuterTicks) 7 else 5, count)
    val stepDeg = 148f / maxSlots.coerceAtLeast(3)

    val currentSelectedIdx by rememberUpdatedState(selectedIndex.coerceIn(0, count - 1))
    val currentOnSelect by rememberUpdatedState(onSelectIndex)
    val currentOnActivate by rememberUpdatedState(onActivateIndex)
    val currentOnCenterClick by rememberUpdatedState(onCenterActionClick)

    val continuousPos = remember(count) { Animatable(selectedIndex.coerceIn(0, count - 1).toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    var lastSyncedIndex by remember(count) { mutableIntStateOf(selectedIndex.coerceIn(0, count - 1)) }
    var activeMotionJob by remember(count) { mutableStateOf<Job?>(null) }

    LaunchedEffect(selectedIndex, isDragging, count) {
        if (!isDragging) {
            val safeSelected = selectedIndex.coerceIn(0, count - 1)
            val cur = continuousPos.value
            val curMod = ((cur.roundToInt() % count) + count) % count
            if (safeSelected != curMod || safeSelected != lastSyncedIndex) {
                lastSyncedIndex = safeSelected
                var diff = safeSelected - curMod
                if (diff > count / 2) diff -= count
                if (diff < -count / 2) diff += count
                val target = cur.roundToInt() + diff.toFloat()
                continuousPos.animateTo(
                    targetValue = target,
                    animationSpec = spring(
                        dampingRatio = 0.84f,
                        stiffness = 380f
                    )
                )
            }
        }
    }

    // Only recompose label slots when the integer snapped center changes (NOT every frame!)
    val centerWhole by remember(count) {
        derivedStateOf { continuousPos.value.roundToInt() }
    }
    val halfSpan = (maxSlots / 2) + 1
    val slotOffsets = remember(halfSpan, count) {
        when (count) {
            1 -> listOf(0)
            2 -> listOf(0, 1)
            else -> (-halfSpan..halfSpan).toList()
        }
    }

    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val baseAngle = if (side == DialSide.LEFT) 0f else 180f
        val cx = when {
            halfOffScreen && side == DialSide.LEFT -> 0f
            halfOffScreen -> widthPx
            side == DialSide.LEFT -> widthPx * 0.18f
            else -> widthPx * 0.82f
        }
        val cy = heightPx * 0.50f
        val radiusPx = if (halfOffScreen) {
            min(heightPx * 0.48f, widthPx * 0.94f).coerceAtLeast(88f)
        } else {
            min(heightPx * 0.45f, widthPx * 0.78f).coerceAtLeast(82f)
        }
        val wedgeInnerR = radiusPx * 0.38f
        val wedgeOuterR = radiusPx * 0.87f
        val activeOuterR = radiusPx * 0.94f
        val reusablePath = remember { Path() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(count, side, widthPx, heightPx) {
                    if (count <= 1) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val dragStart = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                            change.consume()
                        } ?: return@awaitEachGesture

                        activeMotionJob?.cancel()
                        isDragging = true
                        var velocitySlots = 0f
                        var dragPos = continuousPos.value.let { if (it.isFinite()) it else 0f }
                        val initialAngle = Math.toDegrees(
                            atan2((dragStart.position.y - cy).toDouble(), (dragStart.position.x - cx).toDouble())
                        ).toFloat()
                        var lastAngle = if (initialAngle.isFinite()) initialAngle else 0f

                        val completed = drag(dragStart.id) { change ->
                            val dragDeltaY = change.positionChange().y
                            change.consume()
                            val curAngle = Math.toDegrees(
                                atan2((change.position.y - cy).toDouble(), (change.position.x - cx).toDouble())
                            ).toFloat()
                            if (!curAngle.isFinite() || !dragDeltaY.isFinite()) return@drag
                            var degDelta = curAngle - lastAngle
                            if (degDelta > 180f) degDelta -= 360f
                            if (degDelta < -180f) degDelta += 360f
                            if (!degDelta.isFinite()) return@drag
                            lastAngle = curAngle

                            // Match vertical drag sign to angular direction so finger and wheel move together
                            val ySign = if (side == DialSide.LEFT) -1f else 1f
                            val slotDelta = (-degDelta / stepDeg) * 0.65f +
                                (ySign * dragDeltaY / 52f) * 0.35f
                            if (!slotDelta.isFinite()) return@drag
                            velocitySlots = velocitySlots * 0.55f + slotDelta * 0.45f
                            dragPos += slotDelta
                            if (!dragPos.isFinite()) return@drag

                            val idx = ((dragPos.roundToInt() % count) + count) % count
                            if (idx != lastSyncedIndex) {
                                lastSyncedIndex = idx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnSelect(idx)
                            }
                            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                continuousPos.snapTo(dragPos)
                            }
                        }

                        isDragging = false
                        if (completed) {
                            val projected = dragPos +
                                (velocitySlots * 1.8f).coerceIn(-1.5f, 1.5f)
                            val targetSlot = if (projected.isFinite()) projected.roundToInt().toFloat() else currentSelectedIdx.toFloat()
                            val idx = ((targetSlot.roundToInt() % count) + count) % count
                            if (idx != lastSyncedIndex) {
                                lastSyncedIndex = idx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnSelect(idx)
                            }
                            activeMotionJob?.cancel()
                            activeMotionJob = scope.launch {
                                continuousPos.animateTo(
                                    targetValue = targetSlot,
                                    animationSpec = spring(
                                        dampingRatio = 0.80f,
                                        stiffness = 340f
                                    )
                                )
                            }
                        } else {
                            activeMotionJob?.cancel()
                            activeMotionJob = scope.launch {
                                val cur = continuousPos.value
                                val safeCur = if (cur.isFinite()) cur.roundToInt().toFloat() else currentSelectedIdx.toFloat()
                                continuousPos.animateTo(
                                    targetValue = safeCur,
                                    animationSpec = spring(
                                        dampingRatio = 0.86f,
                                        stiffness = 360f
                                    )
                                )
                            }
                        }
                    }
                }
        ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(count, side, widthPx, heightPx, activateOnlyWhenFocused) {
                    detectTapGestures { pos ->
                        val dx = pos.x - cx
                        val dy = pos.y - cy
                        val dist = hypot(dx, dy)
                        if (dist <= wedgeInnerR) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnCenterClick()
                            return@detectTapGestures
                        }
                        if (dist in wedgeInnerR..(activeOuterR * 1.18f)) {
                            var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (!deg.isFinite()) return@detectTapGestures
                            if (side == DialSide.RIGHT && deg < 0f) {
                                deg += 360f
                            }
                            val slotFromCenter = (deg - baseAngle) / stepDeg
                            val curPos = continuousPos.value.let { if (it.isFinite()) it else 0f }
                            val targetWhole = (curPos + slotFromCenter).roundToInt()
                            val tappedIdx = ((targetWhole % count) + count) % count
                            val wasFocused = tappedIdx == currentSelectedIdx
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnSelect(tappedIdx)
                            if (!activateOnlyWhenFocused || wasFocused) {
                                currentOnActivate(tappedIdx)
                            }
                        }
                    }
                }
        ) {
            if (size.width <= 0f || size.height <= 0f || size.minDimension <= 0f) return@Canvas
            val posVal = continuousPos.value.let { if (it.isFinite()) it else 0f }
            val whole = posVal.roundToInt()
            val frac = posVal - whole
            val bearingDeg = posVal * stepDeg

            if (showOuterTicks) {
                val startDeg = if (side == DialSide.LEFT) -88 else 92
                val endDeg = if (side == DialSide.LEFT) 88 else 268
                for (deg in startDeg..endDeg step 6) {
                    val rad = Math.toRadians(deg.toDouble() + (bearingDeg % 6f))
                    val major = deg % 18 == 0
                    val rIn = if (major) radiusPx * 0.94f else radiusPx * 0.965f
                    val rOut = radiusPx * 1.0f
                    val isCenter = abs(deg - baseAngle.toInt()) <= 10
                    drawLine(
                        color = when {
                            isCenter -> accentSecondary.copy(alpha = 0.95f)
                            major -> accentPrimary.copy(alpha = 0.72f)
                            deg % 12 == 0 -> HudElectricBlue.copy(alpha = 0.36f)
                            else -> accentSecondary.copy(alpha = 0.22f)
                        },
                        start = Offset(cx + (rIn * cos(rad)).toFloat(), cy + (rIn * sin(rad)).toFloat()),
                        end = Offset(cx + (rOut * cos(rad)).toFloat(), cy + (rOut * sin(rad)).toFloat()),
                        strokeWidth = if (major) 2f else 1f
                    )
                }
            } else {
                // Dual-tone Crimson/Cyan laser perimeter arcs for high-craft cockpit
                val sweepR = activeOuterR + 4.dp.toPx()
                drawArc(
                    color = accentPrimary.copy(alpha = 0.20f),
                    startAngle = baseAngle - 78f,
                    sweepAngle = 156f,
                    useCenter = false,
                    topLeft = Offset(cx - sweepR, cy - sweepR),
                    size = Size(sweepR * 2f, sweepR * 2f),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = accentPrimary.copy(alpha = 0.72f),
                    startAngle = baseAngle - 78f,
                    sweepAngle = 156f,
                    useCenter = false,
                    topLeft = Offset(cx - sweepR, cy - sweepR),
                    size = Size(sweepR * 2f, sweepR * 2f),
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                )
                val accentSweepR = activeOuterR + 8.dp.toPx()
                drawArc(
                    color = accentSecondary.copy(alpha = 0.65f),
                    startAngle = baseAngle - 34f,
                    sweepAngle = 68f,
                    useCenter = false,
                    topLeft = Offset(cx - accentSweepR, cy - accentSweepR),
                    size = Size(accentSweepR * 2f, accentSweepR * 2f),
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                )
                for (pipDeg in listOf(baseAngle - 74f, baseAngle + 74f)) {
                    val pipRad = Math.toRadians(pipDeg.toDouble())
                    val pipCenter = Offset(
                        cx + (sweepR * cos(pipRad)).toFloat(),
                        cy + (sweepR * sin(pipRad)).toFloat()
                    )
                    drawCircle(color = HudCrimson, radius = 2.4.dp.toPx(), center = pipCenter)
                }
            }

            val gapDeg = 4.2f
            val activeRelSlots = when (count) {
                1 -> listOf(0)
                2 -> listOf(0, 1)
                else -> (-halfSpan..halfSpan).toList()
            }
            for (relSlot in activeRelSlots) {
                val rawSlot = if (count == 2) {
                    val slotPos = ((posVal % 2f) + 2f) % 2f
                    val d = relSlot - slotPos
                    when {
                        d > 1f -> d - 2f
                        d < -1f -> d + 2f
                        else -> d
                    }
                } else {
                    relSlot - frac
                }
                val centerAngle = baseAngle + rawSlot * stepDeg
                val distFromCenter = abs(centerAngle - baseAngle)
                if (distFromCenter > 84f) continue

                val itemIdx = if (count <= 2) relSlot else (((whole + relSlot) % count) + count) % count
                val item = items.getOrNull(itemIdx) ?: continue
                val isFocused = abs(rawSlot) < 0.45f
                val edgeFade = (1f - (distFromCenter / 86f)).coerceIn(0.28f, 1f)
                val outerR = if (isFocused) activeOuterR else wedgeOuterR

                fillAnnularWedgePath(
                    path = reusablePath,
                    cx = cx,
                    cy = cy,
                    innerR = wedgeInnerR,
                    outerR = outerR,
                    startAngleDeg = centerAngle - (stepDeg / 2f) + (gapDeg / 2f),
                    sweepDeg = stepDeg - gapDeg
                )

                val fill = when {
                    isFocused && item.isAccentCrimson -> HudCrimson.copy(alpha = 0.28f)
                    isFocused -> accentPrimary.copy(alpha = 0.24f)
                    else -> Color(0x99060D18).copy(alpha = 0.65f * edgeFade)
                }
                val stroke = when {
                    isFocused && item.isAccentCrimson -> HudCrimson
                    isFocused -> HudIce
                    item.isAccentCrimson -> HudCrimson.copy(alpha = 0.56f * edgeFade)
                    else -> accentPrimary.copy(alpha = 0.36f * edgeFade)
                }
                drawPath(path = reusablePath, color = fill)
                drawPath(
                    path = reusablePath,
                    color = stroke,
                    style = Stroke(width = if (isFocused) 2.dp.toPx() else 1.dp.toPx())
                )
            }

            // Dual-tone inner reactor rings when spinner is disabled
            if (!showCoreSpinner) {
                drawCircle(
                    color = accentPrimary.copy(alpha = 0.55f),
                    radius = wedgeInnerR * 0.90f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.6.dp.toPx())
                )
                drawCircle(
                    color = accentSecondary.copy(alpha = 0.45f),
                    radius = wedgeInnerR * 0.78f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // High-contrast reticle pointer triangle
            reusablePath.reset()
            if (side == DialSide.LEFT) {
                val pointerX = cx + activeOuterR + 3.dp.toPx()
                reusablePath.moveTo(pointerX - 7.dp.toPx(), cy)
                reusablePath.lineTo(pointerX + 4.dp.toPx(), cy - 5.dp.toPx())
                reusablePath.lineTo(pointerX + 4.dp.toPx(), cy + 5.dp.toPx())
            } else {
                val pointerX = cx - activeOuterR - 3.dp.toPx()
                reusablePath.moveTo(pointerX + 7.dp.toPx(), cy)
                reusablePath.lineTo(pointerX - 4.dp.toPx(), cy - 5.dp.toPx())
                reusablePath.lineTo(pointerX - 4.dp.toPx(), cy + 5.dp.toPx())
            }
            reusablePath.close()
            drawPath(reusablePath, color = accentSecondary)
        }

        if (showCoreSpinner) {
            val coreDiameterDp = with(density) { (wedgeInnerR * 1.76f).toDp() }
            ArcCoreSpinnerOverlay(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        val corePx = coreDiameterDp.roundToPx()
                        IntOffset((cx - corePx / 2f).roundToInt(), (cy - corePx / 2f).roundToInt())
                    }
                    .size(coreDiameterDp)
            )
        }

        // Labels read continuousPos inside GPU graphicsLayer{} (zero layout passes during drag)
        val labelBoxW = (radiusPx * 0.46f / density.density).dp.coerceIn(42.dp, 68.dp)
        val labelBoxH = 22.dp
        val sideLabelHalfWPx = with(density) { labelBoxW.toPx() / 2f }
        val sideLabelHalfHPx = with(density) { labelBoxH.toPx() / 2f }
        slotOffsets.forEach { relSlot ->
            val itemIdx = if (count <= 2) relSlot else (((centerWhole + relSlot) % count) + count) % count
            val item = items.getOrNull(itemIdx) ?: return@forEach
            val isFocused = if (count <= 2) itemIdx == currentSelectedIdx else relSlot == 0

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(width = labelBoxW, height = labelBoxH)
                    .graphicsLayer {
                        val posVal = continuousPos.value
                        val whole = posVal.roundToInt()
                        val frac = posVal - whole
                        val rawSlot = if (count == 2) {
                            val slotPos = ((posVal % 2f) + 2f) % 2f
                            val d = relSlot - slotPos
                            when {
                                d > 1f -> d - 2f
                                d < -1f -> d + 2f
                                else -> d
                            }
                        } else {
                            relSlot - frac
                        }
                        val centerAngle = baseAngle + rawSlot * stepDeg
                        if (abs(centerAngle - baseAngle) > 80f) {
                            alpha = 0f
                            translationX = -2000f
                            translationY = -2000f
                        } else {
                            alpha = 1f
                            val angleRad = Math.toRadians(centerAngle.toDouble())
                            val r = (wedgeInnerR + if (abs(rawSlot) < 0.45f) activeOuterR else wedgeOuterR) * 0.52f
                            val lx = cx + (r * cos(angleRad)).toFloat()
                            val ly = cy + (r * sin(angleRad)).toFloat()
                            translationX = lx - sideLabelHalfWPx
                            translationY = ly - sideLabelHalfHPx
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.label,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = if (isFocused) 8.sp else 6.8.sp,
                    lineHeight = 9.sp,
                    color = if (isFocused) HudIce else HudSilver,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = item.badge,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 6.3.sp,
                    lineHeight = 8.sp,
                    color = when {
                        isFocused && item.isAccentCrimson -> HudCrimson
                        isFocused -> accentPrimary
                        else -> HudSlate
                    },
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Accessible Center Hub Action Button (Back, Floating/Fullscreen Toggle, or Edge Trigger)
        val centerBtnDp = if (halfOffScreen) {
            with(density) { (wedgeInnerR * 1.76f).toDp() }.coerceIn(58.dp, 84.dp)
        } else {
            with(density) { (wedgeInnerR * 1.48f).toDp() }.coerceIn(44.dp, 56.dp)
        }
        val btnSizePx = with(density) { centerBtnDp.toPx() }
        val clampedCenterX = when {
            halfOffScreen -> cx
            side == DialSide.LEFT -> cx.coerceAtLeast(btnSizePx * 0.54f)
            else -> cx.coerceAtMost(widthPx - btnSizePx * 0.54f)
        }
        val btnBorderColor = if (centerAccentCrimson) HudCrimson else HudCyan
        val btnBgBrush = remember(centerAccentCrimson) {
            Brush.radialGradient(
                colors = if (centerAccentCrimson) {
                    listOf(Color(0xB82A0A14), Color(0xC2080407))
                } else {
                    listOf(Color(0xB80B2536), Color(0xC2050C16))
                }
            )
        }
        val btnContentAlign = when {
            halfOffScreen && side == DialSide.LEFT -> Alignment.CenterEnd
            halfOffScreen -> Alignment.CenterStart
            else -> Alignment.Center
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        (clampedCenterX - btnSizePx / 2f).roundToInt(),
                        (cy - btnSizePx / 2f).roundToInt()
                    )
                }
                .size(centerBtnDp)
                .clip(CircleShape)
                .background(btnBgBrush)
                .border(1.5.dp, btnBorderColor, CircleShape)
                .clickable(onClick = onCenterActionClick)
                .testTag(centerButtonTestTag),
            contentAlignment = btnContentAlign
        ) {
            Column(
                modifier = if (halfOffScreen) {
                    if (side == DialSide.LEFT) Modifier.padding(end = 6.dp) else Modifier.padding(start = 6.dp)
                } else {
                    Modifier
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = centerLabel,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.5.sp,
                    lineHeight = 9.sp,
                    color = HudIce,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                if (!centerSubLabel.isNullOrBlank()) {
                    Text(
                        text = centerSubLabel,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 6.sp,
                        lineHeight = 7.sp,
                        color = btnBorderColor,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        }
    }
}

@Composable
fun ArcReactorRightHalfDial(
    items: List<DialWedgeItem>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onActivateIndex: (Int) -> Unit,
    onCenterBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    showOuterTicks: Boolean = true,
    activateOnlyWhenFocused: Boolean = false
) {
    ArcReactorSideHalfDial(
        side = DialSide.RIGHT,
        items = items,
        selectedIndex = selectedIndex,
        onSelectIndex = onSelectIndex,
        onActivateIndex = onActivateIndex,
        onCenterActionClick = onCenterBackClick,
        modifier = modifier,
        centerLabel = "< BACK",
        centerAccentCrimson = false,
        centerButtonTestTag = "half_dial_center_back",
        showOuterTicks = showOuterTicks,
        showCoreSpinner = showOuterTicks,
        activateOnlyWhenFocused = activateOnlyWhenFocused
    )
}

@Composable
fun ArcReactorLeftRimAccent(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(18.dp)
            .fillMaxHeight()
    ) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val cy = size.height * 0.5f
        val rOuter = min(size.height * 0.46f, size.width * 4.5f).coerceAtLeast(1f)
        val cx = -rOuter * 0.72f
        drawArc(
            color = HudCyan.copy(alpha = 0.55f),
            startAngle = -68f,
            sweepAngle = 136f,
            useCenter = false,
            topLeft = Offset(cx - rOuter, cy - rOuter),
            size = Size(rOuter * 2f, rOuter * 2f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        val rInner = rOuter * 0.90f
        drawArc(
            color = HudCrimson.copy(alpha = 0.65f),
            startAngle = -36f,
            sweepAngle = 72f,
            useCenter = false,
            topLeft = Offset(cx - rInner, cy - rInner),
            size = Size(rInner * 2f, rInner * 2f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun ArcCoreSpinnerOverlay(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val infinite = rememberInfiniteTransition(label = "arc_core_spin")
    val ring1Angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "core_r1"
    )
    val ring2Angle by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "core_r2"
    )

    val boxMod = if (onClick != null) {
        modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    } else {
        modifier
    }

    Box(modifier = boxMod, contentAlignment = Alignment.Center) {
        // Outer counter-rotating broken calibration & crimson/cyan ring
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = ring1Angle * 0.5f }
        ) {
            if (size.minDimension <= 0f) return@Canvas
            val coreR = size.minDimension * 0.47f
            val c = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(c.x - coreR, c.y - coreR)
            val sz = Size(coreR * 2f, coreR * 2f)
            for (seg in 0 until 3) {
                drawArc(
                    color = if (seg == 1) HudCrimson.copy(alpha = 0.85f) else HudCyan.copy(alpha = 0.78f),
                    startAngle = seg * 120f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = sz,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Iconic 3D Precessing Plasma Gyroscope Ellipses + Mark VI Inverted Triangle + Biometric/Iris Core
        val trianglePath = remember { Path() }
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.minDimension <= 0f) return@Canvas
            val c = Offset(size.width / 2f, size.height / 2f)
            val orbR = size.minDimension * 0.37f
            val hazeRadius = (orbR * 1.18f).coerceAtLeast(1f)

            // Inner glowing plasma ambient haze
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        HudIce.copy(alpha = 0.22f),
                        HudCyan.copy(alpha = 0.16f),
                        HudCrimson.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = c,
                    radius = hazeRadius
                ),
                center = c,
                radius = hazeRadius
            )

            // Iron Man 2 Mark VI Inverted-Triangle Palladium Core Frame (slow gyro precession)
            val triR = orbR * 0.82f
            val v1 = Offset(c.x - triR * 0.86f, c.y - triR * 0.48f)
            val v2 = Offset(c.x + triR * 0.86f, c.y - triR * 0.48f)
            val v3 = Offset(c.x, c.y + triR * 0.96f)
            trianglePath.reset()
            trianglePath.moveTo(v1.x, v1.y)
            trianglePath.lineTo(v2.x, v2.y)
            trianglePath.lineTo(v3.x, v3.y)
            trianglePath.close()
            withTransform({
                rotate(degrees = sin(Math.toRadians(ring1Angle.toDouble())).toFloat() * 7f, pivot = c)
            }) {
                drawPath(
                    path = trianglePath,
                    color = HudCyan.copy(alpha = 0.26f),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = trianglePath,
                    color = HudIce.copy(alpha = 0.88f),
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(color = HudCrimson, radius = 2.dp.toPx(), center = v1)
                drawCircle(color = HudAmber, radius = 2.dp.toPx(), center = v2)
                drawCircle(color = HudCyan, radius = 2.2.dp.toPx(), center = v3)
            }

            // 1) Precessing Electric Blue elliptical orbital loop
            withTransform({
                rotate(degrees = ring1Angle * 0.8f + 24f, pivot = c)
                scale(scaleX = 1f, scaleY = 0.72f, pivot = c)
            }) {
                drawCircle(
                    color = HudElectricBlue.copy(alpha = 0.28f),
                    radius = orbR,
                    center = c,
                    style = Stroke(width = 4.5.dp.toPx())
                )
                drawCircle(
                    color = HudCyan.copy(alpha = 0.90f),
                    radius = orbR,
                    center = c,
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }

            // 2) Counter-precessing Crimson Red plasma ellipse
            withTransform({
                rotate(degrees = ring2Angle + 78f, pivot = c)
                scale(scaleX = 0.74f, scaleY = 1f, pivot = c)
            }) {
                drawCircle(
                    color = HudCrimson.copy(alpha = 0.30f),
                    radius = orbR * 0.96f,
                    center = c,
                    style = Stroke(width = 4.5.dp.toPx())
                )
                drawCircle(
                    color = HudCrimson.copy(alpha = 0.95f),
                    radius = orbR * 0.96f,
                    center = c,
                    style = Stroke(width = 1.7.dp.toPx())
                )
            }

            // 3) High-luminance Ice White / Cyan tilted core loop
            withTransform({
                rotate(degrees = ring1Angle * 1.35f - 40f, pivot = c)
                scale(scaleX = 0.96f, scaleY = 0.68f, pivot = c)
            }) {
                drawCircle(
                    color = HudCyan.copy(alpha = 0.26f),
                    radius = orbR * 0.92f,
                    center = c,
                    style = Stroke(width = 4.dp.toPx())
                )
                drawCircle(
                    color = HudIce.copy(alpha = 0.92f),
                    radius = orbR * 0.92f,
                    center = c,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Center glowing Iris / Biometric core reticle
            val coreR = size.minDimension * 0.18f
            drawCircle(
                color = Color(0xCC040A14),
                radius = coreR,
                center = c
            )
            drawCircle(
                color = HudIce.copy(alpha = 0.80f),
                radius = coreR,
                center = c,
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawArc(
                color = HudCyan.copy(alpha = 0.85f),
                startAngle = -140f,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(c.x - coreR * 0.65f, c.y - coreR * 0.65f),
                size = Size(coreR * 1.3f, coreR * 1.3f),
                style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = HudCrimson.copy(alpha = 0.9f),
                startAngle = 30f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(c.x - coreR * 0.36f, c.y - coreR * 0.36f),
                size = Size(coreR * 0.72f, coreR * 0.72f),
                style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
            )
            drawCircle(
                color = HudIce,
                radius = (coreR * 0.15f).coerceAtLeast(2f),
                center = c
            )
        }
    }
}

private fun fillAnnularWedgePath(
    path: Path,
    cx: Float,
    cy: Float,
    innerR: Float,
    outerR: Float,
    startAngleDeg: Float,
    sweepDeg: Float
) {
    val outerRect = Rect(cx - outerR, cy - outerR, cx + outerR, cy + outerR)
    val innerRect = Rect(cx - innerR, cy - innerR, cx + innerR, cy + innerR)
    val endAngleDeg = startAngleDeg + sweepDeg
    val endInnerRad = endAngleDeg * (PI / 180.0)

    path.reset()
    path.arcTo(outerRect, startAngleDeg, sweepDeg, true)
    path.lineTo(
        cx + (innerR * cos(endInnerRad)).toFloat(),
        cy + (innerR * sin(endInnerRad)).toFloat()
    )
    path.arcTo(innerRect, endAngleDeg, -sweepDeg, false)
    path.close()
}
