# Codex Task Template

Copy this template into a new coding task and replace each placeholder. Keep one task focused on one user-visible outcome.

```text
Create a brand-new branch from the latest main.

Task:
<Short task name>

Goal:
<Describe the user outcome and why it matters.>

Requirements:
1. <Required behavior>
2. <Required behavior>
3. Preserve existing capture, Inbox, persistence, and note-management behavior.
4. Keep changes small and focused.
5. Do not add Firebase, backend, cloud sync, Room, authentication, or new architecture unless explicitly required.
6. Do not modify Gradle files unless absolutely necessary.

Out of scope:
- <Explicitly excluded behavior>
- <Explicitly excluded files or architecture>

Likely files:
- <path/to/file>

Acceptance criteria:
- The Android debug app builds successfully.
- <Observable user behavior>
- <Regression behavior that must still work>

Testing:
- Run: ./gradlew :mobile-android:app:assembleDebug
- Complete the relevant sections of docs/MANUAL_TEST_CHECKLIST.md.
- Report any environment limitation instead of changing build files to bypass it.

After implementation:
- Summarize exactly which files changed and why.
- List every test and check run, including failures or environment limitations.
- Commit the changes on the new branch and prepare a focused pull request.
```

## Task-writing guidance

- State observable behavior instead of prescribing a large refactor.
- Include exact user-facing copy when wording matters.
- Name behaviors that must not regress.
- Identify forbidden dependencies and architecture changes.
- Make acceptance criteria independently verifiable.
- Separate environment problems from product-code changes.
