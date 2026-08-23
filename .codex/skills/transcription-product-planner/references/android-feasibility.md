# Android feasibility and safety

Read this reference when a product idea depends on current Android platform behavior, including lock-screen entry, Quick Settings tiles, widgets, shortcuts, notifications, background work, microphone capture, foreground services, share targets, intents, biometrics, device credentials, or backup.

## Research standard

1. Verify current behavior in primary Android documentation. Note the Android versions and API levels covered.
2. Separate platform guarantees from device or OEM behavior.
3. Classify the recommendation as supported, restricted, permission-dependent, device-dependent, or unsuitable.
4. Identify required permissions, user-visible disclosures, system affordances, lifecycle limits, and store-policy implications.
5. Prefer an explicit user action, the narrowest permission, and a reversible design.

Do not infer that a system surface can bypass device security. Launching from a lock-screen-accessible affordance does not imply that protected app content may be exposed or that recording may begin silently.

## Planning questions

- Can the intended action start while the device is locked, or only open an activity pending unlock?
- Does Android require a foreground service, persistent notification, special permission, or user confirmation?
- Does the behavior differ when the app process is dead, backgrounded, or already open?
- Are microphone and notification permissions independently required on relevant versions?
- What user content is visible before authentication?
- What happens after permission denial, cancellation, process death, reboot, or OEM task restriction?
- Can the first version use an existing Android surface without a new dependency?

Document uncertainty rather than promising behavior that has not been confirmed on the target OS and device. Never design around bypassing lock-screen, background execution, permission, or privacy protections.
