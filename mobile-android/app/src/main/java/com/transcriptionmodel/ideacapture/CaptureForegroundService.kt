package com.transcriptionmodel.ideacapture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class CaptureForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var transcriber: AndroidSpeechTranscriber? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Idea capture", NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START -> startCapture()
            STOP -> stopCapture()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        transcriber?.destroy()
        scope.cancel()
        if (session.isRecording ||
            (session.status == CaptureStatus.Structuring && savedNoteId == null)
        ) {
            session = CaptureSession(
                status = CaptureStatus.Failed,
                errorMessage = "Capture stopped unexpectedly. No note was saved.",
            )
        }
        super.onDestroy()
    }

    private fun startCapture() {
        if (!session.isRecording || transcriber != null) return
        try {
            startForeground(NOTIFICATION_ID, notification())
            val createdTranscriber = AndroidSpeechTranscriber(
                applicationContext,
                onPartialTranscript = { partial ->
                    if (session.isRecording) session = session.copy(
                        partialTranscript = transcriptContinuation(session.committedTranscript, partial),
                    )
                },
                onFinalTranscript = { final ->
                    if (session.isRecording) session = session.copy(
                        committedTranscript = appendTranscript(session.committedTranscript, final),
                        partialTranscript = "",
                    )
                },
                onErrorMessage = ::fail,
            )
            transcriber = createdTranscriber
            createdTranscriber.start()
        } catch (_: RuntimeException) {
            fail("Capture could not start. No note was saved.")
        }
    }

    private fun stopCapture() {
        val active = transcriber ?: return
        if (!session.isRecording) return
        val snapshot = session
        val duration = System.currentTimeMillis() -
            (snapshot.startedAtMillis ?: System.currentTimeMillis())
        session = snapshot.copy(status = CaptureStatus.Structuring, partialTranscript = "")
        active.stopAndGetPendingTranscript { result ->
            result.dispatch(
                isContinuation = false,
                onFailure = ::fail,
                onTranscript = { pending ->
                    val transcript = assembleStoppedTranscript(
                        snapshot.committedTranscript, snapshot.partialTranscript, pending,
                    )
                    active.destroy()
                    transcriber = null
                    if (transcript.isBlank() || transcript.isPlaceholderCaptureTranscript()) {
                        pendingConfirmation = PendingCaptureConfirmation(transcript, duration)
                        session = CaptureSession(status = CaptureStatus.AwaitingConfirmation)
                        finish()
                    } else {
                        save(transcript, duration)
                    }
                },
            )
        }
    }

    private fun save(transcript: String, duration: Long) {
        scope.launch {
            try {
                val note = Note(
                    rawTranscript = transcript,
                    structured = structureTranscript(transcript),
                    durationMillis = duration,
                )
                saveCapturedNote(applicationContext, note)
                savedNoteId = note.id
                session = CaptureSession(status = CaptureStatus.Structured)
            } catch (_: Exception) {
                session = CaptureSession(
                    status = CaptureStatus.Failed,
                    errorMessage = "Capture could not be saved. No note was saved.",
                )
            } finally {
                finish()
            }
        }
    }

    private fun fail(message: String) {
        transcriber?.destroy()
        transcriber = null
        session = CaptureSession(status = CaptureStatus.Failed, errorMessage = message)
        finish()
    }

    private fun finish() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(): Notification {
        val stop = PendingIntent.getService(
            this, 0, Intent(this, javaClass).setAction(STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("Capturing your idea")
            .setContentText("Tap Stop to save.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(android.R.drawable.ic_media_pause, "Stop", stop).build(),
            )
            .build()
    }

    companion object {
        private const val START = "com.transcriptionmodel.ideacapture.START_CAPTURE"
        private const val STOP = "com.transcriptionmodel.ideacapture.STOP_CAPTURE"
        private const val CHANNEL = "idea_capture_recording"
        private const val NOTIFICATION_ID = 1001
        private val confirmationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        var session by mutableStateOf(CaptureSession())
            private set
        internal var pendingConfirmation by mutableStateOf<PendingCaptureConfirmation?>(null)
            private set
        var savedNoteId by mutableStateOf<String?>(null)
            private set

        fun start(context: Context) {
            if (session.isRecording || session.status == CaptureStatus.Structuring) return
            pendingConfirmation = null
            savedNoteId = null
            session = CaptureSession(
                status = CaptureStatus.Recording,
                startedAtMillis = System.currentTimeMillis(),
            )
            try {
                ContextCompat.startForegroundService(
                    context, Intent(context, CaptureForegroundService::class.java).setAction(START),
                )
            } catch (_: RuntimeException) {
                session = CaptureSession(
                    status = CaptureStatus.Failed,
                    errorMessage = "Capture could not start. No note was saved.",
                )
            }
        }

        fun stop(context: Context) {
            if (session.isRecording) context.startService(
                Intent(context, CaptureForegroundService::class.java).setAction(STOP),
            )
        }

        fun confirm(context: Context) {
            val pending = pendingConfirmation ?: return
            pendingConfirmation = null
            session = CaptureSession(status = CaptureStatus.Structuring)
            confirmationScope.launch {
                try {
                    val note = Note(
                        rawTranscript = pending.transcript,
                        structured = structureTranscript(pending.transcript),
                        durationMillis = pending.durationMillis,
                    )
                    saveCapturedNote(context, note)
                    savedNoteId = note.id
                    session = CaptureSession(status = CaptureStatus.Structured)
                } catch (_: Exception) {
                    session = CaptureSession(
                        status = CaptureStatus.Failed,
                        errorMessage = "Capture could not be saved. No note was saved.",
                    )
                }
            }
        }

        fun discard() {
            pendingConfirmation = null
            session = CaptureSession()
        }
    }
}
