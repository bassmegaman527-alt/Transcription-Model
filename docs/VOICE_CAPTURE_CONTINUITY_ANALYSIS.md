# Voice Capture Continuity Analysis

Issue #65 aims to keep an intentional capture logically active until the user
presses **Stop**, even when Android ends an internal recognition request during
a natural thinking pause. This document records the current lifecycle, the
device-dependent boundary that must be measured, and the smallest recommended
first implementation.

This analysis does not change recognition behavior. Physical-device results
must be recorded on the pull request or Issue #65 before checklist item 1 is
considered verified.

## Current implementation

All three capture entry points reuse `AndroidSpeechTranscriber`:

- ordinary Capture screen;
- pinned launcher **Quick capture**; and
- Quick Settings **Quick capture**.

`MainActivity.IdeaCaptureApp` owns one transcriber instance. A partial result
replaces the current `CaptureSession.partialTranscript`. A final result is
appended to `committedTranscript`, clears the partial text, and remains present
across later recognition requests. Explicit Stop combines the committed text,
the last UI partial, and the transcriber's pending result before saving exactly
one Idea.

The transcriber currently requests:

- free-form speech in the device locale;
- partial results and one best result;
- 750 ms of possibly-complete silence;
- 1,500 ms of complete silence;
- a 250 ms restart after results and recoverable errors; and
- a separate 2,000 ms restart scheduled from `onEndOfSpeech()`.

The two silence values are recognizer hints, not guarantees. Android documents
that implementations may ignore them and advises that custom silence values
are rarely appropriate outside segmented sessions.

## Recognition lifecycle and gap locations

The current lifecycle is:

1. Start sets `shouldKeepListening`, creates the recognizer if needed, and
   calls `startListening()`.
2. `isStartPending` blocks another start only until `onReadyForSpeech()`.
3. Partial results replace the current partial text shown by the Capture UI.
4. `onEndOfSpeech()` schedules another `startListening()` for 2,000 ms later.
5. If `onResults()` arrives first, it cancels that callback, commits the final
   segment, and schedules a restart 250 ms later.
6. If a recoverable `onError()` arrives first, it schedules a restart 250 ms
   later. Other errors end automatic listening.
7. Stop cancels scheduled restarts, asks the recognizer to stop, and waits up
   to 1,500 ms for a terminal result before falling back to the latest partial.

Android requires a client to wait for `onResults()` or `onError()` before
starting the next request. `onEndOfSpeech()` is not that terminal callback.
This creates two important timing paths:

| Boundary path | Current behavior | Continuity risk |
| --- | --- | --- |
| Final result arrives less than 2 seconds after end of speech | The 2-second callback is cancelled; the next request starts 250 ms after the result | The microphone is unavailable for the provider's result latency, the fixed 250 ms app delay, and recognizer startup. Speech resumed in that interval can be clipped. |
| Final result takes 2 seconds or longer | The scheduled start can run before the prior request has produced a result or error | The app can violate the recognizer contract and provoke client or recognizer-busy errors, adding another retry delay. |
| Recoverable error | The UI receives the error, then the app waits 250 ms before retrying | Resume latency includes the fixed delay and startup; the old partial can be replaced by a new request before becoming final. |
| Custom endpointing is honored | A pause near or above the recognizer's interpretation of the 750/1,500 ms hints can end the request | Ordinary thinking pauses are more likely to cross an internal boundary. |
| Custom endpointing is ignored | The recognition service selects its own endpoint | Pause behavior varies by device, service version, language, network state, and whether recognition is local or remote. |

The Capture screen remains in `CaptureStatus.Recording` across a normal
`onEndOfSpeech()` boundary, so the visible recording state can remain active
while the recognizer is not yet ready for resumed speech.

## Transcript continuity constraints

The existing accumulation behavior should be preserved in later changes:

- completed segments stay in `committedTranscript` across restarts;
- the current partial is replaced rather than committed until a final result;
- `appendTranscript` avoids a duplicate when the next segment is already the
  committed suffix or extends the entire committed transcript; and
- Stop uses the latest available final or partial text.

Two boundary cases need explicit regression coverage but are not changed in
item 1:

- a recoverable error can leave an uncommitted partial that a later partial
  replaces; and
- partially overlapping results that do not fully contain one another can
  repeat words when appended.

These belong to Issue #65 item 5 unless item 2 directly exposes a regression.

## Android option comparison

