# Manual Test Checklist

Use this checklist for Android changes that could affect the Idea Capture user experience. Run the relevant sections on an emulator or physical Android device and record the device/API level in the pull request.

## Setup

- [ ] Build the debug app with `./gradlew :mobile-android:app:assembleDebug`.
- [ ] Before an update-over-existing-data check, record the current note count and non-sensitive identifying details for at least two saved notes.
- [ ] Install the debug build without clearing app storage or uninstalling the prior build when running the update check.
- [ ] Launch the debug app.
- [ ] Confirm the app opens without crashing.
- [ ] Record the Android device or emulator and API level used.

## Capture

- [ ] The Capture tab initially says **Ready to capture your idea.**
- [ ] Tapping **Start** requests microphone permission when needed.
- [ ] Granting permission starts recognition and shows **Listening...**
- [ ] Denying permission shows a useful error and does not crash.
- [ ] Recognized speech appears in the live transcript.
- [ ] Tapping **Stop** after meaningful speech saves exactly one note and opens Inbox.
- [ ] A blank capture opens the no-speech confirmation dialog.
- [ ] Discarding a blank capture returns to the idle Capture screen without adding a note.
- [ ] Confirming **Save empty note** adds exactly one empty note.

## External capture access

Run the shared lifecycle checks once from the launcher **Quick capture** shortcut
and once from the Quick Settings **Quick capture** tile. Record the surface used
for any failure.

### Surface setup

- [ ] Long-pressing the app icon shows the launcher **Quick capture** shortcut.
- [ ] Dragging the launcher shortcut to the home screen creates a working pinned shortcut.
- [ ] Quick Settings edit mode shows the **Quick capture** tile.
- [ ] Adding, removing, and re-adding the tile leaves it available and functional.
- [ ] Launching from the ordinary app icon opens Capture without starting recognition.

### Shared lifecycle checks

- [ ] With microphone permission granted and the app removed from Recents, invoking the surface opens Capture and shows **Listening now** exactly once.
- [ ] With a prior note already saved and the app open on Inbox or About, invoking the surface returns to Capture, starts listening once, and leaves the prior note unchanged.
- [ ] Invoking the same surface again while already listening does not restart recognition, clear the live transcript, or create another capture session.
- [ ] Speaking meaningful text and tapping **Stop recording** adds exactly one note and opens Inbox.
- [ ] Fully closing and reopening the app preserves the prior note and the new note without duplication.
- [ ] With microphone permission reset, invoking the surface uses the existing permission prompt.
- [ ] Denying microphone permission shows the existing error, starts no recording, and saves no note.
- [ ] Granting permission on retry starts listening exactly once; Stop saves exactly one note.
- [ ] After an external capture, Inbox search, note editing, sharing, and single-note deletion remain functional.

### Quick Settings lock-screen checks

- [ ] On a securely locked device, tapping the tile requests authentication before showing the app or transcript content.
- [ ] Canceling or failing authentication leaves the device locked and does not start recording or save a note.
- [ ] Completing authentication opens Capture and starts listening exactly once.
- [ ] Stopping a lock-screen-originated capture saves exactly one note through the normal Inbox flow.

### Cross-entry sequence

- [ ] Save one meaningful capture from the launcher shortcut, then one from the Quick Settings tile; Inbox contains exactly two new notes.
- [ ] Reopen the app from the ordinary icon after both captures; neither external entry action is replayed and no additional note is created.

## Inbox and persistence

- [ ] Existing notes appear in newest-first order.
- [ ] Search filters notes using expected note content.
- [ ] Editing a note updates it without creating a duplicate.
- [ ] Canceling an edit leaves the note unchanged.
- [ ] Deleting one note requires confirmation and removes only that note.
- [ ] Closing and reopening the app retains saved edits and deletions.

## Expandable Idea persistence and regression

Use non-sensitive, distinctive marker words for Source, Current transcript, and
Development so each stored field can be recognized without exposing private
content. Complete this section in order and do not clear app data between steps.

