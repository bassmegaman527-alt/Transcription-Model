---
name: transcription-feature-implementer
description: Implement focused features and behavior changes in bassmegaman527-alt/Transcription-Model while preserving its current Kotlin/Jetpack Compose architecture and GitHub workflow. Use for requests to add functionality, screens, components, transcription behavior, storage, export, settings, UI, speech processing, or the next focused development item; do not use for diagnosis-only requests or unrelated Android repositories.
---

# Transcription Feature Implementer

Deliver the smallest safe, independently reviewable feature change for `bassmegaman527-alt/Transcription-Model`. Optimize for minimal churn, easy review and rollback, fast iteration, maintainability, and preservation of working behavior.

## Inspect before designing

Always inspect the current repository before proposing implementation details or editing code.

1. Confirm the default branch, current branch or PR, working-tree state, repository instructions, and the issue or checklist controlling the request.
2. Inspect the relevant Kotlin and Compose files, package layout, manifest, module and root Gradle files, existing tests, and callers of the behavior being changed.
3. Search for existing screens, composables, state models, data classes, storage functions, repositories, services, interfaces, and utilities before proposing new ones.
4. Trace the current behavior end to end. Prefer current repository evidence over remembered structure or future-looking documentation.
5. Read Issue #7 and any active successor issue. Issue #7 is closed and its checkboxes may be stale, but preserve its development rule: focus on one selected item before adding adjacent features.

At the time this skill was created, the app used one module, `:mobile-android:app`, and package `com.transcriptionmodel.ideacapture`. Important existing files included `MainActivity.kt`, `CaptureModels.kt`, `AndroidSpeechTranscriber.kt`, `CaptureForegroundService.kt`, `AndroidManifest.xml`, and Preferences DataStore-based note persistence. Reinspect them instead of assuming this snapshot is still current. Do not introduce ViewModels, repositories, services, screens, interfaces, modules, or architectural layers merely because they are common Android patterns.

## Plan before editing

Before making changes, give the user a concise plan using exactly these headings:

### FEATURE

State the requested user-visible or system behavior and the acceptance boundary.

### CURRENT IMPLEMENTATION

Name the relevant current flow, symbols, and repository paths. Distinguish verified facts from assumptions.

### FOCUSED CHANGE

Define the smallest independently useful implementation. Explicitly exclude adjacent features.

### FILES TO MODIFY

List expected existing files and why each must change.

### NEW FILES

List justified new files, or state `None`. Explain why existing files or patterns cannot reasonably hold the change when proposing a new file.

### DEPENDENCIES

State `None` by default. Name and justify any proposed dependency, including why platform or existing project APIs are insufficient.

### RISK

Rate the risk as low, medium, or high and identify concrete regression areas, data migration concerns, permission implications, or rollback difficulty.

### IMPLEMENTATION PLAN

Give a short ordered plan limited to the focused change.

### VERIFICATION

List exact automated and manual checks appropriate to the behavior.

### GIT / PR PLAN

Provide a focused branch name, imperative commit message, PR title, concise PR description, and verification checklist.

If the user requested planning or review only, stop after the plan. If they requested implementation, proceed after presenting the plan unless a missing choice materially changes behavior, privacy, data compatibility, or scope.

## Implement with existing patterns

- Reuse current state, models, composables, storage functions, speech abstractions, services, and navigation patterns whenever practical.
- Preserve package names, naming conventions, formatting, coding style, Gradle style, and directory structure.
- Modify the fewest files and lines that fully satisfy the focused acceptance boundary.
- Prefer Android, Kotlin, Compose, and dependencies already present in the project. Add a dependency only when its value clearly outweighs build, maintenance, and migration costs.
- Add a new file or abstraction only when it has a concrete responsibility that cannot be cleanly expressed through an existing pattern.
- Do not create duplicate ViewModels, repositories, services, screens, interfaces, models, storage paths, or architectural layers.
- Do not perform broad refactors, formatting sweeps, package moves, dependency refreshes, or opportunistic cleanup unless the requested feature genuinely requires them.
- Do not automatically continue into adjacent features after the selected change works.
- Preserve unrelated local or remote changes. If the intended edit overlaps a dirty working tree or another active change, stop and ask how to proceed.

For a large request, identify the smallest user-valuable vertical slice that can be reviewed and merged alone. Put later slices in a brief follow-up list, but do not implement them without a new request.

## Handle implementation failures deliberately

If work reveals a build, runtime, Logcat, permission, speech-capture, or persistence failure, load or defer to `$transcription-android-debugger` rather than guessing.

- Determine whether the failure was directly introduced by the focused change or was pre-existing.
- A directly introduced failure may be corrected within the same focused change after debugger-guided diagnosis.
- A pre-existing or unrelated failure should normally become a separate issue and PR; do not expand the feature PR silently.
- Return to feature implementation only after the first meaningful failure is understood and the authorized scope remains clear.

## Review and verify

Before committing:

1. Review the complete diff and changed-file list.
2. Remove unrelated edits, accidental formatting, generated output, debug logs, commented-out code, and unused imports.
3. Confirm every changed file is necessary for the focused feature.
4. Re-run the narrowest relevant checks, then the app build.

Build from the repository root with the checked-in wrapper:

```powershell
.\gradlew.bat :mobile-android:app:assembleDebug
```

```bash
./gradlew :mobile-android:app:assembleDebug
```

Run targeted tests when they exist and are relevant. Do not claim success from compilation alone for user-facing changes. Provide manual Android Studio, emulator, or physical-device steps that cover:

- the exact new behavior and its expected result;
- the unchanged path most likely to regress;
- relevant empty, denied, cancelled, long-input, restart, or duplicate-action cases;
- persistence across full close and reopen when stored data changes;
- permission and microphone behavior when capture or speech processing changes.

Report checks actually run separately from checks the user must still run.

## Prepare one focused PR

Branch from the latest intended base branch, normally `main`, unless the current issue or user specifies otherwise. Follow an existing branch convention when one is present; otherwise use a concise name such as `codex/<focused-change>`.

Prepare:

- one branch for the focused feature;
- an imperative commit message describing the behavior;
- a PR title describing the single change;
- a concise PR body with summary, scope, verification, and manual checks;
- a checklist that does not mark unperformed validation as complete.

Do not merge unless the user asks. Do not force-push, reset, rewrite history, delete caches, delete local work, discard changes, or perform broad dependency upgrades without first identifying the exact target, warning about the impact, and obtaining explicit authorization.

At handoff, state the implemented behavior, files changed, dependencies added or avoided, validation results, remaining device checks, and PR link. Clearly label any follow-up feature as out of scope.
