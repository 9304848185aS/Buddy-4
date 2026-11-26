# Buddy Development Tracker

Repository: https://github.com/9304848185aS/Buddy-4

This tracker records implementation work only. A task is checked only after its code change is committed and the relevant GitHub validation passes. CI does not replace real-device verification.

## Completed

- [x] Enforce user authority and task-scope boundaries in the AI action-planner instructions.
- [x] Improve multi-step planning instructions: minimal ordered steps, dependencies, no invented targets, and stop at the requested outcome.
- [x] Validate the updated planner instructions with Android Lint, Unit Tests, and Debug APK build in GitHub Actions run #121 (commit `9d9e3a31a83ba2541f33da7aaf4fbdbf598e75b0`).
- [x] Restrict action plans to the supported allowlist: `OPEN_APP`, `TYPE_TEXT`, and `WEB_SEARCH`.
- [x] Reject malformed plans, unexpected JSON fields, unsupported actions, oversized values, and control characters.
- [x] Add parser regression tests for malformed and unsafe plan shapes.
- [x] Fix the escaped-newline regression test.
- [x] Pass Android Lint, Unit Tests, Debug APK build, and APK artifact upload in GitHub Actions run #123 (commit `095c93b8f21d2d7592d00564d95859d0ed7de4ca`).
- [x] Audit current voice-service routing, screen-reading, focused-field typing, and permission declarations.

## In progress

- [ ] Expand phone automation only through Android-supported APIs and user-enabled permissions, with task-scoped confirmation and safe allowlists.

## Remaining implementation work

- [ ] Improve wake-word and listening behavior within Android background-microphone restrictions.
- [ ] Complete privacy/security review across permissions, notification/screen data handling, API-key storage, memory controls, and network behavior.
- [x] Remove dismissed notification previews from Buddy's in-memory list and clear stored AI conversation history when optional memory is switched off. Validated in Build #134 (commit `11bed34f338bdf7659915790f6fe6db6b3dffca1`).
- [ ] Add or improve automated tests for command routing, action execution, confirmation, cancellation, and error paths.
- [ ] Produce and verify a final release APK after the remaining implementation and CI checks.

## Known capability limits to resolve or document

- The current action planner can only open installed apps, type into an already-focused editable field, and launch web searches.
- It cannot yet perform arbitrary screen taps, submit forms, send messages, place calls, make payments, or bypass Android security.
- Speech recognition and continuous listening depend on Android's installed speech service and platform restrictions; this is not a dedicated always-on hotword engine.
- CI verifies code and APK compilation, not behavior on every physical phone.
