package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudCrimson
import com.example.ui.theme.HudCyan
import com.example.ui.theme.HudCyanSoft
import com.example.ui.theme.HudElectricBlue
import com.example.ui.theme.HudIce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

enum class StxAvatarExpression(val badge: String) {
    STANDBY("ATTENTIVE"),
    LISTENING("LISTENING"),
    THINKING("ANALYZING"),
    SPEAKING("VOCALIZING"),
    ALERT("ALERT")
}

object HologramBitmapProcessor {
    private val cache = LruCache<Int, Bitmap>(4)

    fun getOrCreateCutoutBitmap(
        context: Context,
        maxDimPx: Int = 384
    ): Bitmap? {
        val targetDim = maxDimPx.coerceIn(96, 512)
        cache.get(targetDim)?.let { if (!it.isRecycled) return it }

        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, R.drawable.img_stx_hologram, bounds)
            var sample = 1
            val longest = max(bounds.outWidth, bounds.outHeight)
            while (longest > 0 && longest / sample > targetDim * 2) {
                sample *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val raw = BitmapFactory.decodeResource(context.resources, R.drawable.img_stx_hologram, decodeOpts)
                ?: return null

            val scaled = if (max(raw.width, raw.height) > targetDim) {
                val scale = targetDim.toFloat() / max(raw.width, raw.height).toFloat()
                val sw = (raw.width * scale).roundToInt().coerceAtLeast(1)
                val sh = (raw.height * scale).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(raw, sw, sh, true).also {
                    if (it !== raw) raw.recycle()
                }
            } else if (raw.config != Bitmap.Config.ARGB_8888 || !raw.isMutable) {
                raw.copy(Bitmap.Config.ARGB_8888, true).also {
                    if (it !== raw) raw.recycle()
                }
            } else {
                raw
            }

            val w = scaled.width
            val h = scaled.height
            if (w <= 2 || h <= 2) return scaled

            val total = w * h
            val pixels = IntArray(total)
            scaled.getPixels(pixels, 0, w, 0, 0, w, h)

            // Edge-seeded flood-fill marks ONLY the exterior black background (preserves internal hair/eyes)
            val isBg = BooleanArray(total)
            val queue = IntArray(total)
            var head = 0
            var tail = 0

            fun lumaMax(px: Int): Int {
                val r = (px ushr 16) and 0xFF
                val g = (px ushr 8) and 0xFF
                val b = px and 0xFF
                return max(r, max(g, b))
            }

            fun enqueueIfDark(idx: Int) {
                if (!isBg[idx] && lumaMax(pixels[idx]) <= 46) {
                    isBg[idx] = true
                    queue[tail++] = idx
                }
            }

            for (x in 0 until w) {
                enqueueIfDark(x)
                enqueueIfDark((h - 1) * w + x)
            }
            for (y in 0 until h) {
                enqueueIfDark(y * w)
                enqueueIfDark(y * w + (w - 1))
            }

            while (head < tail) {
                val cur = queue[head++]
                val cx = cur % w
                val cy = cur / w
                if (cx > 0) enqueueIfDark(cur - 1)
                if (cx + 1 < w) enqueueIfDark(cur + 1)
                if (cy > 0) enqueueIfDark(cur - w)
                if (cy + 1 < h) enqueueIfDark(cur + w)
            }

            // Soft 2px fringe detection around background contour
            val isFringe = BooleanArray(total)
            for (y in 1 until h - 1) {
                val rowOff = y * w
                for (x in 1 until w - 1) {
                    val idx = rowOff + x
                    if (!isBg[idx]) {
                        if (isBg[idx - 1] || isBg[idx + 1] || isBg[idx - w] || isBg[idx + w]) {
                            isFringe[idx] = true
                        }
                    }
                }
            }

            val bottomFadeStart = h * 0.76f
            for (y in 0 until h) {
                val bottomFade = if (y > bottomFadeStart) {
                    (1f - (y - bottomFadeStart) / (h - bottomFadeStart)).coerceIn(0f, 1f)
                } else {
                    1f
                }
                val rowOff = y * w
                for (x in 0 until w) {
                    val idx = rowOff + x
                    if (isBg[idx]) {
                        pixels[idx] = 0x00000000
                        continue
                    }

                    val px = pixels[idx]
                    val r = (px ushr 16) and 0xFF
                    val g = (px ushr 8) and 0xFF
                    val b = px and 0xFF
                    val maxRgb = max(r, max(g, b))

                    // Convert interior dark tones into translucent holographic cyan glass
                    var alphaF = when {
                        isFringe[idx] -> ((maxRgb - 22f) / 72f).coerceIn(0f, 0.78f)
                        maxRgb < 40 -> (0.28f + (maxRgb / 40f) * 0.34f)
                        else -> (0.58f + ((maxRgb - 40f) / 215f) * 0.40f)
                    }
                    alphaF = (alphaF * bottomFade).coerceIn(0f, 0.96f)

                    // Subtle volumetric cyan projection tint
                    val newR = (r * 0.88f).roundToInt().coerceIn(0, 255)
                    val newG = (g * 0.96f + 12f).roundToInt().coerceIn(0, 255)
                    val newB = max(b, (g * 0.95f + 26f).roundToInt()).coerceIn(0, 255)
                    val newA = (alphaF * 255f).roundToInt().coerceIn(0, 255)

                    pixels[idx] = (newA shl 24) or (newR shl 16) or (newG shl 8) or newB
                }
            }

            scaled.setPixels(pixels, 0, w, 0, 0, w, h)
            cache.put(targetDim, scaled)
            scaled
        }.getOrNull()
    }
}

