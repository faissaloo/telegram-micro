# Nokia C2-01 device proof

Emulator tests protect correctness and regressions; they do not establish real heap headroom, RMS limits, networking behavior, keypad ergonomics, or installability. A release candidate is device verified only when the exact JAR/JAD completes this checklist on the Nokia C2-01 (or the explicitly named equivalent device).

## Record before testing

- Git commit and whether the tree was clean.
- SHA-256 of `bin/TelegramMicro.jar` and `bin/TelegramMicro.jad`.
- JAR byte size and class count reported by `check-device-budget.sh`.
- Phone model, firmware, reported Java runtime/platform properties, free storage, network type, and Java security prompts.
- Whether the account/state is fresh or restored.

Keep this record outside Git if it contains account/device identifiers.

## Installation and cold start

- Install the matching JAD/JAR without manual manifest edits.
- Cold-start after a phone reboot.
- Confirm the first-install entropy screen appears before network authentication.
- Complete entropy input and reach the phone-entry state without an out-of-memory error.
- Exit and relaunch at least three times; verify no duplicate/corrupt RMS state.

## Core user path

- Enter a phone number with the physical keypad and correct mistakes.
- Request and enter a login code; test 2FA if the test account enables it.
- Load dialogs and open recent history for private chat, basic group, and channel where available.
- Scroll long labels/messages and verify command access at 240x320.
- Send a short text, observe pending/success reconciliation, then relaunch and confirm consistent state.
- Trigger a network loss during a safe operation, restore connectivity, and verify bounded recovery.

## Resource and failure checks

- Exercise the maximum bounded dialog/history views repeatedly and watch for progressive slowdown or OOM.
- Open messages containing long text, entities, and supported media metadata.
- Attempt a media item above the configured device-safe limit and confirm a clear bounded refusal.
- Fill or constrain local storage if safely possible and confirm RMS failure does not destroy the authorized session.
- Leave the app idle, resume it, and test after clock/network changes.
- Confirm logs/status do not expose credentials or session secrets.

## Pass criteria

The candidate passes only if the exact artifact completes the checklist without OOM, uncaught exception, corrupted authorization state, unintended network action, or unusable keypad/navigation behavior. Record each omitted scenario as not tested.

If a failure appears only on device, preserve the artifact hash, minimal reproduction, last visible state, and any safe diagnostic code. Add an emulator/unit regression where possible, but retain the device scenario until the same artifact is rerun successfully.

When the failure suggests a runtime quirk, also record the narrow probe transcript and provenance fields from [runtime-emulation.md](runtime-emulation.md). Do not generalize one handset observation to an entire KVM/JRT/Jbed/JBlend/J9/Jeode/Sony Ericsson family without additional evidence.
