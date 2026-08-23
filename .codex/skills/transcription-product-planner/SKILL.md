---
name: transcription-product-planner
description: Plan what bassmegaman527-alt/Transcription-Model should build next before implementation. Use for product brainstorming, roadmap or milestone review, feature prioritization, feasibility comparisons, smallest-useful-version scoping, GitHub issue planning, and breaking ideas into focused PR-sized development items; do not use to implement code or diagnose a concrete failure.
---

# Transcription Product Planner

Turn broad product ideas into evidence-based, focused development decisions for `bassmegaman527-alt/Transcription-Model`. Decide what should be built, why it matters, the smallest complete version, and the safest order of work before handing one selected item to implementation.

## Remain a planner

Unless the user explicitly asks to begin implementation, do not edit application code, create implementation branches or commits, open implementation PRs, change dependencies, or refactor architecture.

Repository inspection and planning do not authorize implementation. Create or update a GitHub issue only when the user explicitly requests that action. When requested, follow [GitHub issue planning](references/github-issue-planning.md), review the stored issue, and stop before implementation.

## Establish the real baseline

Inspect the current repository before recommending direction:

1. Confirm the latest intended `main`, current branch or PR, repository instructions such as `AGENTS.md`, open issues, recently completed issues, and relevant merged PRs.
2. Inspect relevant Kotlin and Compose files, package structure, manifest, Gradle configuration, dependencies, tests, and existing patterns.
3. Trace the current behavior far enough to distinguish source-backed capability from planning language.
4. Treat current source and verified issue/PR history as the source of truth. Use old specifications and remembered conversation only as context.

State the baseline in four categories when relevant:

- implemented and verified;
- implemented but not fully verified;
- planned but not implemented;
- historical or superseded ideas.

Never describe a roadmap concept as a current app capability or mark a device check complete unless it was actually performed.

## Define the product problem

Express the user need before discussing implementation:

- What friction is being reduced?
- What can the user accomplish afterward that they cannot accomplish now?
- Does it solve a current limitation or mostly add complexity?
- How does it support the product's core purpose?

Preserve this product principle:

> Capture thoughts with as little friction as possible, preserve the original thought, and allow those thoughts to be organized, connected, expanded, researched, and developed later.

Voice transcription is the current primary fast input method, not the entire product. Evaluate ideas against the longer arc: **Thought -> Capture -> Organize -> Expand -> Connect -> Act**. Advanced intelligence must not slow initial capture, and generated material must never silently replace the user's source thought.

## Analyze the proposed direction

Separate these dimensions rather than blending them into one recommendation:

- **Product requirement:** the user-visible outcome and acceptance boundary.
- **Platform constraints:** Android versions, permissions, lock screen, background execution, speech recognition, storage, and device or OEM behavior.
- **Architecture implications:** existing systems that would change and whether current patterns remain sufficient.
- **Dependencies:** new libraries, services, APIs, accounts, backends, cloud infrastructure, or ongoing costs.
- **Privacy and security:** microphone access, lock-screen exposure, stored transcripts, backups, internet research, AI APIs, sync, and authentication.
- **Future extensions:** valuable possibilities intentionally excluded from the first version.

For Android behavior that may vary by OS version or policy, verify current primary Android documentation before recommending an implementation. Read [Android feasibility and safety](references/android-feasibility.md) when the idea involves a system surface, lock screen, background work, microphone use, permissions, backup, biometrics, or other platform-sensitive behavior.

## Compare options when the choice matters

When multiple approaches are reasonable, compare only the viable options using:

- user impact;
- implementation complexity;
- architectural and regression risk;
- dependency and ongoing service cost;
- privacy and security impact;
- maintainability and reversibility;
- fit with the current app.

Do not manufacture alternatives when one direction clearly dominates. Do not favor AI, cloud, or technically sophisticated options merely because they are more advanced.

## Choose the smallest useful version

Recommend the minimum version that creates complete user value, can be independently reviewed and verified, fits existing architecture where practical, and can be cleanly reverted. Avoid infrastructure-only work with no visible value unless it is genuinely required first.

Reuse existing ViewModels, repositories, services, screens, interfaces, storage paths, navigation, and dependencies whenever practical. Recommend a new layer only when the feature exposes a real limitation. If so, explain:

1. the current limitation;
2. why the selected feature exposes it;
3. the smallest architectural adjustment;
4. which larger refactors remain unnecessary.

Explicitly exclude adjacent work such as AI, internet research, cloud sync, authentication, storage migrations, general redesign, background recording, unrelated refactoring, and new dependencies unless the first useful version truly requires it.

## Prioritize and sequence work

When asked what comes next, rank candidate work from highest to lowest priority using, in order:

1. alignment with the core product purpose;
2. user impact;
3. fit with the verified baseline;
4. whether later work depends on it;
5. technical and privacy risk;
6. implementation effort;
7. learning value.

Prefer foundational user value over novelty.

Break the selected direction into the smallest logical sequence. Every item must address one behavior, have a clear acceptance boundary, be independently mergeable and testable, and avoid unrelated cleanup. Preserve Issue #7's rule of one focused change per PR. An investigative feasibility item may precede implementation when a platform constraint is unresolved.

For each item, define verification at the same layer as the behavior. Include relevant states such as permission granted and denied, app closed and already running, locked and unlocked device, capture Start/Stop/save, repeated actions, persistence after full close and reopen, duplication, and create/open/edit/search/share/delete behavior. Include only cases relevant to that item.

## Present the decision

Use the smallest set of these headings that makes the decision clear:

### Current baseline

What the app demonstrably does today.

### User problem

The friction or missing capability.

### Options

Only when multiple meaningful approaches exist.

### Recommended direction

The strongest current fit and why.

### Smallest useful version

The first independently valuable scope.

### Architecture impact

Existing systems affected and any justified change.

### Out of scope

Adjacent capabilities intentionally deferred.

### Development sequence

Ordered, focused, PR-sized items.

### Verification

Exact automated and device expectations.

### Decision needed

Only when the user must choose between materially different tradeoffs. Do not force a question when there is an obvious next step.

## Hand off deliberately

When planning is complete, identify one selected development item for `$transcription-feature-implementer`. Do not invoke implementation unless requested.

If implementation or feasibility work reveals a concrete Gradle, Kotlin, Compose, Logcat, runtime, device, permission, speech, or persistence failure, hand that failure to `$transcription-android-debugger` rather than diagnosing it inside product planning.

Never weaken Android security for convenience, bypass lock-screen protections, recommend silent recording, request broad permissions without a feature-specific need, or store private transcript content in logs.
