package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.HudElectricBlue
import com.example.ui.theme.ObsidianGlass
import com.example.ui.theme.ObsidianVoid

@Composable
fun StarkObsidianBackground(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "stark_bg_scan")
    val sweepYFraction by infinite.animateFloat(
        initialValue = -0.15f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_scanline_sweep"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val maxDim = size.maxDimension
                val minDim = size.minDimension
                if (w <= 0f || h <= 0f || maxDim <= 0f) {
                    return@drawWithCache onDrawBehind { }
                }

                val blueGlow = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF081930),
                        Color(0xFF030812),
                        ObsidianVoid
                    ),
                    center = Offset(w * 0.44f, h * 0.44f),
                    radius = (maxDim * 0.72f).coerceAtLeast(1f)
                )
                val crimsonGlow = Brush.radialGradient(
                    colors = listOf(
                        Color(0x28FF1F42),
                        Color(0x0CFF1F42),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.74f, h * 0.62f),
                    radius = (maxDim * 0.56f).coerceAtLeast(1f)
                )
                val amberEdgeGlow = Brush.radialGradient(
                    colors = listOf(
                        Color(0x10FFB020),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.16f, h * 0.22f),
                    radius = (maxDim * 0.42f).coerceAtLeast(1f)
                )

                val dotStep = 18.dp.toPx().coerceAtLeast(4f)
                val cols = (w / dotStep).toInt() + 1
                val rows = (h / dotStep).toInt() + 1
                val cachedDots: List<Offset> = if (cols * rows in 1..4500) {
                    ArrayList<Offset>(cols * rows).apply {
                        var yPos = dotStep * 0.5f
                        while (yPos < h) {
                            var xPos = dotStep * 0.5f
                            while (xPos < w) {
                                add(Offset(xPos, yPos))
                                xPos += dotStep
                            }
                            yPos += dotStep
                        }
                    }
                } else {
                    emptyList()
                }
                val dotStroke = 1.4.dp.toPx()

                val cx = w * 0.5f
                val cy = h * 0.48f
                val baseR = minDim * 0.43f
                val ringStroke = 1.dp.toPx()
                val stepPx = 7.dp.toPx().coerceAtLeast(2f)

                onDrawBehind {
                    drawRect(brush = blueGlow)
                    drawRect(brush = crimsonGlow)
                    drawRect(brush = amberEdgeGlow)

                    if (cachedDots.isNotEmpty()) {
                        drawPoints(
                            points = cachedDots,
                            pointMode = PointMode.Points,
                            color = HudElectricBlue.copy(alpha = 0.14f),
                            strokeWidth = dotStroke,
                            cap = StrokeCap.Round
                        )
                    }

                    // Optical concentric Stark HUD calibration rings
                    drawCircle(
                        color = HudElectricBlue.copy(alpha = 0.09f),
                        radius = baseR,
                        center = Offset(cx, cy),
                        style = Stroke(width = ringStroke)
                    )
                    drawCircle(
                        color = HudCrimson.copy(alpha = 0.08f),
                        radius = baseR * 0.74f,
                        center = Offset(cx, cy),
                        style = Stroke(width = ringStroke)
                    )

                    // Cached tactical scanlines
                    var y = 0f
                    while (y < h) {
                        drawLine(
                            color = Color(0x0600EAFF),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                        y += stepPx
                    }
                }
            }
    ) {
        // Lightweight animated MK-VI holographic telemetry sweep curtain
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val beamHeight = 64.dp.toPx().coerceAtLeast(2f)
            val centerY = size.height * sweepYFraction
            val topY = centerY - beamHeight * 0.5f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        HudCyan.copy(alpha = 0.045f),
                        Color.Transparent
                    ),
                    startY = topY,
                    endY = topY + beamHeight
                ),
                topLeft = Offset(0f, topY),
                size = Size(size.width, beamHeight)
            )
            if (centerY in 0f..size.height) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            HudCyan.copy(alpha = 0.16f),
                            HudElectricBlue.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

/**
 * Covers ALL 4 outer screen edges (top, bottom, left, right) cleanly over the entire window.
 */
