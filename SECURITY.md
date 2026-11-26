# Buddy Android Security Notes

## Scope and honest status
This is a defensive review of the current Android codebase, not a certification or a guarantee that a phone is malware-free. Findings are based on source inspection; runtime, device, provider, and supply-chain checks still require testing.

## Changes in this security hardening pass
- Cleartext HTTP is disabled in the Android application and network security configuration.
- AI endpoint validation requires HTTPS, rejects URL credentials, fragments, non-standard ports, IP literals, localhost/local domains, and DNS results that resolve to private, loopback, link-local, multicast, or IPv6 unique-local addresses.
- AI HTTP redirects are disabled so an HTTPS endpoint cannot silently redirect requests containing the API key.
- AI responses are read with a 64 KiB cap to limit oversized responses.
- API keys continue to be encrypted with Android Keystore; no key should ever be committed to this public repository.
- Android backup is disabled.
- The API key is no longer prefilled into the settings field; leaving it blank preserves the saved key, and a separate button clears it.
- Optional Accessibility service listens only for focused-view events; it is not subscribed to window-state events.
- The listening service is not exported. Accessibility is protected by Android's BIND_ACCESSIBILITY_SERVICE permission.

## Important existing permissions and user controls
- RECORD_AUDIO: needed for voice recognition; listening is started by the user and shown by a foreground notification.
- INTERNET: needed for optional online AI.
- POST_NOTIFICATIONS: used for the visible listening notification on Android 13+.
- Accessibility service: optional. Android grants it powerful system-level access. Keep it OFF unless voice typing is needed. Current implementation only inserts text into the currently focused editable field; it does not read screen text or click/submit controls. Disable it in Android Accessibility settings whenever not in use.
- Buddy must never install APKs, download/install other apps, grant itself permissions, or silently enable Accessibility.

## Not applicable to the current app architecture
The current app has no web frontend, user accounts, checkout, payment processing, database-backed multi-user records, OAuth/SSO, WebSockets, server-side templates, or file-upload API. Therefore browser XSS/CSRF/CORS, IDOR/BOLA, payment/price/coupon, RLS, JWT/session, OAuth, WebSocket, SSTI, and mass-assignment checks are not applicable to the current code as inspected. Reassess if those features are introduced.

## Remaining risks / unverified checks
- The configured AI provider receives prompts and, when enabled, recent chat history. Use only a provider you trust; do not send passwords, payment details, private media, or sensitive personal data.
- The user-configured AI endpoint is checked on-device, but DNS can theoretically change between validation and connection (DNS rebinding); this is not a substitute for a trusted-provider allowlist or backend proxy.
- Accessibility service behaviour and microphone foreground-service behaviour must be tested on the actual phone and Android version.
- No malware scan can prove the entire device is clean. This repository review cannot inspect other apps or files on the user's phone.
- CI now runs Android Lint, unit tests, and a debug build. Dependabot weekly update checks are configured for Gradle dependencies and GitHub Actions.
- CodeQL static analysis and secret scanning should be enabled/verified in repository Security settings; no claim is made that all secrets or malware have been detected.
- GitHub Actions are referenced by major version tags, not immutable commit SHAs; supply-chain pinning remains a hardening item.
- Release signing, APK malware scanning, and device testing remain to be completed. The current workflow produces a debug APK, not a production-signed release.
- A successful build proves compilation only; it does not prove the app is secure.

## Phone safety checklist before installing
1. Install only the APK linked from the Buddy-4 repository's own successful GitHub Actions run, not APKs sent by strangers.
2. Check Android's package installer details and Play Protect warning/status. Do not bypass a malware warning.
3. Grant microphone and notification permissions only when needed.
4. Leave Buddy's Accessibility service OFF unless you explicitly need voice typing; turn it OFF again after use.
5. Never grant Buddy device-admin, install-unknown-apps, VPN, SMS, contacts, call-log, or file-management access unless a future clearly explained feature truly requires it.
6. Keep Android and Google Play system updates current; run Google Play Protect from Play Store.
7. If the phone shows an unexpected install prompt, unknown app, or security warning, cancel and investigate rather than approving it.

## 30-point guide mapping
Applicable now: secret handling, prompt injection/tool least privilege, input/output bounds, network transport, dependency/supply-chain checks, permission minimization, exported component review, secure storage, CI build permissions, and security misconfiguration.
Not applicable now: web-only, payments/commerce, multi-user database, OAuth, upload, and server-side categories listed above.


## Verification record
- Source review and AndroidManifest/permission review performed in this repository.
- CI lint, unit tests, and APK build are configured; results must be checked on the latest workflow run before distributing its artifact.
- No test on the user's physical phone has been performed from this environment.
