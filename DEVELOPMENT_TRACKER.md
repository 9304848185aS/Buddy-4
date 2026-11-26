# Buddy Development Tracker

Repository: https://github.com/9304848185aS/Buddy-4

This tracker records implementation work only. A task is checked only after its code change is committed and the relevant GitHub validation passes. CI does not replace real-device verification.

## Completed

- [x] Enforce user authority and task-scope boundaries in the AI action-planner instructions.
- [x] Restrict action plans to the supported allowlist: `OPEN_APP`, `TYPE_TEXT`, and `WEB_SEARCH`.
- [x] Reject malformed plans, unexpected JSON fields, unsupported actions, oversized values, and control characters.
- [x] Add parser regression tests for malformed and unsafe plan shapes.
- [x] Fix the escaped-newline regression test.
- [x] Pass Android Lint, Unit Tests, and Debug APK build in GitHub Actions run #119 (commit `f106c955c460f6227660cf57631646f574166ca2`).

## In progress

- [ ] Improve multi-step planning quality: minimal ordered steps, explicit dependencies, no invented targets, and stop at the requested outcome.
- [ ] Validate the updated planner prompt with CI and add tests for any deterministic plan-validation rules.

## Remaining implementation work

- [ ] Review and improve voice-command understanding for Hindi, English, and Hinglish, including ambiguous commands and confirmation handling.
- [ ] Improve the JARVIS-style interface while keeping controls accessible and clear.
- [ ] Expand phone automation only through Android-supported APIs and user-enabled permissions, with task-scoped confirmation and safe allowlists.
- [ ] Improve wake-word and listening behavior within Android background-microphone restrictions.
- [ ] Review privacy and security: permissions, notification/screen data handling, API-key storage, memory controls, and network behavior.
- [ ] Add or improve automated tests for command routing, action execution, confirmation, cancellation, and error paths.
- [ ] Produce and verify a final release APK after the remaining implementation and CI checks.

## Known capability limits to resolve or document

- The current action planner can only open installed apps, type into an already-focused editable field, and launch web searches.
- It cannot yet perform arbitrary screen taps, submit forms, send messages, place calls, make payments, or bypass Android security.
- Speech recognition and continuous listening depend on Android's installed speech service and platform restrictions; this is not a dedicated always-on hotword engine.
- CI verifies code and APK compilation, not behavior on every physical phone.
