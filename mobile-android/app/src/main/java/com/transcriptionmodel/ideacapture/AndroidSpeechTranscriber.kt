package com.transcriptionmodel.ideacapture

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class AndroidSpeechTranscriber(
    private val context: Context,
    private val onPartialTranscript: (String) -> Unit,
    private val onFinalTranscript: (String) -> Unit,
    private val onErrorMessage: (String) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val restartListeningRunnable = Runnable { startListening() }
    private val stopResultTimeoutRunnable = Runnable { completePendingStop("") }
    private var speechRecognizer: SpeechRecognizer? = null
    private var shouldKeepListening = false
    private var isStartPending = false
    private var latestPartialTranscript = ""
    private var pendingStopCallback: ((String) -> Unit)? = null

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onErrorMessage("Speech recognition is not available on this device.")
            return
        }

        latestPartialTranscript = ""
        shouldKeepListening = true
        ensureRecognizer()
        startListening()
    }

    fun stopAndGetPendingTranscript(onTranscriptReady: (String) -> Unit) {
        shouldKeepListening = false
        isStartPending = false
        mainHandler.removeCallbacks(restartListeningRunnable)
        mainHandler.removeCallbacks(stopResultTimeoutRunnable)
        pendingStopCallback = onTranscriptReady
        speechRecognizer?.stopListening()
        mainHandler.postDelayed(stopResultTimeoutRunnable, STOP_RESULT_TIMEOUT_MS)
    }

    fun destroy() {
        shouldKeepListening = false
        isStartPending = false
        pendingStopCallback = null
        speechRecognizer?.destroy()
        speechRecognizer = null
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun ensureRecognizer() {
        if (speechRecognizer != null) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isStartPending = false
                }

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    scheduleRestartIfNeeded(END_OF_SPEECH_RESTART_DELAY_MS)
                }

                override fun onError(error: Int) {
                    mainHandler.removeCallbacks(restartListeningRunnable)
                    isStartPending = false
                    onErrorMessage(error.toSpeechRecognizerMessage())
                    if (pendingStopCallback != null) {
                        completePendingStop("")
                        return
                    }
                    restartIfNeeded(error)
                }

                override fun onResults(results: Bundle?) {
                    mainHandler.removeCallbacks(restartListeningRunnable)
                    isStartPending = false
                    val finalTranscript = results?.bestRecognitionResult().orEmpty()
                    finalTranscript.takeIf { it.isNotBlank() }?.let(onFinalTranscript)
                    if (pendingStopCallback != null) {
                        completePendingStop(finalTranscript)
                        return
                    }
                    latestPartialTranscript = ""
                    restartIfNeeded()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.bestRecognitionResult()?.let { partialTranscript ->
                        latestPartialTranscript = partialTranscript
                        onPartialTranscript(partialTranscript)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    private fun startListening() {
        if (!shouldKeepListening || isStartPending) return

        isStartPending = true
        speechRecognizer?.startListening(recognitionIntent())
    }

    private fun completePendingStop(finalTranscript: String) {
        val callback = pendingStopCallback ?: return
        pendingStopCallback = null
        mainHandler.removeCallbacks(stopResultTimeoutRunnable)
        val stoppedTranscript = finalTranscript.ifBlank { latestPartialTranscript }
        latestPartialTranscript = ""
        callback(stoppedTranscript)
    }

    private fun restartIfNeeded(error: Int? = null) {
        if (!shouldKeepListening) return
        if (error != null && error !in recoverableErrors) {
            shouldKeepListening = false
            return
        }

        scheduleRestartIfNeeded(RESTART_DELAY_MS)
    }

    private fun scheduleRestartIfNeeded(delayMillis: Long) {
        if (!shouldKeepListening) return

        mainHandler.removeCallbacks(restartListeningRunnable)
        mainHandler.postDelayed(restartListeningRunnable, delayMillis)
    }

    private fun recognitionIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, COMPLETE_SILENCE_MS)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, POSSIBLY_COMPLETE_SILENCE_MS)
    }

    private fun Bundle.bestRecognitionResult(): String? = getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    private fun Int.toSpeechRecognizerMessage(): String = when (this) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please try again."
        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition paused. Restarting."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for speech recognition."
        SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network timed out."
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized yet. Keep speaking."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Retrying."
        SpeechRecognizer.ERROR_SERVER -> "Speech recognition service error."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening for speech..."
        else -> "Speech recognition error $this."
    }

    private companion object {
        const val RESTART_DELAY_MS = 250L
        const val END_OF_SPEECH_RESTART_DELAY_MS = 2_000L
        const val COMPLETE_SILENCE_MS = 1_500L
        const val POSSIBLY_COMPLETE_SILENCE_MS = 750L
        const val STOP_RESULT_TIMEOUT_MS = 1_500L

        val recoverableErrors = setOf(
            SpeechRecognizer.ERROR_CLIENT,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        )
    }
}
