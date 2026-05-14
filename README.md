# Transcription-Model

Transcription-Model is an Android idea-capture app for quickly saving small thoughts before they are forgotten. Open the app, tap Start, speak naturally, tap Stop, and the app saves a structured note to a local Inbox.

## Current working state

The Android app is confirmed to work with:

- Android Studio project recognition from the repository root.
- Successful Gradle sync/build in Android Studio.
- Physical Android phone execution.
- Android `SpeechRecognizer` speech-to-text.
- Start/Stop thought capture flow.
- Saved notes in the Inbox.
- Local note persistence with DataStore after closing and reopening the app.
- Inbox search/filter.
- Note editing for title and raw transcript.
- Delete note with confirmation.

## Features

- **Immediate voice capture:** Start and stop capture with a simple two-button-state flow.
- **Live speech-to-text:** Uses Android `SpeechRecognizer` for partial and final transcript updates.
- **Structured notes:** Captured transcripts are converted into a title, summary, tags, and action items.
- **Local Inbox:** Saved notes appear in a recency-oriented Inbox.
- **Local persistence:** Notes are stored on-device with Jetpack DataStore.
- **Search/filter:** Inbox search matches note title, raw transcript, summary, tags, and action items.
- **Edit notes:** Edit saved note titles and raw transcripts.
- **Delete notes:** Delete notes after a confirmation prompt.

## Project structure

```text
.
├── build.gradle
├── gradle.properties
├── settings.gradle
└── mobile-android/
    └── app/
        ├── build.gradle
        └── src/main/
            ├── AndroidManifest.xml
            └── java/com/transcriptionmodel/ideacapture/
                ├── AndroidSpeechTranscriber.kt
                └── MainActivity.kt
```

## Open in Android Studio

1. Clone the repository.
2. Open Android Studio.
3. Select **File > Open**.
4. Choose the repository root directory, not only `mobile-android/app`.
5. Let Gradle sync complete.
6. Select the app run configuration for `mobile-android:app` if Android Studio does not select it automatically.

## Run and test

### Physical Android phone

1. Enable Developer Options and USB debugging on the phone.
2. Connect the phone to your computer.
3. In Android Studio, select the connected phone as the target device.
4. Click **Run**.
5. Grant microphone permission when prompted.
6. Test the core flow:
   - Tap **Start**.
   - Speak a short thought.
   - Confirm live transcript text appears.
   - Tap **Stop**.
   - Confirm the note appears in the Inbox.
   - Close and reopen the app.
   - Confirm the note persists.
   - Try Inbox search, edit, and delete.

### Emulator

1. Create or start an Android emulator with microphone input enabled.
2. Select the emulator as the target device in Android Studio.
3. Click **Run**.
4. Grant microphone permission.
5. Test the same Start/Stop, Inbox, persistence, search, edit, and delete flows.

## Current limitations and future ideas

- Notes are local-only; there is no cloud sync yet.
- Speech recognition depends on Android `SpeechRecognizer` availability on the device.
- The app stores transcripts and structured note data, not audio playback.
- Structuring is currently lightweight and local; richer AI structuring can be added later.
- Future improvements could include export/share, tags UI, backup/sync, home-screen shortcuts, and more robust long-session capture.
