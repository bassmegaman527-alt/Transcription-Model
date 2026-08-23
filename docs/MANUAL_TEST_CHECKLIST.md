# Manual Test Checklist

Use this checklist for Android changes that could affect the Idea Capture user experience. Run the relevant sections on an emulator or physical Android device and record the device/API level in the pull request.

## Setup

- [ ] Build the debug app with `./gradlew :mobile-android:app:assembleDebug`.
- [ ] Install and launch the debug app.
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

## Note details and sharing

- [ ] Opening a note reveals its raw transcript and any action items.
- [ ] **Share** opens the Android share sheet.
- [ ] Shared text includes title, summary, tags, raw transcript, and created time.
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
