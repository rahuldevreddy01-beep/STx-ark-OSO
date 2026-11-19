package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanBorder
import com.example.ui.theme.HudElectricBlue
import com.example.ui.theme.ObsidianGlass
import com.example.ui.theme.ObsidianVoid

@Composable
fun StarkObsidianBackground(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        // Deep jet-black atmosphere with dual Red + Electric Blue sci-fi reactor glow
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF071326),
                    Color(0xFF03070F),
                    ObsidianVoid
                ),
                center = Offset(size.width * 0.35f, size.height * 0.38f),
                radius = size.maxDimension * 0.72f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x22FF1F42),
                    Color(0x0AFF1F42),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.76f, size.height * 0.62f),
                radius = size.maxDimension * 0.56f
            )
        )

        // Subtle optical concentric sci-fi calibration rings
        val cx = size.width * 0.5f
        val cy = size.height * 0.48f
        val baseR = size.minDimension * 0.42f
        drawCircle(
            color = HudElectricBlue.copy(alpha = 0.08f),
            radius = baseR,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = HudCrimson.copy(alpha = 0.07f),
            radius = baseR * 0.74f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx())
        )

        // Faint precision scanlines
        val stepPx = 7.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color(0x0700EAFF),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += stepPx
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
        val edgeDepth = 42.dp.toPx()
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
    contentPadding: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(ObsidianGlass)
            .border(1.dp, borderColor, shape)
    ) {
        // Signature dual Red + Electric Blue sci-fi HUD corner brackets
        Canvas(modifier = Modifier.matchParentSize()) {
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
