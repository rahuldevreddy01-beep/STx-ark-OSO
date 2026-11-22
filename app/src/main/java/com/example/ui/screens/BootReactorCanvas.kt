package com.example.ui.screens

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Multi-layer Stark Arc Reactor engine translated 1:1 from the HTML/SVG boot reactor,
 * enriched with dual-tone Electric Cyan/Blue + Titanium-Gold harmonics.
 * Coordinates operate in a normalized 640x640 virtual design space scaled cleanly to any canvas.
 */
@Stable
class BootReactorState {
    private val rng = Random(20260805)

    data class RingSpec(
        val id: String,
        val v0: Float,
        val v1: Float,
        val dir: Float,
        val trails: Int,
        val opacityFn: (Float) -> Float,
        var angle: Float = 0f
    )

    data class Sparkle(
        var angle: Float,
        val radius: Float,
        val speed: Float,
        val phase: Float,
        val freq: Float,
        val size: Float,
        val isGold: Boolean
    )

    private fun smoothstep(a: Float, b: Float, x: Float): Float {
        val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    val rings = listOf(
        RingSpec("dTicks", 2.5f, 18f, 1f, 0, { 1f }),
        RingSpec("d232", 4f, 30f, -1f, 0, { 1f }),
        RingSpec("d240", 3f, 25f, -1f, 0, { 1f }),
        RingSpec("dRingD", 3f, 14f, -1f, 0, { e -> 1f - 0.58f * e }),
        RingSpec("dOuter", 6f, 95f, 1f, 2, { e -> smoothstep(0.08f, 0.52f, e) }),
        RingSpec("dLed", 5f, 60f, -1f, 1, { e -> smoothstep(0.04f, 0.42f, e) * 0.92f }),
        RingSpec("dArcsC", 5f, 70f, 1f, 2, { 1f }),
        RingSpec("dExtra", 10f, 110f, -1f, 2, { e -> smoothstep(0.32f, 0.72f, e) }),
        RingSpec("dDots", 8f, 80f, -1f, 1, { e -> smoothstep(0f, 0.38f, e) }),
        RingSpec("dSweep", 30f, 140f, 1f, 2, { e -> 0.55f + 0.45f * e })
    )

    val sparkles = List(28) { idx ->
        Sparkle(
            angle = rng.nextFloat() * 360f,
            radius = 196f + rng.nextFloat() * 108f,
            speed = 25f + rng.nextFloat() * 70f,
            phase = rng.nextFloat() * (2f * PI.toFloat()),
            freq = 2f + rng.nextFloat() * 5f,
            size = 0.9f + rng.nextFloat() * 1.7f,
            isGold = idx % 3 == 0
        )
    }

    var energy: Float = 0f
        private set
    var targetEnergy: Float = 0f
    var elapsedSec: Float = 0f
        private set
    var filamentAngle1: Float = 0f
        private set
    var filamentAngle2: Float = 0f
        private set
    var hotPhase: Float = 0f
        private set
    var currentScaleMult: Float = 1f
        private set

    fun step(dt: Float, speedMult: Float, autoRotate: Boolean) {
        val motion = if (autoRotate) speedMult.coerceIn(0.2f, 2.5f) else 0f
        energy += (targetEnergy - energy) * (1f - exp(-dt / 0.32f))
        val ek = energy.coerceIn(0f, 1f).pow(1.25f)
        elapsedSec += dt

        for (r in rings) {
            val v = (r.v0 + (r.v1 - r.v0) * ek) * motion
            r.angle = (r.angle + r.dir * v * dt) % 360f
        }
        filamentAngle1 = (filamentAngle1 + (20f + 80f * ek) * dt * motion) % 360f
        filamentAngle2 = (filamentAngle2 - (13f + 57f * ek) * dt * motion) % 360f
        hotPhase += (2.2f + 5f * ek) * dt * motion

        val spAlpha = smoothstep(0.25f, 0.85f, energy)
        if (spAlpha > 0.005f) {
            for (s in sparkles) {
                s.angle = (s.angle + s.speed * (0.3f + ek) * dt * motion) % 360f
            }
        }
    }
}

private class BootReactorPainter {
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val arcRect = RectF()
    private val triPath = Path()
    private val clipPath = Path()
    private val shaderMatrix = Matrix()

