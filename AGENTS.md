# AGENTS.md

## Repository scope

This repository contains the Transcription Project Android app. Treat the current repository state as the source of truth and inspect relevant code before proposing or making changes.

Primary Android module:
- `:mobile-android:app`

Primary package:
- `com.transcriptionmodel.ideacapture`

Do not assume this snapshot is permanent. Reinspect the repository when structure, tooling, or architecture may have changed.

## Development priorities

Optimize for:
1. Correct behavior.
2. Small, focused changes.
3. Preservation of working functionality.
4. Easy review and rollback.
5. Minimal code churn.
6. Maintainability consistent with the existing project.

Issue #7 established the project's development rule: focus on one selected item before adding adjacent features. Issue #7 is closed, so use any active successor issue or explicitly selected task as the current source of scope while preserving the one-focused-change-per-PR rule.

Do not silently expand a task into nearby features, cleanup, refactors, or dependency upgrades.

## Inspect before editing

Before implementation or debugging:
- Inspect the current branch or PR and the relevant repository files.
- Read the controlling issue, checklist, or user-requested acceptance boundary.
- Search for existing implementations and patterns before creating new abstractions.
- Trace the behavior end to end when practical.
- Prefer repository evidence over remembered architecture or old documentation.

When working in a local checkout, inspect `git status` and preserve unrelated user changes. If the requested edit overlaps uncommitted work, do not overwrite it.

## Architecture and code-change rules

Preserve the current Kotlin/Jetpack Compose architecture unless the selected task clearly requires an architectural change.

Prefer modifying or extending existing project patterns. Do not create duplicate:
- ViewModels
- repositories
- services
- screens
- composables
- interfaces
- models
- storage paths
- architectural layers

Do not introduce an abstraction merely because it is common Android practice. Add new files or layers only when they have a clear project-specific responsibility that cannot be cleanly handled by the existing structure.

Avoid:
- broad refactors unrelated to the task
- formatting sweeps
- package moves
- opportunistic cleanup
- generated-file edits
- unnecessary dependencies
- broad Gradle, Kotlin, Android Gradle Plugin, or library upgrades

Prefer Android, Kotlin, Compose, and dependencies already present in the repository.

## Project skills

Use the project-specific Codex skills when their scope matches the task.

### Product planning

Use `$transcription-product-planner` for:
- deciding what to build next
- brainstorming or prioritizing features
- reviewing the roadmap or next milestone
- comparing product or Android feasibility directions
- defining the smallest useful version
- turning an idea into focused development items or a requested GitHub issue

The product-planning skill must inspect the current repository, separate implemented behavior from plans, define the user problem, account for Android and privacy constraints, and stop before implementation unless the user explicitly asks to proceed.

### Feature work

Use `$transcription-feature-implementer` for:
- implementing a feature
- adding functionality
- changing app behavior
- building or modifying screens/components
- transcription behavior changes
- storage, export, settings, UI, or speech-processing work
- the next focused development item

The feature skill must inspect the current repository, scope the smallest independently mergeable change, preserve existing patterns, verify the result, and prepare one focused PR.

### Debugging

Use `$transcription-android-debugger` for:
- Android Studio or Gradle failures
- Kotlin or Compose errors
- Logcat analysis
- runtime crashes or ANRs
- emulator or physical-device problems
- permissions
- speech capture
- persistence failures

The debugging skill should identify the first meaningful failure, trace it to repository evidence, and recommend or implement the smallest safe correction.

If a feature implementation reveals a build/runtime failure, use the debugger workflow to understand that failure instead of guessing.

## Git and PR workflow

Keep one logical change per branch and pull request.

Unless a user or active issue specifies otherwise:
- branch from the latest intended `main`
- use a concise branch name such as `codex/<focused-change>`
- write an imperative commit message
- use a PR title describing the single behavior change
- include scope and verification in the PR body
- do not mark unperformed validation as complete

Review the full diff and changed-file list before handoff. Remove accidental formatting, debug output, generated files, unrelated edits, commented-out code, and unused imports.

Do not merge a PR unless explicitly asked.

## Verification

Use the checked-in Gradle wrapper from the repository root.

Windows:
```powershell
.\gradlew.bat :mobile-android:app:assembleDebug
```

macOS/Linux:
```bash
./gradlew :mobile-android:app:assembleDebug
```

Run narrower relevant tests when available. Compilation alone is not sufficient proof for user-facing behavior.

For Android behavior changes, provide or perform the appropriate emulator/physical-device checks, including the changed behavior and the unchanged path most likely to regress.

For speech/capture changes, consider:
- microphone permission grant and denial
- Start and Stop behavior
- transcript display
- save behavior
- relevant error paths

For persistence changes, verify saved data after a full app close and reopen and check for duplication when relevant.

Clearly distinguish checks actually performed from checks that remain for the user or a device environment.

## Safety and destructive actions

Do not perform any of the following without first identifying the exact target, explaining the impact, and receiving explicit authorization:
- force push
- reset
- history rewrite
- discard or overwrite local changes
- cache deletion or broad state invalidation
- deletion of user work
- broad dependency/toolchain upgrades

Do not treat Gradle deprecation warnings as build failures when the build succeeds.

Avoid logging or exposing raw transcript content when metadata, error codes, or redacted excerpts are sufficient.

## Handoff expectations

At the end of implementation work, report:
- behavior implemented
- scope and files changed
- dependencies added or avoided
- validation actually completed
- manual/device checks still required
- branch and PR information
- any adjacent work intentionally left out of scope

At the end of debugging work, report:
- first meaningful failure
- root cause and repository location
- smallest fix made or recommended
- verification results
- remaining checks
- focused scope suitable for one PR
