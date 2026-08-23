# Expandable Idea Compatibility Plan

Issue #58 evolves a saved capture into an Idea that can grow without changing
the current fast capture flow or losing the words that were originally
captured. This plan records the smallest additive model change and the safe
loading behavior before implementation begins.

## Current implementation

The app currently has one `Note` model with these persisted responsibilities:

- `rawTranscript` stores the captured text and is also editable in the note
  editor.
- `structured` stores the generated title, summary, tags, and action items.
- `createdAtMillis` and `durationMillis` store capture metadata.
- Notes are serialized as a JSON array in Preferences DataStore.

The lifecycle is currently:

1. Stopping a meaningful capture creates a `Note` from the recognized
   transcript and generates its `StructuredNote`.
2. Editing the raw transcript replaces `rawTranscript` and regenerates the
   summary, tags, and action items. A user-entered title is retained.
3. Search considers the title, raw transcript, summary, tags, and action items.
4. Note details and shared text label `rawTranscript` as **Raw transcript**.
5. Loading uses optional JSON reads, so older records can omit structured
   values, timestamps, or duration without a schema migration.

Because the editable transcript is the only stored transcript, the current
model cannot preserve the original capture separately after a transcript edit.

## Target responsibilities

Keep `Note` and the existing JSON/DataStore path. Extend them additively rather
than introducing a database, repository, migration framework, or parallel Idea
model.

| Product concept | Model responsibility | Mutability |
| --- | --- | --- |
| Source | `sourceTranscript` stores the original captured transcript | Set when the note is created or safely backfilled when an older note is loaded; never replaced by normal editing |
| Interpretation | Existing `StructuredNote` stores generated title, summary, tags, and action items | Regenerated through the existing structuring flow when its input changes |
| Development | `developmentContent` stores later user-authored thinking | Editable without changing `sourceTranscript` |

`rawTranscript` must remain during this rollout for source and JSON
compatibility. It continues to support the current transcript-editing and
structuring behavior until a later focused checklist item clarifies that UI.
Renaming or deleting it now would add migration risk without helping preserve
the original capture.

The intended additive model shape is:

```kotlin
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val rawTranscript: String,
    val sourceTranscript: String = rawTranscript,
    val structured: StructuredNote,
    val developmentContent: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val durationMillis: Long,
)
```

The two additions should be staged with their corresponding checklist items:

- Item 2 adds and persists `sourceTranscript` so preservation starts before any
  new development editor exists.
- Item 3 adds and persists `developmentContent` when the focused edit behavior
  is implemented.

No new dependency or storage layer is needed for either field.

## JSON compatibility rules

New JSON records should retain every current key and add the new values at the
top level of each note:

```json
{
  "id": "note-id",
  "rawTranscript": "Current transcript text",
  "sourceTranscript": "Original captured words",
  "developmentContent": "Later user-authored thinking",
  "createdAtMillis": 0,
  "durationMillis": 0,
  "structured": {}
}
```

Loading must follow these rules:

1. Read `rawTranscript` exactly as the current loader does.
2. If `sourceTranscript` is absent or JSON null, initialize it from
   `rawTranscript`. This is the best recoverable source for an existing note;
   the app cannot reconstruct words that were edited before this field existed.
3. If `sourceTranscript` is present, preserve its exact value, including an
   empty string. Do not use `ifBlank { rawTranscript }`, because a deliberately
   empty original capture may later have non-empty edited or developed text.
4. If `developmentContent` is absent or JSON null, initialize it to an empty
   string.
5. Preserve the existing id, structured values, timestamps, duration, ordering,
   and action-item completion state.

The source fallback can be implemented with an explicit key-presence check,
for example:

```kotlin
val rawTranscript = optString("rawTranscript")
val sourceTranscript = if (has("sourceTranscript") && !isNull("sourceTranscript")) {
    getString("sourceTranscript")
} else {
    rawTranscript
}
```

Loading an old note does not need to rewrite the entire DataStore value. The
in-memory fallback is sufficient; the next normal save of the note list will
serialize the additive field. This avoids a separate migration transaction and
keeps rollback simple.

An older version of the app can ignore unknown JSON keys while reading, but it
would omit those keys if it later rewrites the note list. Downgrading and then
editing data is therefore not a supported preservation path. This limitation
should be noted during rollback testing once the fields ship.

## Edit, copy, and capture behavior

The first implementation must preserve these invariants:

- A new note initializes `sourceTranscript` from the final transcript passed to
  the existing `saveCapture` function.
- A blank note stores an explicitly empty source.
- The note editor never accepts a replacement source value.
- Existing `note.copy(rawTranscript = ..., structured = ...)` calls keep the
  already stored `sourceTranscript` because unspecified data-class properties
  are copied unchanged.
- Title-only edits, transcript edits, action-item changes, deletion, and list
  reordering do not alter the source.
- Adding or editing `developmentContent` later does not regenerate or replace
  the source. Whether it affects generated interpretation must be decided in
  that focused behavior PR, not hidden inside persistence.

## Search, details, and sharing rollout

Item 2 should be limited to preserving the source in the model and JSON. The
existing visible labels, search fields, and share output can remain unchanged
in that PR.

The later focused UI and integration items should then:

- show `sourceTranscript` under a clearly labeled **Source** section;
- keep generated title, summary, tags, and actions under
  **Interpretation**;
- show and edit `developmentContent` under **Development**;
- include Source and Development in search without removing the current
  interpretation fields; and
- label Source, Interpretation, and Development distinctly in shared text so
  generated content is not presented as original speech.

During the transition, `rawTranscript` must not be deleted or silently
overwritten. If later UI work changes the role of transcript correction, that
decision needs its own compatibility rule for notes where `rawTranscript` and
`sourceTranscript` differ.

## Recommended first implementation step

Implement checklist item 2 as one focused PR:

1. Add `sourceTranscript` to `Note`, defaulting to `rawTranscript` for normal
   construction compatibility.
2. Serialize `sourceTranscript` beside `rawTranscript`.
3. Load it with the missing/null fallback and present-empty preservation rules
   above.
4. Explicitly initialize it from the captured transcript in `saveCapture` for
   readability at the data-entry boundary.
5. Leave the current screens, search, sharing, structuring behavior, and
   dependencies unchanged.

This is the smallest independently mergeable step: it starts preserving every
new original capture and safely represents all existing notes without bundling
the Development editor or detail-screen redesign.

## Verification boundary for item 2

Automated/build checks:

- Run `:mobile-android:app:assembleDebug` and relevant unit tests.
- Confirm a legacy JSON note without `sourceTranscript` loads with source equal
  to its persisted `rawTranscript`.
- Confirm a record with `"sourceTranscript": ""` keeps an empty source after
  load and save.
- Confirm a new note round-trips with the same source.
- Confirm copying a note with an edited `rawTranscript` leaves its source
  unchanged.

Physical-device checks:

- Update over an installed build containing existing notes; verify their count,
  order, titles, transcript text, action states, and timestamps are unchanged.
- Save a meaningful capture, edit its transcript and title, fully close and
  reopen the app, and verify the note remains single and editable.
- Save an empty note, edit its title or transcript where currently allowed,
  reopen the app, and verify the original empty source remains recoverable.
- Recheck capture Start/Stop, Inbox search, sharing, single-note deletion, and
  delete-all for regressions.

Do not log transcript or development text while verifying compatibility.
