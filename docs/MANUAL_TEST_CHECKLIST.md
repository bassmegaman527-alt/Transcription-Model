# Manual Test Checklist

Use this checklist for Android changes that could affect the Idea Capture user experience. Run the relevant sections on an emulator or physical Android device and record the device/API level in the pull request.

## Setup

- [ ] Build the debug app with `./gradlew :mobile-android:app:assembleDebug`.
- [ ] Before an update-over-existing-data check, record the current note count and non-sensitive identifying details for at least two saved notes.
- [ ] Install the debug build without clearing app storage or uninstalling the prior build when running the update check.
- [ ] Launch the debug app.
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

## Expandable Idea persistence and regression

Use non-sensitive, distinctive marker words for Source, Current transcript, and
Development so each stored field can be recognized without exposing private
content. Complete this section in order and do not clear app data between steps.

### Existing-note compatibility

- [ ] After updating without clearing storage, the recorded pre-existing note count is unchanged.
- [ ] The recorded notes retain their title, summary, tags, action items, transcript, timestamp, duration, and newest-first order.
- [ ] A pre-existing note retains its displayed Source, or adopts its prior transcript as Source when upgrading from a pre-Source build, and shows empty Development when none was added.
- [ ] Fully close and reopen the app; the same notes return without loss or duplication.

### Source preservation and Development

- [ ] From the ordinary app icon, save one meaningful capture containing a unique Source marker; Inbox adds exactly one note.
- [ ] Open the note and record its displayed Source, Interpretation title, summary, tags, action items, timestamp, and duration.
- [ ] Edit the Current transcript to remove the Source marker and add a different marker; add multiline Development with a third marker; save.
- [ ] Detail still shows the original Source exactly as recorded, Current transcript shows the edited text, and Development shows the saved additions.
- [ ] Interpretation reflects the Current transcript while the note count, timestamp, and duration remain unchanged.
- [ ] Fully close and reopen the app; Source, Current transcript, Interpretation, and Development all return without duplication.
- [ ] Edit only Development and save; Source, Current transcript, and Interpretation remain unchanged.
- [ ] Change Development and cancel editing; the previously saved Development and Source remain unchanged.

### Search and sharing

- [ ] Search for the Source marker removed from Current transcript; the note still appears.
- [ ] Search for the Current transcript marker; the note appears.
- [ ] Search for the Development marker; the note appears.
- [ ] Search with one Source term and one Development term; all-term matching returns the note.
- [ ] Share the note; shared text labels Interpretation and Source, includes Original and Current transcript separately, and includes Development.
- [ ] Share a note with no Development and an unchanged transcript; shared text omits empty Development and duplicate Current transcript sections.
- [ ] Clear all Development, save, fully close and reopen, and confirm Development remains empty while Source and Current transcript remain unchanged.
- [ ] The cleared Development marker no longer returns the note in search.

### Capture-entry and deletion regression

- [ ] Complete the Capture section once through the ordinary app-icon flow, including permission grant/denial and blank-capture behavior.
- [ ] Complete the External capture access shared lifecycle once for the launcher shortcut and once for the Quick Settings tile.
- [ ] One meaningful Stop from each of the three entry paths creates exactly one new note; reopening the app creates no duplicates.
- [ ] Delete a different single note; the Source-preservation test note and every other note remain unchanged after reopen.
- [ ] Complete the About and delete-all section; reopen to confirm the Inbox stays empty, then save one new capture successfully.

## Voice capture continuity regression

Run this section as the final Issue #65 physical-device pass after installing
the current PR build without clearing app data. Record the device model, Android
and API version, active speech-recognition service when visible, network state,
and tested commit in the pull request. Keep any notes that still need to be
preserved until the final delete-all check.

Use fixed, non-sensitive marker phrases so missing or repeated words are easy to
spot. Say **alpha orchard** before a pause and **bravo harbor** after it. For a
second and third pause, continue with **charlie meadow** and **delta river**.
Recognizer punctuation and capitalization may vary, but each marker word should
remain present in order and should not be duplicated.

### Ordinary Capture pause matrix

Start a new recording from the ordinary Capture screen for each trial. Repeat
each trial three times. Check a row only when every repetition remains visibly
**Listening now** until Stop, detects resumed speech promptly, retains the
earlier markers, does not consistently clip the first resumed marker, contains
no repeated boundary words or segments, stops promptly, and saves exactly one
new Idea.

