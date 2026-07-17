# TelegramMicro Agent Contract

## North star

Build the smallest trustworthy Telegram client that remains useful on a Nokia C2-01-class J2ME device. Optimize in this order:

1. Protocol and cryptographic correctness.
2. Bounded RAM, storage, network payloads, and allocation churn.
3. Recoverable login, session, and messaging flows.
4. Legible 240x320 keypad-first interaction.
5. Feature breadth only when the preceding properties remain true.

The emulator is evidence, not the target. Never trade a device-safe design for something that only works comfortably on the host.

## Autonomous development loop

For each change, work through this loop without waiting for routine confirmation:

1. Read the nearest implementation, test, and relevant local reference.
2. State the invariant or user outcome being changed.
3. Make the smallest coherent change, including bounded failure behavior.
4. Add or update a regression test that can fail for the original defect.
5. Run the narrowest useful check, then the applicable verification gate below.
6. Inspect runtime/UI evidence when static tests cannot prove the behavior.
7. Report exactly what was proved, what was not exercised, and any residual risk.

Do not stop for a choice that can be resolved from local code, tests, references, or these priorities. Do stop before an irreversible or production-facing action that needs authority.

## Repository map

- `src/app`: MIDlet production code. It must remain CLDC 1.1/MIDP 2.1 compatible.
- `src/test`: target-VM and host-assisted tests.
- `scripts`: the supported build, test, emulator, and verification interface.
- `references/ps2`: local Telegram/MTProto reference library.
- `references/squirreljme`: pinned patches required by the emulator workflow.
- `.agents/skills`: focused repo-local agent workflows.
- `docs/north-star.md`: product and architecture decision filter.
- `docs/verification.md`: evidence lanes and operator-safe live validation.
- `docs/device-proof.md`: real Nokia evidence checklist.
- `docs/parity-roadmap.md`: prioritized capability backlog, not a parity claim.

## Scripted workflow

Prefer scripts over the J2MEWTK UI:

- `./scripts/bootstrap-squirreljme.sh`: obtain and patch the pinned SquirrelJME base.
- `./scripts/build.sh`: build only the production JAR/JAD.
- `./scripts/test.sh`: quick target-VM lane.
- `./scripts/test-full.sh`: quick target-VM lane plus slow host crypto vectors.
- `./scripts/test-target-full.sh`: optional long-running all-target-VM crypto soak.
- `./scripts/verify.sh --quick`: source policy, production artifact, and quick tests.
- `./scripts/verify.sh --full`: default pre-commit/pre-push gate.
- `./scripts/verify.sh --full --ui`: add isolated offline startup/UI proof.
- `./scripts/run.sh`: launch the hosted emulator manually.

Bootstrap may download pinned tools. Once present, ordinary verification should reuse them deterministically.

## Proof required by change type

| Change | Minimum proof |
| --- | --- |
| Pure Java logic/model | Focused regression plus `./scripts/test.sh` |
| MTProto, TL, auth, crypto, transport, parser | Local reference inspection, hostile/boundary regression, `./scripts/test-full.sh` |
| RMS/session/warm state | Round-trip, corrupt/truncated/oversized input test, `./scripts/test-full.sh` |
| Build/bootstrap/SquirrelJME patch | `./scripts/verify.sh --full` from a clean pinned checkout when practical |
| Startup/LCDUI/MCP behavior | `./scripts/verify.sh --full --ui` and inspect the direct surface capture |
| Device budget or lifecycle | Artifact budget gate plus the relevant checklist in `docs/device-proof.md` |
| Live Telegram behavior | Offline gates first, then the explicit production procedure in `docs/verification.md` |

Do not substitute host execution for target-VM evidence without saying so. Do not call emulator evidence real-device proof. A skipped, timed-out, or unavailable lane is an unproved lane, not a pass.

## Reference-first protocol work

Before changing MTProto behavior, Telegram request/response semantics, update handling, or emulator-facing protocol behavior:

1. Identify the constructor/combinator and current handling locally.
2. Inspect the matching behavior in `references/ps2/td/td/mtproto`, `references/ps2/td/td/telegram`, and/or `references/ps2/telegram-ps2`.
3. Record the invariant in a focused test.
4. Implement only the locally required, protocol-correct slice.

