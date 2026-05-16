# Transcription-Model

Transcription-Model is an Android-first idea capture app for turning quick spoken thoughts into organized notes. The goal is to make it easy to capture an idea before it is forgotten, then review it later as a title, summary, tags, raw transcript, and possible action items.

## Product goal

Create a lightweight idea-capture experience that supports:

- Fast voice-first capture for short thoughts, reminders, and brainstorms.
- Automatic transcription from speech to text.
- Simple structuring of raw transcripts into readable notes.
- An Inbox where captured notes can be searched, edited, and deleted.
- Local-first persistence so notes remain available after the app is closed.

## Intended workflow

1. Open the app.
2. Start recording or dictation.
3. Speak naturally until the thought is captured.
4. Stop capture.
5. Review the generated note in an Inbox.
6. Search, edit, or delete saved notes as needed.

## Planned note structure

Each captured thought should eventually be represented with:

- **Title:** A short label generated from the transcript.
- **Summary:** A concise restatement of the main idea.
- **Raw transcript:** The original speech-to-text output.
- **Tags:** Lightweight keywords for filtering and review.
- **Action items:** Optional tasks or follow-ups detected in the note.
- **Created timestamp:** Recency information for Inbox sorting.

## Product documentation

- [SPEC-1: Idea Capture Transcription App](docs/SPEC-1-Idea-Capture-Transcription-App.md)
- [Next Steps: Idea Capture Transcription App](docs/NEXT_STEPS.md)

## Android prototype

The repository includes a native Kotlin Android prototype in [`mobile-android/app`](mobile-android/app). The prototype currently supports:

- Jetpack Compose UI.
- Capture and Inbox screens.
- One-tap start/stop interaction.
- Android speech recognition setup.
- Local note structuring into title, summary, tags, and action items.
- Local persistence for saved notes.
- Recency-ordered note inbox.

## Build

Install Android Studio and the Android SDK, then open the project locally.

From the repository root, run:

```bash
gradle :mobile-android:app:assembleDebug