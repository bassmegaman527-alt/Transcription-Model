# Continue an Idea by Voice Plan

Issue #72 adds an explicit way to speak more thinking into the Development of
one saved Idea. This document completes item 1 by recording the current flow,
the smallest complete continuation design, its compatibility rules, and the
pure logic boundary that must be tested before UI implementation begins.

Item 1 does not change application behavior, dependencies, or stored data.

## Current baseline

### Implemented and verified

- `MainActivity.IdeaCaptureApp` owns the in-memory note list, capture session,
  microphone permission launcher, one `AndroidSpeechTranscriber`, Stop
  assembly, and Preferences DataStore saves.
- Ordinary Capture, the launcher **Quick capture** shortcut, and the Quick
  Settings **Quick capture** tile all enter that same capture path and create a
  new Idea.
- Issue #65 verified that capture stays logically active through normal
  recognition boundaries, retains transcript segments without overlap, stops
  explicitly, and saves exactly one Idea.
- `Note.sourceTranscript` preserves the original capture,
  `Note.rawTranscript` remains the editable Current transcript,
  `Note.structured` stores the Interpretation, and
  `Note.developmentContent` stores later additions.
- Development already round-trips through the current JSON/DataStore record,
  appears on the detail and edit screens, participates in search, and is
  included in shared text when nonblank.

### Implemented without an automated regression seam

- `isSavingCapture`, the transition out of `CaptureStatus.Recording`, and the
  Stop button state prevent ordinary UI repeats from saving the same new
  capture twice.
- `onUpdateNote` replaces a note by stable ID while preserving list order, and
  `Note.copy` preserves fields that are not explicitly changed.
- The Android module currently has no `src/test` sources or local unit-test
  dependency. `testDebugUnitTest` therefore reports `NO-SOURCE`.
- The Android CI workflow assembles the debug APK but does not run a unit-test
  task.
- `CaptureForegroundService.kt` remains in the source tree, but the manifest
  does not register it and no current code calls it. It is not part of capture
  or continuation and should not be revived for this foreground-only feature.

### Planned but not implemented

- There is no continuation target, continuation capture mode, or
  **Continue by voice** action.
- Every valid stopped transcript currently reaches `saveCapture`, which
  prepends a new `Note`; no path can update an existing Idea from speech.

## User problem

The user can type Development for an existing Idea but cannot continue that
Idea using the app's fastest input method. The first useful version should let
the user deliberately select one Idea, speak one continuation, press Stop, and
return to that same Idea with the new words appended to Development.

Initial capture must remain unchanged. A continuation must never create a
second Idea, replace the original thought, regenerate Interpretation, or remain
silently active after the user cancels it.

## Current flow trace

### Detail navigation

`InboxScreen` currently owns `selectedNoteId` and `editingNoteId` with local
Compose state. It resolves those IDs against the current `notes` list and
shows `NoteDetailScreen` or `NoteEditScreen`. `NoteDetailScreen` can go back,
open editing, and share, but it cannot request capture.

Because `selectedNoteId` is private to `InboxScreen`, `IdeaCaptureApp` cannot
move to Capture and later reopen the same detail. The smallest navigation
change is to lift only `selectedNoteId` to `IdeaCaptureApp` and pass its value
and change callbacks through `InboxScreen`. A navigation library, ViewModel,
or new screen stack is not justified.

### Capture and speech callbacks

`IdeaCaptureApp` creates one remembered `AndroidSpeechTranscriber`. Partial
callbacks replace the current partial after removing overlap with committed
text. Final callbacks append through the same word-overlap protection and
clear the partial. Recoverable recognizer boundaries remain internal; fatal
errors remain visible.

`requestOrStartSpeechCapture` switches to Capture, checks the one
`RECORD_AUDIO` permission, and either launches the existing permission
contract or calls `startSpeechCapture`. `startSpeechCapture` resets transient
new-capture state, creates a recording `CaptureSession`, and starts the shared
transcriber.