Treat references as specifications and implementation evidence, not code to copy blindly. Unknown constructors, oversized vectors/frames, invalid DH values, replayed messages, decompression overflow, and truncated payloads must fail boundedly.

## Nokia C2-01 constraints

- Prefer fixed-capacity models, bounded arrays, reusable buffers, and ownership transfer over unbounded collections and repeated copies.
- Put an explicit limit before every network-, RMS-, decompression-, vector-, media-, or user-controlled allocation.
- Keep test classes out of the production JAR. `check-device-budget.sh` enforces the current 512 KiB JAR and 256-class guardrails; lowering these budgets is encouraged when measurement supports it.
- Treat reader state, LCDUI state, crypto scratch, parser scratch, media buffers, and RMS snapshots as one competing working set.
- Avoid background prefetch, large thumbnails, and full-history retention.
- Preserve CLDC/MIDP compatibility; do not introduce modern Java runtime APIs into `src/app`.
- When an exact device limit is unknown, use a conservative named constant and document how real-device evidence should refine it.

## Emulator and MCP

- Prefer hosted SquirrelJME unless investigating a hosted-runtime defect.
- Prefer the embedded MCP server for inspection and input.
- Use `scritch.screenshot` for direct application-surface captures instead of desktop screenshots.
- Hosted mode is not displayless: the pinned GTK2 ScritchUI backend still requires a real X display. In a displayless environment, run deterministic model/target-VM gates and report the UI lane unavailable; do not substitute a virtual X server.
- Use dynamic localhost MCP ports unless a caller explicitly supplies one.
- Keep emulator state in `TELEGRAM_MICRO_EMULATOR_STATE_DIR`; never repurpose `HOME`.
- Stop only the run associated with a known MCP port. `stop-emulator.sh --all` is an explicit emergency cleanup.

## Production and secret safety

- Offline verification is the default. It must not contact Telegram or send a login code.
- Live helpers must retain the shared fail-closed gate: `TELEGRAM_MICRO_LIVE=1`, the exact production acknowledgement, and explicit phone/state input.
- Never embed or print phone numbers, login codes, passwords, API hashes, auth keys, or session contents.
- Never commit `.telegrammicro.env`, local emulator state, logs, captures, or live evidence artifacts.
- A live script may mutate only the account/session explicitly supplied by the operator.

## Evidence and claims

Use precise labels in handoff notes and roadmap updates:

- `unit verified`: a deterministic test proves the local invariant.
- `target-VM verified`: the test executed under SquirrelJME's target VM.
- `offline UI verified`: a clean-state MCP run produced inspected evidence without Telegram production actions.
- `live verified`: an explicitly authorized production-account flow completed.
- `device verified`: the exact JAR/JAD completed the documented Nokia checklist.

Do not say “supported,” “complete,” “parity,” or “device-safe” when only a shell, parser branch, or unit test exists. Keep capability, interoperability, UX, and device evidence separate.

## Git checkpointing

When the task authorizes commits and pushes, create and push a coherent checkpoint after each verified milestone. Before every checkpoint:

1. Run `git diff --check` and the applicable gate.
2. Review the staged diff for credentials, artifacts, unrelated work, and overstated docs.
3. Use a narrow commit message that describes the proved outcome.
4. Push and confirm the remote branch moved before starting the next risky milestone.

Never rewrite or discard user changes to make a checkpoint. Review-only requests do not authorize commits.

## Definition of done

A change is done only when implementation, regression proof, applicable artifact/UI checks, and documentation agree. The final report must include commands and outcomes, identify any production or device lane not run, and avoid turning known gaps into implied success.

## Spoken progress summaries

When working in the terminal, provide concise spoken progress at meaningful milestones: after investigation, before major changes, after tests, or when blocked. Keep each update under 15 seconds and invoke it through interactive zsh:

```sh
zsh -ic 'say "Codex update: progress made."'
```

If speech is unavailable, continue silently. Never speak secrets, credentials, paths containing sensitive identifiers, or long data strings.
