package com.transcriptionmodel.ideacapture

import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SpeechStopTest {
    @Test
    fun `Stop assembly replaces superseded UI partial with returned segment`() {
        val cases = listOf(
            listOf("", "buy flower", "buy flour", "buy flour"),
            listOf("", "call Alice tomorrow", "call Alice", "call Alice"),
            listOf("remember the recipe", "buy flower", "buy flour", "remember the recipe buy flour"),
            listOf("", "buy flower", "buy flour tomorrow", "buy flour tomorrow"),
            listOf("", "buy flour", "buy flour", "buy flour"),
            listOf("", "buy flour", "buy flour tomorrow", "buy flour tomorrow"),
        )
        for ((committed, partialAtStop, returned, expected) in cases) {
            assertEquals(expected, assembleStoppedTranscript(committed, partialAtStop, returned))
        }
    }

    @Test
    fun `Stop assembly preserves committed boundaries and uses one fallback`() {
        assertEquals(
            "remember the recipe buy flour",
            assembleStoppedTranscript("remember the recipe buy", "buy flower", "buy flour"),
        )
        assertEquals("latest partial", assembleStoppedTranscript("", "older partial", "latest partial"))
        assertEquals("visible partial", assembleStoppedTranscript("", "visible partial", ""))
        assertEquals("earlier words", assembleStoppedTranscript("earlier words", "", ""))
        assertEquals("", assembleStoppedTranscript("", "", ""))
    }

    @Test
    fun `selected segment reaches continuation once without changing other Idea fields`() {
        val receiver = ContinuationReceiver("")
        val pending = PendingSpeechStop()
        pending.begin { result ->
            result.dispatch(
                isContinuation = true,
                onFailure = { fail("Unexpected fatal result") },
                onTranscript = { returned ->
                    val assembled = assembleStoppedTranscript("", "buy flower", returned)
                    val applied = applyVoiceContinuation(receiver.notes, "target", "typed cedar", assembled)
                    assertTrue(applied is ContinuationApplicationResult.Applied)
                    receiver.notes = (applied as ContinuationApplicationResult.Applied).notes
                    receiver.persistenceWrites++
                },
            )
        }
        pending.complete("buy flour")
        pending.complete("late duplicate")
        assertEquals(1, receiver.persistenceWrites)
        assertEquals("typed cedar\n\nbuy flour", receiver.notes[1].developmentContent)
        assertEquals(receiver.originalNotes[0], receiver.notes[0])
        assertEquals(receiver.originalNotes[2], receiver.notes[2])
        assertEquals(
            receiver.originalNotes[1].copy(developmentContent = "typed cedar\n\nbuy flour"),
            receiver.notes[1],
        )
    }

    @Test
    fun `fatal Stop errors never dispatch retained text to continuation application`() {
        val errors = mapOf(
            SpeechRecognizer.ERROR_NETWORK to "Network error during speech recognition.",
            SpeechRecognizer.ERROR_SERVER to "Speech recognition service error.",
            SpeechRecognizer.ERROR_AUDIO to "Audio recording error. Please try again.",
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS to
                "Microphone permission is required for speech recognition.",
            999 to "Speech recognition error 999.",
        )
        val retainedTranscripts = listOf(
            "" to "first lantern",
            "earlier words" to "",
            "earlier words" to "first lantern",
        )
        for ((error, message) in errors) {
            for ((committed, partial) in retainedTranscripts) {
                val pending = PendingSpeechStop()
                val receiver = ContinuationReceiver(committed)
                assertTrue(pending.begin(receiver::receive))
                pending.complete("", partial, error)

                assertEquals(message, receiver.failure)
                assertEquals(0, receiver.applicationCalls)
                assertEquals(0, receiver.persistenceWrites)
                assertSame(receiver.originalNotes, receiver.notes)
                assertFalse(pending.isPending)
            }
        }
    }

    @Test
    fun `timeout and duplicate result after fatal completion cannot save or clear failure`() {
        val pending = PendingSpeechStop()
        val receiver = ContinuationReceiver("earlier words")
        pending.begin(receiver::receive)
        pending.complete("", "first lantern", SpeechRecognizer.ERROR_NETWORK)
        pending.complete("") // queued timeout
        pending.complete("late result")
        pending.complete("", "late partial", SpeechRecognizer.ERROR_NO_MATCH)
        assertEquals(1, receiver.completions)
        assertEquals("Network error during speech recognition.", receiver.failure)
        assertEquals(0, receiver.persistenceWrites)
        assertSame(receiver.originalNotes, receiver.notes)
    }

    @Test
    fun `retry after failure appends only fresh transcript once`() {
        val pending = PendingSpeechStop()
        val failed = ContinuationReceiver("old committed words")
        pending.begin(failed::receive)
        pending.complete("", "old partial words", SpeechRecognizer.ERROR_SERVER)
        val retry = ContinuationReceiver("")
        assertTrue(pending.begin(retry::receive))
        pending.complete("new attempt")
        pending.complete("duplicate")
        assertNull(retry.failure)
        assertEquals(1, retry.persistenceWrites)
        assertEquals("typed cedar\n\nnew attempt", retry.notes[1].developmentContent)
        assertEquals(failed.originalNotes, failed.notes)
    }

    @Test
    fun `successful final result takes precedence over partial text`() {
        val pending = PendingSpeechStop()
        val receiver = ContinuationReceiver("")
        pending.begin(receiver::receive)
        pending.complete("final words", "partial words")
        assertEquals(1, receiver.persistenceWrites)
        assertEquals("typed cedar\n\nfinal words", receiver.notes[1].developmentContent)
        assertEquals(
            receiver.originalNotes[1].copy(developmentContent = "typed cedar\n\nfinal words"),
            receiver.notes[1],
        )
        assertEquals(receiver.originalNotes[0], receiver.notes[0])
        assertEquals(receiver.originalNotes[2], receiver.notes[2])
    }

    @Test
    fun `recoverable errors and timeout retain the existing partial fallback`() {
        val nonfatalOutcomes = listOf(
            null,
            SpeechRecognizer.ERROR_CLIENT,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        )
        for (error in nonfatalOutcomes) {
            val pending = PendingSpeechStop()
            val receiver = ContinuationReceiver("")
            pending.begin(receiver::receive)
            pending.complete("", "fallback words", error)
            assertNull(receiver.failure)
            assertEquals(1, receiver.persistenceWrites)
            assertEquals("typed cedar\n\nfallback words", receiver.notes[1].developmentContent)
        }
    }

    @Test
    fun `empty fallback still reaches existing invalid transcript protection`() {
        val pending = PendingSpeechStop()
        val receiver = ContinuationReceiver("")
        pending.begin(receiver::receive)
        pending.complete("")
        assertEquals(1, receiver.applicationCalls)
        assertEquals(0, receiver.persistenceWrites)
        assertSame(receiver.originalNotes, receiver.notes)
    }

    @Test
    fun `ordinary capture keeps fatal error fallback`() {
        val pending = PendingSpeechStop()
        var transcript: String? = null
        pending.begin { result ->
            result.dispatch(false, { fail("Ordinary capture policy changed") }, { transcript = it })
        }
        pending.complete("", "ordinary retained words", SpeechRecognizer.ERROR_NETWORK)
        assertEquals("ordinary retained words", transcript)
    }

    @Test
    fun `repeated Stop cannot replace callback and cancellation invalidates it`() {
        val pending = PendingSpeechStop()
        var completions = 0
        assertTrue(pending.begin { completions++ })
        assertFalse(pending.begin { fail("Repeated Stop replaced the callback") })
        pending.cancel()
        pending.complete("cancelled words")
        assertEquals(0, completions)
        assertTrue(pending.begin { completions++ })
        pending.complete("fresh words")
        pending.complete("duplicate words")
        assertEquals(1, completions)
    }

    // Exercises the production Stop dispatcher before application/persistence effects.
    private class ContinuationReceiver(private val committed: String) {
        val originalNotes = listOf(note("before"), note("target"), note("after"))
        var notes = originalNotes
        var failure: String? = null
        var completions = 0
        var applicationCalls = 0
        var persistenceWrites = 0

        fun receive(result: SpeechStopResult) {
            completions++
            result.dispatch(
                isContinuation = true,
                onFailure = { failure = it },
                onTranscript = { transcript ->
                    applicationCalls++
                    val applied = applyVoiceContinuation(
                        notes, "target", "typed cedar",
                        listOf(committed, transcript).filter { it.isNotBlank() }.joinToString(" "),
                    )
                    if (applied is ContinuationApplicationResult.Applied) {
                        notes = applied.notes
                        persistenceWrites++
                    }
                },
            )
        }

        private fun note(id: String) = Note(
            id = id,
            rawTranscript = "current",
            sourceTranscript = "source",
            structured = StructuredNote("title", "summary", listOf("tag"), emptyList()),
            developmentContent = "typed cedar",
            createdAtMillis = 123L,
            durationMillis = 456L,
        )
    }
}