- [ ] Speak the marker phrases continuously with no deliberate pause.
- [ ] Pause about 0.5-1 second between **alpha orchard** and **bravo harbor**.
- [ ] Pause about 2 seconds between **alpha orchard** and **bravo harbor**.
- [ ] Pause about 3 seconds between **alpha orchard** and **bravo harbor**.
- [ ] Pause about 5 seconds between **alpha orchard** and **bravo harbor**.
- [ ] Use repeated thinking pauses of about 1-3 seconds between all four marker phrases in one capture.
- [ ] After a 2-second pause, resume with **bravo** spoken quietly, then say **harbor** normally.
- [ ] After a 2-second pause, resume with **bravo harbor** at a normal/loud volume.
- [ ] Press **Stop recording** once while speech is active; it returns promptly and saves one complete Idea without restarting capture.
- [ ] Press **Stop recording** once immediately after a 3-5-second pause; it returns promptly and saves one Idea without adding duplicate text.

### Entry points and repeated captures

- [ ] Opening the ordinary app icon shows Capture without starting the microphone; tapping **Start recording** starts exactly one capture.
- [ ] From the pinned launcher **Quick capture** shortcut, complete a representative 2-3-second pause trial; Stop saves exactly one Idea.
- [ ] From the Quick Settings **Quick capture** tile, complete a representative 2-3-second pause trial; Stop saves exactly one Idea.
- [ ] Complete two back-to-back ordinary captures without force-closing the app; each Stop adds exactly one Idea and the first Idea remains unchanged.
- [ ] Save one Idea from each entry point in sequence, then reopen from the ordinary icon; the note count has increased by exactly three and no external action is replayed.
- [ ] Fully close and reopen the app after the repeated captures; all saved Ideas return once, in newest-first order, with no duplicates.

### Permission and recognition-error paths

- [ ] Reset microphone permission in system settings, return to the app, tap **Start recording**, and deny permission; no capture starts, no Idea is saved, and the permission error is visible.
- [ ] Tap **Start recording** again and grant permission; listening starts exactly once and Stop saves exactly one Idea.
- [ ] During the longer and repeated-pause trials, any normal internal recognition boundary stays **Listening now** and does not display **Listening interrupted**.
- [ ] If the active recognition service reports a genuine network, server, audio, or availability failure, the app visibly reports the interruption instead of silently appearing active. Record the error category without private transcript content. Do not disable or uninstall system components solely to force this check.

### Existing Idea regression after continuity trials

- [ ] Complete **Existing-note compatibility** and confirm update-over-existing-data and full-reopen persistence without loss or duplication.
- [ ] Complete **Source preservation and Development** and confirm editing does not change the original Source or create another Idea.
- [ ] Complete **Search and sharing** and confirm Source, Current transcript, and Development remain searchable and shared with the expected labels.
- [ ] Complete **Note details and sharing** and confirm all saved Idea sections remain readable and shareable.
- [ ] Delete one selected Idea, fully reopen the app, and confirm only that Idea was removed.
- [ ] Complete **About and delete all** last, reopen to confirm the Inbox remains empty, then save one new capture successfully.

## Continue an Idea by voice regression

Run this section in order as the final Issue #72 physical-device pass. Install
the current PR build without clearing app data. Use only non-sensitive marker
phrases, and keep the test Ideas until the persistence, utility, and entry-point
checks are complete.

### Test record and baseline

- [ ] Record the device model, Android and API version, active speech-recognition service when visible, network state, tested commit, tester, and date in the pull request.
- [ ] Confirm the pinned launcher **Quick capture** shortcut and Quick Settings **Quick capture** tile are available, then record the starting Inbox count and newest-first order.
- [ ] Create or select Idea A with empty Development. Record its title, Source, Current transcript, Interpretation, timestamp, duration, Inbox position, and the starting note count.
- [ ] Create or select Idea B with typed Development containing **typed cedar**. Record the same non-Development fields, Inbox position, note count, and exact Development.

### Ordered successful continuation pass

1. With microphone permission granted, open Idea A detail and tap **Continue by voice**.
   - [ ] Capture identifies Idea A as the continuation target and starts listening once without a second Start tap. No new Idea is created; entering detail alone must not start the microphone.
   - [ ] Say **first lantern**, and tap **Stop recording** once. The app returns to Idea A detail and Development contains the recognized phrase once.
   - [ ] Idea A's recorded non-Development fields, Inbox position, and the total note count remain unchanged.
2. From Idea B detail, start a continuation, say **second maple**, and stop once.
   - [ ] Development preserves **typed cedar**, then appends the recognized phrase after a blank line; no other field, note, or Inbox position changes.