| Option | Compatibility and behavior | Decision |
| --- | --- | --- |
| Restart only after a terminal result/error | Supported across the app's API 26+ range and follows the `SpeechRecognizer` request contract | **Recommended for item 2.** Remove the app-created pre-terminal restart and eliminate the fixed normal-result delay. |
| Use recognizer default endpointing | Omitting the custom silence extras lets each recognition service use its defaults; behavior remains implementation-dependent | Evaluate in item 3 after measuring item 2. Android's documentation cautions against custom values without a specific reason. |
| Increase custom silence thresholds | May keep a supporting recognizer open longer, but values may be ignored and can delay final results and Stop | Do not select as the first fix. Compare only with defaults in item 3. |
| API 33+ segmented recognition | Can return `onSegmentResults()` until `onEndOfSegmentedSession()`, but support is optional and the request may have no effect | Defer to item 4. Any implementation needs API 26–32 and unsupported-recognizer fallback behavior. |
| Switch to the on-device recognizer | Availability begins on newer Android versions and is not a continuity guarantee | Out of scope for the first fix; changing recognizer selection introduces a separate compatibility decision. |

For API 33+, `checkRecognitionSupport()` can query whether a recognition
intent is supported, but a recognition service may return
`ERROR_CANNOT_CHECK_SUPPORT`. Segmented recognition therefore cannot be treated
as universally available based only on the OS version.

Official Android references:

- [`SpeechRecognizer`](https://developer.android.com/reference/android/speech/SpeechRecognizer)
  documents terminal-callback ordering, busy errors, support checks, and the
  limits of using this API for continuous recognition.
- [`RecognizerIntent`](https://developer.android.com/reference/android/speech/RecognizerIntent)
  documents silence hints and `EXTRA_SEGMENTED_SESSION`.
- [`RecognitionListener`](https://developer.android.com/reference/android/speech/RecognitionListener)
  documents ordinary and segmented callbacks.

## Physical-device baseline

Use non-sensitive fixed words so results can be compared without logging
private transcript content. Run this matrix from the ordinary Capture screen on
the physical development device before implementing item 2.

Record with the result:

- device model and Android/API version;
- active speech recognition service and version when visible in system
  settings;
- app commit;
- network state; and
- whether the same behavior repeats across three trials.

For a single-pause trial, say **alpha**, wait for the listed pause, then say
**bravo charlie** and press Stop after the resumed words appear or after a clear
failure. For the volume rows, say **bravo** quietly or at normal/loud volume as
directed. Do not use a metronome sound that could be interpreted as microphone
input.

| Trial | Minimum repetitions | Record |
| --- | ---: | --- |
| Continuous speech, no deliberate pause | 3 | Complete text, duplicates, Stop response, saved-Idea count |
| Approximately 0.5-second pause | 3 | Whether `bravo` is present and promptly appears |
| Approximately 1-second pause | 3 | Whether `bravo` is present and promptly appears |
| Approximately 2-second pause | 3 | Whether `bravo` is present, any visible interruption, and resume delay |
| Approximately 3-second pause | 3 | Whether `bravo` is present, any visible interruption, and resume delay |
| Approximately 5-second pause | 3 | Whether `bravo` is present, any visible interruption, and resume delay |
| Multiple 1–3-second pauses in one capture | 3 | All segments retained in order without repetition |
| Approximately 2-second pause, quiet first resumed word | 3 | Whether the first resumed word is consistently clipped |
| Approximately 2-second pause, normal/loud first resumed word | 3 | Whether the first resumed word is consistently clipped |

For every row confirm:

- Capture remains logically active until Stop;
- resumed speech is detected without a noticeable multi-second dead period;
- the first resumed word is not consistently clipped;
- earlier transcript content remains present;
- resumed content is not duplicated;
- Stop returns promptly; and
- exactly one Idea is saved for the intentional recording.

If a trial fails, record the duration, whether the first resumed word was quiet
or normal, the visible status text, and the presence/absence of fixed marker
words. Do not record or attach private transcript content.

## Recommended first implementation

Implement Issue #65 item 2 as one focused change in
`AndroidSpeechTranscriber`:

1. Do not schedule a new recognition request from `onEndOfSpeech()`.
2. After a normal `onResults()` callback has committed the segment, post the
   next start to the main loop with no fixed delay.
3. Keep one scheduled restart callback and the existing
   `shouldKeepListening`/pending-start guards.
4. Retain a small bounded retry delay for `ERROR_RECOGNIZER_BUSY` and other
   recoverable error paths so an unsupported or slow recognizer cannot create a
   tight loop.
5. Leave the silence extras, transcript accumulation, segmented recognition,
   UI messages, and dependencies unchanged in that PR.

Posting the normal restart with zero delay still allows the terminal callback
to return before the next main-loop task runs. It removes the fixed 250 ms
normal-result delay and prevents the 2-second callback from starting a request
before a terminal result. Provider result latency and recognizer startup remain
device-dependent and must not be presented as eliminated.

Compare item 2 against the same device matrix. It should recognize the first
resumed marker more reliably or sooner in affected 2–5-second trials, introduce
no busy-error loop, retain every earlier segment exactly once, stop promptly,
and save exactly one Idea. Silence tuning or segmented recognition should only
proceed through their later checklist items after this smaller change is
measured.