    // Cached shaders scaled at unit/virtual design coords centered on (0,0)
    private val bigHaloShader = RadialGradient(
        0f, 0f, 320f,
        intArrayOf(0x3D1E8FFF, 0x1CF5B82E, 0x0A1E8FFF, 0x00000000),
        floatArrayOf(0f, 0.32f, 0.58f, 1f),
        Shader.TileMode.CLAMP
    )
    private val haloGShader = RadialGradient(
        0f, 0f, 128f,
        intArrayOf(0x001E8FFF, 0x001E8FFF, 0x801E8FFF.toInt(), 0x3DF5B82E, 0x001E8FFF),
        floatArrayOf(0f, 0.58f, 0.70f, 0.84f, 1f),
        Shader.TileMode.CLAMP
    )
    private val coreGShader = RadialGradient(
        0f, 0f, 88f,
        intArrayOf(
            0xFFFFFFFF.toInt(),
            0xFFFFF5D6.toInt(),
            0xFF9FE9FF.toInt(),
            0xFF3AA6FF.toInt(),
            0xFF1557E0.toInt(),
            0xFF0A2F8F.toInt(),
            0xFF04164A.toInt()
        ),
        floatArrayOf(0f, 0.09f, 0.20f, 0.36f, 0.60f, 0.84f, 1f),
        Shader.TileMode.CLAMP
    )
    private val hotGShader = RadialGradient(
        0f, 0f, 62f,
        intArrayOf(0xFFFFFFFF.toInt(), 0xE6FFF2C8.toInt(), 0x6659C3FF, 0x001E8FFF),
        floatArrayOf(0f, 0.25f, 0.60f, 1f),
        Shader.TileMode.CLAMP
    )
    private val flareCyanShader = RadialGradient(
        0f, 0f, 1f,
        intArrayOf(0xFFFFFFFF.toInt(), 0xF2E8FDFF.toInt(), 0x885FE2FF.toInt(), 0x0000E5FF),
        floatArrayOf(0f, 0.14f, 0.42f, 1f),
        Shader.TileMode.CLAMP
    )
    private val flareGoldShader = RadialGradient(
        0f, 0f, 1f,
        intArrayOf(0xD8FFF3CC.toInt(), 0x88FFD56B.toInt(), 0x00F5B82E),
        floatArrayOf(0f, 0.35f, 1f),
        Shader.TileMode.CLAMP
    )
    private val armorRingShader = RadialGradient(
        0f, 0f, 160f,
        intArrayOf(0xFF050A12.toInt(), 0xFF0B1624.toInt(), 0xFF0E1A2A.toInt(), 0xFF1A3550.toInt(), 0xFF8C6B28.toInt()),
        floatArrayOf(0.74f, 0.80f, 0.90f, 0.96f, 1f),
        Shader.TileMode.CLAMP
    )

    private val dashTicksMain = DashPathEffect(floatArrayOf(1f, 5.545f), 0f)
    private val dashTicksArc = DashPathEffect(floatArrayOf(1.3f, 2.8f), 0f)
    private val dashMicroDot = DashPathEffect(floatArrayOf(0.2f, 6f), 0f)
    private val dash232 = DashPathEffect(floatArrayOf(0.2f, 7.3f), 0f)
    private val dashArmorOuter = DashPathEffect(floatArrayOf(41.668f, 2f), 0f)
    private val dashArmorInner = DashPathEffect(floatArrayOf(17.024f, 2f), 0f)
    private val dashArmorAccent = DashPathEffect(floatArrayOf(17.024f, 154.19f), 0f)
    private val dashOuterD1 = DashPathEffect(floatArrayOf(14f, 3f, 5f, 3f, 22f, 3f, 8f, 3f, 4f, 3f, 12f, 3f), 0f)
    private val dashOuterD2 = DashPathEffect(floatArrayOf(6f, 3f, 18f, 3f, 4f, 3f, 9f, 3f, 3f, 3f), 0f)
    private val dashLed = DashPathEffect(floatArrayOf(2.4f, 3.2f), 0f)
    private val dashDotsMain = DashPathEffect(floatArrayOf(3.4f, 4.096f), 0f)
    private val dashDotsSub = DashPathEffect(floatArrayOf(1.6f, 6.4f), 0f)

