package com.transcriptionmodel.ideacapture

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IdeaCaptureApp()
        }
    }
}

private enum class AppTab(val label: String) {
    Capture("Capture"),
    Inbox("Inbox"),
}

@Composable
fun IdeaCaptureApp() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val context = LocalContext.current
            var selectedTab by remember { mutableStateOf(AppTab.Capture) }
            val sessionState = remember { mutableStateOf(CaptureSession()) }
            var session by sessionState
            var notes by remember { mutableStateOf(seedNotes()) }
            val speechTranscriber = remember {
                AndroidSpeechTranscriber(
                    context = context.applicationContext,
                    onPartialTranscript = { partialTranscript ->
                        val currentSession = sessionState.value
                        if (currentSession.isRecording) {
                            sessionState.value = currentSession.copy(
                                partialTranscript = partialTranscript,
                                errorMessage = null,
                            )
                        }
                    },
                    onFinalTranscript = { finalTranscript ->
                        val currentSession = sessionState.value
                        if (currentSession.isRecording) {
                            sessionState.value = currentSession.copy(
                                committedTranscript = appendTranscript(
                                    currentSession.committedTranscript,
                                    finalTranscript,
                                ),
                                partialTranscript = "",
                                errorMessage = null,
                            )
                        }
                    },
                    onErrorMessage = { message ->
                        val currentSession = sessionState.value
                        if (currentSession.isRecording) {
                            sessionState.value = currentSession.copy(errorMessage = message)
                        }
                    },
                )
            }

            DisposableEffect(speechTranscriber) {
                onDispose {
                    speechTranscriber.destroy()
                }
            }

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        AppTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                label = { Text(tab.label) },
                                icon = { Text(if (tab == AppTab.Capture) "🎙️" else "🗂️") },
                            )
                        }
                    }
                },
            ) { innerPadding ->
                when (selectedTab) {
                    AppTab.Capture -> CaptureScreen(
                        session = session,
                        notesCount = notes.size,
                        modifier = Modifier.padding(innerPadding),
                        onStart = {
                            if (!hasCapturePermissions(context)) {
                                requestCapturePermissionsIfNeeded(context)
                                session = CaptureSession(
                                    status = CaptureStatus.Failed,
                                    errorMessage = "Microphone permission is required. Grant it, then tap Start again.",
                                )
                            } else {
                                context.startForegroundCaptureService()
                                session = CaptureSession(
                                    status = CaptureStatus.Recording,
                                    startedAtMillis = System.currentTimeMillis(),
                                )
                                speechTranscriber.start()
                            }
                        },
                        onStop = {
                            speechTranscriber.stop()
                            context.stopService(Intent(context, CaptureForegroundService::class.java))
                            val rawTranscript = appendTranscript(
                                session.committedTranscript,
                                session.partialTranscript,
                            ).ifBlank { "Quick idea captured from the prototype." }
                            val startedAt = session.startedAtMillis ?: System.currentTimeMillis()
                            val note = Note(
                                rawTranscript = rawTranscript,
                                structured = structureTranscript(rawTranscript),
                                durationMillis = System.currentTimeMillis() - startedAt,
                            )
                            notes = listOf(note) + notes
                            session = CaptureSession(status = CaptureStatus.Structured)
                            selectedTab = AppTab.Inbox
                        },
                    )

                    AppTab.Inbox -> InboxScreen(
                        notes = notes,
                        modifier = Modifier.padding(innerPadding),
                        onStartCapture = { selectedTab = AppTab.Capture },
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureScreen(
    session: CaptureSession,
    notesCount: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text(
                text = "Idea Capture",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Tap once, speak naturally, and let the app save a structured note.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = when (session.status) {
                            CaptureStatus.Recording -> "Recording live"
                            CaptureStatus.Structured -> "Last capture saved"
                            CaptureStatus.Failed -> "Speech recognition needs attention"
                            else -> "Ready to capture"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = if (session.isRecording) onStop else onStart,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 42.dp, vertical = 28.dp),
                    ) {
                        Text(if (session.isRecording) "Stop" else "Start", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("$notesCount saved notes")
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Live transcript", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = session.liveTranscript.ifBlank { "Your words will appear here as you speak." },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    session.errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Speech recognition uses Android SpeechRecognizer. Speak after tapping Start, then tap Stop to save the recognized transcript into the inbox.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InboxScreen(
    notes: List<Note>,
    onStartCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Inbox", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Recent structured notes")
                }
                OutlinedButton(onClick = onStartCapture) {
                    Text("New")
                }
            }
        }

        if (notes.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("No notes yet", style = MaterialTheme.typography.titleMedium)
                        Text("Start a capture to create your first idea note.")
                    }
                }
            }
        }

        items(notes, key = { it.id }) { note ->
            NoteCard(note = note)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteCard(note: Note) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(note.structured.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("${note.displayTime} • ${note.durationSeconds}s", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(note.structured.summary, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                note.structured.tags.forEach { tag ->
                    AssistChip(onClick = {}, label = { Text(tag) })
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Raw transcript", fontWeight = FontWeight.SemiBold)
                Text(note.rawTranscript)
                if (note.structured.actionItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Action items", fontWeight = FontWeight.SemiBold)
                    note.structured.actionItems.forEach { item -> Text("• ${item.text}") }
                }
                TextButton(onClick = { expanded = false }) {
                    Text("Collapse")
                }
            }
        }
    }
}

private fun requestCapturePermissionsIfNeeded(context: Context) {
    if (context !is MainActivity) return

    val permissions = capturePermissions()
        .filter { permission -> context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED }

    if (permissions.isNotEmpty()) {
        context.requestPermissions(permissions.toTypedArray(), 42)
    }
}

private fun hasCapturePermissions(context: Context): Boolean = capturePermissions()
    .all { permission -> context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED }

private fun capturePermissions(): List<String> = buildList {
    add(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private fun Context.startForegroundCaptureService() {
    val intent = Intent(this, CaptureForegroundService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}

private fun appendTranscript(committed: String, partial: String): String =
    listOf(committed, partial)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .trim()

private fun seedNotes(): List<Note> {
    val transcript = "Build an idea capture app that starts recording in one tap. Remember to keep the raw transcript separate from the structured summary."
    return listOf(
        Note(
            rawTranscript = transcript,
            structured = structureTranscript(transcript),
            durationMillis = 18_000L,
        ),
    )
}
