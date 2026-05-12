# Next Steps: Idea Capture Transcription App

This document turns `SPEC-1` into an actionable engineering plan. It prioritizes the shortest path to a usable Android-first MVP while protecting the core promise: one-tap capture, live transcription, automatic save, and post-capture structuring.

## 1. Immediate decisions to unblock implementation

Before writing production code, make these choices explicit in issues or architecture decision records.

| Decision | Recommended default | Why it matters |
| --- | --- | --- |
| Android package name | `com.transcriptionmodel.ideacapture` | Needed before Firebase setup and app scaffolding. |
| Firebase project | One dev project first, production later | Keeps anonymous auth and Firestore rules testable early. |
| Relay hosting region | Same region as Firestore where possible | Reduces transcription/save latency. |
| Transcription audio format | 16 kHz mono PCM or provider-recommended realtime format | Avoids late rework in Android capture and relay streaming. |
| MVP auth mode | Firebase anonymous sign-in only, with account linking stubbed | Preserves no-onboarding UX while keeping future migration simple. |
| Audio retention | Do not persist audio after streaming | Matches MVP privacy and storage constraints. |

## 2. Phase 0: Repository foundation

Create the project skeleton before implementing features.

### Tasks

1. Add a Gradle Android workspace under `mobile-android/`.
2. Add a Ktor backend workspace under `backend-relay/`.
3. Add `infrastructure/` for Firestore rules, Cloud Run config, and deployment notes.
4. Add root-level developer documentation:
   * `docs/LOCAL_DEVELOPMENT.md`
   * `docs/ENVIRONMENT.md`
   * `docs/ADR/0001-architecture-baseline.md`
5. Add CI checks for formatting, unit tests, and basic build validation.

### Acceptance criteria

* A contributor can clone the repo and run one documented command to build the Android app shell.
* A contributor can run one documented command to start the backend relay locally.
* CI fails on broken Android or backend builds.

## 3. Phase 1: Android app shell and anonymous bootstrap

Build the minimum mobile app foundation before microphone or streaming work.

### Tasks

1. Create an Android Kotlin app using Jetpack Compose and Material 3.
2. Add screens:
   * capture home,
   * inbox,
   * note detail,
   * simple settings/about screen.
3. Configure Firebase Authentication.
4. Implement anonymous sign-in on first launch.
5. Add a Firestore-backed note repository interface with a fake implementation for local development.
6. Add a basic notes list ordered by `audit.updated_at desc`.

### Acceptance criteria

* Launching the app creates or reuses an anonymous Firebase user.
* The app can show an empty inbox without requiring account setup.
* A fake note can be rendered in inbox and note detail screens during local development.

## 4. Phase 2: Capture UI state machine

Implement the visible capture experience with a testable state model before real audio streaming.

### Tasks

1. Define capture states:
   * `Idle`,
   * `Connecting`,
   * `Recording`,
   * `Reconnecting`,
   * `Stopping`,
   * `Saved`,
   * `Structuring`,
   * `Structured`,
   * `Failed`.
2. Implement one large start/stop button on the home screen.
3. Render committed transcript text separately from the current partial transcript.
4. Use Android SpeechRecognizer callbacks to emit partial and final transcript text.
5. Add state transition unit tests.

### Acceptance criteria

* The user can tap once to enter recording mode and tap again to stop.
* The UI can show partial and final transcript text distinctly.
* Invalid state transitions are covered by tests.

## 5. Phase 3: Foreground service and local checkpoints

Build reliability before connecting to the real transcription provider.

### Tasks

1. Add microphone permission flow.
2. Implement an Android foreground service with a persistent notification.
3. Add a stop action to the notification.
4. Add Room tables for local note sessions and transcript segments.
5. Checkpoint transcript state every few seconds while recording.
6. Add process-restart recovery that detects incomplete local sessions.

### Acceptance criteria

* Recording mode can continue while the app is backgrounded.
* The user can stop capture from the notification.
* If the app process is killed during a speech-recognition capture session, the partial transcript can be recovered from Room.

