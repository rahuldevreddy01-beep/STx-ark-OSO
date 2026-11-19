package com.example.hardware

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

data class SpeechUiState(
    val ttsReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val pulseTick: Int = 0
)

/**
 * Real Android TextToSpeech driver for the STX AI holographic avatar.
 * Tracks utterance start, word-boundary range callbacks (for real-time lip sync),
 * and completion/stop events cleanly across threads.
 */
class StxSpeechEngine(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val utteranceCounter = AtomicInteger(0)

    private var tts: TextToSpeech? = null
    @Volatile
    private var initStarted = false
    @Volatile
    private var destroyed = false
    @Volatile
    private var pendingSpeech: String? = null

    private val _speechState = MutableStateFlow(SpeechUiState())
    val speechState: StateFlow<SpeechUiState> = _speechState.asStateFlow()

    fun warmUp() {
        if (destroyed || initStarted) return
        ensureInitialized()
    }

    fun speak(rawText: String) {
        if (destroyed) return
        val clean = sanitizeForSpeech(rawText)
        if (clean.isBlank()) return

        mainHandler.post {
            if (destroyed) return@post
            if (!_speechState.value.ttsReady || tts == null) {
                pendingSpeech = clean
                ensureInitialized()
                return@post
            }
            dispatchSpeak(clean)
        }
    }

    fun stop() {
        pendingSpeech = null
        mainHandler.post {
            runCatching { tts?.stop() }
            _speechState.update { it.copy(isSpeaking = false) }
        }
    }

    fun destroy() {
        destroyed = true
        pendingSpeech = null
        mainHandler.post {
            runCatching { tts?.stop() }
            runCatching { tts?.shutdown() }
            tts = null
            _speechState.value = SpeechUiState()
        }
    }

    private fun ensureInitialized() {
        if (destroyed || initStarted) return
        initStarted = true
        runCatching {
            var engineRef: TextToSpeech? = null
            engineRef = TextToSpeech(context.applicationContext) { status ->
                mainHandler.post {
                    if (destroyed) {
                        runCatching { engineRef?.shutdown() }
                        return@post
                    }
                    if (status == TextToSpeech.SUCCESS) {
                        configureEngine(engineRef)
                        _speechState.update { it.copy(ttsReady = true) }
                        val queued = pendingSpeech
                        pendingSpeech = null
                        if (!queued.isNullOrBlank()) {
                            dispatchSpeak(queued)
                        }
                    } else {
                        initStarted = false
                        _speechState.update { it.copy(ttsReady = false, isSpeaking = false) }
                    }
                }
            }
            tts = engineRef
        }.onFailure {
            initStarted = false
        }
    }

    private fun configureEngine(engine: TextToSpeech?) {
        if (engine == null) return
        runCatching {
            engine.language = Locale.US
            engine.setPitch(1.08f)
            engine.setSpeechRate(1.02f)

            // Prefer a natural female English voice if present on device
            val voices = engine.voices
            val preferred = voices?.firstOrNull { voice ->
                val name = voice.name.lowercase(Locale.US)
                voice.locale.language == "en" &&
                    !voice.isNetworkConnectionRequired &&
                    (name.contains("female") ||
                        name.contains("sfg") ||
                        name.contains("tpf") ||
                        name.contains("iob") ||
                        name.contains("samantha") ||
                        name.contains("karen") ||
                        name.contains("tessa"))
            }
            if (preferred != null) {
                engine.voice = preferred
            }

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    if (destroyed) return
                    _speechState.update {
                        it.copy(isSpeaking = true, pulseTick = it.pulseTick + 1)
                    }
                }

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    if (destroyed) return
                    _speechState.update {
                        it.copy(isSpeaking = true, pulseTick = it.pulseTick + 1)
                    }
                }

                override fun onDone(utteranceId: String?) {
                    if (destroyed) return
                    _speechState.update { it.copy(isSpeaking = false) }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (destroyed) return
                    _speechState.update { it.copy(isSpeaking = false) }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (destroyed) return
                    _speechState.update { it.copy(isSpeaking = false) }
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    if (destroyed) return
                    _speechState.update { it.copy(isSpeaking = false) }
                }
            })
        }
    }

    private fun dispatchSpeak(text: String) {
        val engine = tts ?: return
        val id = "stx_utter_${utteranceCounter.incrementAndGet()}"
        val res = runCatching {
            engine.speak(text.take(360), TextToSpeech.QUEUE_FLUSH, null, id)
        }.getOrDefault(TextToSpeech.ERROR)
        if (res == TextToSpeech.SUCCESS) {
            _speechState.update { it.copy(isSpeaking = true, pulseTick = it.pulseTick + 1) }
        }
    }

    private fun sanitizeForSpeech(input: String): String {
        return input
            .replace(Regex("\\*+|#+|_+`+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