3. Continue Idea B again, say **third orbit**, and stop once.
   - [ ] Development remains in chronological order as **typed cedar**, **second maple**, then **third orbit**, with blank-line separators and no duplicated segment.
4. Continue Idea B once more for the pause and volume trial.
   - [ ] Say **alpha orchard**, pause about 2-3 seconds, resume quietly with **bravo harbor**, pause again, then say **charlie meadow** at normal/loud volume. Capture stays visibly active until Stop and retains every marker in order.
   - [ ] Double-tap **Stop recording** quickly. The continuation is applied exactly once, returns to Idea B detail, and does not restart capture or create an Idea.

### Ordered recovery and failure pass

1. With microphone permission granted, tap **Continue by voice** from Idea A detail.
   - [ ] While listening, tap **Discard continuation**. The microphone stops, the app returns to Idea A detail, and no stored field or note count changes.
2. Start a fresh continuation for Idea A and speak a disposable marker before each navigation trial.
   - [ ] Use system Back while listening. The microphone stops, no spoken text is applied, and retrying later starts a fresh transcript.
   - [ ] Repeat by selecting Inbox or About from the bottom navigation. Capture is discarded, the selected tab opens, and no unfinished continuation is applied.
3. Reset microphone permission in system settings, return to Idea A detail, and tap **Continue by voice**.
   - [ ] The permission request appears from this action. Deny it; a continuation-specific error is visible, the microphone remains inactive, and Development and note count remain unchanged.
   - [ ] The failed state offers **Start continuation** and **Return to Idea**. Tap **Return to Idea** and confirm the unchanged Idea opens.
   - [ ] Start a new continuation attempt. If permission is requested, deny it again, then tap **Start continuation** to retry. Grant permission when prompted; if Android no longer shows a prompt, grant microphone permission in system settings and retry. Listening starts exactly once, and a meaningful Stop applies exactly one continuation to Idea A.
4. Start another continuation and remain silent until Stop.
   - [ ] A blank result is not applied. The app offers **Start continuation** / **Return to Idea** recovery and leaves the Idea and note count unchanged.
   - [ ] Confirm the automated invalid-transcript case rejects the prototype phrase **Quick idea captured from the prototype.** The recognizer is not expected to generate this phrase during silence; do not substitute an arbitrary error message for transcript input.
5. Exercise only naturally available recognition failures.
   - [ ] If the service reports an unrecoverable network, server, audio, or availability failure, the error is visible and no continuation is applied. If no unrecoverable failure occurs, record **not observed**. Recoverable interruptions, including network timeout, may retry internally while capture remains active; do not disable or uninstall system components to force an error.
6. Confirm the automated stale/missing-target safety coverage.
   - [ ] `VoiceContinuationTest` passes its missing-target, changed-Development, and repeated-callback cases. Do not delete or corrupt user data solely to reproduce those states on-device.

### Fatal error while Stop is pending (Issue #81)

- [ ] Record tested commit, device/API, recognizer when visible, network state, and PASS/FAIL/BLOCKED. Use a disposable Idea with existing Development and non-sensitive speech markers.
- [ ] If an unrecoverable recognition error naturally occurs after **Stop recording** while processing, confirm its specific error remains visible, **Start continuation** / **Return to Idea** are available, and the microphone does not restart automatically. Otherwise record **not observed** and cite `SpeechStopTest`'s deterministic fatal-error coverage; do not disable or uninstall system components to force the timing.
- [ ] After an observed failure, wait beyond the Stop timeout, use **Return to Idea**, then fully reopen and confirm Development, Source, Current transcript, Interpretation, timestamps, duration, count, and order are unchanged.
- [ ] In a separate failed attempt when naturally available, use **Start continuation** from the failure screen. A fresh successful retry must append only the new attempt once, including repeated Stop taps. Otherwise record **not observed** and cite the deterministic retry test.
- [ ] Complete the successful/repeated continuation and ordinary/launcher/Quick Settings checks in this section. These targeted checks do not replace the full Issue #72 regression pass.

### Final Stop wording (Issue #83)

- [ ] Record tested commit, device/API, recognizer when visible, network state, and PASS/FAIL/BLOCKED. Use disposable Ideas and non-sensitive markers.
- [ ] In ordinary Capture, stop while partial wording is visible. If the recognizer corrects, shortens, or extends it in the final result, confirm the saved Source contains only the final active-segment wording. If no correction appears, record **not observed** and cite the deterministic `SpeechStopTest` cases.
- [ ] Repeat after a pause with earlier committed words, then continue an Idea with typed Development. Earlier words remain in order; the continuation appears once after a blank line, and every other Idea field, count, and order remain unchanged.
- [ ] Double-tap Stop, fully close/reopen, and confirm no duplicate or obsolete wording. Check the continued text in detail, edit, search, and share preview.
- [ ] Confirm ordinary Capture, pinned launcher **Quick capture**, and Quick Settings **Quick capture** each create exactly one new Idea with no inherited continuation target. Retain the Issue #81 failure/retry/return checks above.