    private val softBlur = BlurMaskFilter(5f, BlurMaskFilter.Blur.NORMAL)
    private val midBlur = BlurMaskFilter(9f, BlurMaskFilter.Blur.NORMAL)

    fun draw(
        c: Canvas,
        w: Float,
        h: Float,
        state: BootReactorState,
        scaleMult: Float,
        bloomMult: Float,
        speedMult: Float,
        autoRotate: Boolean
    ) {
        val cx = w * 0.5f
        val cy = h * 0.5f
        // Virtual SVG reactor fits within +/- 320px (with flare up to 380)
        val baseScale = (min(w, h) / 680f) * scaleMult.coerceIn(0.75f, 1.3f)
        val e = state.energy.coerceIn(0f, 1f)
        val ek = e.pow(1.25f)
        val bloom = bloomMult.coerceIn(0.5f, 1.6f)
        val motion = if (autoRotate) speedMult.coerceIn(0.2f, 2.5f) else 0f

        c.save()
        c.translate(cx, cy)
        c.scale(baseScale, baseScale)

        // 0. Static outer curved HUD brackets (ringA)
        drawOuterArcBrackets(c, bloom)

        // 1. Wide radial aura halo0
        fillPaint.shader = bigHaloShader
        fillPaint.alpha = ((0.62f + 0.38f * e) * 255f).toInt().coerceIn(0, 255)
        c.drawCircle(0f, 0f, 320f, fillPaint)
        fillPaint.shader = null

        // 2. Outer calibrated rings: dTicks, d232, d240
        drawRotatedRing(c, state, "dTicks", e, ek, motion) { alpha ->
            strokePaint.pathEffect = dashTicksMain
            strokePaint.color = 0xFF00E5FF.toInt()
            strokePaint.strokeWidth = 7f
            strokePaint.alpha = (0.28f * alpha * 255f).toInt()
            c.drawCircle(0f, 0f, 250f, strokePaint)

            strokePaint.pathEffect = dashTicksArc
            strokePaint.strokeWidth = 9f
            strokePaint.color = 0xFF00E5FF.toInt()
            strokePaint.alpha = (0.72f * alpha * 255f).toInt()
            drawArcDeg(c, 262f, 8f, 62f)
            drawArcDeg(c, 262f, 200f, 250f)

            // Warm gold opposing calibration arc ticks
            strokePaint.color = 0xFFF5B82E.toInt()
            strokePaint.alpha = (0.65f * alpha * 255f).toInt()
            drawArcDeg(c, 262f, 112f, 152f)
            drawArcDeg(c, 262f, 294f, 334f)

            strokePaint.pathEffect = dashMicroDot
            strokePaint.strokeCap = Paint.Cap.ROUND
            strokePaint.color = 0xFF7FF2FF.toInt()
            strokePaint.strokeWidth = 2f
            strokePaint.alpha = (0.55f * alpha * 255f).toInt()
            drawArcDeg(c, 274f, 18f, 48f)
            drawArcDeg(c, 274f, 212f, 238f)
            strokePaint.strokeCap = Paint.Cap.BUTT
            strokePaint.pathEffect = null
        }

        drawRotatedRing(c, state, "d232", e, ek, motion) { alpha ->
            strokePaint.pathEffect = dash232
            strokePaint.strokeCap = Paint.Cap.ROUND
            strokePaint.color = 0xFF3BA9FF.toInt()
            strokePaint.strokeWidth = 2f
            strokePaint.alpha = (0.52f * alpha * 255f).toInt()
            c.drawCircle(0f, 0f, 232f, strokePaint)
            strokePaint.strokeCap = Paint.Cap.BUTT
            strokePaint.pathEffect = null
        }

        drawRotatedRing(c, state, "d240", e, ek, motion) { alpha ->
            strokePaint.pathEffect = null
            strokePaint.color = 0xFF00E5FF.toInt()
            strokePaint.strokeWidth = 1.2f
            strokePaint.alpha = (0.48f * alpha * 255f).toInt()
            drawArcDeg(c, 240f, -4f, 88f)
            strokePaint.color = 0xFFF5B82E.toInt()
            strokePaint.alpha = (0.44f * alpha * 255f).toInt()
            drawArcDeg(c, 240f, 176f, 268f)
        }

        // Static structural trim circles (206, 190)
        strokePaint.pathEffect = null
        strokePaint.color = 0xFF00E5FF.toInt()
        strokePaint.strokeWidth = 1.3f
        strokePaint.alpha = 155
        c.drawCircle(0f, 0f, 206f, strokePaint)
        strokePaint.color = 0xFFF5B82E.toInt()
        strokePaint.strokeWidth = 1.0f
        strokePaint.alpha = 95
        c.drawCircle(0f, 0f, 190f, strokePaint)

        // 3. Gunmetal segmented armor ring with 20 golden bolts (dRingD)
        drawRotatedRing(c, state, "dRingD", e, ek, motion) { alpha ->
            strokePaint.shader = armorRingShader
            strokePaint.pathEffect = dashArmorOuter
            strokePaint.strokeWidth = 38f
            strokePaint.alpha = (alpha * 255f).toInt()
            c.drawCircle(0f, 0f, 139f, strokePaint)
            strokePaint.shader = null

            strokePaint.color = 0xFF08111B.toInt()
            strokePaint.pathEffect = dashArmorInner
            strokePaint.strokeWidth = 16f
            strokePaint.alpha = (alpha * 255f).toInt()
            c.drawCircle(0f, 0f, 109f, strokePaint)

            strokePaint.color = 0xFF3BA9FF.toInt()
            strokePaint.pathEffect = dashArmorAccent
            strokePaint.strokeWidth = 16f
            strokePaint.alpha = (0.24f * alpha * 255f).toInt()
            c.drawCircle(0f, 0f, 109f, strokePaint)
            strokePaint.pathEffect = null

            // 20 titanium-gold radial bolts
            for (i in 0 until 20) {
                val deg = i * 18f + 9f
                val rad = deg * (PI.toFloat() / 180f)
                val bx = 152f * sin(rad)
                val by = -152f * cos(rad)
                fillPaint.color = if (i % 2 == 0) 0xFFF5B82E.toInt() else 0xFF5FC6FF.toInt()
                fillPaint.alpha = ((if (i % 2 == 0) 0.78f else 0.55f) * alpha * 255f).toInt()
                c.drawCircle(bx, by, 1.55f, fillPaint)
            }
        }

        // Mechanical inner trim circles & housing arcs
        strokePaint.pathEffect = null
        strokePaint.color = 0xFF3BA9FF.toInt()
        strokePaint.strokeWidth = 1.1f
        strokePaint.alpha = 120
        c.drawCircle(0f, 0f, 158.5f, strokePaint)
        strokePaint.color = 0xFFF5B82E.toInt()
        strokePaint.alpha = 110
        c.drawCircle(0f, 0f, 120f, strokePaint)
        strokePaint.color = 0xFF00E5FF.toInt()
        strokePaint.alpha = 85
        c.drawCircle(0f, 0f, 100.5f, strokePaint)

        glowPaint.maskFilter = softBlur
        glowPaint.color = 0xFF3BA9FF.toInt()
        glowPaint.strokeWidth = 2.2f
        glowPaint.alpha = 155
        drawArcDeg(c, 159f, -55f, 55f, glowPaint)
        glowPaint.color = 0xFFF5B82E.toInt()
        glowPaint.alpha = 125
        drawArcDeg(c, 159f, 125f, 235f, glowPaint)
        glowPaint.maskFilter = null

        // 4. Glowing energy rings with dynamic rotation & velocity motion trails
        drawRotatedRing(c, state, "dOuter", e, ek, motion) { alpha ->
            val segs = listOf(18f to 74f, 148f to 204f, 246f to 298f)
            segs.forEachIndexed { idx, (a, b) ->
                strokePaint.pathEffect = null
                strokePaint.color = if (idx == 1) 0xFFF5B82E.toInt() else 0xFF00E5FF.toInt()
                strokePaint.strokeWidth = 1.2f
                strokePaint.alpha = (0.38f * alpha * 255f).toInt()
                drawArcDeg(c, 286f, a - 4f, b + 10f)

                strokePaint.pathEffect = dashOuterD1
                strokePaint.color = if (idx == 1) 0xFFE6A118.toInt() else 0xFF1E8FFF.toInt()
                strokePaint.strokeWidth = 8f
                strokePaint.alpha = (0.85f * alpha * 255f).toInt()
                drawArcDeg(c, 266f, a, b)

                strokePaint.pathEffect = dashOuterD2
                strokePaint.color = if (idx == 1) 0xFFFFD56B.toInt() else 0xFF3BA9FF.toInt()
                strokePaint.strokeWidth = 5.5f
                strokePaint.alpha = (0.62f * alpha * 255f).toInt()
                drawArcDeg(c, 278f, a + 6f, b - 4f)

                strokePaint.pathEffect = dashOuterD1
                strokePaint.color = 0xFFE0FAFF.toInt()
                strokePaint.strokeWidth = 1.4f
                strokePaint.alpha = (0.75f * alpha * 255f).toInt()
                drawArcDeg(c, 266f, a, b)
            }
            strokePaint.pathEffect = null
        }

        drawRotatedRing(c, state, "dLed", e, ek, motion) { alpha ->
            listOf(-70f to 50f, 110f to 230f).forEachIndexed { idx, (a, b) ->
                strokePaint.pathEffect = dashLed
                strokePaint.color = if (idx == 0) 0xFF3BA9FF.toInt() else 0xFFF5B82E.toInt()
                strokePaint.strokeWidth = 6.8f
                strokePaint.alpha = (0.76f * alpha * 255f).toInt()
                drawArcDeg(c, 228f, a, b)

                strokePaint.pathEffect = null
                strokePaint.color = if (idx == 0) 0xFF00E5FF.toInt() else 0xFFFFD56B.toInt()
                strokePaint.strokeWidth = 1.2f
                strokePaint.alpha = (0.42f * alpha * 255f).toInt()
                drawArcDeg(c, 221f, a + 8f, b - 8f)
            }
            strokePaint.pathEffect = null
        }

        drawRotatedRing(c, state, "dArcsC", e, ek, motion) { alpha ->
            // 5 thick iconic radial thruster arcs: cyan-blue + warm Stark gold highlights
            val arcs = listOf(
                Triple(-8f, 38f, 9f),
                Triple(41f, 72f, 6f),
                Triple(112f, 150f, 8f),
                Triple(180f, 222f, 8f),
                Triple(247f, 293f, 9f)
            )
            glowPaint.maskFilter = if (bloom > 0.6f) midBlur else null
            arcs.forEachIndexed { idx, (a, b, wArc) ->
                val isGoldArc = idx == 1 || idx == 3
                glowPaint.color = if (isGoldArc) 0xFFF5B82E.toInt() else 0xFF2394FF.toInt()
                glowPaint.strokeWidth = wArc + 1.5f
                glowPaint.alpha = (0.78f * alpha * 255f).toInt()
                drawArcDeg(c, 198f, a, b, glowPaint)

                strokePaint.pathEffect = null
                strokePaint.color = if (isGoldArc) 0xFFFFEBAA.toInt() else 0xFFA6E2FF.toInt()
                strokePaint.strokeWidth = wArc * 0.36f
                strokePaint.alpha = (0.95f * alpha * 255f).toInt()
                drawArcDeg(c, 198f, a, b)
            }
            glowPaint.maskFilter = null
        }

        drawRotatedRing(c, state, "dExtra", e, ek, motion) { alpha ->
            val extras = listOf(
                Triple(58f, 112f, 4f),
                Triple(226f, 262f, 4f),
                Triple(300f, 318f, 3f)
            )
            extras.forEachIndexed { idx, (a, b, wArc) ->
                strokePaint.pathEffect = null
                strokePaint.color = if (idx == 1) 0xFFFFD56B.toInt() else 0xFF5FD0FF.toInt()
                strokePaint.strokeWidth = wArc
                strokePaint.alpha = (0.88f * alpha * 255f).toInt()
                drawArcDeg(c, 214f, a, b)

                strokePaint.color = 0xFFE8FEFF.toInt()
                strokePaint.strokeWidth = 1.2f
                strokePaint.alpha = (0.92f * alpha * 255f).toInt()
                drawArcDeg(c, 214f, a, b)
            }
        }

        drawRotatedRing(c, state, "dDots", e, ek, motion) { alpha ->
            strokePaint.pathEffect = dashDotsMain
            strokePaint.color = 0xFF7FD8FF.toInt()
            strokePaint.strokeWidth = 3.6f
            strokePaint.alpha = (0.85f * alpha * 255f).toInt()
            c.drawCircle(0f, 0f, 136f, strokePaint)

            strokePaint.pathEffect = dashDotsSub
            strokePaint.color = 0xFFFFD56B.toInt()
            strokePaint.strokeWidth = 1.8f
            strokePaint.alpha = (0.72f * alpha * 255f).toInt()
            c.drawCircle(0f, 0f, 127f, strokePaint)

            strokePaint.pathEffect = null
            strokePaint.color = 0xFF3BA9FF.toInt()
            strokePaint.strokeWidth = 2f
            strokePaint.alpha = (0.72f * alpha * 255f).toInt()
            drawArcDeg(c, 146f, 30f, 90f)
            strokePaint.color = 0xFFF5B82E.toInt()
            drawArcDeg(c, 146f, 200f, 250f)
        }

        drawRotatedRing(c, state, "dSweep", e, ek, motion) { alpha ->
            strokePaint.pathEffect = null
            strokePaint.strokeCap = Paint.Cap.ROUND
            strokePaint.color = 0xFF8FF4FF.toInt()
            strokePaint.strokeWidth = 2.4f
            strokePaint.alpha = (0.78f * alpha * 255f).toInt()
            drawArcDeg(c, 250f, 0f, 22f)
            strokePaint.color = 0xFFFFDF7E.toInt()
            strokePaint.alpha = (0.75f * alpha * 255f).toInt()
            drawArcDeg(c, 250f, 180f, 198f)
            strokePaint.strokeCap = Paint.Cap.BUTT
        }

        // 5. Top and bottom calibration wedge pointers (gold & cyan)
        drawWedgePointers(c)

        // 6. Orbiting energy sparkles (gentle fade-in from early boot)
        val spBase = smoothstepLocal(0.04f, 0.55f, e)
        if (spBase > 0.005f) {
            for (s in state.sparkles) {
                val rad = s.angle * (PI.toFloat() / 180f)
                val sx = s.radius * sin(rad)
                val sy = -s.radius * cos(rad)
                val twinkle = 0.25f + 0.75f * abs(sin(s.phase + state.elapsedSec * s.freq))
                fillPaint.color = if (s.isGold) 0xFFFFDF7E.toInt() else 0xFFA8F0FF.toInt()
                fillPaint.alpha = (spBase * twinkle * 255f).toInt().coerceIn(0, 255)
                c.drawCircle(sx, sy, s.size, fillPaint)
            }
        }

        // 7. Halo ring around core (r=128)
        fillPaint.shader = haloGShader
        fillPaint.alpha = 235
        c.drawCircle(0f, 0f, 128f, fillPaint)
        fillPaint.shader = null

        // 8. Core sphere (r=88) + clipped spinning plasma filaments
        fillPaint.shader = coreGShader
        fillPaint.alpha = 255
        c.drawCircle(0f, 0f, 88f, fillPaint)
        fillPaint.shader = null

        c.save()
        clipPath.reset()
        clipPath.addCircle(0f, 0f, 87f, Path.Direction.CW)
        c.clipPath(clipPath)

        // Filament layer 1 (electric cyan plasma)
        c.save()
        c.rotate(state.filamentAngle1)
        strokePaint.pathEffect = null
        strokePaint.color = 0xFF63C6FF.toInt()
        strokePaint.strokeWidth = 1.7f
        strokePaint.alpha = 95
        for (i in 0 until 6) {
            c.save()
            c.rotate(i * 30f)
            val rx = 76f - i * 3f
            val ry = 20f + i * 5f
            arcRect.set(-rx, -ry, rx, ry)
            c.drawOval(arcRect, strokePaint)
            c.restore()
        }
        c.restore()

        // Filament layer 2 (golden-white hot inner filaments)
        c.save()
        c.rotate(state.filamentAngle2)
        strokePaint.color = 0xFFFFDF91.toInt()
        strokePaint.strokeWidth = 1.15f
        strokePaint.alpha = 78
        for (i in 0 until 3) {
            c.save()
            c.rotate(15f + i * 60f)
            arcRect.set(-58f, -16f, 58f, 16f)
            c.drawOval(arcRect, strokePaint)
            c.restore()
        }
        c.restore()
        c.restore()

        // 9. Breathing hot center core (r=62)
        val hs = 1f + (0.04f + 0.07f * e) * sin(state.hotPhase)
        c.save()
        c.scale(hs, hs)
        fillPaint.shader = hotGShader
        fillPaint.alpha = ((0.85f + 0.15f * e) * 255f).toInt().coerceIn(0, 255)
        c.drawCircle(0f, 0f, 62f, fillPaint)
        fillPaint.shader = null
        c.restore()

        // 10. Core rim & thin rings
        glowPaint.maskFilter = softBlur
        glowPaint.color = 0xFF00E5FF.toInt()
        glowPaint.strokeWidth = 2.0f
        glowPaint.alpha = 230
        c.drawCircle(0f, 0f, 88f, glowPaint)
        glowPaint.maskFilter = null

        strokePaint.color = 0xFFF5B82E.toInt()
        strokePaint.strokeWidth = 1.0f
        strokePaint.alpha = 115
        c.drawCircle(0f, 0f, 93f, strokePaint)

        strokePaint.color = 0xFF9FE0FF.toInt()
        strokePaint.strokeWidth = 0.85f
        strokePaint.alpha = 70
        c.drawCircle(0f, 0f, 70f, strokePaint)

        // 11. Anamorphic horizontal lens flare + vertical glint
        val flick = (if (motion > 0f) 1f else 0f) * e * 0.14f *
            abs(sin(state.elapsedSec * 23f) * sin(state.elapsedSec * 37.1f))
        val flareAlpha = ((0.85f + 0.15f * e - flick).coerceIn(0f, 1f) * 255f).toInt()
        c.save()
        c.scale(1f + 0.35f * e, 1f + 0.45f * e)
        drawScaledShaderEllipse(c, 380f, 2.5f, flareCyanShader, flareAlpha)
        drawScaledShaderEllipse(c, 220f, 4.2f, flareGoldShader, (flareAlpha * 0.72f).toInt())
        drawScaledShaderEllipse(c, 190f, 5f, flareCyanShader, flareAlpha)
        drawScaledShaderEllipse(c, 120f, 8f, flareCyanShader, (flareAlpha * 0.7f).toInt())
        drawScaledShaderEllipse(c, 78f, 13f, flareCyanShader, (flareAlpha * 0.55f).toInt())
        drawScaledShaderEllipse(c, 1.8f, 34f, flareCyanShader, (flareAlpha * 0.55f).toInt())
        drawScaledShaderEllipse(c, 12f, 12f, flareCyanShader, flareAlpha)
        c.restore()

        c.restore()
    }