The continuation flow should reuse those permission and speech operations.
It should not construct another recognizer or fork transcript accumulation.

### Stop and save

The current Stop callback:

1. accepts the first tap only while `!isSavingCapture && session.isRecording`;
2. captures the committed, partial, start-time, and duration values;
3. leaves Recording before asking the transcriber to stop;
4. combines the committed, UI partial, and terminal transcript with overlap
   protection;
5. asks for confirmation when the result is blank or matches a prototype
   placeholder; and
6. otherwise calls `saveCapture`, which prepends one new `Note`, writes the
   list, and opens Inbox.

A continuation must branch only after the same stopped transcript has been
assembled. A valid continuation must use an update helper instead of
`saveCapture`. Blank and placeholder continuation results must never reuse the
new-note confirmation action because **Save empty note** or **Save anyway**
would violate the zero-new-Idea invariant.

### Note update, persistence, search, and sharing

`onUpdateNote` maps the current list and replaces only the note with the same
ID, then saves the full list to the existing `notes_json` Preferences DataStore
key. Serialization already includes `developmentContent`; no schema or
migration change is needed.

Search already includes Development alongside Source, Current transcript,
Interpretation, tags, and action items. Sharing already emits a Development
section when the field is nonblank. Appending and persisting Development is
therefore sufficient for both utilities to reflect a successful continuation.

## Smallest complete continuation flow

### Target state

Keep one ephemeral target in `IdeaCaptureApp` for the intentional continuation
attempt:

```kotlin
private data class ContinuationCaptureTarget(
    val attemptId: String,
    val noteId: String,
    val expectedDevelopmentContent: String,
)
```

- `noteId` identifies the only Idea that may be updated.
- `expectedDevelopmentContent` is the optimistic baseline for applying this
  attempt once without overwriting a newer Development value.
- `attemptId` distinguishes the active permission, recording, Stop, or cancel
  callback from any stale asynchronous callback.
- The state is in memory only. It must not be persisted or logged.

The mode is a continuation only while this target is active. A null target is
the existing new-Idea mode.

### Successful path

1. The user opens one Idea and taps **Continue by voice**.
2. Resolve the selected ID against the current note list. If it exists, create
   a new continuation target using its current Development as the expected
   baseline, move to Capture, and invoke the existing permission/start flow.
3. The Capture screen clearly says it is continuing the target Idea and shows
   its current title. The button action itself is the explicit user action that
   may request permission and begin listening; there is no background or
   pre-Start recording.
4. Partial/final speech uses the existing `CaptureSession`, transcript
   overlap logic, continuity behavior, and `AndroidSpeechTranscriber`.
5. The first valid Stop assembles one transcript through the current Stop
   path. Its callback first confirms that the same attempt ID is still active.
6. Apply the transcript to the latest in-memory note list through the pure
   continuation helper defined below.
7. On success, replace the notes list with the helper result, persist it once
   through `saveNotes`, clear continuation/capture state, open Inbox, and
   reopen the updated Idea detail by its stable ID.

The successful path updates one existing list element. It does not call
`saveCapture`, construct a `Note`, change note ordering, or change note count.
From the first runtime continuation PR, every non-`Applied` result must use a
safe no-write fallback and must never fall through to `saveCapture`; item 4 can
then add the complete retry and return experience without leaving an unsafe
intermediate state.

### Preserved-field contract

| State | Successful continuation result |
| --- | --- |
| Target `id` | Unchanged |
| `developmentContent` | Existing content preserved; normalized addition appended with a blank-line separator |
| `sourceTranscript` | Unchanged |
| `rawTranscript` | Unchanged |
| `structured` title, summary, tags, and actions | Unchanged; no regeneration |
| `createdAtMillis` and `durationMillis` | Unchanged |
| Other notes | Value and order unchanged |
| Target position | Unchanged |
| Note count | Unchanged |

## Append and duplicate rules

Normalize only the stopped addition:

