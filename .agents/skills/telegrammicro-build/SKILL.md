---
name: telegrammicro-build
description: Build, test, or verify TelegramMicro and its pinned SquirrelJME toolchain. Use for compile failures, test selection, CI reproduction, production JAR/JAD checks, clean-bootstrap proof, or pre-commit/pre-push validation in this repository.
---

# TelegramMicro build and verification

Run commands from the repository root. Use root scripts directly; do not recreate their environment or add skill-local wrappers.

## Choose the evidence lane

- Production package only: `./scripts/build.sh` then `./scripts/check-device-budget.sh`.
- Fast development regression: focused test if available, then `./scripts/test.sh`.
- Standard offline checkpoint: `./scripts/verify.sh --full`.
- Startup or LCDUI change: with a real display, run `./scripts/verify.sh --full --ui`; without one, run `./scripts/verify.sh --full` and report the UI lane unavailable.
- Slow crypto inside the target VM: `./scripts/test-target-full.sh`; label this as a long soak.
- Bootstrap/SquirrelJME patch change: use a new `SQUIRRELJME_DIR`, then run the standard offline checkpoint.

Read `docs/verification.md` before substituting one lane for another.

## Workflow

1. Inspect `git status --short` and preserve unrelated user changes.
2. Reproduce with the narrowest command that exercises the failure.
3. Fix the production invariant and add/update a regression.
4. Run the narrow check, then the change-type gate from `AGENTS.md`.
5. Inspect `git diff --check`, the production artifact budget, and any UI artifact required by the change.
6. Report target-VM, host, offline UI, live, and device evidence separately.
7. After the checkpoint passes, commit the narrow change and push it when repository writes are authorized; never include unrelated or overlay-only changes.

Do not report the full gate as passed if a lane was skipped, timed out, or replaced. Never use live Telegram helpers as ordinary build verification.

## Failure routing

- Tool download, checksum, base commit, or patch fingerprint: inspect `scripts/common.sh` and `references/squirreljme`.
- Java compile/package failure: inspect `build/app-classes`, generated config under `build`, and the first compiler error.
- Target-VM-only test failure: compare the same focused test on the host only for diagnosis; target evidence remains required.
- JAR/class budget failure: reduce production footprint or justify and document a measured budget change; never hide tests in the production JAR.
- UI smoke exit 69: run the lane in a real graphical session and report it unavailable until then. For other UI failures, inspect the retained `artifacts/ui-smoke` log/capture and use the emulator skill.