### Existing-note compatibility

- [ ] After updating without clearing storage, the recorded pre-existing note count is unchanged.
- [ ] The recorded notes retain their title, summary, tags, action items, transcript, timestamp, duration, and newest-first order.
- [ ] A pre-existing note retains its displayed Source, or adopts its prior transcript as Source when upgrading from a pre-Source build, and shows empty Development when none was added.
- [ ] Fully close and reopen the app; the same notes return without loss or duplication.

### Source preservation and Development

- [ ] From the ordinary app icon, save one meaningful capture containing a unique Source marker; Inbox adds exactly one note.
- [ ] Open the note and record its displayed Source, Interpretation title, summary, tags, action items, timestamp, and duration.
- [ ] Edit the Current transcript to remove the Source marker and add a different marker; add multiline Development with a third marker; save.
- [ ] Detail still shows the original Source exactly as recorded, Current transcript shows the edited text, and Development shows the saved additions.
- [ ] Interpretation reflects the Current transcript while the note count, timestamp, and duration remain unchanged.
- [ ] Fully close and reopen the app; Source, Current transcript, Interpretation, and Development all return without duplication.
- [ ] Edit only Development and save; Source, Current transcript, and Interpretation remain unchanged.
- [ ] Change Development and cancel editing; the previously saved Development and Source remain unchanged.

### Search and sharing

- [ ] Search for the Source marker removed from Current transcript; the note still appears.
- [ ] Search for the Current transcript marker; the note appears.
- [ ] Search for the Development marker; the note appears.
- [ ] Search with one Source term and one Development term; all-term matching returns the note.
- [ ] Share the note; shared text labels Interpretation and Source, includes Original and Current transcript separately, and includes Development.
- [ ] Share a note with no Development and an unchanged transcript; shared text omits empty Development and duplicate Current transcript sections.
- [ ] Clear all Development, save, fully close and reopen, and confirm Development remains empty while Source and Current transcript remain unchanged.
- [ ] The cleared Development marker no longer returns the note in search.

### Capture-entry and deletion regression

- [ ] Complete the Capture section once through the ordinary app-icon flow, including permission grant/denial and blank-capture behavior.
- [ ] Complete the External capture access shared lifecycle once for the launcher shortcut and once for the Quick Settings tile.
- [ ] One meaningful Stop from each of the three entry paths creates exactly one new note; reopening the app creates no duplicates.
- [ ] Delete a different single note; the Source-preservation test note and every other note remain unchanged after reopen.
- [ ] Complete the About and delete-all section; reopen to confirm the Inbox stays empty, then save one new capture successfully.

## Voice capture continuity regression

Run this section as the final Issue #65 physical-device pass after installing
the current PR build without clearing app data. Record the device model, Android
and API version, active speech-recognition service when visible, network state,
and tested commit in the pull request. Keep any notes that still need to be
preserved until the final delete-all check.

Use fixed, non-sensitive marker phrases so missing or repeated words are easy to
spot. Say **alpha orchard** before a pause and **bravo harbor** after it. For a
second and third pause, continue with **charlie meadow** and **delta river**.
Recognizer punctuation and capitalization may vary, but each marker word should
remain present in order and should not be duplicated.

### Ordinary Capture pause matrix

Start a new recording from the ordinary Capture screen for each trial. Repeat
each trial three times. Check a row only when every repetition remains visibly
**Listening now** until Stop, detects resumed speech promptly, retains the
earlier markers, does not consistently clip the first resumed marker, contains
no repeated boundary words or segments, stops promptly, and saves exactly one
new Idea.

