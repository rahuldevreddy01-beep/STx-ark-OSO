package com.example.hardware

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class HudSoundEngine {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    @Volatile
    private var lastCueAtMs = 0L

    fun playDetentTick(enabled: Boolean, muted: Boolean) {
        if (!enabled || muted) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastCueAtMs < 48L) return
        lastCueAtMs = now
        playTone(freqHz = 1180.0, durationMs = 14, volume = 0.14f)
    }

    fun playSelectConfirm(enabled: Boolean, muted: Boolean) {
        if (!enabled || muted) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastCueAtMs < 30L) return
        lastCueAtMs = now
        playTone(freqHz = 1480.0, durationMs = 24, volume = 0.18f)
    }

    fun playBackCue(enabled: Boolean, muted: Boolean) {
        if (!enabled || muted) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastCueAtMs < 30L) return
        lastCueAtMs = now
        playTone(freqHz = 820.0, durationMs = 20, volume = 0.14f)
    }

    private fun playTone(freqHz: Double, durationMs: Int, volume: Float) {
        scope.launch {
            var track: AudioTrack? = null
            try {
                val sampleRate = 22050
                val minBytes = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(1024)
                val toneSamples = (sampleRate * durationMs) / 1000
                val totalSamples = maxOf(toneSamples, (minBytes / 2) + 1)
                val samples = ShortArray(totalSamples)
                for (i in 0 until toneSamples) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-i.toDouble() / (toneSamples * 0.35))
                    val value = sin(2.0 * PI * freqHz * t) * env * volume * Short.MAX_VALUE
                    samples[i] = value.toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                }
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(samples.size * 2)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
                delay(durationMs.toLong() + 18L)
            } catch (_: Throwable) {
                // Ignore audio hardware failures cleanly
            } finally {
                runCatching { track?.stop() }
                runCatching { track?.release() }
            }
        }
    }
}