    private fun drawOuterArcBrackets(c: Canvas, bloom: Float) {
        glowPaint.maskFilter = if (bloom > 0.6f) softBlur else null
        glowPaint.color = 0xFF00E5FF.toInt()
        glowPaint.strokeWidth = 1.4f
        glowPaint.alpha = 185
        drawArcDeg(c, 298f, 225f, 315f, glowPaint)
        drawArcDeg(c, 298f, 45f, 135f, glowPaint)

        glowPaint.color = 0xFFF5B82E.toInt()
        glowPaint.strokeWidth = 1.0f
        glowPaint.alpha = 120
        drawArcDeg(c, 283f, 242f, 298f, glowPaint)
        drawArcDeg(c, 283f, 62f, 118f, glowPaint)
        glowPaint.maskFilter = null
    }

    private fun drawWedgePointers(c: Canvas) {
        fillPaint.shader = null
        // Top triangle at (0, -132) -> (6, -122) -> (-6, -122)
        fillPaint.color = 0xFFF5B82E.toInt()
        fillPaint.alpha = 240
        triPath.reset()
        triPath.moveTo(0f, -132f)
        triPath.lineTo(6.5f, -121f)
        triPath.lineTo(-6.5f, -121f)
        triPath.close()
        c.drawPath(triPath, fillPaint)

        // Bottom triangle at (0, +140) -> (6, +130) -> (-6, +130)
        fillPaint.color = 0xFF00E5FF.toInt()
        triPath.reset()
        triPath.moveTo(0f, 140f)
        triPath.lineTo(6.5f, 129f)
        triPath.lineTo(-6.5f, 129f)
        triPath.close()
        c.drawPath(triPath, fillPaint)
    }