- [ ] Speak the marker phrases continuously with no deliberate pause.
- [ ] Pause about 0.5-1 second between **alpha orchard** and **bravo harbor**.
- [ ] Pause about 2 seconds between **alpha orchard** and **bravo harbor**.
- [ ] Pause about 3 seconds between **alpha orchard** and **bravo harbor**.
- [ ] Pause about 5 seconds between **alpha orchard** and **bravo harbor**.
- [ ] Use repeated thinking pauses of about 1-3 seconds between all four marker phrases in one capture.
- [ ] After a 2-second pause, resume with **bravo** spoken quietly, then say **harbor** normally.
- [ ] After a 2-second pause, resume with **bravo harbor** at a normal/loud volume.
- [ ] Press **Stop recording** once while speech is active; it returns promptly and saves one complete Idea without restarting capture.
- [ ] Press **Stop recording** once immediately after a 3-5-second pause; it returns promptly and saves one Idea without adding duplicate text.

### Entry points and repeated captures

- [ ] Opening the ordinary app icon shows Capture without starting the microphone; tapping **Start recording** starts exactly one capture.
- [ ] From the pinned launcher **Quick capture** shortcut, complete a representative 2-3-second pause trial; Stop saves exactly one Idea.
- [ ] From the Quick Settings **Quick capture** tile, complete a representative 2-3-second pause trial; Stop saves exactly one Idea.
- [ ] Complete two back-to-back ordinary captures without force-closing the app; each Stop adds exactly one Idea and the first Idea remains unchanged.
- [ ] Save one Idea from each entry point in sequence, then reopen from the ordinary icon; the note count has increased by exactly three and no external action is replayed.
- [ ] Fully close and reopen the app after the repeated captures; all saved Ideas return once, in newest-first order, with no duplicates.

### Permission and recognition-error paths

- [ ] Reset microphone permission in system settings, return to the app, tap **Start recording**, and deny permission; no capture starts, no Idea is saved, and the permission error is visible.
- [ ] Tap **Start recording** again and grant permission; listening starts exactly once and Stop saves exactly one Idea.
- [ ] During the longer and repeated-pause trials, any normal internal recognition boundary stays **Listening now** and does not display **Listening interrupted**.
- [ ] If the active recognition service reports a genuine network, server, audio, or availability failure, the app visibly reports the interruption instead of silently appearing active. Record the error category without private transcript content. Do not disable or uninstall system components solely to force this check.

### Existing Idea regression after continuity trials

- [ ] Complete **Existing-note compatibility** and confirm update-over-existing-data and full-reopen persistence without loss or duplication.
- [ ] Complete **Source preservation and Development** and confirm editing does not change the original Source or create another Idea.
- [ ] Complete **Search and sharing** and confirm Source, Current transcript, and Development remain searchable and shared with the expected labels.
- [ ] Complete **Note details and sharing** and confirm all saved Idea sections remain readable and shareable.
- [ ] Delete one selected Idea, fully reopen the app, and confirm only that Idea was removed.
- [ ] Complete **About and delete all** last, reopen to confirm the Inbox remains empty, then save one new capture successfully.

## Note details and sharing

- [ ] Opening a note visibly separates Source, Interpretation, and Development.
- [ ] When Current transcript differs from Source, both are labeled and readable.
- [ ] Interpretation includes the title, summary, tags, and action items.
- [ ] Development shows saved content or the **No development added yet.** empty state.
- [ ] **Share** opens the Android share sheet.
- [ ] Shared text labels Interpretation and Source and includes title, summary, tags, Original transcript, and created time.
- [ ] Shared text includes Current transcript only when it differs from Source.
- [ ] Shared text includes Development only when content has been saved.
- [ ] Shared text includes action items when the note has them.

## About and delete all

- [ ] The About tab shows the app description, privacy note, and version label.
- [ ] **Delete all notes** opens a confirmation dialog.
- [ ] Canceling delete-all keeps every note.
- [ ] Confirming delete-all removes every note.
- [ ] Reopening the app after delete-all still shows an empty Inbox.
- [ ] A new capture can be saved after deleting all notes.

## Regression notes

Document failed checks, unexpected behavior, screenshots, and follow-up issues in the pull request. Do not include private transcript or note content.
