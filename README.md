# Transcription-Model

Transcribe, summarize, and brainstorm with an Android-first idea capture app.

## Product documentation

* [SPEC-1: Idea Capture Transcription App](docs/SPEC-1-Idea-Capture-Transcription-App.md)
* [Next Steps: Idea Capture Transcription App](docs/NEXT_STEPS.md)
* [GitHub Main Page Tabs and Sections](docs/GITHUB_MAIN_TABS.md)
* [Implementation Backlog](docs/IMPLEMENTATION_BACKLOG.md)
* [Contributing Guide](CONTRIBUTING.md)

## Android prototype

The repository now includes a native Kotlin Android prototype in [`mobile-android/app`](mobile-android/app). The prototype implements:

* a Jetpack Compose capture screen with one-tap start/stop interaction,
* a foreground service and persistent notification stub for background capture,
* Android SpeechRecognizer-powered live speech-to-text for local capture before provider integration,
* automatic local structuring into title, summary, tags, and action items,
* a recency-ordered note inbox with expandable raw transcript details.

### Build

Install the Android SDK, then run:

```bash
gradle :mobile-android:app:assembleDebug
```

The current container used for this repository cannot complete Android builds because it lacks the Android SDK and cannot reach Google Maven through the network proxy. Run the build in a local Android development environment or CI image with Android SDK platform 35 installed.
