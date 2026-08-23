    package com.transcriptionmodel.ideacapture

    import android.Manifest
    import android.content.Context
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.rememberLauncherForActivityResult
    import androidx.activity.compose.setContent
    import androidx.activity.result.contract.ActivityResultContracts
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

    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                IdeaCaptureApp()
            }
        }
    }

    private enum class AppTab(val label: String, val icon: String) {
        Capture("Capture", "🎙️"),
        Inbox("Inbox", "🗂️"),
        About("About", "ℹ️"),
    }

    @Composable
    fun IdeaCaptureApp() {
        MaterialTheme(colorScheme = lightColorScheme()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                val context = LocalContext.current
                val appContext = context.applicationContext
                var selectedTab by remember { mutableStateOf(AppTab.Capture) }
                val sessionState = remember { mutableStateOf(CaptureSession()) }
                var session by sessionState
                var notes by remember { mutableStateOf(emptyList<Note>()) }
                var inboxSearchQuery by remember { mutableStateOf("") }
                var isSavingCapture by remember { mutableStateOf(false) }
                var pendingEmptyCaptureDurationMillis by remember { mutableStateOf<Long?>(null) }
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

                fun startSpeechCapture() {
                    isSavingCapture = false
                    pendingEmptyCaptureDurationMillis = null
                    session = CaptureSession(
                        status = CaptureStatus.Recording,
                        startedAtMillis = System.currentTimeMillis(),
                    )
                    speechTranscriber.start()
                }

                fun saveCapture(transcript: String, durationMillis: Long) {
                    val note = Note(
                        rawTranscript = transcript,
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
                    pendingEmptyCaptureDurationMillis = null
                }

                val microphonePermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { isGranted ->
                    if (isGranted) {
                        startSpeechCapture()
                    } else {
                        session = CaptureSession(
                            status = CaptureStatus.Failed,
                            errorMessage = "Microphone permission is required. Grant it, then tap Start again.",
                        )
                    }
                }

                DisposableEffect(speechTranscriber) {
                    onDispose {
                        speechTranscriber.destroy()
                    }
                }

                pendingEmptyCaptureDurationMillis?.let { durationMillis ->
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("No speech captured") },
                        text = {
                            Text("No speech was captured. Do you want to save an empty note or discard this capture?")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (pendingEmptyCaptureDurationMillis != null) {
                                        pendingEmptyCaptureDurationMillis = null
                                        saveCapture(transcript = "", durationMillis = durationMillis)
                                    }
                                },
                            ) {
                                Text("Save empty note")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    pendingEmptyCaptureDurationMillis = null
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
                                    onClick = { selectedTab = tab },
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
                            modifier = Modifier.padding(innerPadding),
                            onStart = {
                                if (hasCapturePermissions(context)) {
                                    startSpeechCapture()
                                } else {
                                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onStop = {
                                if (!isSavingCapture && session.isRecording) {
                                    isSavingCapture = true
                                    val committedTranscript = session.committedTranscript
                                    val partialTranscript = session.partialTranscript
                                    val startedAt = session.startedAtMillis ?: System.currentTimeMillis()
                                    val durationMillis = System.currentTimeMillis() - startedAt
                                    session = session.copy(
                                        status = CaptureStatus.Structuring,
                                        partialTranscript = "",
                                    )
                                    speechTranscriber.stopAndGetPendingTranscript { pendingTranscript ->
                                        val rawTranscript = appendTranscript(
                                            committedTranscript,
                                            partialTranscript,
                                            pendingTranscript,
                                        )
                                        if (rawTranscript.isBlank()) {
                                            session = session.copy(status = CaptureStatus.AwaitingConfirmation)
                                            pendingEmptyCaptureDurationMillis = durationMillis
                                        } else {
                                            saveCapture(rawTranscript, durationMillis)
                                        }
                                    }
                                }
                            },
                        )

                        AppTab.Inbox -> InboxScreen(
                            notes = notes,
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
                            onStartCapture = { selectedTab = AppTab.Capture },
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
        val statusTitle = when {
            session.status == CaptureStatus.Recording && session.errorMessage != null -> "Listening interrupted"
            session.status == CaptureStatus.Recording -> "Listening now"
            session.status == CaptureStatus.Structuring -> "Processing recording"
            session.status == CaptureStatus.Saved || session.status == CaptureStatus.Structured -> "Saved to Inbox"
            session.status == CaptureStatus.Failed -> "Unable to start listening"
            session.status == CaptureStatus.AwaitingConfirmation -> "No speech captured"
            else -> "Ready to capture"
        }
        val statusMessage = when (session.status) {
            CaptureStatus.Idle -> "Tap Start, then speak naturally."
            CaptureStatus.Recording -> session.errorMessage ?: "Speak naturally. Your words will appear below."
            CaptureStatus.Saved, CaptureStatus.Structured -> "Your note is safely saved and ready in the Inbox."
            CaptureStatus.Structuring -> "Finishing the transcript and saving your note..."
            CaptureStatus.AwaitingConfirmation -> "Choose whether to save an empty note or discard this capture."
            CaptureStatus.Failed -> session.errorMessage ?: "Speech recognition could not start. Please try again."
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
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = if (session.isRecording) onStop else onStart,
                            enabled = session.status != CaptureStatus.Structuring,
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
        searchQuery: String,
        onSearchQueryChange: (String) -> Unit,
        onDeleteNote: (Note) -> Unit,
        onUpdateNote: (Note) -> Unit,
        onStartCapture: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val visibleNotes = notes.filterBySearchQuery(searchQuery)

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

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search notes") },
                    placeholder = { Text("Search title, transcript, tags, or tasks") },
                )
            }

            if (visibleNotes.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = if (searchQuery.isBlank()) "No notes yet" else "No matching notes.",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (searchQuery.isBlank()) {
                                Text("Start a capture to create your first idea note.")
                            }
                        }
                    }
                }
            }

            items(visibleNotes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    onDeleteNote = onDeleteNote,
                    onUpdateNote = onUpdateNote,
                )
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun NoteCard(
        note: Note,
        onDeleteNote: (Note) -> Unit,
        onUpdateNote: (Note) -> Unit,
    ) {
        val context = LocalContext.current
        var expanded by remember { mutableStateOf(false) }
        var showDeleteConfirmation by remember { mutableStateOf(false) }
        var showEditDialog by remember { mutableStateOf(false) }
        var editableTitle by remember(note.id, note.structured.title) { mutableStateOf(note.structured.title) }
        var editableRawTranscript by remember(note.id, note.rawTranscript) { mutableStateOf(note.rawTranscript) }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete this note?") },
                text = { Text("This removes the note from your Inbox on this device.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            onDeleteNote(note)
                        },
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                },
            )
        }

        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = {
                    editableTitle = note.structured.title
                    editableRawTranscript = note.rawTranscript
                    showEditDialog = false
                },
                title = { Text("Edit note") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = editableTitle,
                            onValueChange = { editableTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Title") },
                        )
                        OutlinedTextField(
                            value = editableRawTranscript,
                            onValueChange = { editableRawTranscript = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Raw transcript") },
                            minLines = 4,
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val cleanedTranscript = editableRawTranscript.trim()
                            val regeneratedStructure = structureTranscript(cleanedTranscript)
                            val updatedNote = note.copy(
                                rawTranscript = cleanedTranscript,
                                structured = regeneratedStructure.copy(
                                    title = editableTitle.trim().ifBlank { regeneratedStructure.title },
                                ),
                            )
                            showEditDialog = false
                            onUpdateNote(updatedNote)
                        },
                        enabled = editableRawTranscript.isNotBlank(),
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            editableTitle = note.structured.title
                            editableRawTranscript = note.rawTranscript
                            showEditDialog = false
                        },
                    ) {
                        Text("Cancel")
                    }
                },
            )
        }

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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { shareNote(context, note) }) {
                            Text("Share")
                        }
                        TextButton(onClick = { expanded = false }) {
                            Text("Collapse")
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showEditDialog = true }) {
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
        appendLine("Title: ${structured.title}")
        appendLine("Summary: ${structured.summary}")
        appendLine("Tags: ${structured.tags.joinToString(", ")}")
        if (structured.actionItems.isNotEmpty()) {
            appendLine("Action items:")
            structured.actionItems.forEach { actionItem -> appendLine("- ${actionItem.text}") }
        }
        appendLine("Raw transcript: $rawTranscript")
        append("Created: ${DateFormat.getDateTimeInstance().format(Date(createdAtMillis))}")
    }

    private fun List<Note>.filterBySearchQuery(query: String): List<Note> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) return this

        return filter { note -> note.matchesSearchQuery(normalizedQuery) }
    }

    private fun Note.matchesSearchQuery(query: String): Boolean {
        val searchableText = buildString {
            append(structured.title).append(' ')
            append(rawTranscript).append(' ')
            append(structured.summary).append(' ')
            append(structured.tags.joinToString(" ")).append(' ')
            append(structured.actionItems.joinToString(" ") { it.text })
        }.lowercase()

        return searchableText.contains(query)
    }

    private fun hasCapturePermissions(context: Context): Boolean = capturePermissions()
        .all { permission -> context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED }

    private fun capturePermissions(): List<String> = listOf(Manifest.permission.RECORD_AUDIO)

    private fun appendTranscript(vararg transcriptParts: String): String = transcriptParts
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .fold("") { transcript, nextPart ->
            when {
                transcript.isBlank() -> nextPart
                transcript.endsWith(nextPart) -> transcript
                nextPart.startsWith(transcript) -> nextPart
                else -> "$transcript $nextPart"
            }
        }

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

    private fun JSONObject.toNote(): Note = Note(
        id = optString("id", UUID.randomUUID().toString()),
        rawTranscript = optString("rawTranscript"),
        structured = optJSONObject("structured")?.toStructuredNote() ?: structureTranscript(optString("rawTranscript")),
        createdAtMillis = optLong("createdAtMillis", System.currentTimeMillis()),
        durationMillis = optLong("durationMillis"),
    )

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
