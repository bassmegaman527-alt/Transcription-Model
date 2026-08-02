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