@Composable
fun EdgeFlashOverlay(
    active: Boolean,
    crimsonMode: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "edge_flash_alpha"
    )
    if (alpha <= 0.01f) return

    val coreColor = if (crimsonMode) HudCrimson else Color(0xFFE6FCFF)
    val glowColor = if (crimsonMode) Color(0xCCFF1F42) else HudCyan

    Canvas(modifier = modifier.fillMaxSize()) {
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        val edgeDepth = 42.dp.toPx().coerceIn(2f, minOf(size.width, size.height))
        val strokeW = 4.dp.toPx()

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(glowColor.copy(alpha = 0.65f * alpha), Color.Transparent),
                startY = 0f,
                endY = edgeDepth
            ),
            size = Size(size.width, edgeDepth)
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, glowColor.copy(alpha = 0.65f * alpha)),
                startY = size.height - edgeDepth,
                endY = size.height
            ),
            topLeft = Offset(0f, size.height - edgeDepth),
            size = Size(size.width, edgeDepth)
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(glowColor.copy(alpha = 0.65f * alpha), Color.Transparent),
                startX = 0f,
                endX = edgeDepth
            ),
            size = Size(edgeDepth, size.height)
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, glowColor.copy(alpha = 0.65f * alpha)),
                startX = size.width - edgeDepth,
                endX = size.width
            ),
            topLeft = Offset(size.width - edgeDepth, 0f),
            size = Size(edgeDepth, size.height)
        )

        drawRect(
            color = coreColor.copy(alpha = 0.9f * alpha),
            topLeft = Offset(strokeW / 2f, strokeW / 2f),
            size = Size(size.width - strokeW, size.height - strokeW),
            style = Stroke(width = strokeW)
        )
    }
}

@Composable
fun HudBracketCard(
    modifier: Modifier = Modifier,
    borderColor: Color = HudCyanBorder,
    accentCornerColor: Color = HudCyan,
    bottomLeftAccent: Color = HudCrimson,
    containerColor: Color = ObsidianGlass,
    contentPadding: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
    ) {
        // Signature Stark smoked-glass sheen + dual Crimson & Electric Cyan HUD corner brackets
        Canvas(modifier = Modifier.matchParentSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentCornerColor.copy(alpha = 0.08f),
                        Color.Transparent,
                        bottomLeftAccent.copy(alpha = 0.04f)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
            // Subtle top specular highlight hairline
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accentCornerColor.copy(alpha = 0.35f),
                        HudAmber.copy(alpha = 0.14f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, 1.dp.toPx()),
                end = Offset(size.width * 0.72f, 1.dp.toPx()),
                strokeWidth = 0.8.dp.toPx()
            )
            val len = 12.dp.toPx()
            val stroke = 2.dp.toPx()
            // Top-left (Electric Cyan/Blue)
            drawLine(accentCornerColor, Offset(0f, stroke / 2), Offset(len, stroke / 2), stroke, StrokeCap.Square)
            drawLine(accentCornerColor, Offset(stroke / 2, 0f), Offset(stroke / 2, len), stroke, StrokeCap.Square)
            // Top-right (Crimson Red accent balance)
            drawLine(bottomLeftAccent, Offset(size.width - len, stroke / 2), Offset(size.width, stroke / 2), stroke, StrokeCap.Square)
            drawLine(bottomLeftAccent, Offset(size.width - stroke / 2, 0f), Offset(size.width - stroke / 2, len), stroke, StrokeCap.Square)
            // Bottom-left (Crimson Red)
            drawLine(bottomLeftAccent, Offset(0f, size.height - stroke / 2), Offset(len, size.height - stroke / 2), stroke, StrokeCap.Square)
            drawLine(bottomLeftAccent, Offset(stroke / 2, size.height - len), Offset(stroke / 2, size.height), stroke, StrokeCap.Square)
            // Bottom-right (Electric Cyan/Blue)
            drawLine(accentCornerColor, Offset(size.width - len, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke, StrokeCap.Square)
            drawLine(accentCornerColor, Offset(size.width - stroke / 2, size.height - len), Offset(size.width - stroke / 2, size.height), stroke, StrokeCap.Square)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            content = content
        )
    }
}
