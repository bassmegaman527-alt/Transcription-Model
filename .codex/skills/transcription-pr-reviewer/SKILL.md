---
name: transcription-pr-reviewer
description: Review focused pull requests, branches, commits, or working-tree diffs for bassmegaman527-alt/Transcription-Model against the controlling GitHub issue before physical-device testing or merge. Use when asked to review the latest PR, check Codex's work, validate an implementation or completed checklist item, compare changes with an issue, assess test or merge readiness, prepare phone verification, or audit the current diff for regressions. Do not use for another repository or to implement fixes during the review.
---

# Transcription PR Reviewer

Independently evaluate one focused change between planning or implementation and physical-device testing or merge. Base every conclusion on current repository and GitHub evidence, not remembered architecture or planning documents.

## Review contract

- Treat a review request as inspection-only authorization.
- Never edit code, update the PR, post comments or reviews, create commits, change labels, merge, close issues, or mark device checks complete without an explicit follow-up request.
- Never perform destructive Git operations.
- Never mark a physical-device check complete unless the user reports that exact check passing.
- Review only `bassmegaman527-alt/Transcription-Model`. If the repository identity is different or cannot be confirmed, stop with an evidence-missing verdict.
- Complete the review before offering or applying a requested fix. Route feature or behavior corrections through `$transcription-feature-implementer`; route demonstrated build, runtime, permission, speech, persistence, or device failures through `$transcription-android-debugger` when those skills are available.

## Establish the source of truth

Before judging the change:

1. Inspect the connected repository and read `AGENTS.md` completely.
2. Confirm the latest intended `main` from current remote or GitHub evidence. Do not assume the local baseline is current.
3. Identify the exact PR, branch, commit range, or working-tree diff under review.
4. Read the controlling GitHub issue and the exact selected checklist item. Record its observable acceptance boundary and exclusions.
5. Inspect relevant recently merged PRs only when necessary to understand the baseline.
6. Confirm whether the PR is open, draft, closed, or merged.
7. Inspect every changed file and commit, plus the PR description, comments, reviews, and available CI results.

If the controlling issue, selected item, current base, or complete diff cannot be established, do not infer it. State the missing evidence and choose `Unable to determine because required evidence is missing` when it prevents a reliable verdict.

## Check scope compliance

Confirm that the change:

- Implements only the selected issue item and satisfies its user-observable acceptance boundary.
- Does not implement work assigned to later checklist items.
- Contains no unrelated cleanup, formatting sweep, dependency upgrade, package move, or architecture change.
- Preserves Issue #7's rule of one focused, independently mergeable change per PR.
- Reuses existing Kotlin, Compose, speech, persistence, and navigation patterns when practical.
- Does not duplicate an existing ViewModel, repository, service, screen, composable, model, interface, persistence path, or architectural layer.

Treat unnecessary architectural expansion as a finding even when the project compiles.

## Trace changed behavior end to end

Search for all callers and state consumers before declaring the change safe. Follow the affected path through each applicable layer:

- User action, Compose UI, navigation, and state transitions.
- Microphone permission handling and `AndroidSpeechTranscriber`.
- Partial and final transcript accumulation, recognizer restarts, Stop, and save.
- Duplicate protection and repeated-action behavior.
- `Note`, `StructuredNote`, and `CaptureSession`.
- Source, Current transcript, Interpretation, and Development boundaries.
- DataStore JSON serialization and backward compatibility.
- Search, editing, sharing, deletion, and persistence after close/reopen.
- Ordinary Capture, launcher Quick capture, and Quick Settings Quick capture.
- Back, cancellation, denial, missing-target, and failure paths.

Inspect only the areas relevant to the selected item, but include unchanged callers or consumers when the diff can affect them.

## Apply project-specific regression checks

Test the diff against every applicable invariant:

### Save and continuation integrity

- One intentional recording must not save more than one Idea.
- A continuation must update exactly one intended Idea, not create a new Idea or update several.
- A stale continuation target must not leak into ordinary or external Quick capture.
- Repeated Stop or repeated actions must not apply a save or update twice.
- Missing-target behavior must not lose data or redirect content to another Idea.

