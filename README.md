# TelegramMicro

[![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-blue.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
[![Release: preview](https://img.shields.io/badge/release-v0.1.0--preview.1-orange.svg)](https://github.com/faissaloo/telegram-micro/releases/tag/v0.1.0-preview.1)

TelegramMicro is an experimental Telegram client for CLDC 1.1/MIDP 2.1 phones. Its design target is the Nokia C2-01: bounded memory, keypad-first interaction, recoverable state, and protocol correctness take priority over broad desktop-style parity.

It is not yet an everyday-ready client. The repository has deterministic build/test automation and bounded foundations for authentication, sessions, dialogs/history and in-chat search, updates, outbound text, limited media, and secret-chat lifecycle work. Interoperability breadth and real-device proof remain incomplete; see [the roadmap](docs/parity-roadmap.md) and [device checklist](docs/device-proof.md).

The first published build is a developer preview. Download its JAR/JAD from [v0.1.0-preview.1](https://github.com/faissaloo/telegram-micro/releases/tag/v0.1.0-preview.1), and read the [release-readiness assessment](docs/release-readiness.md) before installing it. The public binary is intentionally credential-free, so it can inspect startup and device/runtime compatibility but cannot request a Telegram login code. A functional account build requires your own Telegram API ID/hash as described below.

## Screenshots

These are sanitized, account-free captures from deterministic SquirrelJME fixtures. They demonstrate the 240x320-targeted application surface, not physical-device proof.

<p align="center">
  <img src="docs/images/secure-setup.png" width="220" alt="One-time secure setup entropy collection screen">
  <img src="docs/images/history-message-state.png" width="220" alt="Bounded chat history with timestamps and sent/read state">
  <img src="docs/images/message-search-results.png" width="220" alt="Bounded in-chat search result screen">
</p>

## Quick start

The supported workflow is scripted SquirrelJME, not the legacy J2MEWTK UI.

```sh
./scripts/bootstrap-squirreljme.sh
./scripts/verify.sh --full
./scripts/run.sh
```

Bootstrap pins SquirrelJME to the commit recorded in `scripts/common.sh`, applies `references/squirreljme/*.patch`, and obtains checksum-pinned JDK/CMake tools when they are absent. Defaults live under `/tmp`; the source-checkout directory is keyed by the base commit and patch content so a new patch set never mutates an older checkout. Override `TELEGRAM_MICRO_BOOTSTRAP_DIR` and `SQUIRRELJME_DIR` for durable caches.

Build outputs are:

- `build/dist/TelegramMicro.jar`
- `build/dist/TelegramMicro.jad`

Prepare a credential-free public release bundle with:

```sh
./scripts/package-release.sh v0.1.0-preview.1
```

The packager fails closed if generated credentials are present and writes the JAR, JAD, and `SHA256SUMS` under `build/release/`.

## Verification lanes

```sh
./scripts/test.sh                 # quick tests in the target VM
./scripts/test-full.sh            # target-VM quick lane + slow host crypto vectors
./scripts/test-target-full.sh     # optional long target-VM crypto soak
./scripts/verify.sh --quick       # policy, artifact, and quick test gate
./scripts/verify.sh --full        # standard pre-push gate
./scripts/verify.sh --full --ui   # add isolated startup/history/search MCP proof
```

The production artifact gate excludes test classes and enforces current JAR/class guardrails. The full gate also runs account-free hosted GCF loopback and runtime-profile propagation probes. Each UI lane uses a fresh emulator-state directory, a dynamic localhost MCP port, and a direct PNG capture from the application surface.

See [verification.md](docs/verification.md) for the proof matrix, clean-bootstrap checks, and exact evidence vocabulary.

## Telegram API credentials

Telegram API ID/hash values are build-time inputs:

```sh
cp .telegrammicro.env.example .telegrammicro.local.env
./scripts/build.sh
```

Edit the ignored local file before building. Without credentials, the login UI remains inspectable but the app blocks `auth.sendCode`. Never commit API hashes, phone numbers, login codes, passwords, or session state.

## Emulator and MCP

`./scripts/run.sh` defaults to hosted SquirrelJME. To inspect a manual run, provide an unused localhost port:

```sh
SQUIRRELJME_MCP_PORT="$PORT" ./scripts/run.sh
```

The emulator has a validated runtime-profile boundary. Broad vendor profiles are identity-only, while the narrow C2-01 firmware profile adds confirmed system properties but no behavioral quirks:

```sh
TELEGRAM_MICRO_RUNTIME_PROFILE=nokia-jrt ./scripts/run.sh
TELEGRAM_MICRO_RUNTIME_PROFILE=nokia-s40-pr9.1-rm72x-v11.40 ./scripts/run.sh
```

See [runtime-emulation.md](docs/runtime-emulation.md) for profile IDs, exact current evidence, and the device-probe requirements for adding a real runtime divergence.

Use the embedded ScritchUI MCP endpoint and `scritch.screenshot` for direct application-surface evidence. The pinned GTK2 backend still needs a real X display even in hosted mode; a displayless agent should report the UI lane unavailable rather than add a virtual X server. Keep isolated state in `TELEGRAM_MICRO_EMULATOR_STATE_DIR`; do not replace the process `HOME`.

Stop a known run with:

```sh
./scripts/stop-emulator.sh "$PORT"
```

`./scripts/stop-emulator.sh --all` is deliberately explicit and should only be used for broad cleanup.

## Live Telegram validation

Live helpers can send login codes or mutate the supplied account/session. They are disabled unless all production gates are explicit:

```sh
TELEGRAM_MICRO_LIVE=1 \
TELEGRAM_MICRO_LIVE_ACK=telegram-production \
TELEGRAM_MICRO_TEST_PHONE="$TEST_PHONE" \
./scripts/validate-send-code.sh
```

Sign-in stores reusable emulator state under its ignored evidence directory and prints `state_dir=...`. Follow-up restore/sync scripts require that directory through `TELEGRAM_MICRO_VALIDATE_STATE_DIR`; no personal account or state path is built in. Details and script effects are documented in [verification.md](docs/verification.md).

## Project guidance

- [North star and decision rules](docs/north-star.md)
- [Verification and live-operation safety](docs/verification.md)
- [Real Nokia proof checklist](docs/device-proof.md)
- [Prioritized parity roadmap](docs/parity-roadmap.md)
- [Telegram feature matrix](docs/telegram-feature-matrix.md)
- [Keypad-first UX guidance](docs/ux-guidelines.md)
- [J2ME runtime emulation and quirk evidence](docs/runtime-emulation.md)
- [Nokia C2-01 RM-722 firmware 11.40 evidence](docs/firmware-evidence/rm722-v11.40.md)
- [RAP3Gv4 QEMU deep-emulation research plan](docs/rap3gv4-qemu-plan.md)
- [Release readiness and feature-completeness assessment](docs/release-readiness.md)
- [Agent development contract](AGENTS.md)

Local Telegram/MTProto reference implementations are under the ignored `references/ps2` tree. Protocol changes should be reference-first and should include hostile boundary tests.

Useful platform references:

- [CLDC 1.1](https://docs.oracle.com/javame/config/cldc/ref-impl/cldc1.1/jsr139/)
- [MIDP 2.0](https://docs.oracle.com/javame/config/cldc/ref-impl/midp2.0/jsr118/)