    private inline fun drawRotatedRing(
        c: Canvas,
        state: BootReactorState,
        id: String,
        e: Float,
        ek: Float,
        motion: Float,
        drawContent: (alpha: Float) -> Unit
    ) {
        val spec = state.rings.first { it.id == id }
        val baseOp = spec.opacityFn(e).coerceIn(0f, 1f)
        if (baseOp <= 0.004f) return

        val v = (spec.v0 + (spec.v1 - spec.v0) * ek) * motion
        val trailAmt = smoothstepLocal(18f, 70f, v)
        if (spec.trails > 0 && trailAmt > 0.01f) {
            for (k in spec.trails - 1 downTo 0) {
                val trailAngle = spec.angle - spec.dir * v * 0.035f * (k + 1)
                val trailAlpha = baseOp * trailAmt * (if (k > 0) 0.16f else 0.38f)
                c.save()
                c.rotate(trailAngle)
                drawContent(trailAlpha)
                c.restore()
            }
        }
        c.save()
        c.rotate(spec.angle)
        drawContent(baseOp)
        c.restore()
    }

    private fun drawArcDeg(
        c: Canvas,
        radius: Float,
        startFromTopDeg: Float,
        endFromTopDeg: Float,
        paint: Paint = strokePaint
    ) {
        arcRect.set(-radius, -radius, radius, radius)
        val startAngle = startFromTopDeg - 90f
        val sweepAngle = endFromTopDeg - startFromTopDeg
        c.drawArc(arcRect, startAngle, sweepAngle, false, paint)
    }

