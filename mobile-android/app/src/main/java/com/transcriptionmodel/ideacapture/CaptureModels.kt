package com.transcriptionmodel.ideacapture

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class CaptureStatus {
    Idle,
    Recording,
    AwaitingConfirmation,
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

    val title = rawTranscript.toMeaningfulTitle()
    val summary = rawTranscript.toConciseSummary()

    val tags = cleanWords
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key }
        .filterNot { it in fillerWords }
        .take(5)
        .ifEmpty { listOf("idea") }

    val actionItems = rawTranscript.toActionItems()

    return StructuredNote(
        title = title,
        summary = summary,
        tags = tags,
        actionItems = actionItems,
    )
}

private fun String.toActionItems(): List<ActionItem> = splitToSequence('.', '!', '?', ';', '\n')
    .map { candidate ->
        candidate
            .trim()
            .trimStart('-', '•')
            .trim()
            .replace(actionWhitespaceRegex, " ")
    }
    .filter { candidate -> candidate.isNotBlank() }
    .filter { candidate -> actionIntentPatterns.any { pattern -> pattern.containsMatchIn(candidate) } }
    .distinctBy { candidate -> candidate.lowercase(Locale.getDefault()) }
    .take(MAX_ACTION_ITEMS)
    .map { candidate -> ActionItem(text = candidate) }
    .toList()

private fun String.toConciseSummary(): String {
    val normalizedTranscript = trim().replace(Regex("\\s+"), " ")
    if (normalizedTranscript.isBlank()) {
        return EMPTY_SUMMARY
    }
    if (normalizedTranscript.length <= MAX_SUMMARY_LENGTH) {
        return normalizedTranscript
    }

    val leadingSentences = summarySentenceRegex
        .findAll(normalizedTranscript)
        .map { match -> match.value.trim() }
        .filter { sentence -> sentence.isNotBlank() }
        .take(MAX_SUMMARY_SENTENCES)
        .joinToString(" ")

    return leadingSentences
        .ifBlank { normalizedTranscript }
        .toSummaryExcerpt()
}

private fun String.toSummaryExcerpt(): String {
    val availableLength = MAX_SUMMARY_LENGTH - SUMMARY_ELLIPSIS.length
    val clippedSummary = take(availableLength).trimEnd()
    val lastWordBoundary = clippedSummary.lastIndexOf(' ')
    val summaryAtWordBoundary = when {
        length <= availableLength -> clippedSummary
        lastWordBoundary > 0 -> clippedSummary.take(lastWordBoundary)
        else -> clippedSummary
    }

    return summaryAtWordBoundary
        .trimEnd(' ', '.', '!', '?', ',', ':', ';', '-') + SUMMARY_ELLIPSIS
}

private fun String.toMeaningfulTitle(): String {
    val normalizedTranscript = trim()
    if (normalizedTranscript.isBlank() || normalizedTranscript.isGenericCaptureFallback()) {
        return "Untitled idea"
    }

    val titleWords = normalizedTranscript
        .splitToSequence(Regex("[.!?\\n]+"))
        .map { sentence ->
            val words = sentence
                .splitToSequence(Regex("\\s+"))
                .map { word -> word.trim(',', '.', '!', '?', ':', ';', '"', '“', '”', '(', ')') }
                .filter { word -> word.isNotBlank() }
                .toList()
            val meaningfulStart = words.indexOfFirst { word ->
                word.lowercase(Locale.getDefault()) !in titleLeadInWords
            }

            if (meaningfulStart < 0) {
                emptyList()
            } else {
                words
                    .drop(meaningfulStart)
                    .take(MAX_TITLE_WORDS)
                    .dropLastWhile { word -> word.lowercase(Locale.getDefault()) in titleTrailingWords }
            }
        }
        .firstOrNull { words -> words.isNotEmpty() }
        .orEmpty()

    val title = titleWords
        .joinToString(" ")
        .take(MAX_TITLE_LENGTH)
        .trim()
        .trim(',', '.', '!', '?', ':', ';', '-')

    return title
        .takeIf { it.isNotBlank() }
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        ?: "Untitled idea"
}

private fun String.isGenericCaptureFallback(): Boolean {
    val normalized = lowercase(Locale.getDefault())
        .trim(',', '.', '!', '?', ':', ';', '"')

    return normalized in genericCaptureFallbacks
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

private const val MAX_TITLE_WORDS = 6
private const val MAX_TITLE_LENGTH = 60
private const val MAX_SUMMARY_SENTENCES = 2
private const val MAX_SUMMARY_LENGTH = 240
private const val MAX_ACTION_ITEMS = 5
private const val SUMMARY_ELLIPSIS = "..."
private const val EMPTY_SUMMARY = "No transcript captured."

private val summarySentenceRegex = Regex("[^.!?]+[.!?]?")
private val actionWhitespaceRegex = Regex("\\s+")

private val genericCaptureFallbacks = setOf(
    "quick idea captured from the prototype",
    "quick idea captured from prototype",
)

private val titleLeadInWords = setOf(
    "a",
    "about",
    "an",
    "and",
    "are",
    "can",
    "captured",
    "could",
    "for",
    "from",
    "have",
    "i",
    "idea",
    "is",
    "just",
    "like",
    "maybe",
    "my",
    "need",
    "note",
    "of",
    "on",
    "our",
    "prototype",
    "quick",
    "recording",
    "really",
    "should",
    "so",
    "that",
    "the",
    "this",
    "to",
    "transcript",
    "um",
    "uh",
    "want",
    "wanted",
    "we",
    "would",
)

private val titleTrailingWords = setOf(
    "a",
    "an",
    "and",
    "for",
    "from",
    "of",
    "on",
    "the",
    "to",
    "with",
)

private val actionIntentPatterns = listOf(
    Regex("""\b(?:i|we)\s+(?:need|have|want)\s+to\b""", RegexOption.IGNORE_CASE),
    Regex("""^(?:need|have|want)\s+to\b""", RegexOption.IGNORE_CASE),
    Regex("""\b(?:i|we)\s+(?:should|must)\b""", RegexOption.IGNORE_CASE),
    Regex("""^(?:should|must)\b""", RegexOption.IGNORE_CASE),
    Regex("""\b(?:remember|remind me|don't forget|do not forget|make sure)\s+to\b""", RegexOption.IGNORE_CASE),
    Regex("""^(?:todo|to do)\b(?:\s*[:\-]\s*|\s+)""", RegexOption.IGNORE_CASE),
    Regex(
        """^(?:please\s+)?(?:call|email|send|schedule|buy|review|update|finish|check|create|build|write|plan|follow\s+up)\b""",
        RegexOption.IGNORE_CASE,
    ),
)