### Background ordinary voice capture

- [ ] Record device/API, recognizer, tested commit, and PASS/FAIL/BLOCKED. Grant microphone and notification permissions when requested. Start an ordinary capture, switch to another app, lock the screen, then return. The same capture remains active with its prior words visible.
- [ ] Repeat and use **Stop** in the persistent notification while the app is away. Return to the app; exactly one Idea appears in the Inbox with the expected Source, and it survives a full close/reopen. Repeat with the in-app **Stop** control and double-tap Stop; each attempt saves at most once.
- [ ] Try the pinned launcher shortcut and Quick Settings tile during an active ordinary capture. They return to the same capture without starting a second session or changing the target.
- [ ] Deny microphone permission, then notification permission on Android 13 or newer. Each denial shows a clear error and creates no Idea. Grant the permission in Settings and retry successfully.
- [ ] Stop after silence. Return from another app and confirm the empty-capture dialog still offers **Save empty note** and **Discard**. Each choice applies once; no note is saved on Discard.
- [ ] Continue an existing Idea by voice and verify its prior Development, Source, and note count follow the established continuation checks. This path must not create an ordinary Idea.

### Persistence and existing Idea utilities

- [ ] Fully close and reopen the app. Ideas A and B return once, in the same relative order, with all continued Development in chronological order and no duplicate note or segment.
- [ ] Open both details; Source, Current transcript, Interpretation, timestamp, and duration still match the recorded baselines.
- [ ] Search for a unique continuation marker; only the expected Idea appears immediately after continuation and again after full reopen.
- [ ] Share the continued Idea; the preview labels and includes Development once and preserves the existing Interpretation, Source, and Current transcript rules.
- [ ] Edit only Development and save; detail, search, sharing, and full reopen show the edit without changing any recorded non-Development field or creating a note.
- [ ] Clear Development on one disposable test Idea and save; full reopen keeps it empty while all non-Development fields remain unchanged.
- [ ] Delete one disposable test Idea, fully reopen, and confirm only that Idea was removed.

### New-Idea entry-point regression

Record the Inbox count immediately before each entry. Use a different marker for
each capture and Stop once after meaningful speech.

1. Ordinary Capture
   - [ ] Opening the app normally and tapping **Start recording** shows ordinary Capture, not continuation mode, and Stop increases the note count by exactly one.
2. Pinned launcher **Quick capture**
   - [ ] Invoking the shortcut starts ordinary Quick capture exactly once, never shows a continuation target, and Stop increases the note count by exactly one.
3. Quick Settings **Quick capture**
   - [ ] Invoking the tile starts ordinary Quick capture exactly once, never shows a continuation target, and Stop increases the note count by exactly one.
4. Final reopen
   - [ ] Fully close and reopen the app. Exactly the three new Ideas return once in newest-first order, no external action is replayed, and no prior Idea received their transcripts as Development.

### Final destructive utility check

- [ ] Open **About**, start **Delete all notes**, and cancel. Every Idea remains present.
- [ ] Start **Delete all notes** again and confirm. Fully close and reopen the app; the Inbox remains empty.
- [ ] Save one new ordinary capture after delete-all; exactly one new Idea appears and no continuation target is active.

### Final Issue #72 result

- [ ] Every required check above is complete or has an explicit safe **not observed** note, with no private transcript content in evidence.
- [ ] Record the overall result as **PASS**, **FAIL**, or **BLOCKED** in the pull request, including any failed step and a non-sensitive screenshot or error category when useful.
- [ ] Mark Issue #72 item 6 complete only after the ordered pass succeeds on a physical device.

## Note details and sharing

- [ ] Opening a note visibly separates Source, Interpretation, and Development.
- [ ] When Current transcript differs from Source, both are labeled and readable.
- [ ] Interpretation includes the title, summary, tags, and action items.
- [ ] Development shows saved content or the **No development added yet.** empty state.
- [ ] **Share** opens the Android share sheet.
- [ ] Shared text labels Interpretation and Source and includes title, summary, tags, Original transcript, and created time.
- [ ] Shared text includes Current transcript only when it differs from Source.
- [ ] Shared text includes Development only when content has been saved.
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
