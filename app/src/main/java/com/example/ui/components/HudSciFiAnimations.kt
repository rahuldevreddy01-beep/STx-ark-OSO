package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudElectricBlue
import com.example.ui.theme.HudIce
import com.example.ui.theme.ObsidianVoid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Staggered GPU-only sci-fi entrance animation wrapper.
 * Uses graphicsLayer so child Composables never recompose during enter animation.
 */
@Composable
fun StxEntrancePanel(
    visibleKey: Any?,
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    slideFromX: Float = 0f,
    slideFromY: Float = 24f,
    initialScale: Float = 0.94f,
    content: @Composable () -> Unit
) {
    val progress = remember(visibleKey) { Animatable(0f) }

    LaunchedEffect(visibleKey) {
        progress.snapTo(0f)
        if (delayMs > 0) delay(delayMs.toLong())
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            val p = progress.value.coerceIn(0f, 1f)
            val inv = 1f - p
            alpha = p
            val sc = initialScale + (1f - initialScale) * p
            scaleX = sc
            scaleY = sc
            translationX = slideFromX * density * inv
            translationY = slideFromY * density * inv
        }
    ) {
        content()
    }
}

/**
 * Brief Sci-Fi Arc Reactor Boot / Unlock Pulse Overlay.
 * Plays on app open or unlock like the glowing core burst in holographic hardware UIs.
 */
@Composable
fun ArcReactorBootPulse(
    triggerKey: Any?,
    modifier: Modifier = Modifier
) {
    var visible by remember(triggerKey) { mutableStateOf(true) }
    val sweepProgress = remember(triggerKey) { Animatable(0f) }

    LaunchedEffect(triggerKey) {
        visible = true
        sweepProgress.snapTo(0f)
        launch {
            sweepProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
        }
        delay(430L)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(80)) + scaleIn(initialScale = 0.65f, animationSpec = tween(180)),
        exit = fadeOut(tween(180)) + scaleOut(targetScale = 1.18f, animationSpec = tween(180)),
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianVoid.copy(alpha = 0.35f))
        ) {
            val p = sweepProgress.value
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseR = size.minDimension * (0.12f + p * 0.38f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        HudCyan.copy(alpha = 0.24f * (1f - p)),
                        HudCrimson.copy(alpha = 0.16f * (1f - p)),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = (baseR * 1.4f).coerceAtLeast(10f)
                ),
                center = Offset(cx, cy),
                radius = baseR * 1.4f
            )

            val outerRectSize = Size(baseR * 2f, baseR * 2f)
            val outerTopLeft = Offset(cx - baseR, cy - baseR)
            val rot = p * 180f
            for (seg in 0 until 3) {
                drawArc(
                    color = if (seg == 0) HudCrimson else HudCyan,
                    startAngle = rot + seg * 120f,
                    sweepAngle = 72f,
                    useCenter = false,
                    topLeft = outerTopLeft,
                    size = outerRectSize,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            val innerR = baseR * 0.64f
            drawCircle(
                color = HudElectricBlue.copy(alpha = 0.85f * (1f - p * 0.6f)),
                radius = innerR,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = HudIce.copy(alpha = (1f - p).coerceIn(0f, 1f)),
                radius = (baseR * 0.18f).coerceAtLeast(4f),
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * Lightweight animated Red + Electric Blue Sci-Fi Pulse Strip for HUD headers/stages.
 */
@Composable
fun SciFiDualPulseBeam(
    isAlertCrimson: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "scifi_beam")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beam_phase"
    )

    Canvas(modifier = modifier) {
        val h = size.height
        val w = size.width
        drawLine(
            color = HudElectricBlue.copy(alpha = 0.28f),
            start = Offset(0f, h / 2f),
            end = Offset(w, h / 2f),
            strokeWidth = 1.dp.toPx()
        )
        val segW = w * 0.24f
        val x1 = (phase * (w + segW)) - segW
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, HudCyan, HudIce, HudCrimson, Color.Transparent),
                startX = x1,
                endX = x1 + segW
            ),
            topLeft = Offset(x1.coerceIn(0f, w), 0f),
            size = Size(segW.coerceAtMost(w), h)
        )
        if (isAlertCrimson) {
            drawCircle(
                color = HudCrimson,
                radius = (h * 0.48f).coerceAtLeast(2f),
                center = Offset(w * 0.5f, h / 2f)
            )
        }
    }
}
