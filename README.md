# AetherOS

**Refining Android. Redefining Experience.**

AetherOS is a **non-root Galaxy customization and diagnostics app**, inspired by the fluid feel of iOS. It is **not** a custom ROM and does not claim to replace One UI.

## Version 0.1 — functional foundation

- **Overview:** battery percentage, charging, power saver, available/total RAM and device details.
- **Appearance:** shortcuts to supported Android display, wallpaper, launcher, battery and accessibility settings; optional Galaxy Good Lock module launcher.
- **Aether Launcher (beta):** user-selectable HOME replacement with a clean app grid and search; doesn't override your default launcher until you choose it.
- **StandBy:** full-screen landscape desk clock while this activity is open. No lock-screen bypass, no automatic background wake.
- **Usage insights:** last-24-hour foreground use from Android Usage Access (user must grant special access); links to each app's settings. Screen time is **not** battery drain.
- **Profiles:** Balanced, Battery and Fluid settings presets stored inside AetherOS. They show safe recommendations, **not** fabricated system changes.
- **Restore:** reset AetherOS's own saved preferences. External Samsung/Android changes must be reversed in the same settings screens.

## Not possible for a normal APK

AetherOS **cannot** silently replace Samsung's native AOD/lockscreen, alter arbitrary system-wide animation or haptic settings, disable other packages, purge other apps' memory, or edit Good Lock settings without a public integration API. Those controls remain user-managed. The current app does **not** implement privileged ADB/Shizuku operations, and it never disables critical services or finance apps.

## Build

Android Studio (JDK 17): open project and run `app`.

CLI with Android SDK and Gradle 8.11.1:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions builds and stores the debug APK as a workflow artifact on pushes to `main`.

## Usage

1. Install the debug APK.
2. Open **Appearance** for AOD/wallpaper/customization entry points.
3. Try **Launcher** preview, then deliberately select AetherOS under Android Default home app if desired.
4. Open **Insights**, optionally enable Usage Access, and review app usage.
5. Choose a profile to save AetherOS suggestions. Open Android settings to make changes yourself.

## Privacy and safety

No analytics, ads, network API, telemetry, or cloud account. Usage information stays on device. Optional Usage Access is used only to render local foreground-use history. No accessibility service, dangerous package manager actions, root requirements, boot receiver, or always-running foreground service.

## Roadmap

- User-consented live wallpaper and smoother AOD-adjacent transitions.
- Better per-device Samsung support detection.
- Opt-in Shizuku/ADB companion with per-package allowlist, dry-run, explicit confirmation, and restore log.
- Performance measurements before/after any opt-in adjustment.

**License:** No license granted yet. © Project owner.
