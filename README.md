# Buddy Android Assistant

A Kotlin Android voice assistant prototype. Tap **START LISTENING** and then speak commands directly — you do not need to say “Buddy” before every command.

## Voice commands currently supported
- Say **रुक जा**, **stop listening**, or **stop** to stop the microphone service.
- Ask for the current time.
- Open YouTube, Chrome, Gmail, Play Store, Photos, Camera, Maps, Contacts, Messages, Phone dialer, and Calculator (availability depends on apps installed on the phone).
- Open Android Settings, Wi-Fi settings, or Bluetooth settings.
- Say an alarm request with a numeric time (for example, "alarm 7:30") to open the system alarm screen for confirmation.

## Build the APK
Every push to `main` starts the **Build Buddy APK** GitHub Actions workflow. When the workflow succeeds:
1. Open the repository's **Actions** tab.
2. Open the latest successful **Build Buddy APK** run.
3. Under **Artifacts**, download **Buddy-debug-apk**.
4. Extract the ZIP and install `app-debug.apk` on your Android phone. Android may ask you to allow installation from that source.

## Important limitations
- This is an early voice-control prototype, not yet a full conversational AI or unrestricted phone operator.
- You must open Buddy and tap **START LISTENING** to start the microphone. Android restricts microphone foreground services and background starts; Buddy cannot promise always-on listening while locked or after reboot.
- Speech recognition availability and language quality depend on the phone's speech service and may require internet. Offline speech recognition must be installed and enabled separately if supported by the device.
- Voice actions are limited to the commands listed above. Arbitrary taps, secure settings, sending messages, making calls, payments, banking, and private-data access are not automated.
- Sensitive actions should require clear confirmation. Use **STOP** in the app or say **रुक जा** to stop listening.

## Project
- Package: `com.example.buddy`
- Minimum Android: 8.0 (API 26)
- Target Android: API 35
