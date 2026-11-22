package com.example.hardware

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.example.R
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Decodes and caches the STX hologram portrait bitmap with exterior black background keyed out
 * into a clean translucent cyan cutout for both the system overlay service and STX AI screen.
 */
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

            val isFringe = BooleanArray(total)
            for (y in 1 until h - 1) {
                val rowOff = y * w
                for (x in 1 until w - 1) {
                    val idx = rowOff + x
                    if (!isBg[idx] && (isBg[idx - 1] || isBg[idx + 1] || isBg[idx - w] || isBg[idx + w])) {
                        isFringe[idx] = true
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

                    var alphaF = when {
                        isFringe[idx] -> ((maxRgb - 22f) / 72f).coerceIn(0f, 0.78f)
                        maxRgb < 40 -> (0.28f + (maxRgb / 40f) * 0.34f)
                        else -> (0.58f + ((maxRgb - 40f) / 215f) * 0.40f)
                    }
                    alphaF = (alphaF * bottomFade).coerceIn(0f, 0.96f)

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
