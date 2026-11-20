package com.example.hardware

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

data class VoiceUiState(
    val isListening: Boolean = false,
    val partialTranscript: String = "",
    val rmsNormalized: Float = 0f,
    val lastError: String? = null
)

class VoiceCommandManager(
    private val context: Context,
    private val onFinalTranscript: (String) -> Unit,
    private val onStatusMessage: (String) -> Unit,
    private val onFallbackIntentNeeded: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private val sessionCounter = AtomicInteger(0)
    @Volatile
    private var destroyed = false
    @Volatile
    private var lastToggleMs = 0L

    private val _voiceState = MutableStateFlow(VoiceUiState())
    val voiceState: StateFlow<VoiceUiState> = _voiceState.asStateFlow()

    fun toggleListening() {
        if (destroyed) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastToggleMs < 550L) return
        lastToggleMs = now
        if (_voiceState.value.isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun startListening() {
        if (destroyed) return
        mainHandler.post {
            if (destroyed) return@post
            if (!LauncherPermissionManager.hasAudioPermission(context)) {
                _voiceState.value = VoiceUiState(
                    isListening = false,
                    lastError = "GRANT MIC PERMISSION"
                )
                onStatusMessage("GRANT MICROPHONE PERMISSION")
                return@post
            }
            val sessionId = sessionCounter.incrementAndGet()

            val available = runCatching {
                SpeechRecognizer.isRecognitionAvailable(context)
            }.getOrDefault(false)

            if (!available) {
                _voiceState.value = VoiceUiState(
                    isListening = false,
                    lastError = "IN-APP SPEECH ENGINE UNAVAILABLE"
                )
                onFallbackIntentNeeded()
                return@post
            }

            try {
                runCatching { recognizer?.cancel() }
                runCatching { recognizer?.destroy() }
                recognizer = null

                val sr = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                recognizer = sr
                sr.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (destroyed || sessionCounter.get() != sessionId) return
                        _voiceState.value = VoiceUiState(
                            isListening = true,
                            partialTranscript = "LISTENING…"
                        )
                        onStatusMessage("STX AI MIC LISTENING…")
                    }

                    override fun onBeginningOfSpeech() {
                        if (destroyed || sessionCounter.get() != sessionId) return
                        _voiceState.value = _voiceState.value.copy(isListening = true)
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        if (destroyed || sessionCounter.get() != sessionId) return
                        val norm = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _voiceState.value = _voiceState.value.copy(rmsNormalized = norm)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() {
                        if (destroyed || sessionCounter.get() != sessionId) return
                        _voiceState.value = _voiceState.value.copy(rmsNormalized = 0f)
                    }

                    override fun onError(error: Int) {
                        if (destroyed || sessionCounter.get() != sessionId) return
                        val shouldTryDialogFallback = error == SpeechRecognizer.ERROR_CLIENT ||
                            error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                            error == 11 // ERROR_SERVER_DISCONNECTED (API 31+)

                        val reason = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "MIC AUDIO CAPTURE ERROR"
                            SpeechRecognizer.ERROR_CLIENT -> "MIC ENGINE RESET — TRY DIALOG"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "GRANT MIC PERMISSION"
                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "SPEECH NETWORK TIMEOUT"
                            SpeechRecognizer.ERROR_NO_MATCH -> "NO SPEECH DETECTED — TRY AGAIN"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "MIC ENGINE BUSY"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "MIC TIMEOUT — SPEAK CLEARLY"
                            else -> "VOICE LINK UNREADY ($error)"
                        }
                        _voiceState.value = VoiceUiState(isListening = false, lastError = reason)
                        runCatching { recognizer?.destroy() }
                        recognizer = null
                        if (shouldTryDialogFallback) {
                            onFallbackIntentNeeded()
                        } else {
                            onStatusMessage(reason)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        if (destroyed || sessionCounter.get() != sessionId) return
                        val text = runCatching {
                            results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                .orEmpty()
                                .trim()
                        }.getOrDefault("")
                        _voiceState.value = VoiceUiState(isListening = false)
                        if (text.isNotEmpty()) {
                            onFinalTranscript(text)
                        } else {
                            onStatusMessage("NO SPEECH DETECTED")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        if (destroyed || sessionCounter.get() != sessionId) return
                        val text = runCatching {
                            partialResults
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                .orEmpty()
                                .trim()
                        }.getOrDefault("")
                        if (text.isNotEmpty()) {
                            _voiceState.value = _voiceState.value.copy(partialTranscript = text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }
                sr.startListening(intent)
            } catch (_: Throwable) {
                _voiceState.value = VoiceUiState(isListening = false)
                runCatching { recognizer?.destroy() }
                recognizer = null
                onFallbackIntentNeeded()
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            sessionCounter.incrementAndGet()
            runCatching { recognizer?.stopListening() }
            _voiceState.value = _voiceState.value.copy(isListening = false, rmsNormalized = 0f)
        }
    }

    fun destroy() {
        destroyed = true
        sessionCounter.incrementAndGet()
        mainHandler.post {
            runCatching { recognizer?.cancel() }
            runCatching { recognizer?.destroy() }
            recognizer = null
        }
    }
}