### Content boundaries

- Source must not be modified or replaced unexpectedly.
- Development must be appended or replaced only as required by the issue.
- Interpretation must not regenerate when the issue excludes it.
- Transcript segments must not be lost or duplicated, including after recognizer restarts.
- Private transcript or Development content must not appear in logs, tests, screenshots, or committed fixtures.

### Persistence and state safety

- Existing saved-note JSON must remain readable.
- Updating one field must not change unrelated note fields.
- Note order, note count, timestamps, and original duration must remain stable unless the issue explicitly changes them.
- Permission denial, cancellation, Back, or failure must not leave capture in an invalid state.

### Platform and scope safety

- New permissions, exported components, dependencies, or manifest behavior must be justified by the selected item.
- Destructive behavior must require appropriate confirmation.
- Broad changes to `MainActivity.kt` must be necessary for the selected behavior.

Do not assume successful compilation proves user-facing correctness.

## Verify with the narrowest appropriate checks

Inspect the complete diff and run the checks supported by the repository and review environment. Normally include:

```bash
./gradlew :mobile-android:app:assembleDebug
./gradlew :mobile-android:app:testDebugUnitTest
git diff --check
```

Also run relevant focused tests and inspect GitHub Actions status. Run lint or other tasks only when already configured and relevant. Do not add dependencies, modify Gradle, delete caches, or upgrade the toolchain merely to make a review command run. Treat a successful build with Gradle deprecation warnings as successful.

For every check, distinguish among:

- Executed in the current review and passed or failed.
- Confirmed through GitHub CI.
- Blocked by the environment.
- Requiring Android Studio.
- Requiring the user's physical Android phone.

If a test task reports `NO-SOURCE`, state that no unit tests executed.

## Review test quality

When tests exist, confirm that they:

- Assert meaningful behavior and the changed acceptance boundary.
- Cover relevant failure or edge states and the likeliest regression.
- Would fail for the incorrect behavior they claim to prevent.
- Avoid private transcript or personal data.
- Avoid fragile timing and unrelated implementation details.

Classify missing tests according to the actual regression risk; do not automatically block every small change.

## Classify findings

Report concrete findings before explanatory summaries and order them by severity:

- **Blocker:** Data loss, privacy or security exposure, crash, failed acceptance criterion, broken persistence, duplicate save or update, or another unsafe merge condition.
- **High:** Likely user-facing regression, incorrect state transition, incomplete issue implementation, or missing compatibility protection.
- **Medium:** Meaningful maintainability, scope, validation, or test weakness that should be addressed before or immediately after merge.
- **Device gate:** Behavior that cannot be confirmed without a physical Android device.

For every finding include:

- Severity.
- Repository-relative file and relevant line or symbol.
- Observed evidence.
- Why it matters.
- Smallest safe correction or verification needed.
- Whether it blocks device testing, merging, or neither.

Do not invent findings. If none exist, write exactly: `No material code-review findings.` Then identify any remaining verification risk under the appropriate later heading.

## Return the review

Use these headings in this order.

### REVIEW VERDICT

Choose exactly one:

- Changes required before testing
- Ready for physical-device testing
- Ready to merge after listed verification
- Ready to merge
- Unable to determine because required evidence is missing

### CONTROLLING SCOPE

State the issue, exact checklist item, user-visible goal, and intentionally excluded work.

### FINDINGS

List findings in severity order. If none, use the required no-findings sentence.

### AUTOMATED VERIFICATION

List each command or CI check and its actual result. Separate completed checks from blocked or unperformed checks.

### PHYSICAL-DEVICE GATE

Give the smallest exact phone-testing sequence for the changed behavior and for the unchanged path most likely to regress.

### MERGE READINESS

State exactly what must happen before merge. Never merge automatically.

### DEFERRED WORK

List only adjacent work intentionally assigned to later issue items. Do not turn it into new implementation recommendations unless it is necessary to resolve a finding.
