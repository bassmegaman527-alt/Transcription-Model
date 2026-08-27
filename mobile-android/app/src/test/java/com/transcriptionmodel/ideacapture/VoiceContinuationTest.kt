package com.transcriptionmodel.ideacapture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceContinuationTest {
    @Test
    fun `normalizes a continuation for empty Development`() {
        val target = note(id = "target", developmentContent = "")

        val applied = appliedResult(
            notes = listOf(target),
            targetNoteId = target.id,
            expectedDevelopmentContent = target.developmentContent,
            stoppedTranscript = "  alpha\n\t bravo   charlie  ",
        )

        assertEquals("alpha bravo charlie", applied.updatedNote.developmentContent)
        assertEquals(applied.updatedNote, applied.notes.single())
    }

    @Test
    fun `appends with a blank line without rewriting existing Development`() {
        val existingDevelopment = "Typed context.\nSecond line."
        val target = note(id = "target", developmentContent = existingDevelopment)

        val applied = appliedResult(
            notes = listOf(target),
            targetNoteId = target.id,
            expectedDevelopmentContent = existingDevelopment,
            stoppedTranscript = "  spoken   addition  ",
        )

        assertEquals(
            "$existingDevelopment\n\nspoken addition",
            applied.updatedNote.developmentContent,
        )
    }

    @Test
    fun `preserves unrelated fields list count and ordering`() {
        val before = note(id = "before", developmentContent = "Before", createdAtMillis = 3_000L)
        val target = note(id = "target", developmentContent = "Original", createdAtMillis = 2_000L)
        val after = note(id = "after", developmentContent = "After", createdAtMillis = 1_000L)
        val notes = listOf(before, target, after)

        val applied = appliedResult(
            notes = notes,
            targetNoteId = target.id,
            expectedDevelopmentContent = target.developmentContent,
            stoppedTranscript = "New thought",
        )

        val expectedTarget = target.copy(developmentContent = "Original\n\nNew thought")
        assertNotSame(notes, applied.notes)
        assertEquals(listOf("before", "target", "after"), applied.notes.map { note -> note.id })
        assertEquals(notes.size, applied.notes.size)
        assertEquals(before, applied.notes[0])
        assertEquals(expectedTarget, applied.notes[1])
        assertEquals(after, applied.notes[2])
        assertEquals(expectedTarget, applied.updatedNote)
        assertEquals("Original", notes[1].developmentContent)
    }

    @Test
    fun `rejects blank and prototype placeholder transcripts`() {
        val target = note(id = "target", developmentContent = "Existing")
        val notes = listOf(target)

        listOf(
            "  \n\t  ",
            " Quick   idea captured from the prototype. ",
        ).forEach { stoppedTranscript ->
            val result = applyVoiceContinuation(
                notes = notes,
                targetNoteId = target.id,
                expectedDevelopmentContent = target.developmentContent,
                stoppedTranscript = stoppedTranscript,
            )

            assertSame(ContinuationApplicationResult.InvalidTranscript, result)
            assertEquals("Existing", target.developmentContent)
        }
    }

    @Test
    fun `reports a missing target without changing the list`() {
        val notes = listOf(note(id = "present", developmentContent = "Existing"))

        val result = applyVoiceContinuation(
            notes = notes,
            targetNoteId = "missing",
            expectedDevelopmentContent = "",
            stoppedTranscript = "New thought",
        )

        assertSame(ContinuationApplicationResult.TargetMissing, result)
        assertEquals("Existing", notes.single().developmentContent)
    }

    @Test
    fun `reports a changed Development baseline without overwriting it`() {
        val target = note(id = "target", developmentContent = "Newer saved value")
        val notes = listOf(target)

        val result = applyVoiceContinuation(
            notes = notes,
            targetNoteId = target.id,
            expectedDevelopmentContent = "Older capture-start value",
            stoppedTranscript = "Voice addition",
        )

        assertSame(ContinuationApplicationResult.TargetChanged, result)
        assertEquals("Newer saved value", notes.single().developmentContent)
    }

    @Test
    fun `applies one attempt once but allows the same words in a new attempt`() {
        val target = note(id = "target", developmentContent = "")
        val first = appliedResult(
            notes = listOf(target),
            targetNoteId = target.id,
            expectedDevelopmentContent = "",
            stoppedTranscript = "Repeated thought",
        )

        val repeatedCallback = applyVoiceContinuation(
            notes = first.notes,
            targetNoteId = target.id,
            expectedDevelopmentContent = "",
            stoppedTranscript = "Repeated thought",
        )
        assertSame(ContinuationApplicationResult.TargetChanged, repeatedCallback)

        val nextAttempt = appliedResult(
            notes = first.notes,
            targetNoteId = target.id,
            expectedDevelopmentContent = first.updatedNote.developmentContent,
            stoppedTranscript = "Repeated thought",
        )
        assertEquals("Repeated thought\n\nRepeated thought", nextAttempt.updatedNote.developmentContent)
    }

    @Test
    fun `keeps distinct intentional continuations in chronological order`() {
        val before = note(id = "before", developmentContent = "Before", createdAtMillis = 3_000L)
        val target = note(id = "target", developmentContent = "Typed starting point", createdAtMillis = 2_000L)
        val after = note(id = "after", developmentContent = "After", createdAtMillis = 1_000L)
        val notes = listOf(before, target, after)

        val first = appliedResult(
            notes = notes,
            targetNoteId = target.id,
            expectedDevelopmentContent = target.developmentContent,
            stoppedTranscript = "First spoken addition",
        )
        val second = appliedResult(
            notes = first.notes,
            targetNoteId = target.id,
            expectedDevelopmentContent = first.updatedNote.developmentContent,
            stoppedTranscript = "Second spoken addition",
        )

        val expectedDevelopment =
            "Typed starting point\n\nFirst spoken addition\n\nSecond spoken addition"
        assertEquals(expectedDevelopment, second.updatedNote.developmentContent)
        assertEquals(target.copy(developmentContent = expectedDevelopment), second.updatedNote)
        assertEquals(listOf("before", "target", "after"), second.notes.map { note -> note.id })
        assertEquals(notes.size, second.notes.size)
        assertEquals(before, second.notes[0])
        assertEquals(after, second.notes[2])
    }

    @Test
    fun `continued Development is immediately searchable and shareable`() {
        val target = note(id = "target", developmentContent = "Typed starting point")
        val applied = appliedResult(
            notes = listOf(target),
            targetNoteId = target.id,
            expectedDevelopmentContent = target.developmentContent,
            stoppedTranscript = "Cobalt roadmap checkpoint",
        )

        assertEquals(
            listOf(applied.updatedNote),
            applied.notes.filterBySearchQuery("cobalt checkpoint"),
        )
        val sharedText = applied.updatedNote.toShareText()
        assertTrue(sharedText.contains("Development"))
        assertTrue(sharedText.contains("Typed starting point"))
        assertTrue(sharedText.contains("Cobalt roadmap checkpoint"))
    }

    private fun appliedResult(
        notes: List<Note>,
        targetNoteId: String,
        expectedDevelopmentContent: String,
        stoppedTranscript: String,
    ): ContinuationApplicationResult.Applied {
        val result = applyVoiceContinuation(
            notes = notes,
            targetNoteId = targetNoteId,
            expectedDevelopmentContent = expectedDevelopmentContent,
            stoppedTranscript = stoppedTranscript,
        )
        assertTrue("Expected Applied but was $result", result is ContinuationApplicationResult.Applied)
        return result as ContinuationApplicationResult.Applied
    }

    private fun note(
        id: String,
        developmentContent: String,
        createdAtMillis: Long = 1_000L,
    ): Note = Note(
        id = id,
        rawTranscript = "Current transcript $id",
        sourceTranscript = "Source transcript $id",
        structured = StructuredNote(
            title = "Title $id",
            summary = "Summary $id",
            tags = listOf("tag-$id"),
            actionItems = listOf(
                ActionItem(
                    id = "action-$id",
                    text = "Action $id",
                    done = true,
                ),
            ),
        ),
        developmentContent = developmentContent,
        createdAtMillis = createdAtMillis,
        durationMillis = 4_321L,
    )
}
