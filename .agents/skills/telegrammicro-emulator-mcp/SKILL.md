---
name: telegrammicro-emulator-mcp
description: Inspect or automate TelegramMicro's LCDUI in hosted SquirrelJME through the embedded ScritchUI MCP server. Use for startup, keypad input, screen-state, direct screenshot, restore/reconnect UI, display-backed emulator diagnostics, or displayless UI-lane triage.
---

# TelegramMicro emulator and MCP

Default to offline, isolated evidence. Offline means no production Telegram access; it does not mean displayless. Read `docs/verification.md` before any production-account flow.

## Preferred proof

For startup/LCDUI changes, first check that Linux has a real `DISPLAY`. When it does, run:

```sh
./scripts/verify.sh --full --ui
```

This selects a dynamic localhost port, uses isolated emulator state, checks that first-install entropy blocks network startup, captures the application surface directly, validates the PNG, and cleans the run. Inspect the retained capture and log under `artifacts/ui-smoke`.

Without a real display, run `./scripts/verify.sh --full` and report the UI lane unavailable. Do not add a virtual X server or treat model/target-VM proof as UI proof.

## Manual diagnosis

1. Confirm that the environment has a real display before launching the pinned GTK2 backend.
2. Build first with `./scripts/build.sh`.
3. Select an unused localhost port; do not hardcode a shared default.
4. Create an isolated state directory and set `TELEGRAM_MICRO_EMULATOR_STATE_DIR`.
5. Launch `./scripts/run.sh` with `SQUIRRELJME_MCP_PORT`.
6. Initialize MCP, call `scritch.snapshot`, resolve controls by kind/actions/labels, and use `scritch.screenshot` on the application surface.
7. Stop only that run with `./scripts/stop-emulator.sh "$PORT"`.

Reuse `scripts/lib/mcp.sh` from shell automation for port validation, readiness, capture target selection, and PNG validation. Do not scrape the desktop or route routine captures through a virtual display.

## Interaction rules

- Resolve controls from the current snapshot; object IDs are runtime evidence, not stable selectors.
- Prefer labels, kinds, actions, and visible-window ancestry over coordinate clicks.
- Capture before and after a state transition when the result is not terminal-visible.
- Keep scratch JSON and emulator state in a unique temporary directory with mode-safe evidence output.
- Never replace `HOME`; use `TELEGRAM_MICRO_EMULATOR_STATE_DIR`.
- Treat an unavailable MCP control or missing expected marker in a runnable lane as failed proof, not a reason to weaken the assertion.
- Treat a missing real display as an unavailable lane, not a passing or failing product result.

## Live boundary

Use only the existing `scripts/validate-*.sh` helper matching the requested production outcome. Never bypass `TELEGRAM_MICRO_LIVE=1`, the exact acknowledgement, explicit phone/state input, or the shared live-validation library. Never print credentials or session contents. Live evidence does not replace offline regression or physical-device proof.