## 6. Phase 4: Backend relay skeleton

Implement a secure relay shell before connecting upstream transcription.

### Tasks

1. Create a Ktor service with:
   * `GET /healthz`,
   * `wss://.../v1/stream/transcribe`,
   * `POST /v1/notes/{noteId}/structure` placeholder.
2. Verify Firebase ID tokens on WebSocket start messages.
3. Enforce basic per-user/session limits.
4. Add structured logs that exclude transcript content.
5. Add local integration tests for WebSocket start, audio chunk ack, stop, and error messages.

### Acceptance criteria

* Invalid or missing Firebase tokens are rejected.
* Valid sessions receive deterministic acknowledgements in local tests.
* Logs include correlation IDs but not transcript text.

## 7. Phase 5: Realtime transcription integration

Connect Android audio streaming through the relay to the transcription provider.

### Tasks

1. Encode and stream microphone audio chunks from Android.
2. Proxy chunks through the backend relay.
3. Forward partial and final transcription events back to the app.
4. Persist final transcript segments to Firestore idempotently by `(note_id, seq)`.
5. Assemble `raw_transcript` only from final segments.
6. Add reconnect behavior with bounded unsent audio buffering.

### Acceptance criteria

* A real spoken capture produces visible live transcript text on device.
* Stopping capture saves a transcript-only note in Firestore.
* Duplicate final segment messages do not duplicate text in the saved transcript.
* Network interruptions either reconnect or save a partial note with a clear failure reason.

## 8. Phase 6: Post-capture structuring

Add automatic note cleanup after transcript save.

### Tasks

1. Implement the structuring endpoint or worker.
2. Send only the raw transcript and required metadata to the LLM.
3. Validate the response schema for title, summary, tags, and action items.
4. Persist structured fields to Firestore.
5. Add retry support through WorkManager or backend idempotency.
6. Add a manual retry action in the note detail screen.

### Acceptance criteria

* Every successfully saved transcript enters structuring automatically.
* Notes eventually show title, summary, tags, and action items.
* Invalid LLM responses are rejected and retried without corrupting raw transcript text.

## 9. Phase 7: MVP hardening

Make the MVP safe enough for early testers.

### Tasks

1. Add Firestore security rules and emulator tests.
2. Add archive/soft-delete fields and UI actions.
3. Add transcript edit support with transcript revision tracking.
4. Add search over recent notes.
5. Add analytics for core product metrics.
6. Add crash reporting and operational dashboards.
7. Add privacy notes that explain transcript storage and lack of audio retention.

### Acceptance criteria

* Users can only read and write their own notes.
* Archived notes are hidden from the default inbox.
* Edited transcripts can be restructured without losing the original raw capture intent.
* MVP metrics are available for test cohorts.

## 10. Suggested first issues

Create these issues first, in order:

1. Scaffold Android Compose workspace.
2. Scaffold Ktor relay workspace.
3. Configure Firebase anonymous auth for development.
4. Add Firestore note schema and security rules draft.
5. Implement capture state machine around SpeechRecognizer partial/final transcript callbacks.
6. Implement foreground service notification for active speech capture.
7. Add Room checkpoint schema and recovery test.
8. Add relay WebSocket contract tests.
9. Add relay contract tests before replacing local SpeechRecognizer with provider streaming.
10. Integrate provider realtime transcription behind a feature flag.

## 11. Definition of MVP done

The MVP is done when all of the following are true:

* A new user can install the Android app and start recording without creating an account.
* The user can tap once to start capture and tap again to stop.
* Live transcript text appears while speaking.
* Capture continues while the app is backgrounded with a foreground-service notification.
* Stopping capture automatically saves a transcript-only note.
* Raw transcript text is preserved separately from structured output.
* The app automatically generates a title, summary, tags, and action items after each capture.
* The inbox is searchable and ordered by recency.
* Basic recovery prevents loss of interrupted sessions.
* Firestore rules prevent cross-user note access.
