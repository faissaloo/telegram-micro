# Capability roadmap

This is a prioritized evidence backlog, not a claim of parity with Telegram clients or the local PS2 reference. The target is the narrow useful core in [north-star.md](north-star.md), constrained by Nokia C2-01 resources. Use [telegram-feature-matrix.md](telegram-feature-matrix.md) for the extensive product inventory and [ux-guidelines.md](ux-guidelines.md) for keypad-first interaction decisions.

## Evidence states

- `implemented`: production code path exists.
- `unit verified`: deterministic regression covers its local invariant.
- `target-VM verified`: relevant tests ran under SquirrelJME.
- `offline UI verified`: clean-state UI behavior was captured through MCP.
- `live verified`: an explicitly authorized Telegram account completed the flow.
- `device verified`: the exact artifact passed [device-proof.md](device-proof.md).

These states are independent. A parser can be unit verified while the end-to-end feature remains unusable.

## P0: trustworthy install, login, read, send, recover

| Capability | Current bounded foundation | Next evidence/work |
| --- | --- | --- |
| Deterministic build | Pinned SquirrelJME base/patches and checksum-pinned tools; production/test artifacts split | Keep CI green; exercise clean bootstrap after patch/tool changes |
| First-install entropy | Production CSPRNG seed gate and offline startup marker | Physical keypad/usability and repeated cold-start proof |
| MTProto authentication | Handshake, nonce/hash checks, DH validation, auth-key/session persistence paths | Maintain hostile transcript vectors; live and device login matrix |
| Login UI | Phone/code/password state models and MCP automation | Error/retry/expiry UI proof; physical keypad proof |
| Durable session | Versioned bounded snapshots, corrupt/oversized rejection, RMS tests | Power-loss/full-storage device scenarios and migration policy |
| Dialog/history read | Bounded models, parsing, cache and update foundations | Expand constructor interoperability conservatively; long-session live/device proof |
| Message context | Sender, content, media and pending state are bounded; wire dates are parsed | Preserve/restore timestamps, then add compact delivery/read state without paint-time allocation |
| Text send | Bounded compose/request/reconciliation and recovery foundations | Transport-loss duplicate/retry matrix; live/device proof |
| Reconnect/catch-up | Session restore, difference/channel-difference and fallback foundations | More gap/too-long/unknown-update traces and device radio-loss proof |
| Failure visibility | Bounded failure log/status surfaces | Make every P0 terminal failure actionable without exposing secrets |

P0 is complete only when the exact release artifact passes the real-device checklist for fresh login and restored-session read/send/recovery. That evidence does not exist merely because emulator lanes pass.

## P1: interoperability hardening

1. Build a curated fixture corpus for currently observed config, auth, dialogs, messages, updates, and RPC errors. Record constructor IDs and reference provenance without storing secrets.
2. For every parser family, cover unknown constructor, truncation, excessive count/length, allocation refusal, and conservative recovery.
3. Exercise DC migration/export-import authorization and media DC behavior without retaining multiple unbounded connections.
4. Strengthen outbound idempotence across restart and ambiguous acknowledgements.
5. Define durable-state compatibility windows and explicit reset/re-auth behavior for unsupported migrations.
6. Add long-running bounded-state soak tests that assert capacities and lack of progressive retained growth.

## P2: device-suitable usability

1. Tune screen density, focus, softkeys, and error copy using physical 240x320 keypad evidence.
2. Add contact/search/profile operations only where the result can remain bounded and cached cheaply.
3. Support media only through declared byte/dimension/chunk budgets, with streaming or durable storage when safe.
4. Add accessibility/readability options only if their font/layout cost fits the working-set budget.
5. Measure startup, dialog load, history open, send, and reconnect latency on device and keep regression notes by artifact hash.

## Deferred until the core is device-proven

- Full roster/profile/admin parity.
- Attachment upload and general media rendering.
- Background prefetch/sync.
- Large local search/history indexes.
- Multi-account operation.
- Full secret-chat messaging. Lifecycle/DH registry work is not encrypted-message interoperability.

## How to advance an item

Every advancement should include:

1. The user outcome and bounded resource invariant.
2. The local reference paths used for protocol behavior.
3. A regression that fails without the change.
4. The exact verification lanes run.
5. Honest labels for live and device lanes not run.
6. Updated limits/checklists when new measurement changes the design budget.

Keep exhaustive Telegram surface area in `telegram-feature-matrix.md`. Add an item here only when it protects the north-star path or represents the next smallest useful, device-bounded capability.
