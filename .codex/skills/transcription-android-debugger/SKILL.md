---
name: transcription-android-debugger
description: Diagnose and safely fix Android Studio, Gradle, Kotlin, Jetpack Compose, Logcat, emulator, physical-device, permission, speech-capture, and persistence failures in bassmegaman527-alt/Transcription-Model. Use only for the Transcription Project Android app; do not route unrelated Android projects here.
---

# Transcription Android Debugger

Find the first meaningful failure, connect it to the repository code that caused it, and recommend or implement the smallest safe correction. Preserve the app's current architecture and the user's one-focused-change-per-PR workflow.

## Start from the real project state

1. Determine whether the user wants diagnosis only or a fix. Inspection and diagnosis do not authorize edits. A request to fix or implement does.
2. Inspect the connected `bassmegaman527-alt/Transcription-Model` repository when it is available. Prefer the current branch and files over remembered snapshots.
3. Read repository instructions such as `AGENTS.md`, then inspect `git status`, the current branch, and relevant diffs before editing a local checkout. Preserve unrelated user changes.
4. Read the issue or checklist governing the requested work. Issue #7 established the rule to focus on the first unchecked item before adding features; it is now closed, so verify whether an active successor issue or an explicitly selected checklist item controls the task.
5. Read [references/project-map.md](references/project-map.md) for stable project paths, versions, and verification commands.

## Triage the evidence

Ask only for evidence that is materially missing. A build transcript, Logcat export, screenshot, stack trace, or exact reproduction sequence may already be sufficient.

Classify the failure before proposing a fix:

- **Build or sync:** Gradle configuration, dependency resolution, Kotlin compilation, resources, manifest merging, packaging, or install tasks.
- **Runtime:** launch crash, Compose exception, lifecycle/state bug, `SpeechRecognizer` failure, DataStore persistence, permission handling, or an ANR.
- **Device or environment:** SDK/JDK mismatch, missing platform tools, ADB state, emulator image, physical-device authorization, microphone availability, or Android Studio configuration.

Use the first meaningful failure, not the final summary line:

- For builds, start with the earliest compiler or task error that points to a project file. Later unresolved symbols and `BUILD FAILED` messages may be cascades.
- For crashes, identify the failing process and thread, then follow the complete `Caused by` chain to the deepest actionable cause. Prefer the first stack frame in `com.transcriptionmodel.ideacapture` over framework-only frames.
- For device issues, separate an app defect from ADB, emulator, permission, microphone, or OS behavior before changing source.
- Treat Gradle deprecation notices as warnings when the build says `BUILD SUCCESSFUL`. Do not upgrade Gradle or plugins merely to silence them.

For large Logcat files, run `python scripts/triage_logcat.py <logcat-file>` to extract crash, ANR, package, and app-frame signals. The script is an aid; confirm every conclusion against the surrounding raw log.

## Trace evidence to code

Search exact exception text, symbols, task names, package names, and app stack frames with `rg` or connected repository search. Open the smallest relevant set of files and trace callers before editing.

For this app, inspect these areas first when relevant:

- `mobile-android/app/src/main/java/com/transcriptionmodel/ideacapture/MainActivity.kt` for Compose UI, capture state, speech recognition, note structuring, and DataStore behavior.
- `mobile-android/app/src/main/AndroidManifest.xml` for permissions, components, and exported attributes.
- `mobile-android/app/build.gradle` and root Gradle files for Android, Kotlin, Compose, SDK, and dependency failures.

State the evidence as a causal chain: observed message -> relevant app frame or build task -> repository file and symbol -> why it fails. Distinguish verified facts from hypotheses.

## Make only the authorized change

When the user asks for a fix:

- Change the fewest files and lines that address the demonstrated cause.
- Preserve the single-module Kotlin/Jetpack Compose structure unless the selected issue requires an architectural change.
- Keep one logical change per branch and PR. Do not bundle cleanup, formatting sweeps, dependency refreshes, or unrelated features.
- Do not edit generated build output or IDE caches.
- Do not force-push, reset, discard changes, delete caches, invalidate broad state, or perform broad Gradle/plugin/dependency upgrades without warning the user and obtaining explicit authorization.
- Avoid logging or reproducing raw transcript content when metadata and error codes are enough. Redact transcript text from production-oriented logging.

If the repository is dirty and the intended change overlaps user edits, stop and ask how to proceed. Do not overwrite their work.

## Verify at the same layer

Re-run the failing command or reproduction first, then add the narrowest relevant checks:

- Build from the repository root with the checked-in Gradle wrapper.
- For runtime work, install and reproduce on the same emulator or physical device and Android version when possible.
- For capture changes, check permission grant and denial, Start, Stop, transcript display, save, and the relevant error path.
- For persistence changes, save a note, fully close and reopen the app, and confirm the note remains without duplication.
- For crash fixes, confirm the original exception no longer appears and scan for the next meaningful failure rather than claiming the entire app is fixed.

Do not claim success from static inspection alone. If the environment cannot run a required check, say exactly which check remains for Android Studio or the user's phone.

## Report the result

Lead with:

1. **First meaningful failure** — exact error, task, or exception.
2. **Cause** — repository path and symbol, with the causal explanation.
3. **Action** — smallest fix made or recommended; say explicitly when no files were changed.
4. **Verification** — exact command and device steps, including observed results and remaining checks.
5. **Scope** — issue/checklist item addressed and files changed, suitable for one focused PR.

When several errors are present, label secondary errors as downstream rather than presenting them as independent root causes.
