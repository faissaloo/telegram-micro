---
name: telegrammicro-emulator-mcp
description: Inspect or automate TelegramMicro's LCDUI in hosted SquirrelJME through the embedded ScritchUI MCP server. Use for startup, keypad input, screen-state, direct screenshot, restore/reconnect UI, or headless emulator diagnostics.
---

# TelegramMicro emulator and MCP

Default to offline, isolated evidence. Read `docs/verification.md` before any production-account flow.

## Preferred proof

For startup/LCDUI changes, run:

```sh
./scripts/verify.sh --full --ui
```

This selects a dynamic localhost port, uses isolated emulator state, checks that first-install entropy blocks network startup, captures the application surface directly, validates the PNG, and cleans the run. Inspect the retained capture and log under `artifacts/ui-smoke`.

## Manual diagnosis

1. Build first with `./scripts/build.sh`.
2. Select an unused localhost port; do not hardcode a shared default.
3. Create an isolated state directory and set `TELEGRAM_MICRO_EMULATOR_STATE_DIR`.
4. Launch `./scripts/run.sh` with `SQUIRRELJME_MCP_PORT`.
5. Initialize MCP, call `scritch.snapshot`, resolve controls by kind/actions/labels, and use `scritch.screenshot` on the application surface.
6. Stop only that run with `./scripts/stop-emulator.sh "$PORT"`.

Reuse `scripts/lib/mcp.sh` from shell automation for port validation, readiness, capture target selection, and PNG validation. Do not scrape X11 or route routine captures through a virtual display.

## Interaction rules

- Resolve controls from the current snapshot; object IDs are runtime evidence, not stable selectors.
- Prefer labels, kinds, actions, and visible-window ancestry over coordinate clicks.
- Capture before and after a state transition when the result is not terminal-visible.
- Keep scratch JSON and emulator state in a unique temporary directory with mode-safe evidence output.
- Never replace `HOME`; use `TELEGRAM_MICRO_EMULATOR_STATE_DIR`.
- Treat an unavailable MCP control or missing expected marker as a failed proof, not a reason to weaken the assertion.

## Live boundary

Use only the existing `scripts/validate-*.sh` helper matching the requested production outcome. Never bypass `TELEGRAM_MICRO_LIVE=1`, the exact acknowledgement, explicit phone/state input, or the shared live-validation library. Never print credentials or session contents. Live evidence does not replace offline regression or physical-device proof.
