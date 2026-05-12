# Contributing

Thanks for helping build the Idea Capture Transcription App. The current goal is an Android-first MVP that supports one-tap voice capture, live transcription, transcript-only note storage, automatic structuring, and a searchable inbox.

## Start here

1. Read the product specification in [`docs/SPEC-1-Idea-Capture-Transcription-App.md`](docs/SPEC-1-Idea-Capture-Transcription-App.md).
2. Review the phased roadmap in [`docs/NEXT_STEPS.md`](docs/NEXT_STEPS.md).
3. Pick an issue from the current GitHub Project board or create one using the issue templates.
4. Keep MVP scope tight: single-user, Android-only, online-only, no audio playback, no collaboration.

## Development flow

1. Create or claim a GitHub issue.
2. Create a branch from `main`.
3. Make focused changes that match the issue acceptance criteria.
4. Run the relevant checks locally.
5. Open a pull request using the PR template.
6. Include screenshots or recordings for visible Android UI changes.

## Local build

The Android prototype is under `mobile-android/app`.

```bash
gradle :mobile-android:app:assembleDebug
```

A complete Android build requires an environment with the Android SDK, Android SDK platform 35, and access to Google Maven.

## Privacy and security expectations

* Do not commit provider API keys, Firebase service account keys, real transcripts, or real audio.
* Do not log transcript content in production code.
* Keep raw transcript preservation separate from generated structured fields.
* Use Firebase authenticated owner IDs for note access once persistence is implemented.
