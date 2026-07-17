---
name: telegrammicro-protocol-change
description: Change or diagnose Telegram MTProto, TL parsing, authentication, crypto, transport, updates, RPC, session, or secret-chat behavior in TelegramMicro. Use whenever a constructor/combinator, protocol state transition, wire boundary, or Telegram interoperability path is involved.
---

# TelegramMicro protocol changes

Apply a reference-first, bounded workflow. Read `docs/north-star.md` and the protocol sections of `AGENTS.md` before editing.

## Locate authoritative local evidence

1. Find the constructor ID/name and current send/receive path under `src/app` and its tests under `src/test`.
2. Inspect the closest implementation/spec evidence in:
   - `references/ps2/td/td/mtproto` for transport, auth, envelope, replay, salt, and crypto behavior;
   - `references/ps2/td/td/telegram` for API requests, updates, dialogs/messages, and session behavior;
   - `references/ps2/telegram-ps2/src`, `tests`, `docs`, and `schemas` for the constrained reference client and fixtures.
3. Note constructor/layer differences. Do not copy reference code blindly or broaden the local parser speculatively.

## Define the invariant

State the exact accepted shape/state transition and the bounded rejection behavior. Include applicable limits for frame/body length, TL bytes, vector/container count, inflate output, bigint/DH inputs, media chunks, retries, queues, RMS payloads, and cached models.

For auth/crypto work, also identify the transcript binding: expected nonce, server nonce, hash, fingerprint, time/salt, message ID, sequence number, or replay state.

## Implement and prove

1. Add a focused regression that fails for the original defect.
2. Add hostile siblings where relevant: unknown constructor, wrong nonce/hash, truncation, excessive length/count, integer overflow, replay, invalid DH range/subgroup, and decompression overflow.
3. Validate before allocation, copy, iteration, or durable mutation.
4. Prefer a conservative bounded fallback over silently accepting an unsupported shape.
5. Run the focused test, `./scripts/test.sh`, then `./scripts/verify.sh --full`.
6. Add `--ui` only when startup or visible runtime state changed. Use an explicitly authorized live helper only when interoperability cannot be proved offline.

Report which local references were inspected and keep unit, target-VM, host, live, and device evidence separate. A parsed constructor is not full feature support; secret-chat lifecycle/DH state is not encrypted-message interoperability.
