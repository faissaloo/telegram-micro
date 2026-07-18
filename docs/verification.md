# Verification

TelegramMicro separates deterministic offline proof from production-account and real-device proof. The default gate is always offline.

## Standard gates

| Command | What it proves | What it does not prove |
| --- | --- | --- |
| `./scripts/build.sh` | Production JAR/JAD compile and package | Tests, runtime, device installation |
| `./scripts/test.sh` | Quick registered tests execute in the SquirrelJME target VM | Slow crypto vectors, UI, Telegram interoperability |
| `./scripts/test-full.sh` | Quick target-VM lane plus large crypto vectors on the host JVM | All vectors in target VM, UI, live service |
| `./scripts/test-target-full.sh` | All registered tests including slow crypto vectors in the target VM | UI, live service; this can be a long soak |
| `./scripts/verify.sh --quick` | Shell/source policy, clean diff checks, production budgets, quick tests | Slow vectors and UI |
| `./scripts/verify.sh --full` | Standard offline pre-push gate | UI, production Telegram, physical Nokia |
| `./scripts/verify.sh --full --ui` | Standard gate plus clean-state first-install/entropy and deterministic history UI captures | Login interoperability or real device |

`verify-ui.sh` and `verify-history-ui.sh` require a real display for the pinned GTK2 ScritchUI backend, even though the app runs through the hosted VM. They fail before starting the emulator when Linux has no `DISPLAY`; do not replace this evidence lane with a virtual X server. The first-install lane requires the entropy marker and rejects evidence if bootstrap/reconnect starts before it. The history lane runs a test-JAR-only, account-free model fixture through the production canvas and proves timestamp plus `read`/`sent` layout states. Both write ignored artifacts under `artifacts/ui-smoke` and clean only their isolated temporary state.

## Change-to-proof matrix

- Parser/TL/transport changes need malformed, truncated, oversized, and unknown-shape regressions where applicable.
- PTS-bearing mutation acknowledgements need contiguous, duplicate/stale, and gap cases for both cloud and channel state.
- Authentication/crypto changes need local reference comparison, transcript/nonce failure tests, and the full offline gate.
- Durable-state changes need current round-trip, legacy compatibility when promised, and corrupt/truncated/oversized rejection.
- UI/startup changes need direct MCP inspection in addition to model tests.
- Build/bootstrap changes should be tested against a clean checkout of the pinned SquirrelJME base.
- Heap/device-budget changes need the artifact gate and eventually the physical-device checklist.

## Clean bootstrap proof

For bootstrap or SquirrelJME-patch work, point `SQUIRRELJME_DIR` at an unused directory and run the standard gate. The bootstrap script owns only the explicit directory, verifies the pinned base and patch fingerprint, and rejects unrelated dirty state.

Do not delete or reset an existing checkout to manufacture cleanliness. Use a new temporary destination.

## Live Telegram safety gate

The `validate-*.sh` scripts can contact Telegram, send a login code, sign in, modify session state, join/leave a channel, or exercise account data. Each script uses `scripts/lib/live-validation.sh` and refuses to start unless:

- `TELEGRAM_MICRO_LIVE=1`;
- `TELEGRAM_MICRO_LIVE_ACK=telegram-production` exactly;
- phone-taking scripts receive `TELEGRAM_MICRO_TEST_PHONE` explicitly;
- restore/sync scripts receive an existing `TELEGRAM_MICRO_VALIDATE_STATE_DIR` explicitly.

They select a dynamic MCP port by default, isolate scratch files, keep evidence under ignored `artifacts/live`, validate captured PNGs, and never print credential values. `TELEGRAM_MICRO_VALIDATE_HOME` is intentionally rejected.

The helpers have these effects:

| Script | Production effect / proof target |
| --- | --- |
| `validate-login-input.sh` | Starts the production-capable app and types, but does not submit, the explicit phone value |
| `validate-send-code.sh` | Submits the phone and requests a Telegram login code |
| `validate-code-restore.sh` | Requests one code, relaunches the same isolated state, verifies code-stage restore |
| `validate-sign-in.sh` | Requests a code, accepts code/optional 2FA input, persists an authorized state directory |
| `validate-restore-session.sh` | Reopens an explicitly supplied authorized state and waits for dialogs |
| `validate-channel-sync.sh` | Opens channel data from the explicitly supplied authorized state and exercises channel catch-up |
| `validate-reconnect-session.sh` | Triggers the in-app reconnect path for the explicitly supplied authorized state |

Use a dedicated test account. Review the chosen script before acknowledging production. Do not run live helpers in CI.

## Evidence language

Every report should identify the lane and outcome, for example:

- `target-VM verified: quick lane passed`;
- `host verified: slow crypto vectors passed`;
- `offline UI verified: direct first-install capture inspected`;
- `live lane not run: no production-account authorization`;
- `device lane not run: no Nokia handset attached`.

Never collapse those statements into “all supported” or “device-safe.” Include the failing test/command and retained artifact path when a lane fails.
