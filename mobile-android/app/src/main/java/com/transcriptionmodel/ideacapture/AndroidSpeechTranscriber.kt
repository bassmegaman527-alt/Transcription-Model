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
    private var speechRecognizer: SpeechRecognizer? = null
    private var shouldKeepListening = false
    private var isStartPending = false

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onErrorMessage("Speech recognition is not available on this device.")
            return
        }

        shouldKeepListening = true
        ensureRecognizer()
        startListening()
    }

    fun stop() {
        shouldKeepListening = false
        isStartPending = false
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        shouldKeepListening = false
        isStartPending = false
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
                    isStartPending = false
                    onErrorMessage(error.toSpeechRecognizerMessage())
                    restartIfNeeded(error)
                }

                override fun onResults(results: Bundle?) {
                    isStartPending = false
                    results.bestRecognitionResult()?.let(onFinalTranscript)
                    restartIfNeeded()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults.bestRecognitionResult()?.let(onPartialTranscript)
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

    private fun restartIfNeeded(error: Int? = null) {
        if (!shouldKeepListening) return
        if (error != null && error !in recoverableErrors) {
            shouldKeepListening = false
            return
        }

        mainHandler.postDelayed(
            { startListening() },
            RESTART_DELAY_MS,
        )
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

    private fun Int.toSpeechRecognizerMessage(): String = when (this) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please try again."
        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition stopped. Tap Start to try again."
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

        val recoverableErrors = setOf(
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        )
    }
}
