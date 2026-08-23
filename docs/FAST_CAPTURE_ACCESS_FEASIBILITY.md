# Fast Capture Access Feasibility

Issue #52 aims to reduce the time between having a thought and beginning voice
capture. This document evaluates Android system entry points against the current
app and selects the smallest first implementation.

## Current app constraints

- The app supports Android API 26 and later and targets API 35.
- `MainActivity` owns the existing Capture screen and recording lifecycle.
- Microphone permission and capture startup already flow through
  `requestOrStartSpeechCapture`.
- Normal capture remains an explicit Start action followed by an explicit Stop
  action.
- A fast entry point must reuse that flow. It must not add a second recorder,
  duplicate permission handling, weaken device security, or introduce a
  foreground service unless ongoing background capture later requires one.

## Entry-point comparison

| Entry point | User path | Lock-screen value | App impact | Recommendation |
| --- | --- | --- | --- | --- |
| Static launcher shortcut, optionally pinned | Long-press the launcher icon and tap **Quick capture**; a user can also place the shortcut on the home screen for one-tap access | Not a dependable lock-screen entry; shortcut information is unavailable before the user first unlocks the device after boot | Small: shortcut XML, manifest metadata, strings/icon, and intent handling in the existing activity; no new permission or dependency | **Implement first** |
| Quick Settings tile | Swipe to Quick Settings and tap a user-added tile | Best practical lock-screen-oriented surface, but secure devices should require unlock before showing or starting sensitive capture | Medium: `TileService`, manifest declaration, icon, activity `PendingIntent`, and user setup; no recording service is needed when the tile only opens the foreground activity | Implement as the lock-screen follow-up |
| Home-screen widget | Tap a widget after placing it | Primarily a home-screen surface; lock-screen widget support depends on the system host and device capabilities | Medium: provider, metadata, layouts/resources, receiver lifecycle, and setup | Defer unless shortcuts prove insufficient |
| Notification action | Tap an action in an ongoing notification | Potentially visible from the lock screen, subject to notification and privacy settings | Medium/high: notification permission on Android 13+, channel and lifecycle management, and persistent UI clutter | Do not use as the initial entry point |
| Full-screen notification intent or activity shown over the lock screen | System interrupts the user with capture UI | Could appear prominently, but is inappropriate for a user-initiated note-taking action and could expose sensitive UI | High policy, privacy, and user-experience risk; Android restricts full-screen intent use to urgent calling and alarm cases | Do not implement |

## Selected first implementation: static Quick capture shortcut

A static launcher shortcut is the smallest reliable addition for the app's
current Android range. Static shortcuts are intended for consistent actions,
and pinned shortcuts are supported throughout the app's minimum API range. The
surface needs no service, runtime permission, dependency, or second recording
path.

The implementation PR for checklist item 2 should:

1. Add a static **Quick capture** shortcut associated with `MainActivity`.
2. Send a dedicated intent action, such as
   `com.transcriptionmodel.ideacapture.action.QUICK_CAPTURE`.
3. On a cold or warm launch, select the existing Capture destination and call
   the existing permission/start callback exactly once.
4. Preserve ordinary launcher behavior and the current explicit Start then Stop
   lifecycle.
5. Avoid starting again when capture is already active or when the same intent
   is redelivered.
6. Reuse `MainActivity` with appropriate launch flags and warm-intent handling
   rather than creating another activity or capture component.

This first implementation improves access in two useful forms: a discoverable
long-press action and a user-pinned one-tap home-screen action. It does not claim
to solve secure lock-screen access.

## Lock-screen-oriented follow-up: Quick Settings tile

A Quick Settings tile is the best candidate for checklist item 3 because it is
reachable from the system shade on many devices, including while the device is
locked. Availability and presentation can vary by device maker, and Android
does not add an app's tile automatically. Users must add it, although Android
13 and later can show a system prompt that requests tile addition.

The tile should only open the existing foreground activity. On a securely
locked device, it should request unlock before displaying or starting capture;
it must not bypass the keyguard or reveal transcript content. On Android 14 and
later, activity launch from the tile should use the required `PendingIntent`
form of `startActivityAndCollapse`. Recording can then begin through the same
visible-activity permission and capture flow, avoiding a microphone foreground
service and its background-start restrictions.

## Deferred and excluded surfaces

- A widget adds more files and lifecycle surface than a pinned shortcut while
  initially serving the same home-screen use case. Reconsider it only if device
  testing or user feedback shows that launcher shortcuts are not discoverable
  or prominent enough.
- An always-present notification would add permission and notification-channel
  behavior before recording begins. It is not justified by the first checklist
  item and should not be used merely as an app launcher.
- Full-screen intents are intrusive and restricted to urgent calling and alarm
  use cases. They are not suitable for idea capture.
- No option should start recording behind a secure lock screen. Fast access must
  preserve Android's unlock boundary and the app's microphone-permission flow.

## Verification boundary for the shortcut implementation

The item 2 PR should include physical-device checks for:

- ordinary app-icon launch still opening Capture without starting by itself;
- shortcut cold launch opening Capture and starting once;
- shortcut warm launch returning to Capture and starting once;
- permission grant, denial, and previously granted paths;
- explicit Stop followed by the existing transcript and save behavior;
- tapping the shortcut while already listening not creating a second start;
- pinned-shortcut behavior on a launcher that supports pinning; and
- app close/reopen with no duplicate saved capture.

## Official Android references

- [Create shortcuts](https://developer.android.com/develop/ui/compose/system/shortcuts/creating-shortcuts)
- [App shortcuts overview](https://developer.android.com/develop/ui/compose/system/shortcuts)
- [Manage shortcuts](https://developer.android.com/develop/ui/compose/system/shortcuts/managing-shortcuts)
- [Create custom Quick Settings tiles](https://developer.android.com/develop/ui/views/quicksettings-tiles)
- [`TileService` API reference](https://developer.android.com/reference/android/service/quicksettings/TileService)
- [App widgets overview](https://developer.android.com/develop/ui/views/appwidgets/overview)
- [Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
- [Android 14 full-screen intent restrictions](https://developer.android.com/about/versions/14/behavior-changes-14#secure-fsi)
- [Foreground-service background start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)

Last reviewed against official Android documentation: 2026-08-23.
