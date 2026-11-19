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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanSoft
import com.example.ui.theme.HudIce
import com.example.ui.theme.HudSilver
import com.example.ui.theme.HudSlate
import com.example.ui.theme.JetBrainsMonoFontFamily
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
    val bearingAngle = remember(count) { Animatable(-selectedIndex * stepDeg) }
    var isDragging by remember { mutableStateOf(false) }
    var lastSyncedIndex by remember(count) { mutableIntStateOf(selectedIndex) }
    var angularVelocity by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(selectedIndex, count) {
        if (!isDragging && selectedIndex != lastSyncedIndex) {
            lastSyncedIndex = selectedIndex
            val desiredBase = -selectedIndex * stepDeg
            val current = bearingAngle.value
            val deltaMod = (((desiredBase - current) % 360f + 540f) % 360f) - 180f
            bearingAngle.animateTo(
                targetValue = current + deltaMod,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
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
        val wedgeOuterR = radiusPx * 0.85f
        val activeOuterR = radiusPx * 0.90f
        val reusablePath = remember { Path() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(count) {
                    detectTapGestures { pos ->
                        val dx = pos.x - cx
                        val dy = pos.y - cy
                        val dist = hypot(dx, dy)
                        if (dist <= wedgeInnerR) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onActivateIndex(selectedIndex)
                            return@detectTapGestures
                        }
                        if (dist in wedgeInnerR..(activeOuterR * 1.1f)) {
                            val deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            val rel = ((deg - (-90f + bearingAngle.value)) % 360f + 360f) % 360f
                            val tappedIdx = (((rel + stepDeg / 2f) / stepDeg).toInt()) % count
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelectIndex(tappedIdx)
                            onActivateIndex(tappedIdx)
                        }
                    }
                }
                .pointerInput(count) {
                    var lastPointerAngle = 0f
                    detectDragGestures(
                        onDragStart = { start ->
                            isDragging = true
                            angularVelocity = 0f
                            lastPointerAngle = Math.toDegrees(
                                atan2((start.y - cy).toDouble(), (start.x - cx).toDouble())
                            ).toFloat()
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val curAngle = Math.toDegrees(
                                atan2((change.position.y - cy).toDouble(), (change.position.x - cx).toDouble())
                            ).toFloat()
                            var delta = curAngle - lastPointerAngle
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f
                            lastPointerAngle = curAngle
                            angularVelocity = angularVelocity * 0.45f + delta * 0.55f

                            scope.launch {
                                val nextAngle = bearingAngle.value + delta
                                bearingAngle.snapTo(nextAngle)
                                val detentSlot = (-nextAngle / stepDeg).roundToInt()
                                val normalizedIdx = ((detentSlot % count) + count) % count
                                if (normalizedIdx != lastSyncedIndex) {
                                    lastSyncedIndex = normalizedIdx
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectIndex(normalizedIdx)
                                }
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            scope.launch {
                                val projected = bearingAngle.value + (angularVelocity * 3.4f)
                                    .coerceIn(-stepDeg * 1.8f, stepDeg * 1.8f)
                                val nearestDetentSlot = (-projected / stepDeg).roundToInt()
                                val targetAngle = -nearestDetentSlot * stepDeg
                                val normalizedIdx = ((nearestDetentSlot % count) + count) % count
                                if (normalizedIdx != lastSyncedIndex) {
                                    lastSyncedIndex = normalizedIdx
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectIndex(normalizedIdx)
                                }
                                bearingAngle.animateTo(
                                    targetValue = targetAngle,
                                    animationSpec = spring(
                                        dampingRatio = 0.72f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            scope.launch {
                                val nearestDetentSlot = (-bearingAngle.value / stepDeg).roundToInt()
                                bearingAngle.animateTo(
                                    targetValue = -nearestDetentSlot * stepDeg,
                                    animationSpec = spring(
                                        dampingRatio = 0.8f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    )
                }
        ) {
            val currentAngle = bearingAngle.value
            val tickOuter = radiusPx * 0.99f
            val tickInnerMinor = radiusPx * 0.955f
            val tickInnerMajor = radiusPx * 0.93f
            val tickPhase = currentAngle * 0.25f
            for (t in 0 until 72) {
                val a = Math.toRadians((t * 5.0) - 90.0 + tickPhase)
                val major = t % 6 == 0
                val rIn = if (major) tickInnerMajor else tickInnerMinor
                drawLine(
                    color = if (major) HudCyan.copy(alpha = 0.72f) else HudCyan.copy(alpha = 0.25f),
                    start = Offset(cx + (rIn * cos(a)).toFloat(), cy + (rIn * sin(a)).toFloat()),
                    end = Offset(cx + (tickOuter * cos(a)).toFloat(), cy + (tickOuter * sin(a)).toFloat()),
                    strokeWidth = if (major) 2f else 1f
                )
            }

            drawCircle(
                color = HudCyan.copy(alpha = 0.26f),
                radius = radiusPx * 0.92f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )

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
                    isSelected && item.isAccentCrimson -> HudCrimson.copy(alpha = 0.24f)
                    isSelected -> HudCyan.copy(alpha = 0.22f)
                    else -> Color(0x8807121E)
                }
                val strokeColor = when {
                    isSelected && item.isAccentCrimson -> HudCrimson
                    isSelected -> HudIce
                    item.isAccentCrimson -> HudCrimson.copy(alpha = 0.45f)
                    else -> HudCyan.copy(alpha = 0.34f)
                }

                drawPath(path = reusablePath, color = fillColor)
                drawPath(
                    path = reusablePath,
                    color = strokeColor,
                    style = Stroke(width = if (isSelected) 2.dp.toPx() else 1.dp.toPx())
                )
            }

            val tipY = cy - activeOuterR - 2.dp.toPx()
            reusablePath.reset()
            reusablePath.moveTo(cx, tipY + 6.dp.toPx())
            reusablePath.lineTo(cx - 5.dp.toPx(), tipY - 3.dp.toPx())
            reusablePath.lineTo(cx + 5.dp.toPx(), tipY - 3.dp.toPx())
            reusablePath.close()
            drawPath(reusablePath, color = HudCrimson)
        }

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

        // Labels read bearingAngle inside offset{} (layout phase) so Text nodes never recompose on drag frames
        val labelBoxW = (radiusPx * 0.48f / density.density).dp.coerceIn(38.dp, 62.dp)
        val labelBoxH = 24.dp
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
                    .offset {
                        val angleDeg = -90f + index * stepDeg + bearingAngle.value
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val lx = cx + (labelR * cos(angleRad)).toFloat()
                        val ly = cy + (labelR * sin(angleRad)).toFloat()
                        val w = labelBoxW.roundToPx()
                        val h = labelBoxH.roundToPx()
                        IntOffset((lx - w / 2f).roundToInt(), (ly - h / 2f).roundToInt())
                    }
                    .size(width = labelBoxW, height = labelBoxH),
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
    showCoreSpinner: Boolean = false
) {
    if (items.isEmpty()) return
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val count = items.size
    val maxSlots = min(if (showOuterTicks) 7 else 5, count)
    val stepDeg = 148f / maxSlots.coerceAtLeast(3)

    val continuousPos = remember(count) { Animatable(selectedIndex.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    var lastSyncedIndex by remember(count) { mutableIntStateOf(selectedIndex) }
    var velocitySlots by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(selectedIndex, count) {
        if (!isDragging && selectedIndex != lastSyncedIndex) {
            lastSyncedIndex = selectedIndex
            val cur = continuousPos.value
            val curMod = ((cur.roundToInt() % count) + count) % count
            var diff = selectedIndex - curMod
            if (diff > count / 2) diff -= count
            if (diff < -count / 2) diff += count
            val target = cur.roundToInt() + diff.toFloat()
            continuousPos.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    // Only recompose label slots when the integer snapped center changes (NOT every frame!)
    val centerWhole by remember(count) {
        derivedStateOf { continuousPos.value.roundToInt() }
    }
    val halfSpan = (maxSlots / 2) + 1
    val slotOffsets = remember(halfSpan) { (-halfSpan..halfSpan).toList() }

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val baseAngle = if (side == DialSide.LEFT) 0f else 180f
        val cx = if (side == DialSide.LEFT) widthPx * 0.16f else widthPx * 0.84f
        val cy = heightPx * 0.50f
        val radiusPx = min(heightPx * 0.45f, widthPx * 0.78f).coerceAtLeast(82f)
        val wedgeInnerR = radiusPx * 0.41f
        val wedgeOuterR = radiusPx * 0.87f
        val activeOuterR = radiusPx * 0.94f
        val reusablePath = remember { Path() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(count, side) {
                    var lastAngle = 0f
                    detectDragGestures(
                        onDragStart = { start ->
                            isDragging = true
                            velocitySlots = 0f
                            lastAngle = Math.toDegrees(
                                atan2((start.y - cy).toDouble(), (start.x - cx).toDouble())
                            ).toFloat()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val curAngle = Math.toDegrees(
                                atan2((change.position.y - cy).toDouble(), (change.position.x - cx).toDouble())
                            ).toFloat()
                            var degDelta = curAngle - lastAngle
                            if (degDelta > 180f) degDelta -= 360f
                            if (degDelta < -180f) degDelta += 360f
                            lastAngle = curAngle

                            val ySign = if (side == DialSide.LEFT) 1f else -1f
                            val slotDelta = (-degDelta / stepDeg) * 0.72f +
                                (ySign * dragAmount.y / 56f) * 0.28f
                            velocitySlots = velocitySlots * 0.45f + slotDelta * 0.55f

                            scope.launch {
                                val nextPos = continuousPos.value + slotDelta
                                continuousPos.snapTo(nextPos)
                                val idx = ((nextPos.roundToInt() % count) + count) % count
                                if (idx != lastSyncedIndex) {
                                    lastSyncedIndex = idx
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectIndex(idx)
                                }
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            scope.launch {
                                val projected = continuousPos.value +
                                    (velocitySlots * 2.4f).coerceIn(-2.4f, 2.4f)
                                val targetSlot = projected.roundToInt().toFloat()
                                val idx = ((targetSlot.roundToInt() % count) + count) % count
                                if (idx != lastSyncedIndex) {
                                    lastSyncedIndex = idx
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectIndex(idx)
                                }
                                continuousPos.animateTo(
                                    targetValue = targetSlot,
                                    animationSpec = spring(
                                        dampingRatio = 0.78f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            scope.launch {
                                continuousPos.animateTo(
                                    targetValue = continuousPos.value.roundToInt().toFloat(),
                                    animationSpec = spring(
                                        dampingRatio = 0.8f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    )
                }
                .pointerInput(count, selectedIndex, side) {
                    detectTapGestures { pos ->
                        val dx = pos.x - cx
                        val dy = pos.y - cy
                        val dist = hypot(dx, dy)
                        if (dist <= wedgeInnerR) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCenterActionClick()
                            return@detectTapGestures
                        }
                        if (dist in wedgeInnerR..(activeOuterR * 1.15f)) {
                            var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (side == DialSide.RIGHT && deg < 0f) {
                                deg += 360f
                            }
                            val slotFromCenter = (deg - baseAngle) / stepDeg
                            val targetWhole = (continuousPos.value + slotFromCenter).roundToInt()
                            val tappedIdx = ((targetWhole % count) + count) % count
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelectIndex(tappedIdx)
                            onActivateIndex(tappedIdx)
                        }
                    }
                }
        ) {
            val posVal = continuousPos.value
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
                            isCenter -> HudCrimson.copy(alpha = 0.9f)
                            major -> HudCyan.copy(alpha = 0.65f)
                            else -> HudCyan.copy(alpha = 0.22f)
                        },
                        start = Offset(cx + (rIn * cos(rad)).toFloat(), cy + (rIn * sin(rad)).toFloat()),
                        end = Offset(cx + (rOut * cos(rad)).toFloat(), cy + (rOut * sin(rad)).toFloat()),
                        strokeWidth = if (major) 2f else 1f
                    )
                }
            } else {
                // Lightweight clean perimeter arc for ultra-smooth 60fps Apps cockpit
                drawArc(
                    color = HudCyan.copy(alpha = 0.28f),
                    startAngle = baseAngle - 78f,
                    sweepAngle = 156f,
                    useCenter = false,
                    topLeft = Offset(cx - activeOuterR - 2.dp.toPx(), cy - activeOuterR - 2.dp.toPx()),
                    size = Size((activeOuterR + 2.dp.toPx()) * 2f, (activeOuterR + 2.dp.toPx()) * 2f),
                    style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            val gapDeg = 4.2f
            for (relSlot in -halfSpan..halfSpan) {
                val rawSlot = relSlot - frac
                val centerAngle = baseAngle + rawSlot * stepDeg
                val distFromCenter = abs(centerAngle - baseAngle)
                if (distFromCenter > 84f) continue

                val itemIdx = (((whole + relSlot) % count) + count) % count
                val item = items[itemIdx]
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
                    isFocused && item.isAccentCrimson -> HudCrimson.copy(alpha = 0.26f)
                    isFocused -> HudCyan.copy(alpha = 0.24f)
                    else -> Color(0x9906101B).copy(alpha = 0.6f * edgeFade)
                }
                val stroke = when {
                    isFocused && item.isAccentCrimson -> HudCrimson
                    isFocused -> HudIce
                    item.isAccentCrimson -> HudCrimson.copy(alpha = 0.52f * edgeFade)
                    else -> HudCyan.copy(alpha = 0.34f * edgeFade)
                }
                drawPath(path = reusablePath, color = fill)
                drawPath(
                    path = reusablePath,
                    color = stroke,
                    style = Stroke(width = if (isFocused) 2.dp.toPx() else 1.dp.toPx())
                )
            }

            // Static crisp inner reactor ring when spinner is disabled (zero animation overhead)
            if (!showCoreSpinner) {
                drawCircle(
                    color = (if (centerAccentCrimson) HudCrimson else HudCyan).copy(alpha = 0.45f),
                    radius = wedgeInnerR * 0.88f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Crimson focus pointer triangle
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
            drawPath(reusablePath, color = HudCrimson)
        }

        if (showCoreSpinner) {
            val coreDiameterDp = with(density) { (wedgeInnerR * 1.72f).toDp() }
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

        // Labels only recompose when integer centerWhole changes; continuousPos.value is read ONLY in offset{}
        val labelBoxW = (radiusPx * 0.44f / density.density).dp.coerceIn(40.dp, 64.dp)
        val labelBoxH = 22.dp
        slotOffsets.forEach { relSlot ->
            val itemIdx = (((centerWhole + relSlot) % count) + count) % count
            val item = items[itemIdx]
            val isFocused = relSlot == 0

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        val posVal = continuousPos.value
                        val whole = posVal.roundToInt()
                        val frac = posVal - whole
                        val rawSlot = relSlot - frac
                        val centerAngle = baseAngle + rawSlot * stepDeg
                        if (abs(centerAngle - baseAngle) > 80f) {
                            return@offset IntOffset(-2000, -2000)
                        }
                        val angleRad = Math.toRadians(centerAngle.toDouble())
                        val r = (wedgeInnerR + if (abs(rawSlot) < 0.45f) activeOuterR else wedgeOuterR) * 0.51f
                        val lx = cx + (r * cos(angleRad)).toFloat()
                        val ly = cy + (r * sin(angleRad)).toFloat()
                        val w = labelBoxW.roundToPx()
                        val h = labelBoxH.roundToPx()
                        IntOffset((lx - w / 2f).roundToInt(), (ly - h / 2f).roundToInt())
                    }
                    .size(width = labelBoxW, height = labelBoxH),
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
                    color = if (isFocused) HudCyan else HudSlate,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Accessible Center Hub Action Button (Back or Floating/Fullscreen Toggle)
        val centerBtnDp = if (widthPx < with(density) { 128.dp.toPx() }) 44.dp else 52.dp
        val btnSizePx = with(density) { centerBtnDp.toPx() }
        val clampedCenterX = if (side == DialSide.LEFT) {
            cx.coerceAtLeast(btnSizePx * 0.54f)
        } else {
            cx.coerceAtMost(widthPx - btnSizePx * 0.54f)
        }
        val btnBorderColor = if (centerAccentCrimson) HudCrimson else HudCyan
        val btnBgBrush = remember(centerAccentCrimson) {
            Brush.radialGradient(
                colors = if (centerAccentCrimson) {
                    listOf(Color(0xEE2A0A14), Color(0xEE0B0508))
                } else {
                    listOf(Color(0xEE0B2536), Color(0xEE050C16))
                }
            )
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
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

@Composable
fun ArcReactorRightHalfDial(
    items: List<DialWedgeItem>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onActivateIndex: (Int) -> Unit,
    onCenterBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    showOuterTicks: Boolean = true
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
        showCoreSpinner = showOuterTicks
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
        val cy = size.height * 0.5f
        val rOuter = min(size.height * 0.46f, size.width * 4.5f)
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
private fun ArcCoreSpinnerOverlay(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "arc_core_spin")
    val ring1Angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "core_r1"
    )
    val ring2Angle by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 17000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "core_r2"
    )

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = ring1Angle }
        ) {
            val coreR = size.minDimension * 0.48f
            val c = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(c.x - coreR, c.y - coreR)
            val sz = Size(coreR * 2f, coreR * 2f)
            for (seg in 0 until 3) {
                drawArc(
                    color = HudCyan.copy(alpha = 0.85f),
                    startAngle = seg * 120f,
                    sweepAngle = 78f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = sz,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = ring2Angle }
        ) {
            val r2 = size.minDimension * 0.37f
            val c = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(c.x - r2, c.y - r2)
            val sz = Size(r2 * 2f, r2 * 2f)
            for (seg in 0 until 4) {
                drawArc(
                    color = if (seg == 0) HudCrimson.copy(alpha = 0.9f) else HudCyanSoft.copy(alpha = 0.6f),
                    startAngle = seg * 90f,
                    sweepAngle = 55f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = sz,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            val r3 = size.minDimension * 0.27f
            drawCircle(
                color = HudIce.copy(alpha = 0.72f),
                radius = r3,
                center = c,
                style = Stroke(width = 1.5.dp.toPx())
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
