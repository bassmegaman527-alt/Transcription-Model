# GitHub Main Page Tabs and Sections

Use this checklist to configure the repository's main GitHub navigation and README so contributors can quickly understand the project, find planned work, and help build the Android MVP.

## Recommended GitHub repository tabs

Enable these standard GitHub tabs for the repository:

| GitHub tab | Recommendation | Purpose for this project |
| --- | --- | --- |
| **Code** | Keep enabled | Primary source view for the Android app, docs, Gradle files, and future backend code. |
| **Issues** | Enable | Track implementation tasks, bugs, MVP gaps, design decisions, and user feedback. |
| **Pull requests** | Keep enabled | Review app, backend, docs, infrastructure, and test changes before merge. |
| **Actions** | Enable after CI is added | Run Android builds, lint, unit tests, backend checks, and security scans. |
| **Projects** | Enable | Manage the MVP roadmap with columns such as Backlog, Ready, In Progress, Review, and Done. |
| **Discussions** | Optional, enable when collaborators join | Separate open-ended product/design conversations from actionable Issues. |
| **Wiki** | Optional, usually skip at first | Prefer `/docs` in the repo until documentation becomes too large for Markdown files. |
| **Security** | Enable | Publish security policy, dependency alerts, vulnerability reporting, and private advisories. |
| **Insights** | Keep enabled | Review contribution activity, traffic, dependency graph, and code frequency. |
| **Releases** | Use when builds are distributable | Publish APK/AAB test builds, release notes, and MVP milestone snapshots. |

## Recommended README sections on the GitHub main page

The README is what most visitors see first on the **Code** tab. Keep these sections visible and in this order:

1. **Project name and one-sentence pitch**
   * Example: `Transcribe, summarize, and brainstorm with an Android-first idea capture app.`
2. **Current status**
   * Explain that the repo currently contains an Android prototype with fake live transcription and local structuring.
3. **Product documentation**
   * Link to `SPEC-1` and `NEXT_STEPS`.
4. **Android prototype**
   * Summarize the implemented screens and prototype behavior.
5. **Build / local development**
   * Show the Gradle command and Android SDK requirement.
6. **Roadmap**
   * Link to the GitHub Project board or `docs/NEXT_STEPS.md`.
7. **Contributing**
   * Explain how to open issues, submit PRs, and choose tasks.
8. **Security and privacy**
   * Link to future `SECURITY.md` and privacy notes once added.

## Suggested GitHub Project board tabs/views

Create one GitHub Project for the MVP and add these views:

| Project view | Type | Purpose |
| --- | --- | --- |
| **MVP Board** | Board | Kanban workflow for day-to-day execution. |
| **Milestones** | Table | Group issues by Prototype, Background Capture, Structured Notes, and Production Readiness. |
| **Android** | Filtered table | Show only mobile UI, service, Room, permissions, and Firebase tasks. |
| **Backend** | Filtered table | Show relay, auth verification, transcription provider, structuring, and observability tasks. |
| **Bugs** | Filtered table | Track defects separately from roadmap features. |

Recommended board columns:

1. **Backlog**
2. **Ready**
3. **In Progress**
4. **Review**
5. **Blocked**
6. **Done**

## Suggested issue labels

Add these labels before creating the first batch of issues:

* `area:android`
* `area:backend`
* `area:docs`
* `area:firebase`
* `area:infrastructure`
* `area:transcription`
* `type:bug`
* `type:feature`
* `type:task`
* `type:research`
* `priority:p0`
* `priority:p1`
* `priority:p2`
* `status:blocked`
* `good first issue`

## Suggested milestones

Create these GitHub milestones to match the product plan:

1. **M1: Usable capture prototype**
2. **M2: Reliable background capture**
3. **M3: Structured notes MVP**
4. **M4: Production readiness**

## Suggested first pinned items

Pin these items on the repository or Project board when they exist:

1. `SPEC-1: Idea Capture Transcription App`
2. `Next Steps: Idea Capture Transcription App`
3. `M1: Usable capture prototype` milestone
4. `Scaffold Android Compose workspace` issue
5. `Configure Firebase anonymous auth` issue
6. `Implement foreground service recording pipeline` issue

## App navigation tabs for the Android main screen

For the Android app itself, keep the MVP bottom navigation minimal:

| App tab | MVP priority | Purpose |
| --- | --- | --- |
| **Capture** | Must have | One-tap recording, live transcript, current session status. |
| **Inbox** | Must have | Recent notes, structured summaries, raw transcript details. |
| **Search** | Should have after basic inbox | Search saved transcripts, titles, tags, and actions. |
| **Settings** | Should have | Account linking, privacy notes, diagnostics, app version. |

Do not add Projects, Teams, or Collaboration tabs to the app for MVP because they conflict with the single-user quick-capture focus.