@Composable
fun rememberHologramCutoutImageBitmap(maxDimPx: Int = 384): ImageBitmap? {
    val context = LocalContext.current
    val bitmapState by produceState<ImageBitmap?>(initialValue = null, context, maxDimPx) {
        value = withContext(Dispatchers.Default) {
            HologramBitmapProcessor.getOrCreateCutoutBitmap(context, maxDimPx)?.asImageBitmap()
        }
    }
    return bitmapState
}

/**
 * Full Volumetric Holographic STX AI Girl Stage:
 * - Zero black background (edge-keyed alpha cutout + translucent holographic tint)
 * - Volumetric light projection rays, rising energy motes, scanline & glitch shimmer
 * - Real-time TTS lip synchronization + expressive eyes, blinking, eyebrows & head motion
 */
@Composable
fun StxHologramGirlCore(
    isSpeaking: Boolean,
    isListening: Boolean,
    isThinking: Boolean,
    isAlert: Boolean = false,
    speechPulseTick: Int = 0,
    voiceRms: Float = 0f,
    onAvatarTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cutoutBitmap = rememberHologramCutoutImageBitmap(maxDimPx = 384)
    val scope = rememberCoroutineScope()

    val expression = when {
        isAlert -> StxAvatarExpression.ALERT
        isSpeaking -> StxAvatarExpression.SPEAKING
        isListening -> StxAvatarExpression.LISTENING
        isThinking -> StxAvatarExpression.THINKING
        else -> StxAvatarExpression.STANDBY
    }

    val holoPrimary = when (expression) {
        StxAvatarExpression.ALERT -> HudCrimson
        StxAvatarExpression.LISTENING -> HudCrimson
        StxAvatarExpression.THINKING -> HudAmber
        StxAvatarExpression.SPEAKING -> HudCyan
        StxAvatarExpression.STANDBY -> HudCyan
    }

    // Continuous organic floating, breathing, scanline, and particle timers
    val infinite = rememberInfiniteTransition(label = "stx_holo_core")
    val hoverPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "holo_hover_phase"
    )
    val scanY by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "holo_scan_y"
    )
    val particleProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
        label = "holo_particles"
    )
    val speechWavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(680, easing = LinearEasing), RepeatMode.Restart),
        label = "speech_formant_phase"
    )

    // Natural eye blinking + tap reaction blink
    val blinkProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        var cycle = 0
        while (isActive) {
            val waitMs = if (cycle % 3 == 2) 2100L else 3600L
            delay(waitMs)
            blinkProgress.animateTo(1f, tween(70, easing = FastOutSlowInEasing))
            blinkProgress.animateTo(0f, tween(110, easing = FastOutSlowInEasing))
            if (cycle % 4 == 3) {
                delay(140L)
                blinkProgress.animateTo(1f, tween(65))
                blinkProgress.animateTo(0f, tween(95))
            }
            cycle++
        }
    }

    // TTS word-boundary syllable impulse for lip articulation
    val wordImpulse = remember { Animatable(0f) }
    LaunchedEffect(speechPulseTick, isSpeaking) {
        if (isSpeaking) {
            wordImpulse.snapTo(1f)
            wordImpulse.animateTo(0.22f, tween(durationMillis = 190, easing = FastOutSlowInEasing))
        } else {
            wordImpulse.animateTo(0f, tween(durationMillis = 160))
        }
    }

    val speakingIntensity by animateFloatAsState(
        targetValue = if (isSpeaking) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "speaking_intensity"
    )
    val listeningIntensity by animateFloatAsState(
        targetValue = if (isListening) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "listening_intensity"
    )
    val thinkingIntensity by animateFloatAsState(
        targetValue = if (isThinking) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "thinking_intensity"
    )

    val mouthPath = remember { Path() }
    val upperLipPath = remember { Path() }
    val lowerLipPath = remember { Path() }
    val leftBrowPath = remember { Path() }
    val rightBrowPath = remember { Path() }
    val beamPath = remember { Path() }

    BoxWithConstraints(
        modifier = modifier.let { base ->
            if (onAvatarTap != null) {
                base.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    scope.launch {
                        blinkProgress.animateTo(1f, tween(65))
                        blinkProgress.animateTo(0f, tween(100))
                    }
                    onAvatarTap()
                }
            } else {
                base
            }
        },
        contentAlignment = Alignment.Center
    ) {
        val squareDp = minOf(maxWidth, maxHeight).coerceAtLeast(1.dp)

        // Back Volumetric Projection Cone + Emitter Halo + Rising Particles (renders BEHIND transparent hologram girl)
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val cx = size.width * 0.5f
            val pedY = size.height * 0.90f
            val topY = size.height * 0.08f
            val pedRadiusX = (min(size.width, size.height) * 0.36f).coerceAtLeast(4f)

            // Volumetric upward hologram projection cone
            beamPath.reset()
            beamPath.moveTo(cx - pedRadiusX * 0.62f, pedY)
            beamPath.lineTo(cx - pedRadiusX * 1.18f, topY)
            beamPath.lineTo(cx + pedRadiusX * 1.18f, topY)
            beamPath.lineTo(cx + pedRadiusX * 0.62f, pedY)
            beamPath.close()
            drawPath(
                path = beamPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        holoPrimary.copy(alpha = 0.06f + speakingIntensity * 0.05f),
                        holoPrimary.copy(alpha = 0.20f + listeningIntensity * 0.08f)
                    ),
                    startY = topY,
                    endY = pedY
                )
            )

            // Subtle hologram face/aura glow (no dark rectangle!)
            val auraR = (min(size.width, size.height) * 0.44f).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        holoPrimary.copy(alpha = 0.16f + speakingIntensity * 0.10f),
                        HudElectricBlue.copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    center = Offset(cx, size.height * 0.44f),
                    radius = auraR
                ),
                center = Offset(cx, size.height * 0.44f),
                radius = auraR
            )

            // Rising holographic energy motes
            val moteCount = 14
            for (i in 0 until moteCount) {
                val p = (particleProgress + i / moteCount.toFloat()) % 1f
                val spreadX = sin(i * 2.4f + p * 3.1f) * pedRadiusX * (0.55f + p * 0.45f)
                val mx = cx + spreadX
                val my = pedY - p * (pedY - topY)
                val alpha = (sin(p * PI) * (0.28 + speakingIntensity * 0.35)).toFloat().coerceIn(0f, 0.7f)
                drawCircle(
                    color = (if (i % 4 == 0) HudIce else holoPrimary).copy(alpha = alpha),
                    radius = (1.1f + (i % 3) * 0.55f).dp.toPx(),
                    center = Offset(mx, my)
                )
            }
        }

        // Centered Transparent-Cutout Avatar + Live Synchronized Lip & Facial Expressions
        val floatYDp = sin(hoverPhase) * 3.2f
        val speakNodDeg = sin(speechWavePhase * 0.5f) * 1.15f * speakingIntensity
        val listenTiltDeg = -1.8f * listeningIntensity + 1.4f * thinkingIntensity

        Box(
            modifier = Modifier
                .size(squareDp)
                .graphicsLayer {
                    translationY = floatYDp.dp.toPx() - speakingIntensity * 1.5.dp.toPx()
                    rotationZ = speakNodDeg + listenTiltDeg
                    val breathScale = 1f + sin(hoverPhase * 1.3f) * 0.012f + speakingIntensity * 0.015f
                    scaleX = breathScale
                    scaleY = breathScale
                },
            contentAlignment = Alignment.Center
        ) {
            if (cutoutBitmap != null) {
                Image(
                    bitmap = cutoutBitmap,
                    contentDescription = stringResource(R.string.cd_stx_hologram),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // First-frame fallback uses Screen blend so black background disappears immediately even before async cutout finishes
                Image(
                    painter = painterResource(id = R.drawable.img_stx_hologram),
                    contentDescription = stringResource(R.string.cd_stx_hologram),
                    contentScale = ContentScale.Fit,
                    colorFilter = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                        }
                )
            }

            // Real-Time Facial Feature & Lip-Sync Overlay aligned to normalized portrait landmarks
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (w <= 8f || h <= 8f) return@Canvas

                val blink = blinkProgress.value
                val leftEyeX = w * 0.422f
                val rightEyeX = w * 0.578f
                val eyeY = h * 0.368f
                val eyeRx = w * 0.036f
                val eyeRy = w * 0.019f

                // Subtle eye gaze tracking (scans slightly while thinking)
                val gazeX = (sin(hoverPhase * 1.6f) * thinkingIntensity * w * 0.008f) +
                    (sin(hoverPhase * 0.7f) * (1f - thinkingIntensity) * w * 0.002f)
                val gazeY = -listeningIntensity * h * 0.002f

                // 1) Animated Glowing Iris Luminance & Eye Blinks
                if (blink < 0.72f) {
                    val eyeOpenFactor = (1f - blink).coerceIn(0f, 1f)
                    val irisAlpha = (0.30f + speakingIntensity * 0.25f + listeningIntensity * 0.32f) * eyeOpenFactor
                    val irisR = (w * 0.014f).coerceAtLeast(1f)
                    for (ex in listOf(leftEyeX, rightEyeX)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    HudIce.copy(alpha = irisAlpha),
                                    holoPrimary.copy(alpha = irisAlpha * 0.65f),
                                    Color.Transparent
                                ),
                                center = Offset(ex + gazeX, eyeY + gazeY),
                                radius = irisR * 1.8f
                            ),
                            center = Offset(ex + gazeX, eyeY + gazeY),
                            radius = irisR * 1.8f
                        )
                        // Tiny specular catchlight
                        drawCircle(
                            color = HudIce.copy(alpha = (0.75f * eyeOpenFactor)),
                            radius = (irisR * 0.34f).coerceAtLeast(0.8f),
                            center = Offset(ex + gazeX - irisR * 0.35f, eyeY + gazeY - irisR * 0.35f)
                        )
                    }
                }

                // Smooth eyelid closing sweep during blinks
                if (blink > 0.05f) {
                    val lidAlpha = (blink * 0.88f).coerceIn(0f, 0.92f)
                    val lidH = eyeRy * 2.1f * blink
                    for (ex in listOf(leftEyeX, rightEyeX)) {
                        drawOval(
                            color = Color(0xFF041C2E).copy(alpha = lidAlpha),
                            topLeft = Offset(ex - eyeRx, eyeY - eyeRy),
                            size = Size(eyeRx * 2f, lidH.coerceAtLeast(1f))
                        )
                        drawLine(
                            color = HudCyanSoft.copy(alpha = lidAlpha),
                            start = Offset(ex - eyeRx * 0.95f, eyeY - eyeRy + lidH * 0.85f),
                            end = Offset(ex + eyeRx * 0.95f, eyeY - eyeRy + lidH * 0.85f),
                            strokeWidth = (w * 0.0045f).coerceAtLeast(1f),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // 2) Expressive Holographic Eyebrow Highlights (lift on listening/speaking, furrow on thinking/alert)
                val browLift = (-listeningIntensity * h * 0.011f) -
                    (speakingIntensity * sin(speechWavePhase * 0.5f).coerceAtLeast(0f) * h * 0.006f) +
                    (thinkingIntensity * h * 0.004f)
                val innerFurrow = (thinkingIntensity + if (isAlert) 1f else 0f).coerceIn(0f, 1f) * h * 0.008f
                val browY = h * 0.320f + browLift

                leftBrowPath.reset()
                leftBrowPath.moveTo(leftEyeX - eyeRx * 1.2f, browY + h * 0.004f)
                leftBrowPath.quadraticTo(
                    leftEyeX,
                    browY - h * 0.010f,
                    leftEyeX + eyeRx * 1.1f,
                    browY + innerFurrow
                )
                rightBrowPath.reset()
                rightBrowPath.moveTo(rightEyeX - eyeRx * 1.1f, browY + innerFurrow)
                rightBrowPath.quadraticTo(
                    rightEyeX,
                    browY - h * 0.010f,
                    rightEyeX + eyeRx * 1.2f,
                    browY + h * 0.004f
                )
                val browStroke = (w * 0.0055f).coerceAtLeast(1f)
                val browAlpha = 0.42f + listeningIntensity * 0.32f + thinkingIntensity * 0.28f
                drawPath(
                    path = leftBrowPath,
                    color = holoPrimary.copy(alpha = browAlpha),
                    style = Stroke(width = browStroke, cap = StrokeCap.Round)
                )
                drawPath(
                    path = rightBrowPath,
                    color = holoPrimary.copy(alpha = browAlpha),
                    style = Stroke(width = browStroke, cap = StrokeCap.Round)
                )

                // 3) Real-Time Speaking Lip Sync & Smile/Expression Mouth Articulation
                val mouthCx = w * 0.500f
                val mouthCy = h * 0.494f

                val formant = (
                    sin(speechWavePhase * 2.0f) * 0.44f +
                        sin(speechWavePhase * 3.3f + 0.7f) * 0.36f +
                        0.50f
                    ).coerceIn(0f, 1f)
                val speakOpen = (wordImpulse.value * 0.55f + formant * 0.45f) * speakingIntensity
                val listenOpen = (voiceRms.coerceIn(0f, 1f) * 0.14f) * listeningIntensity
                val mouthOpen = (speakOpen + listenOpen).coerceIn(0f, 1f)

                val spreadMod = if (isSpeaking) {
                    1f + sin(speechWavePhase * 1.5f) * 0.16f
                } else if (expression == StxAvatarExpression.STANDBY) {
                    1.05f
                } else {
                    0.96f
                }
                val mouthHalfW = w * 0.044f * spreadMod
                val smileCurve = when (expression) {
                    StxAvatarExpression.STANDBY -> h * 0.0055f
                    StxAvatarExpression.LISTENING -> h * 0.004f
                    StxAvatarExpression.SPEAKING -> h * 0.0045f
                    StxAvatarExpression.THINKING -> -h * 0.002f
                    StxAvatarExpression.ALERT -> -h * 0.0045f
                }

                // Soft vocal modulation aura around mouth & jawline when speaking
                if (mouthOpen > 0.05f) {
                    val auraMouthR = (mouthHalfW * (1.8f + mouthOpen * 0.6f)).coerceAtLeast(2f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                holoPrimary.copy(alpha = 0.24f * mouthOpen),
                                Color.Transparent
                            ),
                            center = Offset(mouthCx, mouthCy),
                            radius = auraMouthR
                        ),
                        center = Offset(mouthCx, mouthCy),
                        radius = auraMouthR
                    )
                }

                val upperLift = mouthOpen * h * 0.006f
                val lowerDrop = mouthOpen * h * 0.022f

                if (mouthOpen > 0.07f) {
                    // Inner dark holographic vocal aperture
                    mouthPath.reset()
                    mouthPath.moveTo(mouthCx - mouthHalfW * 0.88f, mouthCy)
                    mouthPath.quadraticTo(
                        mouthCx,
                        mouthCy - upperLift - h * 0.004f,
                        mouthCx + mouthHalfW * 0.88f,
                        mouthCy
                    )
                    mouthPath.quadraticTo(
                        mouthCx,
                        mouthCy + lowerDrop + h * 0.007f,
                        mouthCx - mouthHalfW * 0.88f,
                        mouthCy
                    )
                    mouthPath.close()
                    drawPath(
                        path = mouthPath,
                        color = Color(0xE002101B)
                    )

                    // Subtle teeth glint on open vowels
                    if (mouthOpen > 0.28f) {
                        drawLine(
                            color = HudIce.copy(alpha = (0.48f * mouthOpen).coerceIn(0f, 0.55f)),
                            start = Offset(mouthCx - mouthHalfW * 0.42f, mouthCy - upperLift * 0.2f + h * 0.002f),
                            end = Offset(mouthCx + mouthHalfW * 0.42f, mouthCy - upperLift * 0.2f + h * 0.002f),
                            strokeWidth = (h * 0.0055f).coerceAtLeast(1f),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Luminous Upper & Lower Lip Contours
                upperLipPath.reset()
                upperLipPath.moveTo(mouthCx - mouthHalfW, mouthCy - smileCurve)
                upperLipPath.quadraticTo(
                    mouthCx - mouthHalfW * 0.35f,
                    mouthCy - upperLift - h * 0.005f,
                    mouthCx,
                    mouthCy - upperLift - h * 0.002f
                )
                upperLipPath.quadraticTo(
                    mouthCx + mouthHalfW * 0.35f,
                    mouthCy - upperLift - h * 0.005f,
                    mouthCx + mouthHalfW,
                    mouthCy - smileCurve
                )

                lowerLipPath.reset()
                lowerLipPath.moveTo(mouthCx - mouthHalfW * 0.94f, mouthCy - smileCurve * 0.7f)
                lowerLipPath.quadraticTo(
                    mouthCx,
                    mouthCy + lowerDrop + h * 0.009f + smileCurve * 0.5f,
                    mouthCx + mouthHalfW * 0.94f,
                    mouthCy - smileCurve * 0.7f
                )

                val lipAlpha = (0.58f + mouthOpen * 0.40f + listeningIntensity * 0.16f).coerceIn(0f, 0.98f)
                val lipStroke = (w * 0.0055f).coerceAtLeast(1.1f)
                drawPath(
                    path = upperLipPath,
                    color = HudIce.copy(alpha = lipAlpha),
                    style = Stroke(width = lipStroke, cap = StrokeCap.Round)
                )
                drawPath(
                    path = lowerLipPath,
                    color = holoPrimary.copy(alpha = lipAlpha),
                    style = Stroke(width = lipStroke * 1.15f, cap = StrokeCap.Round)
                )

                // Subtle holographic scanlines across the avatar silhouette
                val scanStep = 4.5.dp.toPx().coerceAtLeast(3f)
                var sy = h * 0.10f
                while (sy < h * 0.88f) {
                    drawLine(
                        color = holoPrimary.copy(alpha = 0.055f),
                        start = Offset(w * 0.18f, sy),
                        end = Offset(w * 0.82f, sy),
                        strokeWidth = 0.9f
                    )
                    sy += scanStep
                }

                // Active vertical holographic refresh beam sweeping across portrait
                val activeBeamY = h * (0.12f + scanY * 0.76f)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            holoPrimary.copy(alpha = 0.42f + speakingIntensity * 0.25f),
                            HudIce.copy(alpha = 0.48f),
                            holoPrimary.copy(alpha = 0.42f + speakingIntensity * 0.25f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(w * 0.14f, activeBeamY),
                    end = Offset(w * 0.86f, activeBeamY),
                    strokeWidth = 1.6.dp.toPx()
                )
            }
        }

        // Front 3D Elliptical Holo-Emitter Pedestal + Live Audio-Reactive Ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val cx = size.width * 0.5f
            val pedY = size.height * 0.90f
            val pedW = (min(size.width, size.height) * 0.68f).coerceAtLeast(6f)
            val pedH = 16.dp.toPx().coerceAtLeast(4f)
            val reactiveBoost = speakingIntensity * 0.35f + (voiceRms * listeningIntensity * 0.35f)

            // Outer emitter halo ring
            drawOval(
                color = holoPrimary.copy(alpha = 0.28f + reactiveBoost),
                topLeft = Offset(cx - pedW * 0.54f, pedY - pedH * 0.62f),
                size = Size(pedW * 1.08f, pedH * 1.24f),
                style = Stroke(width = 3.dp.toPx())
            )
            // Crisp primary emitter ring
            drawOval(
                color = holoPrimary.copy(alpha = 0.85f),
                topLeft = Offset(cx - pedW * 0.5f, pedY - pedH * 0.5f),
                size = Size(pedW, pedH),
                style = Stroke(width = 1.6.dp.toPx())
            )
            // Inner ice-white core ring
            drawOval(
                color = HudIce.copy(alpha = 0.48f + reactiveBoost * 0.4f),
                topLeft = Offset(cx - pedW * 0.32f, pedY - pedH * 0.30f),
                size = Size(pedW * 0.64f, pedH * 0.60f),
                style = Stroke(width = 1.1.dp.toPx())
            )
        }
    }
}

/**
 * Compact circular holographic avatar badge (zero black square!) for the floating orb and header chip.
 */
@Composable
fun StxHologramOrbAvatar(
    isSpeaking: Boolean = false,
    isListening: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cutoutBitmap = rememberHologramCutoutImageBitmap(maxDimPx = 160)
    val glowColor = if (isListening) HudCrimson else HudCyan

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (isSpeaking || isListening) 0.34f else 0.20f),
                        Color(0xAA041422),
                        Color(0x66020810)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (cutoutBitmap != null) {
            Image(
                bitmap = cutoutBitmap,
                contentDescription = "STX AI Hologram",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_stx_hologram),
                contentDescription = "STX AI Hologram",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp)
            )
        }
    }
}
