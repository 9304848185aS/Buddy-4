# Buddy Android Voice Assistant

A Kotlin Android voice-control prototype. Tap **START LISTENING**, then speak commands directly — you do not need to say “Buddy” before every command.

## Current commands
- Stop listening: **रुक जा**, **stop listening**, or **stop**.
- Time, date, and battery status.
- Open YouTube, Chrome, Gmail, Play Store, Photos, Camera, Maps, Contacts, Messages, Phone dialer, Wi-Fi/Bluetooth settings, Android Settings, and Calculator.
- Try opening other installed apps by saying their visible app name (for example, "open WhatsApp"). This depends on Android package visibility and the app's launcher entry.
- Search the web by saying **search [your words]** or **सर्च [your words]**.
- Set an alarm by saying a numeric time such as **alarm 7:30**; Buddy opens the system clock screen for your confirmation.
- Basic Hindi greetings and assistant identity responses.

## Build and install
Every push to `main` starts the **Build Buddy APK** GitHub Actions workflow. When the workflow succeeds:
1. Open the repository's **Actions** tab.
2. Open the latest successful **Build Buddy APK** run.
3. Under **Artifacts**, download **Buddy-debug-apk**.
4. Extract the ZIP and install `app-debug.apk`. Android may ask you to allow installation from that source.

## Limitations — read before use
- This is not yet a general-purpose AI model. Offline capability currently means local command handling and phone intents, not an installed Qwen model or unrestricted conversation.
- Online search opens a browser/search provider; it is not an online LLM chat integration.
- Speech recognition availability, language support, and offline recognition depend on the phone's installed speech service. The app does not bundle an offline speech-recognition model.
- You must open Buddy and tap **START LISTENING**. Android restricts background microphone starts and continuous listening; do not expect it to listen after force-stop, reboot, or reliably while locked.
- Buddy does not silently send messages, place calls, access private media, make payments, operate banking apps, or bypass Android security. Sensitive actions require user interaction.
- Use **STOP** or say **रुक जा** to end microphone use.

## Technical
- Package: `com.example.buddy`
- Minimum Android: 8.0 (API 26)
- Target Android: API 35
- Language: Kotlin
