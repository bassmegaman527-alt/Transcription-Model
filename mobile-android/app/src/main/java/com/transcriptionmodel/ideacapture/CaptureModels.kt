package com.transcriptionmodel.ideacapture

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class CaptureStatus {
    Idle,
    Recording,
    Saved,
    Structuring,
    Structured,
    Failed,
}

data class ActionItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val done: Boolean = false,
)

data class StructuredNote(
    val title: String,
    val summary: String,
    val tags: List<String>,
    val actionItems: List<ActionItem>,
)

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val rawTranscript: String,
    val structured: StructuredNote,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val durationMillis: Long,
) {
    val displayTime: String
        get() = timeFormatter.format(Date(createdAtMillis))

    val durationSeconds: Long
        get() = durationMillis.coerceAtLeast(0L) / 1_000L

    private companion object {
        val timeFormatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    }
}

data class CaptureSession(
    val status: CaptureStatus = CaptureStatus.Idle,
    val startedAtMillis: Long? = null,
    val committedTranscript: String = "",
    val partialTranscript: String = "",
    val errorMessage: String? = null,
) {
    val isRecording: Boolean = status == CaptureStatus.Recording

    val liveTranscript: String
        get() = listOf(committedTranscript, partialTranscript)
            .filter { it.isNotBlank() }
            .joinToString(" ")
}

fun structureTranscript(rawTranscript: String): StructuredNote {
    val cleanWords = rawTranscript
        .split(' ', '\n', '\t')
        .map { it.trim(',', '.', '!', '?', ':', ';', '"').lowercase() }
        .filter { it.length > 3 }

    val title = rawTranscript
        .split('.', '!', '?')
        .firstOrNull { it.isNotBlank() }
        ?.trim()
        ?.take(60)
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        ?: "Untitled idea"

    val summary = when {
        rawTranscript.length <= 180 -> rawTranscript
        else -> rawTranscript.take(177).trimEnd() + "..."
    }

    val tags = cleanWords
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key }
        .filterNot { it in fillerWords }
        .take(5)
        .ifEmpty { listOf("idea") }

    val actionItems = rawTranscript
        .split('.', '!', '?')
        .map { it.trim() }
        .filter { sentence -> actionPrefixes.any { sentence.lowercase().startsWith(it) } }
        .distinctBy { it.lowercase() }
        .take(5)
        .map { ActionItem(text = it) }

    return StructuredNote(
        title = title,
        summary = summary,
        tags = tags,
        actionItems = actionItems,
    )
}

private val fillerWords = setOf(
    "about",
    "after",
    "again",
    "because",
    "could",
    "should",
    "that",
    "this",
    "with",
)

private val actionPrefixes = listOf(
    "i need to",
    "need to",
    "todo",
    "to do",
    "remember to",
    "follow up",
    "create",
    "build",
    "write",
)