    private fun drawScaledShaderEllipse(
        c: Canvas,
        rx: Float,
        ry: Float,
        shader: RadialGradient,
        alpha: Int
    ) {
        if (alpha <= 1) return
        shaderMatrix.reset()
        shaderMatrix.setScale(rx, ry)
        shader.setLocalMatrix(shaderMatrix)
        fillPaint.shader = shader
        fillPaint.alpha = alpha
        arcRect.set(-rx, -ry, rx, ry)
        c.drawOval(arcRect, fillPaint)
        fillPaint.shader = null
    }

    private fun smoothstepLocal(a: Float, b: Float, x: Float): Float {
        val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}

@Composable
fun BootReactorCanvas(
    state: BootReactorState,
    frameTick: Long,
    scaleMult: Float,
    bloomMult: Float,
    speedMult: Float,
    autoRotate: Boolean,
    modifier: Modifier = Modifier
) {
    val painter = androidx.compose.runtime.remember { BootReactorPainter() }
    androidx.compose.foundation.Canvas(modifier = modifier) {
        @Suppress("UNUSED_VARIABLE")
        val tick = frameTick
        if (size.width <= 1f || size.height <= 1f) return@Canvas
        drawIntoCanvas { canvas ->
            painter.draw(
                c = canvas.nativeCanvas,
                w = size.width,
                h = size.height,
                state = state,
                scaleMult = scaleMult,
                bloomMult = bloomMult,
                speedMult = speedMult,
                autoRotate = autoRotate
            )
        }
    }
}
