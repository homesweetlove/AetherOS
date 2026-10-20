# AetherOS

**Refining Android. Redefining Experience.**

AetherOS is a **non-root Galaxy customization and diagnostics app**, inspired by the fluid feel of iOS. It is **not** a custom ROM and does not claim to replace One UI.

## Version 0.3 — GPT Agent & Hybrid Mode

- **AI engine selector**: Local / Hybrid / GPT, directly inside the Aether AI screen.
- **Safe multi-step plans**: GPT can propose up to four allowlisted steps, each shown as a separate preview/approval. Nothing is auto-executed.
- **Secure Oracle gateway**: `gateway/` contains a dependency-free Node HTTPS-reverse-proxy backend powered by the official OpenAI Responses API, with strict JSON schema, action validation and rate limiting.
- **Credential security**: OpenAI API key is stored **only on your server**. Android stores an independent, random device token encrypted with Android Keystore. HTTPS endpoint is user-configured and never automatically contacted in Local mode.
- **Privacy**: Sending a command in GPT mode forwards the text to your endpoint and then OpenAI. Contacts, entire screens, or device files are not automatically sent.
- **Current scope**: GPT can answer a question or propose KakaoTalk/SMS **drafts**, open an exact-match app, and navigate to a safe settings page. It cannot freely click arbitrary apps, execute OS shell commands, or send messages without user confirmation.
- **Setup**: see [gateway/README.md](gateway/README.md). The backend requires your own API access and charges; a ChatGPT subscription alone does not automatically provide API credit.

## Version 0.2 — Visuals and Aether AI foundation

- **Aether Flow live wallpaper**: native animated gradient, parallax on home paging, invisible-state pause, reduced refresh in power saving mode.
- **StandBy OLED care**: in-app brightness slider and subtle minute-by-minute clock position shift. This remains an app activity, not Samsung AOD.

- **Quick Settings tile**: add “Aether AI” to the Android notification shade using Edit tiles, then tap to open the assistant without an always-listening service.
- **Aether AI tab**: Korean text commands to propose KakaoTalk/SMS drafts, launch an exact-match installed app, or open Android settings.
- **On-device natural language**: deterministic Korean parser first; on supported Gemini Nano (ML Kit GenAI Prompt / AICore) devices, tries local generative parsing for more complex requests. No cloud model and no general reasoning guaranteed on unsupported phones.
- **Microphone**: explicit tap-to-talk; uses Android on-device SpeechRecognizer if available. No background microphone or cloud fallback.
- **KakaoTalk**: native share composer, with recipient selected by the user. Optional opt-in AccessibilityService scoped to KakaoTalk can attempt recipient search and draft entry based on an explicit command, but **never clicks Send**. UI automation is experimental, untested on real KakaoTalk versions and may stop safely.
- **SMS**: optionally resolves an exact contact name locally after READ_CONTACTS approval, opens an SMS draft with the recipient/body set, and lets the user press Send. The app does not hold SEND_SMS permission or send silently.
- **Safety**: the interpreted plan is previewed before execution. Accessibility can be disabled any time. No screenshots, cloud telemetry or screen-text storage.

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

CLI with Android SDK and Gradle 8.13:

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

No analytics, ads, telemetry, or cloud account. Gemini Nano/AICore may require a device-side model download through the platform; AetherOS itself does not upload prompts to its own server. Usage information stays on device. Optional Usage Access is used only to render local foreground-use history. The optional, explicitly enabled KakaoTalk-only accessibility helper performs a narrowly scoped draft workflow. No arbitrary-app screen reading, automatic message sending, package manager changes, root requirements, boot receiver, or always-running foreground service.

## Assistant examples

- `엄마한테 카톡으로 오늘 늦는다고 보내줘`
- `문자로 아빠에게 도착했어 보내줘`
- `카메라 열어줘`
- `배터리 설정 열어줘`

The parser intentionally refuses unspecified, risky or unsupported actions. Official KakaoTalk Message API cannot send arbitrary personal messages to all KakaoTalk friends; native share requires recipient choice. Accessibility automation is not a replacement for the supported KakaoTalk API. Read the consent disclosure before enabling it.

## Roadmap

- User-consented live wallpaper and smoother AOD-adjacent transitions.
- Better per-device Samsung support detection.
- Opt-in Shizuku/ADB companion with per-package allowlist, dry-run, explicit confirmation, and restore log.
- Performance measurements before/after any opt-in adjustment.

**License:** No license granted yet. © Project owner.
