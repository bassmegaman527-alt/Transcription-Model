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
    private val pendingStop = PendingSpeechStop()

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

    fun stopAndGetPendingTranscript(onTranscriptReady: (SpeechStopResult) -> Unit) {
        if (!pendingStop.begin(onTranscriptReady)) return

        shouldKeepListening = false
        isStartPending = false
        mainHandler.removeCallbacks(restartListeningRunnable)
        mainHandler.removeCallbacks(stopResultTimeoutRunnable)
        speechRecognizer?.stopListening()
        mainHandler.postDelayed(stopResultTimeoutRunnable, STOP_RESULT_TIMEOUT_MS)
    }

    fun cancel() {
        shouldKeepListening = false
        isStartPending = false
        latestPartialTranscript = ""
        pendingStop.cancel()
        mainHandler.removeCallbacks(restartListeningRunnable)
        mainHandler.removeCallbacks(stopResultTimeoutRunnable)
        speechRecognizer?.cancel()
    }

    fun destroy() {
        cancel()
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

                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    mainHandler.removeCallbacks(restartListeningRunnable)
                    isStartPending = false
                    if (pendingStop.isPending) {
                        completePendingStop("", error)
                        return
                    }
                    if (error in recoverableErrors) {
                        commitLatestPartialForRestart()
                        restartIfNeeded(error)
                        return
                    }

                    shouldKeepListening = false
                    onErrorMessage(error.toSpeechRecognizerMessage())
                }

                override fun onResults(results: Bundle?) {
                    mainHandler.removeCallbacks(restartListeningRunnable)
                    isStartPending = false
                    val finalTranscript = results?.bestRecognitionResult().orEmpty()
                    finalTranscript.takeIf { it.isNotBlank() }?.let(onFinalTranscript)
                    if (pendingStop.isPending) {
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

    private fun completePendingStop(finalTranscript: String, error: Int? = null) {
        if (!pendingStop.isPending) return
        mainHandler.removeCallbacks(stopResultTimeoutRunnable)
        val partialTranscript = latestPartialTranscript
        latestPartialTranscript = ""
        pendingStop.complete(finalTranscript, partialTranscript, error)
    }

    private fun commitLatestPartialForRestart() {
        val partialTranscript = latestPartialTranscript
        latestPartialTranscript = ""
        partialTranscript.takeIf { it.isNotBlank() }?.let(onFinalTranscript)
    }

    private fun restartIfNeeded(error: Int? = null) {
        if (!shouldKeepListening) return
        if (error != null && error !in recoverableErrors) {
            shouldKeepListening = false
            return
        }

        val restartDelayMillis = if (error == null) {
            RESULT_RESTART_DELAY_MS
        } else {
            RECOVERABLE_ERROR_RESTART_DELAY_MS
        }
        scheduleRestartIfNeeded(restartDelayMillis)
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
    }

    private fun Bundle.bestRecognitionResult(): String? = getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    private companion object {
        const val RESULT_RESTART_DELAY_MS = 0L
        const val RECOVERABLE_ERROR_RESTART_DELAY_MS = 250L
        const val STOP_RESULT_TIMEOUT_MS = 1_500L
    }
}

// Retain fallback text for ordinary captures while carrying fatal errors to continuations.
data class SpeechStopResult(val transcript: String, val fatalErrorMessage: String? = null) {
    internal fun dispatch(
        isContinuation: Boolean,
        onFailure: (String) -> Unit,
        onTranscript: (String) -> Unit,
    ) {
        if (isContinuation && fatalErrorMessage != null) {
            onFailure(fatalErrorMessage)
        } else {
            onTranscript(transcript)
        }
    }
}

// The recognizer result, error, and timeout compete for this same one-shot callback.
internal class PendingSpeechStop {
    private var callback: ((SpeechStopResult) -> Unit)? = null
    val isPending: Boolean get() = callback != null

    fun begin(onComplete: (SpeechStopResult) -> Unit): Boolean {
        if (isPending) return false
        callback = onComplete
        return true
    }

    fun cancel() {
        callback = null
    }

    fun complete(finalTranscript: String, partialTranscript: String = "", error: Int? = null) {
        val onComplete = callback ?: return
        callback = null
        onComplete(
            SpeechStopResult(
                transcript = finalTranscript.ifBlank { partialTranscript },
                fatalErrorMessage = error?.takeIf { it !in recoverableErrors }
                    ?.toSpeechRecognizerMessage(),
            ),
        )
    }
}

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

private val recoverableErrors = setOf(
    SpeechRecognizer.ERROR_CLIENT,
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
    SpeechRecognizer.ERROR_NO_MATCH,
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
)