1. trim leading and trailing whitespace;
2. collapse internal whitespace sequences to one space; and
3. reject a blank or `isPlaceholderCaptureTranscript()` result.

Do not rewrite the existing Development. When it is blank, the normalized
addition becomes Development. Otherwise append two newline characters and the
addition so typed and spoken thinking remain readable.

Do not suppress a continuation merely because its words match an earlier
continuation. The user may intentionally repeat the same words in a separate
attempt. Duplicate protection must be scoped to one attempt, not to transcript
text.

Capture `expectedDevelopmentContent` when the attempt begins. The pure helper
may apply only when the target's current Development still equals that
baseline. After the first application, the value no longer matches; a second
callback for the same attempt therefore cannot append again. A later
intentional attempt captures the new baseline and may append even identical
words.

## Pure automated-test seam for item 2

Add only a pure list transformation beside `Note` in `CaptureModels.kt`:

```kotlin
internal sealed interface ContinuationApplicationResult {
    data class Applied(
        val notes: List<Note>,
        val updatedNote: Note,
    ) : ContinuationApplicationResult

    data object InvalidTranscript : ContinuationApplicationResult
    data object TargetMissing : ContinuationApplicationResult
    data object TargetChanged : ContinuationApplicationResult
}

internal fun applyVoiceContinuation(
    notes: List<Note>,
    targetNoteId: String,
    expectedDevelopmentContent: String,
    stoppedTranscript: String,
): ContinuationApplicationResult
```

The helper should return a copied list only for `Applied`. It must not access
Compose state, Android context, DataStore, the recognizer, time, or random IDs.
The caller remains responsible for checking the active attempt ID and for
persisting an `Applied` result.

Item 2 needs one local unit-test dependency, one test source, and the existing
CI workflow extended to run `:mobile-android:app:testDebugUnitTest`. It does not
need a ViewModel, repository, fake recognizer, DataStore test double, Compose UI
test, or general extraction from `MainActivity`.

Minimum pure tests:

- blank Development receives one trimmed, whitespace-normalized addition;
- existing Development is preserved exactly and receives one blank-line
  separator plus the normalized addition;
- Source, Current transcript, Interpretation, timestamps, duration, action
  state, list count, target position, and every other note remain unchanged;
- blank and prototype-placeholder transcripts return `InvalidTranscript` with
  no changed list;
- an unknown target ID returns `TargetMissing`;
- a changed Development baseline returns `TargetChanged` rather than
  overwriting the current value;
- applying the same attempt to the first returned list with its original
  baseline does not append twice; and
- a new attempt using the updated baseline may intentionally append the same
  transcript again.

## Safety and recovery behavior

| Situation | Required behavior |
| --- | --- |
| Permission denied | Keep every Idea unchanged. Show the existing permission failure in continuation context with **Try again** and **Return to Idea** paths. |
| Permission granted after retry | Start one continuation attempt for the same still-existing target; do not fall through to new-Idea capture. |
| Permission callback after cancellation | Compare the active attempt ID. Ignore a stale callback and do not start recognition. |
| Recognition unavailable or fatal error | Do not apply partial text. Keep the target available for retry or return, and visibly report the failure. |
| Blank or placeholder stopped result | Create no Idea and change no Development. Offer **Try again** or **Return to Idea**; do not expose **Save empty note** or **Save anyway**. |
| Target missing at start | Do not open continuation capture. Return to Inbox with a clear unavailable-target message. |
| Target missing at Stop | Discard the stopped result, create no Idea, and return to Inbox with a clear message. Never redirect the transcript into `saveCapture`. |
| Development changed during capture | Return `TargetChanged`, preserve the newer value, and offer a fresh retry from the updated Idea. |
| System Back, explicit cancel, or tab navigation before apply | Cancel the active recognizer, invalidate the attempt ID, clear transient transcript state, persist nothing, and return to the target detail when it still exists or Inbox otherwise. |
| Repeated Stop | The existing `isSavingCapture`/status guard accepts one Stop. The active attempt check and Development-baseline helper provide the second line of defense. |
| New capture requested after a failed/cancelled continuation | Clear the continuation target only when the new capture is actually accepted; ordinary and external entry points remain new-Idea mode. |
| Quick capture invoked while already recording a continuation | Preserve the existing no-restart behavior; do not clear the active target or create another session. |
| Process/activity loss before a successful apply | Ephemeral target and transcript disappear. No Idea changes because persistence occurs only after `Applied`. |

