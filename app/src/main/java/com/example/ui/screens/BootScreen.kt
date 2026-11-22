package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LauncherPreferences
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudElectricBlue
import com.example.ui.theme.HudIce
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MichromaFontFamily
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.StarkGold
import com.example.ui.theme.StarkGoldBright
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val SmoothBootEasing = CubicBezierEasing(0.24f, 0.46f, 0.38f, 1.0f)

/**
 * Minimal, pure-black STx-ARK OSO boot screen with a smooth, unhurried 0% -> 100%
 * animated boot curve, clean cyan + Stark gold Arc Reactor centerpiece, and restrained typography.
 */
@Composable
fun BootScreen(
    preferences: LauncherPreferences,
    onBootComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnComplete by rememberUpdatedState(onBootComplete)
    val progressAnim = remember { Animatable(0f) }
    val entranceAlpha = remember { Animatable(0f) }
    val reactorState = remember { BootReactorState() }
    var frameTick by remember { mutableLongStateOf(0L) }
    var skipTriggered by remember { mutableStateOf(false) }
    var showSystemOnline by remember { mutableStateOf(false) }

    val progressPct by remember {
        derivedStateOf { progressAnim.value.roundToInt().coerceIn(0, 100) }
    }

    val statusText by remember {
        derivedStateOf {
            when {
                progressPct < 20 -> "Initializing core..."
                progressPct < 45 -> "Calibrating dual arc..."
                progressPct < 72 -> "Syncing telemetry..."
                progressPct < 96 -> "Finalizing interface..."
                else -> "System ready."
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "smoothBootShimmer")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "barShimmer"
    )

    LaunchedEffect(Unit) {
        entranceAlpha.animateTo(1f, animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(skipTriggered) {
        if (skipTriggered) {
            progressAnim.animateTo(
                targetValue = 100f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
            showSystemOnline = true
            delay(280L)
            latestOnComplete()
        } else {
            progressAnim.snapTo(0f)
            // Single continuous slow, silky 0 -> 100 progression with no choppy pauses
            progressAnim.animateTo(
                targetValue = 100f,
                animationSpec = tween(durationMillis = 6200, easing = SmoothBootEasing)
            )
            showSystemOnline = true
            delay(450L)
            latestOnComplete()
        }
    }

    LaunchedEffect(preferences.orbAutoRotate, preferences.orbRotationSpeed) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastNanos == 0L) lastNanos = now
                val dt = ((now - lastNanos) / 1e9f).coerceIn(0f, 0.05f)
                lastNanos = now
                reactorState.targetEnergy = (progressAnim.value / 100f).coerceIn(0f, 1f)
                reactorState.step(
                    dt = dt,
                    speedMult = preferences.orbRotationSpeed,
                    autoRotate = preferences.orbAutoRotate
                )
                frameTick = now
            }
        }
    }

    BackHandler(enabled = true) {
        if (!skipTriggered) skipTriggered = true
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .testTag("first_boot_overlay")
    ) {
        val isLandscape = maxWidth > maxHeight || maxHeight < 580.dp
        val contentAlpha = entranceAlpha.value * (if (showSystemOnline) 0.12f else 1f)

        if (isLandscape) {
            MinimalLandscapeBootLayout(
                preferences = preferences,
                reactorState = reactorState,
                frameTick = frameTick,
                progressPct = progressPct,
                progressFraction = (progressAnim.value / 100f).coerceIn(0f, 1f),
                shimmerPhase = shimmerPhase,
                statusText = statusText,
                contentAlpha = contentAlpha,
                onSkipClick = {
                    if (progressPct >= 100) latestOnComplete() else skipTriggered = true
                }
            )
        } else {
            MinimalPortraitBootLayout(
                preferences = preferences,
                reactorState = reactorState,
                frameTick = frameTick,
                progressPct = progressPct,
                progressFraction = (progressAnim.value / 100f).coerceIn(0f, 1f),
                shimmerPhase = shimmerPhase,
                statusText = statusText,
                contentAlpha = contentAlpha,
                maxHeight = maxHeight,
                onSkipClick = {
                    if (progressPct >= 100) latestOnComplete() else skipTriggered = true
                }
            )
        }

        AnimatedVisibility(
            visible = showSystemOnline,
            enter = fadeIn(tween(320)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD8000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "SYSTEM ONLINE",
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 22.sp,
                        letterSpacing = 5.sp,
                        color = Color(0xFFC8FBFF),
                        style = TextStyle(
                            shadow = Shadow(
                                color = HudCyan.copy(alpha = 0.70f),
                                blurRadius = 16f
                            )
                        ),
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, StarkGold, HudCyan, Color.Transparent)
                                )
                            )
                    )
                    Text(
                        text = "STx-ARK OSO // READY",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        letterSpacing = 3.sp,
                        color = StarkGoldBright.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimalPortraitBootLayout(
    preferences: LauncherPreferences,
    reactorState: BootReactorState,
    frameTick: Long,
    progressPct: Int,
    progressFraction: Float,
    shimmerPhase: Float,
    statusText: String,
    contentAlpha: Float,
    maxHeight: Dp,
    onSkipClick: () -> Unit
) {
    val compact = maxHeight < 680.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .alpha(contentAlpha)
            .padding(horizontal = 28.dp, vertical = if (compact) 16.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.32f))

        // Clean centered Arc Reactor floating on pure black void
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.35f)
                .testTag("stx_html_orb_view"),
            contentAlignment = Alignment.Center
        ) {
            BootReactorCanvas(
                state = reactorState,
                frameTick = frameTick,
                scaleMult = preferences.orbScale,
                bloomMult = preferences.orbBloomStrength,
                speedMult = preferences.orbRotationSpeed,
                autoRotate = preferences.orbAutoRotate,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(if (compact) 12.dp else 20.dp))

        // Thin, airy brand header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "STx-ARK OSO",
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = if (compact) 22.sp else 26.sp,
                letterSpacing = 3.2.sp,
                color = HudIce,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = HudCyan.copy(alpha = 0.45f),
                        blurRadius = 14f
                    )
                ),
                modifier = Modifier.testTag("first_boot_title")
            )

            Text(
                text = "POWERED BY STx",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 9.5.sp,
                letterSpacing = 2.8.sp,
                color = StarkGoldBright.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("first_boot_powered_by")
            )
        }

        Spacer(modifier = Modifier.height(if (compact) 22.dp else 30.dp))

        // Smooth animated percentage + minimal hatched chamfered progress bar + quiet single status line
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MinimalPercentReadout(
                progressPct = progressPct,
                fontSize = if (compact) 26.sp else 30.sp,
                modifier = Modifier.testTag("first_boot_progress_text")
            )

            Spacer(modifier = Modifier.height(14.dp))

            SmoothChamferedProgressBar(
                progressFraction = progressFraction,
                shimmerPhase = shimmerPhase,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedContent(
                targetState = statusText,
                transitionSpec = {
                    fadeIn(tween(180, delayMillis = 90)) togetherWith fadeOut(tween(90))
                },
                label = "bootStatusCrossfade"
            ) { text ->
                Text(
                    text = text,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.1.sp,
                    color = Color(0xFF7D8996),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.55f))

        // Quiet minimal bottom action pill
        MinimalSkipAction(
            isReady = progressPct >= 100,
            onClick = onSkipClick
        )
    }
}

