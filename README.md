# Buddy Android Assistant

A starter Android voice-assistant project built with Kotlin and Android SpeechRecognizer/TextToSpeech.

## Current prototype features
- Tap **START LISTENING** and grant microphone permission.
- Say **Buddy** or **बडी** to activate the command listener.
- Hindi speech response.
- Open YouTube, Chrome, and Android Settings by voice.
- Ask for the current time.
- Say an alarm request with a numeric time (for example, "alarm 7:30") to open the system alarm screen for confirmation.
- Tap **STOP** to stop the foreground listening service.

## Build the APK
Every push to `main` starts the **Build Buddy APK** GitHub Actions workflow. When the workflow succeeds:
1. Open the repository's **Actions** tab.
2. Open the latest successful **Build Buddy APK** run.
3. Under **Artifacts**, download **Buddy-debug-apk**.
4. Extract the ZIP and install `app-debug.apk` on your Android phone. Android may ask you to allow installation from that source.

## Important limitations
- This is an early prototype, not a full hands-free assistant. Android speech recognition may depend on the device's speech service and internet connection.
- Wake-word detection is not a dedicated on-device wake-word engine and is not guaranteed to work while the app is closed or the phone is locked.
- The alarm command opens the phone's clock/alarm confirmation screen; it does not silently set an alarm.
- App launching is limited to the listed apps. No payment, banking, private-photo, or messaging actions are implemented.
- Keep microphone access under your control; use **STOP** when you do not want Buddy listening.

## Project
- Package: `com.example.buddy`
- Minimum Android: 8.0 (API 26)
- Target Android: API 35
