# Implementation Backlog

Now that the GitHub repository tabs, labels, milestones, and project board are configured, create issues from this backlog and move them through the MVP board. These are ordered to get from the current prototype to a usable capture MVP.

## Sprint 1: Make the Android prototype buildable and testable

### 1. Add Android CI build

**Labels:** `area:android`, `type:task`, `priority:p0`

**Goal:** Add GitHub Actions so every pull request builds the Android app in an Android SDK environment.

**Acceptance criteria:**

- [ ] CI installs or uses Android SDK platform 35.
- [ ] CI runs `gradle :mobile-android:app:assembleDebug`.
- [ ] README build status badge is added after the workflow is stable.

### 2. Add unit tests for transcript structuring

**Labels:** `area:android`, `type:task`, `priority:p0`

**Goal:** Verify `structureTranscript` generates title, summary, tags, and action items predictably.

**Acceptance criteria:**

- [ ] Tests cover short transcript title generation.
- [ ] Tests cover summary truncation.
- [ ] Tests cover action item extraction.
- [ ] Tests cover fallback tags.

### 3. Split Compose screens into feature files

**Labels:** `area:android`, `type:task`, `priority:p1`

**Goal:** Move capture and inbox UI out of `MainActivity` so the app can scale cleanly.

**Acceptance criteria:**

- [ ] Capture UI lives in a capture screen file.
- [ ] Inbox UI lives in an inbox screen file.
- [ ] MainActivity only wires app state and navigation.

## Sprint 2: Replace prototype state with app architecture

### 4. Add ViewModel state management

**Labels:** `area:android`, `type:task`, `priority:p0`

**Goal:** Move capture state, note list state, and speech recognition callbacks out of composables.

**Acceptance criteria:**

- [ ] Capture state is managed by a ViewModel.
- [ ] Inbox state is managed by a ViewModel or repository abstraction.
- [ ] Composables render immutable UI state and emit events.

### 5. Add local repository abstraction

**Labels:** `area:android`, `area:firebase`, `type:task`, `priority:p0`

**Goal:** Introduce interfaces for notes and capture sessions before adding Firestore and Room.

**Acceptance criteria:**

- [ ] `NoteRepository` interface supports create, list, update, and archive operations.
- [ ] Fake repository powers the prototype.
- [ ] Repository API mirrors the Firestore note schema from the spec.

### 6. Add Room checkpoint schema

**Labels:** `area:android`, `type:task`, `priority:p1`

**Goal:** Add local persistence for interrupted capture sessions.

**Acceptance criteria:**

- [ ] `local_note_sessions` entity exists.
- [ ] `local_transcript_segments` entity exists.
- [ ] DAO can upsert partial sessions and finalized segments.
- [ ] Recovery query returns incomplete sessions.

## Sprint 3: Build real capture foundations

### 7. Harden microphone capture pipeline

**Labels:** `area:android`, `area:transcription`, `type:feature`, `priority:p0`

**Goal:** Build on the SpeechRecognizer prototype with a provider-ready microphone audio pipeline behind a feature flag.

**Acceptance criteria:**

- [ ] Runtime microphone permission flow is user-friendly.
- [ ] Audio chunks are produced at the target sample rate.
- [ ] Capture can start and stop repeatedly without crashing.
- [ ] No audio is persisted to disk.

### 8. Connect foreground service to capture state

**Labels:** `area:android`, `type:task`, `priority:p0`

**Goal:** Make the foreground service own long-running capture and expose state to the UI.

**Acceptance criteria:**

- [ ] Capture continues when the app backgrounds.
- [ ] Notification stop action stops the active session.
- [ ] UI can rebind and display the active transcript state.

### 9. Add backend relay skeleton

**Labels:** `area:backend`, `type:task`, `priority:p0`

**Goal:** Create a Ktor relay service with health check and WebSocket contract endpoints.

**Acceptance criteria:**

- [ ] `GET /healthz` returns healthy status.
- [ ] WebSocket accepts start, audio chunk, ping, and stop messages.
- [ ] Relay emits deterministic ack and error messages in tests.
- [ ] Transcript content is excluded from logs.

## Sprint 4: Firebase and MVP persistence

### 10. Configure Firebase anonymous auth

**Labels:** `area:firebase`, `area:android`, `type:feature`, `priority:p0`

**Goal:** Allow immediate anonymous use and prepare for future account linking.

**Acceptance criteria:**

- [ ] App signs in anonymously on first launch.
- [ ] Existing anonymous user is reused on relaunch.
- [ ] Auth failures are surfaced without blocking local prototype mode.

### 11. Add Firestore note persistence

**Labels:** `area:firebase`, `area:android`, `type:feature`, `priority:p0`

**Goal:** Save transcript-only notes and structured output to Firestore.

**Acceptance criteria:**

- [ ] Notes are scoped by Firebase UID.
- [ ] Inbox reads notes ordered by `audit.updated_at desc`.
- [ ] Raw transcript is stored separately from structured output.
- [ ] Soft-delete/archive fields are represented.

### 12. Add Firestore security rules and tests

**Labels:** `area:firebase`, `type:task`, `priority:p0`

**Goal:** Prevent cross-user transcript access before real user testing.

**Acceptance criteria:**

- [ ] Authenticated users can read/write only their own notes.
- [ ] Anonymous users are treated as authenticated owners.
- [ ] Emulator tests cover allowed and denied reads/writes.

## Sprint 5: Live transcription and structuring

### 13. Integrate realtime transcription through relay

**Labels:** `area:backend`, `area:transcription`, `area:android`, `type:feature`, `priority:p0`

**Goal:** Stream Android audio through the relay and display partial/final transcript events.

**Acceptance criteria:**

- [ ] Partial transcript appears while speaking.
- [ ] Final transcript segments are persisted idempotently.
- [ ] Reconnect behavior handles short network interruptions.
- [ ] Failed sessions save partial transcript with clear status.

### 14. Implement post-capture structuring worker

**Labels:** `area:backend`, `area:transcription`, `type:feature`, `priority:p1`

**Goal:** Generate title, summary, tags, and action items after each saved capture.

**Acceptance criteria:**

- [ ] Structuring request includes idempotency key.
- [ ] LLM response is schema-validated.
- [ ] Invalid responses are retried or marked failed.
- [ ] Raw transcript is never overwritten by generated text.

## Recommended first move

Create issues 1 through 6 first. They make the current prototype easier to build, test, and extend before adding provider credentials, Firebase persistence, or production infrastructure.
