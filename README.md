# Buddy Android Voice Assistant

A Kotlin Android voice-control prototype. Tap **START LISTENING**, then say **Buddy** to get its attention or speak a command directly.

## Current voice commands
- Stop listening: **रुक जा**, **stop listening**, or **stop**.
- Time, date, and battery status.
- Media volume up/down, mute, and unmute by voice.
- Ask **what can you do** / **तुम क्या कर सकते हो** to hear Buddy's supported features.
- Open YouTube, Chrome, Gmail, Play Store, Photos, Camera, Maps, Contacts, Messages, Phone dialer, Wi-Fi/Bluetooth settings, Android Settings, and Calculator.
- Try opening other installed apps by saying their visible app name (for example, open WhatsApp). This uses Android launcher visibility; apps without a launcher entry may not be available.
- Optional voice typing: enable ENABLE OPTIONAL TEXT TYPING in Buddy, then enable Buddy in Android Accessibility settings. Open the target app yourself, tap its text field so the cursor is visible, and say type [words], टाइप करो [शब्द], or लिखो [शब्द]. Buddy only inserts text into the currently focused editable field. Check the text and press Search or Send yourself.
- Search the web by saying **search [your words]** or **सर्च [your words]**.
- Set an alarm by saying a numeric time such as **alarm 7:30**; Buddy opens the system clock screen for your confirmation.
- Basic Hindi greetings and assistant identity responses.
- Unrecognized voice questions are sent to the configured online AI provider. For clear app-opening, focused-field typing, or web-search requests, the AI planner may propose up to three supported steps. Buddy asks for a separate spoken yes/no confirmation before each step; cancelling or timing out cancels the remaining steps.

## Online AI setup
Open Buddy's **AI Brain Settings** and enter an HTTPS OpenAI-compatible Chat Completions endpoint, the provider's exact model ID, and an API key. Save the settings. The API key is encrypted on-device using Android Keystore; older plaintext keys are migrated when possible. The app does not include a free or unlimited AI service; provider quotas and pricing can change. Never put an API key in this public repository or share it in a chat.

## Optional conversation memory
Memory is **OFF by default**. If enabled and saved in settings, Buddy stores up to five recent user/assistant exchanges in local app preferences and sends that context with later AI questions. Use **CLEAR AI CHAT MEMORY** to clear the saved exchanges. Do not store passwords, payment information, or other sensitive details. This local preference storage is not encrypted storage.

## Build and install
Every push to `main` starts the **Build Buddy APK** GitHub Actions workflow. When the workflow succeeds:
1. Open the repository's **Actions** tab.
2. Open the latest successful **Build Buddy APK** run.
3. Under **Artifacts**, download **Buddy-debug-apk**.
4. Extract the ZIP and install `app-debug.apk`. Android may ask you to allow installation from that source.

## Limitations — read before use
- Offline capability currently means only local command handling and Android intents. There is no bundled offline Qwen/LLM model.
- Buddy prefers Android's on-device speech recognizer when the phone exposes one; otherwise it uses the system recognizer with offline preference. Actual offline recognition and language support depend on the installed speech service and language pack. The app does not bundle its own speech-recognition model.
- You must open Buddy and tap **START LISTENING** first. While that foreground listening session is active, saying **Buddy** gets its attention; this is not an always-on hotword engine. Android restricts background microphone starts and continuous listening; do not expect it to listen after force-stop, reboot, or reliably while locked.
- Online AI planning requires internet and a configured provider. The AI action planner is restricted to at most three safe steps: OPEN_APP, TYPE_TEXT in the already-focused field, or WEB_SEARCH. Buddy asks for separate spoken confirmation before every step; an unconfirmed step automatically expires after 15 seconds and cancels the remaining steps. If the plan is unclear or unsupported, it answers normally instead. It does not perform arbitrary screen taps.
- Accessibility typing is optional and disabled until you enable it in Android Settings. AI-planned typing also requires your spoken confirmation and only inserts text into the currently focused field. It does not read screen contents, click buttons, submit forms, or press Send/Search. You must review and submit text yourself.
- Buddy does not silently send messages, place calls, access private media, make payments, operate banking apps, or bypass Android security. Sensitive actions require user interaction.
- Use **STOP** or say **रुक जा** to end microphone use.

## First phone test checklist
After installing the latest successful debug APK:
1. Open Buddy and grant microphone permission. Grant notification permission when Android asks.
2. Tap **START LISTENING** and confirm the ongoing Buddy listening notification appears.
3. Say **Buddy**, then try **time**, **date**, **battery**, **open YouTube**, and **रुक जा**.
4. Start listening again. For voice typing, enable Buddy only if you want this optional feature, open a notes app, focus its text field, and say **टाइप करो नमस्ते**. Verify the text appears; submit it yourself.
5. Test online AI only after configuring your own provider endpoint, model, and API key. Try a harmless question first. For an AI-proposed supported action, test both **हाँ** and **नहीं**; if you do not answer within 15 seconds, it should cancel.
6. Tap **STOP** and confirm the listening notification disappears.

These are manual device checks, not tests already performed by the repository build. GitHub Actions verifies that the Android project compiles and packages; it cannot verify microphone behavior, speech recognition, accessibility settings, or actual phone actions on your device.

## Technical
- Package: `com.example.buddy`
- Minimum Android: 8.0 (API 26)
- Target Android: API 35
- Language: Kotlin
