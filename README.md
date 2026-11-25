# Buddy Android Voice Assistant

A Kotlin Android voice-control prototype. Tap **START LISTENING**, then speak a command directly — you do not need to say “Buddy” before every command.

## Current voice commands
- Stop listening: **रुक जा**, **stop listening**, or **stop**.
- Time, date, and battery status.
- Open YouTube, Chrome, Gmail, Play Store, Photos, Camera, Maps, Contacts, Messages, Phone dialer, Wi-Fi/Bluetooth settings, Android Settings, and Calculator.
- Try opening other installed apps by saying their visible app name (for example, open WhatsApp). This uses Android launcher visibility; apps without a launcher entry may not be available.
- Optional voice typing: enable ENABLE OPTIONAL TEXT TYPING in Buddy, then enable Buddy in Android Accessibility settings. Open the target app yourself, tap its text field so the cursor is visible, and say type [words], टाइप करो [शब्द], or लिखो [शब्द]. Buddy only inserts text into the currently focused editable field. Check the text and press Search or Send yourself.
- Search the web by saying **search [your words]** or **सर्च [your words]**.
- Set an alarm by saying a numeric time such as **alarm 7:30**; Buddy opens the system clock screen for your confirmation.
- Basic Hindi greetings and assistant identity responses.
- Unrecognized voice questions are sent to the configured online AI provider. For clear app-opening, focused-field typing, or web-search requests, the AI planner may propose one supported action and waits for your spoken yes/no confirmation before doing it.

## Online AI setup
Open Buddy's **AI Brain Settings** and enter an HTTPS OpenAI-compatible Chat Completions endpoint, the provider's exact model ID, and an API key. Save the settings. The app does not include a free or unlimited AI service; provider quotas and pricing can change. Never put an API key in this public repository or share it in a chat.

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
- Speech recognition availability, language support, and offline recognition depend on the phone's installed speech service. The app does not bundle an offline speech-recognition model.
- You must open Buddy and tap **START LISTENING**. Android restricts background microphone starts and continuous listening; do not expect it to listen after force-stop, reboot, or reliably while locked. The current build does not implement an always-on “Buddy” wake word.
- Online AI planning requires internet and a configured provider. The AI action planner is restricted to one action at a time: OPEN_APP, TYPE_TEXT in the already-focused field, or WEB_SEARCH. Buddy asks for spoken confirmation before executing. If the plan is unclear or unsupported, it answers normally instead. It does not create multi-step plans or arbitrary screen taps.
- Accessibility typing is optional and disabled until you enable it in Android Settings. AI-planned typing also requires your spoken confirmation and only inserts text into the currently focused field. It does not read screen contents, click buttons, submit forms, or press Send/Search. You must review and submit text yourself.
- Buddy does not silently send messages, place calls, access private media, make payments, operate banking apps, or bypass Android security. Sensitive actions require user interaction.
- Use **STOP** or say **रुक जा** to end microphone use.

## Technical
- Package: `com.example.buddy`
- Minimum Android: 8.0 (API 26)
- Target Android: API 35
- Language: Kotlin