`AndroidSpeechTranscriber` currently exposes `stopAndGetPendingTranscript` and
`destroy`, but not a user-cancel operation. Item 4 may add a small `cancel()`
method to that same class which clears restart/Stop callbacks and invokes the
platform recognizer's `cancel()`. This is cancellation support for the existing
pipeline, not a second pipeline.

Android documents that `SpeechRecognizer.stopListening()` completes captured
speech through a later result/error, while `cancel()` cancels recognition; both
are main-thread APIs. Compose's existing `BackHandler` pattern can intercept
Back and route it through the same continuation cancellation function. Runtime
microphone permission should continue through the existing
`ActivityResultContracts.RequestPermission` launcher and remain tied to the
user's explicit action.

Official Android references:

- [`SpeechRecognizer`](https://developer.android.com/reference/android/speech/SpeechRecognizer)
  documents Stop, cancel, callback ordering, main-thread use, and the
  `RECORD_AUDIO` requirement.
- [Request runtime permissions](https://developer.android.com/training/permissions/requesting)
  recommends the existing activity-result permission contract and graceful
  behavior after denial.
- [Compose and other libraries](https://developer.android.com/develop/ui/compose/libraries)
  documents `BackHandler` for custom Compose Back behavior.

## Architecture impact

The current single-activity, Compose-state, Preferences DataStore architecture
is sufficient. Later items should make only these targeted changes:

- lift the selected detail ID from `InboxScreen` to `IdeaCaptureApp`;
- add one ephemeral continuation-target value beside existing capture state;
- pass one continuation callback from detail to the top-level owner;
- branch valid Stop application between existing new-Idea save and the pure
  continuation helper; and
- add cancellation to the existing transcriber when item 4 addresses recovery.

No new screen framework, ViewModel, repository, service, storage key, schema,
permission, raw-audio path, or transcription dependency is needed.

## Development boundaries after item 1

### Item 2: targeted automated regression foundation

Add the pure helper/result, its focused unit tests, the minimum local test
dependency, and the CI unit-test command. Do not add continuation UI or change
runtime capture behavior.

### Item 3: first successful path

Add the detail action, targeted selected-ID lift, visible continuation mode,
existing permission/capture reuse, valid Stop application, persistence, and
return to the updated detail. Non-applied results must already fail closed with
no Idea mutation; keep the change otherwise limited to one successful
continuation.

### Item 4: safety and recovery

Add mode-specific invalid-result handling, stale-attempt checks, safe missing
target behavior, explicit cancellation, navigation interception, and clear
retry/return states. Do not expand into general navigation redesign.

### Item 5: repeated continuation and utility integration

Verify and refine chronological repeated appends and immediate existing
detail/edit/search/share behavior while preserving every non-Development
field, note count, and order.

### Item 6: final regression

Add and complete the ordered device pass from Issue #72. Recheck all new-Idea
entry points so none can inherit a stale continuation target.

## Verification boundary for item 1

Automated checks for this documentation-only PR:

- `:mobile-android:app:assembleDebug` succeeds;
- `:mobile-android:app:testDebugUnitTest` is invoked and is expected to remain
  `NO-SOURCE` until item 2;
- `git diff --check` succeeds; and
- the complete changed-file list contains only this plan.

No physical-device behavior changed in item 1. Device verification begins with
the runtime continuation path and culminates in item 6. Do not mark item 1
complete until this plan is reviewed and merged, and do not begin item 2 in the
same PR.
