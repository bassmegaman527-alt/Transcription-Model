# Transcription Project map

Use this reference for stable project-specific context. Reinspect the repository before relying on details that may have changed.

## Repository and product

- Repository: `bassmegaman527-alt/Transcription-Model`
- Default branch: `main`
- Android module: `:mobile-android:app`
- Application ID and namespace: `com.transcriptionmodel.ideacapture`
- Current prototype: Android-first voice idea capture with Capture and Inbox screens, speech-to-text, lightweight note structuring, search/edit/delete behavior, and local persistence.

## Current implementation anchors

- Main Android source: `mobile-android/app/src/main/java/com/transcriptionmodel/ideacapture/`
- Primary implementation file: `MainActivity.kt`
- Manifest: `mobile-android/app/src/main/AndroidManifest.xml`
- Module build file: `mobile-android/app/build.gradle`
- Root build files: `build.gradle`, `settings.gradle`, `gradle.properties`
- Wrapper: `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.properties`

The present prototype uses Kotlin, Jetpack Compose, Android `SpeechRecognizer`, and Preferences DataStore. Do not assume proposed future modules such as `feature-capture`, Room, WorkManager, a foreground service, or backend components already exist; confirm them in the current tree.

## Toolchain baseline

- Gradle wrapper: 8.9
- Android Gradle Plugin: 8.7.3
- Kotlin and Compose plugin: 2.0.21
- `compileSdk` / `targetSdk`: 35
- `minSdk`: 26
- Java/Kotlin target: 11

Use the checked-in wrapper rather than a globally installed Gradle version:

```powershell
.\gradlew.bat :mobile-android:app:assembleDebug
```

```bash
./gradlew :mobile-android:app:assembleDebug
```

If Android Studio invokes the same task successfully but prints Gradle deprecation warnings, record the warnings separately; they are not the cause of a successful build failing.

## Issue #7 development rule

Issue #7, **Daily-use MVP Checklist**, established: "Focus only on the first unchecked item before adding new features." Its checklist covered the capture loop, reliability, and usability. The issue is closed as completed even though its body still displays unchecked boxes. Preserve the one-item focus, but do not infer current project status from those stale boxes. Check the current issue, branch, PR, and user request.

## Runtime evidence

Filter Logcat around:

- process/package `com.transcriptionmodel.ideacapture`
- `FATAL EXCEPTION`, `AndroidRuntime`, `Caused by`, and `ANR in`
- the first app-owned stack frame under `com.transcriptionmodel.ideacapture`
- `SpeechRecognizer` callbacks and Android speech error codes
- permission denial or microphone availability messages
- DataStore read/write exceptions

For privacy, prefer `note_id`, `uid`, `session_id`, duration/latency, reconnect count, and error code over raw transcript text in logs.