@Composable
private fun MinimalLandscapeBootLayout(
    preferences: LauncherPreferences,
    reactorState: BootReactorState,
    frameTick: Long,
    progressPct: Int,
    progressFraction: Float,
    shimmerPhase: Float,
    statusText: String,
    contentAlpha: Float,
    onSkipClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .alpha(contentAlpha)
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(0.95f)
                .fillMaxHeight()
                .testTag("stx_html_orb_view"),
            contentAlignment = Alignment.Center
        ) {
            BootReactorCanvas(
                state = reactorState,
                frameTick = frameTick,
                scaleMult = preferences.orbScale,
                bloomMult = preferences.orbBloomStrength,
                speedMult = preferences.orbRotationSpeed,
                autoRotate = preferences.orbAutoRotate,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
            )
        }

        Column(
            modifier = Modifier
                .weight(1.05f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "STx-ARK OSO",
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                letterSpacing = 3.sp,
                color = HudIce,
                style = TextStyle(
                    shadow = Shadow(color = HudCyan.copy(alpha = 0.45f), blurRadius = 12f)
                ),
                modifier = Modifier.testTag("first_boot_title")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "POWERED BY STx",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 9.sp,
                letterSpacing = 2.6.sp,
                color = StarkGoldBright.copy(alpha = 0.85f),
                modifier = Modifier.testTag("first_boot_powered_by")
            )

            Spacer(modifier = Modifier.height(20.dp))

            MinimalPercentReadout(
                progressPct = progressPct,
                fontSize = 24.sp,
                modifier = Modifier.testTag("first_boot_progress_text")
            )

            Spacer(modifier = Modifier.height(10.dp))

            SmoothChamferedProgressBar(
                progressFraction = progressFraction,
                shimmerPhase = shimmerPhase,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedContent(
                targetState = statusText,
                transitionSpec = {
                    fadeIn(tween(180, delayMillis = 90)) togetherWith fadeOut(tween(90))
                },
                label = "landStatusCrossfade"
            ) { text ->
                Text(
                    text = text,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = Color(0xFF7D8996)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            MinimalSkipAction(
                isReady = progressPct >= 100,
                onClick = onSkipClick
            )
        }
    }
}

@Composable
private fun MinimalPercentReadout(
    progressPct: Int,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokePx = with(density) { 0.90.dp.toPx() }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "$progressPct",
                fontFamily = MichromaFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = fontSize,
                color = HudCyan.copy(alpha = 0.22f),
                style = TextStyle(
                    shadow = Shadow(color = HudCyan.copy(alpha = 0.72f), blurRadius = 12f)
                )
            )
            Text(
                text = "$progressPct",
                fontFamily = MichromaFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = fontSize,
                color = Color(0xFF00141A)
            )
            Text(
                text = "$progressPct",
                fontFamily = MichromaFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = fontSize,
                color = HudCyan,
                style = TextStyle(
                    drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round)
                )
            )
        }

        Text(
            text = "%",
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = fontSize * 0.52f,
            color = StarkGoldBright,
            style = TextStyle(
                shadow = Shadow(color = StarkGold.copy(alpha = 0.60f), blurRadius = 8f)
            ),
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

@Composable
private fun SmoothChamferedProgressBar(
    progressFraction: Float,
    shimmerPhase: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 4f || h <= 4f) return@Canvas

        val ch = h * 0.48f
        val outerPath = Path().apply {
            moveTo(0f, h * 0.5f)
            lineTo(ch, 0f)
            lineTo(w - ch, 0f)
            lineTo(w, h * 0.5f)
            lineTo(w - ch, h)
            lineTo(ch, h)
            close()
        }
        drawPath(outerPath, color = Color(0x99020B10))
        drawPath(
            outerPath,
            brush = Brush.horizontalGradient(
                listOf(
                    HudCyan.copy(alpha = 0.65f),
                    StarkGold.copy(alpha = 0.70f),
                    HudCyan.copy(alpha = 0.65f)
                )
            ),
            style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Round)
        )

        val pad = 3.dp.toPx()
        val iw = (w - pad * 2f).coerceAtLeast(4f)
        val ih = (h - pad * 2f).coerceAtLeast(4f)
        val ic = ih * 0.46f
        val innerPath = Path().apply {
            moveTo(pad, h * 0.5f)
            lineTo(pad + ic, pad)
            lineTo(pad + iw - ic, pad)
            lineTo(pad + iw, h * 0.5f)
            lineTo(pad + iw - ic, pad + ih)
            lineTo(pad + ic, pad + ih)
            close()
        }

        val fillWidth = iw * progressFraction.coerceIn(0f, 1f)
        if (fillWidth > 0.5f) {
            clipPath(innerPath) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(HudElectricBlue, HudCyan, StarkGold),
                        startX = pad,
                        endX = pad + fillWidth.coerceAtLeast(1f)
                    ),
                    topLeft = Offset(pad, pad),
                    size = Size(fillWidth, ih)
                )

                // Soft continuous shimmer wave traveling inside filled track
                val beamX = pad + fillWidth * shimmerPhase
                val beamHalf = (fillWidth * 0.28f).coerceIn(14.dp.toPx(), 46.dp.toPx())
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x55FFF6D6),
                            Color.Transparent
                        ),
                        startX = beamX - beamHalf,
                        endX = beamX + beamHalf
                    ),
                    topLeft = Offset(pad, pad),
                    size = Size(fillWidth, ih)
                )

                // Subtle diagonal hatch texture
                val step = 6.dp.toPx()
                val skew = ih * 0.68f
                var x = pad - skew
                val maxX = pad + fillWidth + skew
                while (x < maxX) {
                    val stripe = Path().apply {
                        moveTo(x, pad + ih)
                        lineTo(x + 1.6.dp.toPx(), pad + ih)
                        lineTo(x + 1.6.dp.toPx() + skew, pad)
                        lineTo(x + skew, pad)
                        close()
                    }
                    drawPath(stripe, color = Color.Black.copy(alpha = 0.26f))
                    x += step
                }

                // Crisp leading light edge
                val edgeX = (pad + fillWidth - 1.8.dp.toPx()).coerceAtLeast(pad)
                drawRect(
                    color = Color(0xFFFFF8E2),
                    topLeft = Offset(edgeX, pad),
                    size = Size(1.8.dp.toPx(), ih)
                )
            }
        }
    }
}

@Composable
private fun MinimalSkipAction(
    isReady: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x66040C12))
            .border(
                width = 0.9.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        HudCyan.copy(alpha = 0.45f),
                        StarkGold.copy(alpha = 0.65f),
                        HudCyan.copy(alpha = 0.45f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .testTag("first_boot_enter_btn"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isReady) "ENTER" else "SKIP BOOT",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                letterSpacing = 2.4.sp,
                color = Color(0xFFD0DBE5)
            )
            Text(
                text = ">",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = StarkGoldBright
            )
        }
    }
}
