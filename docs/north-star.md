# North star

TelegramMicro should be the smallest trustworthy Telegram client that is genuinely useful on a Nokia C2-01-class phone. “Small” means bounded in memory, storage, payload size, and interaction cost. “Trustworthy” means protocol-correct, fail-closed around secrets, recoverable after interruption, and honest about what has been proved.

## Product outcomes

The intended useful core is:

1. Install and start reliably on the target device.
2. Collect entropy before the first network authentication attempt.
3. Log in and restore an authorized session without leaking credentials.
4. Read a bounded set of dialogs and recent messages.
5. Send and reconcile bounded text messages.
6. Recover predictably after restart, network loss, malformed traffic, or full local storage.
7. Present a clear 240x320 keypad-first UI with useful failure states.

Everything else is subordinate to this path. A feature is not valuable if it makes login, reading, sending, or recovery unreliable on the target.

## Decision filter

When choosing between implementations, prefer the one that:

- has an explicit worst-case allocation and wire/storage bound;
- can be proved with deterministic tests and observable runtime evidence;
- fails locally without corrupting durable authorization state;
- reuses buffers/objects and transfers ownership clearly;
- keeps unsupported Telegram shapes detectable and recoverable;
- degrades to less history/media/metadata instead of process failure;
- remains comprehensible enough to audit against local Telegram references.

## Non-goals

- Full desktop/mobile Telegram parity.
- Unbounded history, roster, update, or media caches.
- Background media prefetch or large image pipelines.
- Claiming secret-chat support before encrypted payload exchange is interoperable.
- Treating emulator success as proof of heap headroom, RMS behavior, radio behavior, keypad ergonomics, or installation on a Nokia handset.
- Silently ignoring protocol constructors to make a demo appear stable.

## Architecture invariants

- Every external length/count is validated before allocation or iteration.
- Authentication randomness is never sourced from a predictable PRNG and network auth waits for the first-install entropy gate.
- Protocol transcript checks bind the expected nonces/hashes before accepting state transitions.
- Durable state has a version, size cap, corrupt-input path, and bounded migration story.
- Production JARs contain no tests, credentials, live evidence, or host-only dependencies.
- Unsupported behavior is surfaced through bounded failure/recovery state rather than accidental success.

## How progress is measured

Track progress as a ladder of evidence, not a feature count:

1. Local invariant is unit verified.
2. Behavior executes in the SquirrelJME target VM.
3. Offline LCDUI behavior is directly captured and inspected.
4. Explicitly authorized Telegram interoperability succeeds.
5. The exact artifact succeeds on the Nokia device checklist.

A roadmap item advances only to the evidence level actually demonstrated.
