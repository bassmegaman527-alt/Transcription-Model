    package com.transcriptionmodel.ideacapture

    import android.Manifest
    import android.content.Context
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.os.Bundle
    import androidx.activity.compose.BackHandler
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.rememberLauncherForActivityResult
    import androidx.activity.compose.setContent
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.compose.foundation.background
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
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
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.lazy.rememberLazyListState
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.material3.AlertDialog
    import androidx.compose.material3.AssistChip
    import androidx.compose.material3.Button
    import androidx.compose.material3.Card
    import androidx.compose.material3.CardDefaults
    import androidx.compose.material3.ExperimentalMaterial3Api
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.NavigationBar
    import androidx.compose.material3.NavigationBarItem
    import androidx.compose.material3.OutlinedButton
    import androidx.compose.material3.OutlinedTextField
    import androidx.compose.material3.Scaffold
    import androidx.compose.material3.Surface
    import androidx.compose.material3.Text
    import androidx.compose.material3.TextButton
    import androidx.compose.material3.lightColorScheme
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.DisposableEffect
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableIntStateOf
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.rememberCoroutineScope
    import androidx.compose.runtime.setValue
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.unit.dp
    import androidx.datastore.preferences.core.edit
    import androidx.datastore.preferences.core.stringPreferencesKey
    import androidx.datastore.preferences.preferencesDataStore
    import java.io.IOException
    import java.text.DateFormat
    import java.util.Date
    import java.util.UUID
    import kotlinx.coroutines.flow.first
    import kotlinx.coroutines.launch
    import org.json.JSONArray
    import org.json.JSONObject

    private val Context.notesDataStore by preferencesDataStore(name = "idea_capture_notes")
    private val notesJsonKey = stringPreferencesKey("notes_json")
    private val whitespaceSeparator = Regex("\\s+")
    private const val ACTION_QUICK_CAPTURE = "com.transcriptionmodel.ideacapture.action.QUICK_CAPTURE"

    class QuickCaptureActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    action = ACTION_QUICK_CAPTURE
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    )
                },
            )
            finish()
        }
    }

    class MainActivity : ComponentActivity() {
        private var quickCaptureRequestId by mutableIntStateOf(0)

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            handleLaunchIntent(intent)
            setContent {
                IdeaCaptureApp(quickCaptureRequestId = quickCaptureRequestId)
            }
        }

        override fun onNewIntent(intent: Intent) {
            super.onNewIntent(intent)
            handleLaunchIntent(intent)
        }

        private fun handleLaunchIntent(incomingIntent: Intent) {
            if (incomingIntent.action == ACTION_QUICK_CAPTURE) {
                quickCaptureRequestId += 1
                setIntent(Intent(incomingIntent).apply { action = null })
            } else {
                setIntent(incomingIntent)
            }
        }
    }

    private enum class AppTab(val label: String, val icon: String) {
        Capture("Capture", "🎙️"),
        Inbox("Inbox", "🗂️"),
        About("About", "ℹ️"),
    }

    private data class PendingCaptureConfirmation(
        val transcript: String,
        val durationMillis: Long,
    )

    private data class ContinuationCaptureTarget(
        val attemptId: String,
        val noteId: String,
        val expectedDevelopmentContent: String,
    )

    @Composable
    fun IdeaCaptureApp(quickCaptureRequestId: Int = 0) {
        MaterialTheme(colorScheme = lightColorScheme()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                val context = LocalContext.current
                val appContext = context.applicationContext
                var selectedTab by remember { mutableStateOf(AppTab.Capture) }
                val sessionState = remember { mutableStateOf(CaptureSession()) }
                var session by sessionState
                var notes by remember { mutableStateOf(emptyList<Note>()) }
                var selectedNoteId by remember { mutableStateOf<String?>(null) }
                var continuationTarget by remember { mutableStateOf<ContinuationCaptureTarget?>(null) }
                var inboxSearchQuery by remember { mutableStateOf("") }
                var isSavingCapture by remember { mutableStateOf(false) }
                var isRequestingMicrophonePermission by remember { mutableStateOf(false) }
                var pendingCaptureConfirmation by remember { mutableStateOf<PendingCaptureConfirmation?>(null) }
                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(appContext) {
                    notes = loadSavedNotes(appContext)
                }

                val speechTranscriber = remember {
                    AndroidSpeechTranscriber(
                        context = appContext,
                        onPartialTranscript = { partialTranscript ->
                            val currentSession = sessionState.value
                            if (currentSession.isRecording) {
                                sessionState.value = currentSession.copy(
                                    partialTranscript = transcriptContinuation(
                                        currentSession.committedTranscript,
                                        partialTranscript,
                                    ),
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

                fun startSpeechCapture() {
                    isRequestingMicrophonePermission = false
                    isSavingCapture = false
                    pendingCaptureConfirmation = null
                    session = CaptureSession(
                        status = CaptureStatus.Recording,
                        startedAtMillis = System.currentTimeMillis(),
                    )
                    speechTranscriber.start()
                }

                fun saveCapture(transcript: String, durationMillis: Long) {
                    val note = Note(
                        rawTranscript = transcript,
                        sourceTranscript = transcript,
                        structured = structureTranscript(transcript),
                        durationMillis = durationMillis,
                    )
                    val updatedNotes = listOf(note) + notes
                    notes = updatedNotes
                    session = CaptureSession(status = CaptureStatus.Structuring)
                    coroutineScope.launch {
                        saveNotes(appContext, updatedNotes)
                        session = CaptureSession(status = CaptureStatus.Structured)
                        selectedTab = AppTab.Inbox
                    }
                    pendingCaptureConfirmation = null
                }

                val microphonePermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { isGranted ->
                    isRequestingMicrophonePermission = false
                    if (isGranted) {
                        startSpeechCapture()
                    } else {
                        session = CaptureSession(
                            status = CaptureStatus.Failed,
                            errorMessage = "Microphone permission is required. Grant it, then tap Start again.",
                        )
                    }
                }

                fun canStartSpeechCapture(): Boolean =
                        !session.isRecording &&
                            session.status != CaptureStatus.Structuring &&
                            session.status != CaptureStatus.AwaitingConfirmation &&
                            !isRequestingMicrophonePermission

                fun requestPermissionOrStartSpeechCapture() {
                    if (hasCapturePermissions(context)) {
                        startSpeechCapture()
                    } else {
                        isRequestingMicrophonePermission = true
                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                val requestOrStartNewSpeechCapture: () -> Unit = {
                    selectedTab = AppTab.Capture
                    if (canStartSpeechCapture()) {
                        continuationTarget = null
                        selectedNoteId = null
                        requestPermissionOrStartSpeechCapture()
                    }
                }

                val requestOrRetryContinuation: () -> Unit = {
                    selectedTab = AppTab.Capture
                    if (continuationTarget != null && canStartSpeechCapture()) {
                        requestPermissionOrStartSpeechCapture()
                    }
                }

                val requestOrStartContinuation: (String) -> Unit = { noteId ->
                    val targetNote = notes.firstOrNull { note -> note.id == noteId }
                    selectedTab = AppTab.Capture
                    if (targetNote != null && canStartSpeechCapture()) {
                        continuationTarget = ContinuationCaptureTarget(
                            attemptId = UUID.randomUUID().toString(),
                            noteId = targetNote.id,
                            expectedDevelopmentContent = targetNote.developmentContent,
                        )
                        selectedNoteId = targetNote.id
                        requestPermissionOrStartSpeechCapture()
                    }
                }

                LaunchedEffect(quickCaptureRequestId) {
                    if (quickCaptureRequestId > 0) {
                        requestOrStartNewSpeechCapture()
                    }
                }

                DisposableEffect(speechTranscriber) {
                    onDispose {
                        speechTranscriber.destroy()
                    }
                }

                pendingCaptureConfirmation?.let { pendingCapture ->
                    val isEmptyCapture = pendingCapture.transcript.isBlank()
                    AlertDialog(
                        onDismissRequest = {},
                        title = {
                            Text(if (isEmptyCapture) "No speech captured" else "Placeholder transcript detected")
                        },
                        text = {
                            Text(
                                if (isEmptyCapture) {
                                    "No speech was captured. Do you want to save an empty note or discard this capture?"
                                } else {
                                    "The captured text matches a prototype placeholder. Do you want to save it anyway or discard this capture?"
                                },
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (pendingCaptureConfirmation != null) {
                                        pendingCaptureConfirmation = null
                                        saveCapture(
                                            transcript = pendingCapture.transcript,
                                            durationMillis = pendingCapture.durationMillis,
                                        )
                                    }
                                },
                            ) {
                                Text(if (isEmptyCapture) "Save empty note" else "Save anyway")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    pendingCaptureConfirmation = null
                                    isSavingCapture = false
                                    session = CaptureSession()
                                    selectedTab = AppTab.Capture
                                },
                            ) {
                                Text("Discard")
                            }
                        },
                    )
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            AppTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = {
                                        selectedTab = tab
                                        if (tab != AppTab.Inbox) {
                                            selectedNoteId = null
                                        }
                                    },
                                    label = { Text(tab.label) },
                                    icon = { Text(tab.icon) },
                                )
                            }
                        }
                    },
                ) { innerPadding ->
                    when (selectedTab) {
                        AppTab.Capture -> CaptureScreen(
                            session = session,
                            notesCount = notes.size,
                            continuationTargetTitle = continuationTarget
                                ?.let { target -> notes.firstOrNull { note -> note.id == target.noteId } }
                                ?.structured
                                ?.title,
                            modifier = Modifier.padding(innerPadding),
                            onStart = if (continuationTarget == null) {
                                requestOrStartNewSpeechCapture
                            } else {
                                requestOrRetryContinuation
                            },
                            onStop = {
                                if (!isSavingCapture && session.isRecording) {
                                    isSavingCapture = true
                                    val activeContinuationTarget = continuationTarget
                                    val committedTranscript = session.committedTranscript
                                    val partialTranscript = session.partialTranscript
                                    val startedAt = session.startedAtMillis ?: System.currentTimeMillis()
                                    val durationMillis = System.currentTimeMillis() - startedAt
                                    session = session.copy(
                                        status = CaptureStatus.Structuring,
                                        partialTranscript = "",
                                    )
                                    speechTranscriber.stopAndGetPendingTranscript stop@ { pendingTranscript ->
                                        val rawTranscript = appendTranscript(
                                            committedTranscript,
                                            partialTranscript,
                                            pendingTranscript,
                                        )
                                        if (activeContinuationTarget != null) {
                                            if (continuationTarget?.attemptId != activeContinuationTarget.attemptId) {
                                                isSavingCapture = false
                                                return@stop
                                            }
                                            when (
                                                val result = applyVoiceContinuation(
                                                    notes = notes,
                                                    targetNoteId = activeContinuationTarget.noteId,
                                                    expectedDevelopmentContent =
                                                        activeContinuationTarget.expectedDevelopmentContent,
                                                    stoppedTranscript = rawTranscript,
                                                )
                                            ) {
                                                is ContinuationApplicationResult.Applied -> {
                                                    notes = result.notes
                                                    coroutineScope.launch {
                                                        saveNotes(appContext, result.notes)
                                                        continuationTarget = null
                                                        session = CaptureSession(status = CaptureStatus.Structured)
                                                        selectedNoteId = result.updatedNote.id
                                                        selectedTab = AppTab.Inbox
                                                    }
                                                }

                                                ContinuationApplicationResult.InvalidTranscript -> {
                                                    isSavingCapture = false
                                                    session = CaptureSession(
                                                        status = CaptureStatus.Failed,
                                                        errorMessage =
                                                            "No continuation was saved. Tap Start continuation to try again.",
                                                    )
                                                }

                                                ContinuationApplicationResult.TargetMissing -> {
                                                    isSavingCapture = false
                                                    session = CaptureSession(
                                                        status = CaptureStatus.Failed,
                                                        errorMessage =
                                                            "The target Idea is no longer available. No continuation was saved.",
                                                    )
                                                }

                                                ContinuationApplicationResult.TargetChanged -> {
                                                    isSavingCapture = false
                                                    session = CaptureSession(
                                                        status = CaptureStatus.Failed,
                                                        errorMessage =
                                                            "The target Idea changed. No continuation was saved.",
                                                    )
                                                }
                                            }
                                        } else if (
                                            rawTranscript.isBlank() ||
                                            rawTranscript.isPlaceholderCaptureTranscript()
                                        ) {
                                            session = session.copy(status = CaptureStatus.AwaitingConfirmation)
                                            pendingCaptureConfirmation = PendingCaptureConfirmation(
                                                transcript = rawTranscript,
                                                durationMillis = durationMillis,
                                            )
                                        } else {
                                            saveCapture(rawTranscript, durationMillis)
                                        }
                                    }
                                }
                            },
                        )

                        AppTab.Inbox -> InboxScreen(
                            notes = notes,
                            selectedNoteId = selectedNoteId,
                            onSelectedNoteIdChange = { selectedNoteId = it },
                            searchQuery = inboxSearchQuery,
                            onSearchQueryChange = { inboxSearchQuery = it },
                            onDeleteNote = { noteToDelete ->
                                val updatedNotes = notes.filterNot { it.id == noteToDelete.id }
                                notes = updatedNotes
                                coroutineScope.launch {
                                    saveNotes(appContext, updatedNotes)
                                }
                            },
                            onUpdateNote = { updatedNote ->
                                val updatedNotes = notes.map { note ->
                                    if (note.id == updatedNote.id) updatedNote else note
                                }
                                notes = updatedNotes
                                coroutineScope.launch {
                                    saveNotes(appContext, updatedNotes)
                                }
                            },
                            modifier = Modifier.padding(innerPadding),
                            onStartCapture = requestOrStartNewSpeechCapture,
                            onContinueByVoice = requestOrStartContinuation,
                        )

                        AppTab.About -> AboutScreen(
                            onDeleteAllNotes = {
                                notes = emptyList()
                                coroutineScope.launch {
                                    saveNotes(appContext, emptyList())
                                }
                            },
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
                } // Scaffold
            } // Surface
        } // MaterialTheme



    @Composable
    private fun AboutScreen(
        onDeleteAllNotes: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        var showDeleteAllConfirmation by remember { mutableStateOf(false) }

        if (showDeleteAllConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteAllConfirmation = false },
                title = { Text("Delete all notes?") },
                text = { Text("This removes all saved notes from this device. This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteAllConfirmation = false
                            onDeleteAllNotes()
                        },
                    ) {
                        Text("Delete all notes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllConfirmation = false }) {
                        Text("Cancel")
                    }
                },
            )
        }

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
                    text = "Capture spoken ideas and turn them into organized notes you can review and share.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Privacy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("• Notes are stored locally on this device.")
                        Text("• The app stores transcript and note text.")
                        Text("• The app does not intentionally store audio recordings.")
                        Text("• Speech recognition depends on Android SpeechRecognizer.")
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Saved data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Permanently remove every saved note from this device.")
                        OutlinedButton(
                            onClick = { showDeleteAllConfirmation = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Delete all notes", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CaptureScreen(
        session: CaptureSession,
        notesCount: Int,
        continuationTargetTitle: String?,
        onStart: () -> Unit,
        onStop: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val statusTitle = when {
            session.status == CaptureStatus.Recording && session.errorMessage != null -> "Listening interrupted"
            session.status == CaptureStatus.Recording -> "Listening now"
            session.status == CaptureStatus.Structuring -> "Processing recording"
            session.status == CaptureStatus.Saved || session.status == CaptureStatus.Structured -> "Saved to Inbox"
            session.status == CaptureStatus.Failed -> "Unable to start listening"
            session.status == CaptureStatus.AwaitingConfirmation -> "Review capture"
            else -> "Ready to capture"
        }
        val statusMessage = when (session.status) {
            CaptureStatus.Idle -> "Tap Start recording, then speak naturally."
            CaptureStatus.Recording -> session.errorMessage ?: "Speak naturally. Your words will appear below."
            CaptureStatus.Saved, CaptureStatus.Structured -> "Your note is safely saved and ready in the Inbox."
            CaptureStatus.Structuring -> "Finishing the transcript and saving your note..."
            CaptureStatus.AwaitingConfirmation -> "Choose whether to save or discard this capture."
            CaptureStatus.Failed -> session.errorMessage ?: "Speech recognition could not start. Please try again."
        }
        val isActivelyListening = session.isRecording && session.errorMessage == null
        val statusContainerColor = if (isActivelyListening) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
        val statusContentColor = if (isActivelyListening) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }

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
                    text = if (continuationTargetTitle == null) {
                        "Tap once, speak naturally, and let the app save a structured note."
                    } else {
                        "Speak an addition, then stop explicitly to add it to Development."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (continuationTargetTitle != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Continue by voice",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = continuationTargetTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("This recording will be added to this Idea's Development section.")
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = statusContainerColor,
                        contentColor = statusContentColor,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (isActivelyListening) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(MaterialTheme.colorScheme.error, CircleShape),
                                )
                                Text(
                                    text = "Recording",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (session.errorMessage != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                statusContentColor
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = if (session.isRecording) onStop else onStart,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = session.status != CaptureStatus.Structuring,
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 42.dp, vertical = 28.dp),
                        ) {
                            Text(
                                text = when {
                                    session.isRecording -> "Stop recording"
                                    continuationTargetTitle != null -> "Start continuation"
                                    else -> "Start recording"
                                },
                                style = MaterialTheme.typography.titleLarge,
                            )
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
                    }
                }
            }

            item {
                Text(
                    text = "Speech recognition uses Android SpeechRecognizer. Speak after tapping Start recording, then tap Stop recording to save the recognized transcript into the inbox.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun InboxScreen(
        notes: List<Note>,
        selectedNoteId: String?,
        onSelectedNoteIdChange: (String?) -> Unit,
        searchQuery: String,
        onSearchQueryChange: (String) -> Unit,
        onDeleteNote: (Note) -> Unit,
        onUpdateNote: (Note) -> Unit,
        onStartCapture: () -> Unit,
        onContinueByVoice: (String) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        var editingNoteId by remember { mutableStateOf<String?>(null) }
        val inboxListState = rememberLazyListState()
        val editingNote = notes.firstOrNull { note -> note.id == editingNoteId }
        val selectedNote = notes.firstOrNull { note -> note.id == selectedNoteId }

        if (editingNote != null) {
            NoteEditScreen(
                note = editingNote,
                onCancel = { editingNoteId = null },
                onSave = { updatedNote ->
                    editingNoteId = null
                    onUpdateNote(updatedNote)
                },
                modifier = modifier,
            )
            return
        }

        if (selectedNote != null) {
            NoteDetailScreen(
                note = selectedNote,
                onBack = { onSelectedNoteIdChange(null) },
                onEditNote = { editingNoteId = selectedNote.id },
                onContinueByVoice = { onContinueByVoice(selectedNote.id) },
                modifier = modifier,
            )
            return
        }

        val visibleNotes = notes.filterBySearchQuery(searchQuery)
        val inboxIsEmpty = notes.isEmpty()

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            state = inboxListState,
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
                        Text("New capture")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search notes") },
                    placeholder = { Text("Search title, source, development, or tags") },
                )
            }

            if (searchQuery.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (visibleNotes.size == 1) "1 result" else "${visibleNotes.size} results",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = { onSearchQueryChange("") }) {
                            Text("Clear search")
                        }
                    }
                }
            }

            if (visibleNotes.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = if (inboxIsEmpty) "No saved notes yet" else "No notes match your search",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                if (inboxIsEmpty) {
                                    "Capture and save an idea to create your first note. Saved notes will appear here."
                                } else {
                                    "Try fewer or different words, or clear the search to see all notes."
                                },
                            )
                            if (inboxIsEmpty) {
                                Button(
                                    onClick = {
                                        onSearchQueryChange("")
                                        onStartCapture()
                                    },
                                ) {
                                    Text("Start first capture")
                                }
                            }
                        }
                    }
                }
            }

            items(visibleNotes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    onOpenNote = { onSelectedNoteIdChange(note.id) },
                    onEditNote = { editingNoteId = note.id },
                    onDeleteNote = onDeleteNote,
                )
            }
        }
    }

    @Composable
    private fun NoteEditScreen(
        note: Note,
        onCancel: () -> Unit,
        onSave: (Note) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        var editableTitle by remember(note.id, note.structured.title) {
            mutableStateOf(note.structured.title)
        }
        var editableRawTranscript by remember(note.id, note.rawTranscript) {
            mutableStateOf(note.rawTranscript)
        }
        var editableDevelopmentContent by remember(note.id, note.developmentContent) {
            mutableStateOf(note.developmentContent)
        }
        val cleanedTitle = editableTitle.trim()
        val cleanedTranscript = editableRawTranscript.trim()
        val cleanedDevelopmentContent = editableDevelopmentContent.trim()
        val titleChanged = cleanedTitle != note.structured.title
        val transcriptChanged = cleanedTranscript != note.rawTranscript
        val developmentChanged = cleanedDevelopmentContent != note.developmentContent
        val hasChanges = titleChanged || transcriptChanged || developmentChanged
        val canSaveWithoutTranscript = note.rawTranscript.isBlank()
        val canSave = hasChanges && (cleanedTranscript.isNotBlank() || canSaveWithoutTranscript)

        BackHandler(onBack = onCancel)

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TextButton(onClick = onCancel) {
                    Text("Cancel editing")
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Edit note", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Update the title, transcript, or development, then save your changes.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(
                        text = "Saving transcript changes regenerates the summary, tags, and action items.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = editableTitle,
                        onValueChange = { editableTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Title") },
                    )
                    Text(
                        text = "Leave the title blank to use the title generated from the transcript.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = editableRawTranscript,
                        onValueChange = { editableRawTranscript = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Raw transcript") },
                        minLines = 10,
                        maxLines = 16,
                    )
                    if (editableRawTranscript.isBlank()) {
                        Text(
                            text = if (note.rawTranscript.isBlank()) {
                                "This note has no transcript. Add one or update the title to save."
                            } else {
                                "Transcript is required to save changes."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = editableDevelopmentContent,
                        onValueChange = { editableDevelopmentContent = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Development") },
                        placeholder = { Text("Add thoughts, context, or next steps") },
                        minLines = 6,
                        maxLines = 12,
                    )
                    Text(
                        text = "Your development is saved separately from the original capture.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val updatedStructure = if (titleChanged || transcriptChanged) {
                            val regeneratedStructure = structureTranscript(cleanedTranscript)
                            regeneratedStructure.copy(
                                title = cleanedTitle.ifBlank { regeneratedStructure.title },
                            )
                        } else {
                            note.structured
                        }
                        onSave(
                            note.copy(
                                rawTranscript = cleanedTranscript,
                                structured = updatedStructure,
                                developmentContent = cleanedDevelopmentContent,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canSave,
                ) {
                    Text("Save changes")
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun NoteDetailScreen(
        note: Note,
        onBack: () -> Unit,
        onEditNote: () -> Unit,
        onContinueByVoice: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val context = LocalContext.current
        BackHandler(onBack = onBack)

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TextButton(onClick = onBack) {
                    Text("Back to Inbox")
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Interpretation title",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = note.structured.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${note.displayTime} • ${note.durationSeconds}s",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Source", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "The original captured transcript, preserved as recorded.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = note.sourceTranscript.ifBlank { "No source transcript captured." },
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        if (note.rawTranscript != note.sourceTranscript) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Current transcript",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Edited transcript used for the current interpretation.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = note.rawTranscript.ifBlank { "No current transcript." },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Interpretation",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Generated details from the current transcript.",
                            style = MaterialTheme.typography.bodySmall,
                        )

                        Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(note.structured.summary, style = MaterialTheme.typography.bodyLarge)

                        Text("Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (note.structured.tags.isEmpty()) {
                            Text("No tags.")
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                note.structured.tags.forEach { tag ->
                                    AssistChip(onClick = {}, label = { Text(tag) })
                                }
                            }
                        }

                        Text("Action items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (note.structured.actionItems.isEmpty()) {
                            Text("No action items.")
                        } else {
                            note.structured.actionItems.forEach { actionItem ->
                                Text("• ${actionItem.text}")
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Development",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Your notes and additions after the original capture.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = note.developmentContent.ifBlank { "No development added yet." },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onContinueByVoice,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Continue by voice")
                }
            }

            item {
                Button(
                    onClick = onEditNote,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Edit note")
                }
            }

            item {
                OutlinedButton(
                    onClick = { shareNote(context, note) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Share note")
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun NoteCard(
        note: Note,
        onOpenNote: () -> Unit,
        onEditNote: () -> Unit,
        onDeleteNote: (Note) -> Unit,
    ) {
        var showDeleteConfirmation by remember { mutableStateOf(false) }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete note?") },
                text = {
                    Text(
                        "“${note.structured.title}” will be permanently removed from this device. " +
                            "This action cannot be undone.",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            onDeleteNote(note)
                        },
                    ) {
                        Text("Delete note")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Keep note")
                    }
                },
            )
        }

        Card(modifier = Modifier.fillMaxWidth(), onClick = onOpenNote) {
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEditNote) {
                        Text("Edit")
                    }
                    TextButton(onClick = { showDeleteConfirmation = true }) {
                        Text("Delete")
                    }
                }
            }
        }
    }

    private fun shareNote(context: Context, note: Note) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.structured.title)
            putExtra(Intent.EXTRA_TEXT, note.toShareText())
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share note"))
    }

    private fun Note.toShareText(): String = buildString {
        appendLine("Interpretation")
        appendLine("Title: ${structured.title}")
        appendLine("Summary: ${structured.summary}")
        appendLine("Tags: ${structured.tags.joinToString(", ")}")
        if (structured.actionItems.isNotEmpty()) {
            appendLine("Action items:")
            structured.actionItems.forEach { actionItem -> appendLine("- ${actionItem.text}") }
        }

        appendLine()
        appendLine("Source")
        appendLine("Original transcript: ${sourceTranscript.ifBlank { "No source transcript captured." }}")
        if (rawTranscript != sourceTranscript) {
            appendLine("Current transcript: ${rawTranscript.ifBlank { "No current transcript." }}")
        }

        if (developmentContent.isNotBlank()) {
            appendLine()
            appendLine("Development")
            appendLine(developmentContent)
        }

        appendLine()
        append("Created: ${DateFormat.getDateTimeInstance().format(Date(createdAtMillis))}")
    }

    private fun List<Note>.filterBySearchQuery(query: String): List<Note> {
        val queryTerms = query
            .trim()
            .lowercase()
            .split(whitespaceSeparator)
            .filter { term -> term.isNotBlank() }
        if (queryTerms.isEmpty()) return this

        return filter { note -> note.matchesSearchQuery(queryTerms) }
    }

    private fun Note.matchesSearchQuery(queryTerms: List<String>): Boolean {
        val searchableText = buildString {
            append(structured.title).append(' ')
            append(sourceTranscript).append(' ')
            append(rawTranscript).append(' ')
            append(developmentContent).append(' ')
            append(structured.summary).append(' ')
            append(structured.tags.joinToString(" ")).append(' ')
            append(structured.actionItems.joinToString(" ") { it.text })
        }.lowercase()

        return queryTerms.all { term -> searchableText.contains(term) }
    }

    private fun hasCapturePermissions(context: Context): Boolean = capturePermissions()
        .all { permission -> context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED }

    private fun capturePermissions(): List<String> = listOf(Manifest.permission.RECORD_AUDIO)

    private fun appendTranscript(vararg transcriptParts: String): String = transcriptParts
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .fold("") { transcript, nextPart -> appendTranscriptPart(transcript, nextPart) }

    private fun appendTranscriptPart(transcript: String, nextPart: String): String {
        if (transcript.isBlank()) return nextPart

        val continuation = transcriptContinuation(transcript, nextPart)
        return if (continuation.isBlank()) transcript else "$transcript $continuation"
    }

    private fun transcriptContinuation(transcript: String, nextPart: String): String {
        if (transcript.isBlank()) return nextPart

        val transcriptWords = transcript.split(whitespaceSeparator)
        val nextWords = nextPart.split(whitespaceSeparator)
        val transcriptMatchKeys = transcriptWords.map { word -> word.toTranscriptMatchKey() }
        val nextMatchKeys = nextWords.map { word -> word.toTranscriptMatchKey() }
        val overlapWordCount = (minOf(transcriptWords.size, nextWords.size) downTo 1)
            .firstOrNull { wordCount ->
                val transcriptOverlap = transcriptMatchKeys.takeLast(wordCount)
                val nextOverlap = nextMatchKeys.take(wordCount)
                transcriptOverlap.none { key -> key.isBlank() } && transcriptOverlap == nextOverlap
            }
            ?: 0
        return nextWords.drop(overlapWordCount).joinToString(" ")
    }

    private fun String.toTranscriptMatchKey(): String = lowercase()
        .trim { character -> !character.isLetterOrDigit() }

    private suspend fun loadSavedNotes(context: Context): List<Note> = try {
        val notesJson = context.notesDataStore.data.first()[notesJsonKey].orEmpty()
        if (notesJson.isBlank()) {
            emptyList()
        } else {
            JSONArray(notesJson).toNotes().sortedByDescending { it.createdAtMillis }
        }
    } catch (_: IOException) {
        emptyList()
    } catch (_: RuntimeException) {
        emptyList()
    }

    private suspend fun saveNotes(context: Context, notes: List<Note>) {
        context.notesDataStore.edit { preferences ->
            preferences[notesJsonKey] = notes.toJsonArray().toString()
        }
    }

    private fun List<Note>.toJsonArray(): JSONArray = JSONArray().also { notesArray ->
        forEach { note -> notesArray.put(note.toJsonObject()) }
    }

    private fun Note.toJsonObject(): JSONObject = JSONObject()
        .put("id", id)
        .put("rawTranscript", rawTranscript)
        .put("sourceTranscript", sourceTranscript)
        .put("developmentContent", developmentContent)
        .put("createdAtMillis", createdAtMillis)
        .put("durationMillis", durationMillis)
        .put("structured", structured.toJsonObject())

    private fun StructuredNote.toJsonObject(): JSONObject = JSONObject()
        .put("title", title)
        .put("summary", summary)
        .put("tags", JSONArray(tags))
        .put("actionItems", actionItems.toActionItemsJsonArray())

    private fun List<ActionItem>.toActionItemsJsonArray(): JSONArray = JSONArray().also { actionItemsArray ->
        forEach { actionItem -> actionItemsArray.put(actionItem.toJsonObject()) }
    }

    private fun ActionItem.toJsonObject(): JSONObject = JSONObject()
        .put("id", id)
        .put("text", text)
        .put("done", done)

    private fun JSONArray.toNotes(): List<Note> = List(length()) { index ->
        getJSONObject(index).toNote()
    }

    private fun JSONObject.toNote(): Note {
        val rawTranscript = optString("rawTranscript")
        val sourceTranscript = if (has("sourceTranscript") && !isNull("sourceTranscript")) {
            optString("sourceTranscript")
        } else {
            rawTranscript
        }

        return Note(
            id = optString("id", UUID.randomUUID().toString()),
            rawTranscript = rawTranscript,
            sourceTranscript = sourceTranscript,
            structured = optJSONObject("structured")?.toStructuredNote() ?: structureTranscript(rawTranscript),
            developmentContent = if (has("developmentContent") && !isNull("developmentContent")) {
                optString("developmentContent")
            } else {
                ""
            },
            createdAtMillis = optLong("createdAtMillis", System.currentTimeMillis()),
            durationMillis = optLong("durationMillis"),
        )
    }

    private fun JSONObject.toStructuredNote(): StructuredNote = StructuredNote(
        title = optString("title", "Untitled idea"),
        summary = optString("summary"),
        tags = optJSONArray("tags")?.toStringList().orEmpty(),
        actionItems = optJSONArray("actionItems")?.toActionItems().orEmpty(),
    )

    private fun JSONArray.toStringList(): List<String> = List(length()) { index -> getString(index) }

    private fun JSONArray.toActionItems(): List<ActionItem> = List(length()) { index ->
        getJSONObject(index).toActionItem()
    }

    private fun JSONObject.toActionItem(): ActionItem = ActionItem(
        id = optString("id", UUID.randomUUID().toString()),
        text = optString("text"),
        done = optBoolean("done", false),
    )
