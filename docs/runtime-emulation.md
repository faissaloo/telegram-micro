# J2ME runtime emulation

TelegramMicro targets real CLDC 1.1/MIDP phones, not one idealized desktop VM. This document defines the evidence boundary for SquirrelJME runtime profiles. A profile name is not a claim that its vendor quirks have been reproduced.

## Current implementation

The pinned hosted SquirrelJME build accepts `-Xprofile:<id>`, validates aliases, and exposes the canonical value as `cc.squirreljme.runtime.profile` inside the target VM. Unknown names fail before the MIDlet starts. The default is `ideal`.

At present, every vendor profile deliberately retains ideal behavior. The narrow C2-01 profile additionally supplies static firmware-confirmed system properties, which are identity data rather than behavioral quirks. The only implemented runtime correction in this patch is specification-oriented hosted GCF client TCP behavior. No Nokia, Sony Ericsson, Jbed, JBlend, J9, or Jeode behavioral divergence is claimed yet.

| Canonical ID | Accepted short aliases | Current vendor-specific behavior | Evidence still required |
| --- | --- | --- | --- |
| `ideal` | — | Specification-oriented baseline | Continue expanding MIDP/CLDC conformance probes |
| `nokia-kvm` | `kvm`, `nokia` | None | Exact Nokia model, firmware, runtime generation, and probe transcript |
| `nokia-jrt` | `jrt` | None | Exact Series 40 model/JRT version and probe transcript |
| `nokia-s40-pr9.1-rm72x-v11.40` | `c2-01-v11.40`, `rm72x-v11.40` | Confirmed identity properties only; no behavioral divergence | Physical RM-721/RM-722 11.40 probe transcript |
| `myriad-jbed` | `jbed` | None | Exact handset/Jbed build and probe transcript |
| `aplix-jblend` | `jblend` | None | Exact handset/JBlend build and probe transcript |
| `ibm-j9` | `j9` | None | Exact J9 bundle/version and probe transcript |
| `insignia-jeode` | `jeode`, `jecode` | None | Exact Jeode build/device and probe transcript |
| `sony-ericsson-jp` | `sony-ericsson`, `semc` | None | Exact Sony Ericsson Java Platform generation, handset firmware, and probe transcript |

Use a profile for a manual hosted run with:

```sh
TELEGRAM_MICRO_RUNTIME_PROFILE=nokia-jrt ./scripts/run.sh
```

For a direct SquirrelJME launch, add `-Xprofile:nokia-jrt` to `SQUIRRELJME_VM_ARGS`. If both are present, `TELEGRAM_MICRO_RUNTIME_PROFILE` is appended last and therefore wins. This selector changes emulator behavior only; it does not rewrite TelegramMicro's production JAR.

The narrow C2-01 profile is based on the static [RM-722 firmware 11.40 evidence](firmware-evidence/rm722-v11.40.md). It exposes only the confirmed CLDC, MIDP, default-encoding, JTWI, and MSA-subset properties. Static firmware strings and product configuration do not establish exception ordering, limits, or MIDP-visible command behavior, so those surfaces remain ideal pending a physical probe.

## Ideal GCF boundary

The hosted backend currently implements outbound `socket://host:port` TCP connections using the Java SE host network. Its target-VM probe covers:

- DNS resolution through `localhost` and an isolated IPv4 loopback server;
- exact `Connector.READ`, `WRITE`, and `READ_WRITE` enforcement;
- rejection of mode zero and a missing client port before connecting;
- one input and one output stream per connection;
- continued use of already-open streams after `Connection.close()`;
- input/output half-close behavior so the opposite direction can continue;
- MIDP `DELAY`, `LINGER`, `KEEPALIVE`, `RCVBUF`, and `SNDBUF` mapping;
- the existing SquirrelJME `TIMEOUT` extension at the provider boundary; and
- the convenience `Connector.openOutputStream()` lifetime rule.

This does not implement server sockets, UDP/datagrams, SSL/TLS sockets, HTTP completeness, target-native SpringCoat networking, permission prompts, radio behavior, or vendor-specific GCF quirks. A localhost pass is not Telegram interoperability or device proof.

The normative baseline is Oracle's archived [MIDP 2.0 `SocketConnection`](https://docs.oracle.com/javame/config/cldc/ref-impl/midp2.0/jsr118/javax/microedition/io/SocketConnection.html), [`Connector`](https://docs.oracle.com/javame/config/cldc/ref-impl/midp2.0/jsr118/javax/microedition/io/Connector.html), [`StreamConnection`](https://docs.oracle.com/javame/config/cldc/ref-impl/midp2.0/jsr118/javax/microedition/io/StreamConnection.html), and CLDC [`Connection`](https://docs.oracle.com/javame/config/cldc/ref-impl/cldc1.1/jsr139/javax/microedition/io/Connection.html) documentation. IBM's archived [J9 CLDC/MIDP installation guide](https://public.dhe.ibm.com/software/pervasive/info/products/wctme/CLDCPmst.pdf) establishes the J9 CLDC 1.1/MIDP 2.0 runtime family, but does not by itself justify a behavioral quirk.

## Quirk acceptance rule

A vendor-specific divergence may be added only when all of these exist:

1. A named runtime, version, handset model, and firmware or an authoritative vendor document precise enough to identify the behavior.
2. A minimal MIDlet probe with bounded output and no account secrets.
3. The observed ideal result and vendor result, including exception type/order where relevant.
4. A focused regression which fails without the profile rule.
5. A default-`ideal` regression proving that specification-oriented behavior did not change.
6. Documentation of resource impact and whether the quirk is required by TelegramMicro.

Do not infer a whole runtime family's behavior from one handset. Prefer the narrowest key that evidence supports. If model/firmware discrimination becomes necessary, extend the selector explicitly instead of silently broadening a runtime profile.

## Device/runtime probe inventory

Capture the following without allocating unbounded result collections. Each probe should emit one short line per case and a final checksum or pass count.

| Surface | Measurements to record |
| --- | --- |
| Identity | `microedition.*`, locale, encoding, VM/vendor/platform properties, optional API presence |
| Language/CLDC | class initialization failure, verifier rejection, exception ordering, `arraycopy`, UTF-8 edge cases, clock/thread behavior |
| GCF | URI grammar, DNS, modes, permission timing, timeout behavior, stream reopen/close, socket options, HTTP redirect/header rules, TLS/cipher availability |
| RMS | suite/name scope, maximum record/store size, enumeration order, delete/open behavior, full-store and corrupt-record exceptions |
| LCDUI | Canvas/full-screen dimensions, key codes/game actions, softkeys, commands, traversal, repaint coalescing, fonts and clipping |
| Lifecycle | start/pause/destroy ordering, background/resume, interruption by calls, network teardown, RMS flush timing |
| Resources | usable heap, allocation-failure shape, thread/stack limits, JAR/JAD/class caps, GC pauses, image/media decode ceilings |
| Vendor APIs | Nokia UI/file APIs, Sony Ericsson properties/APIs, and other optional packages actually present on the measured device |

For every physical run, record artifact hashes plus the metadata in [device-proof.md](device-proof.md). Keep raw traces outside Git if they reveal device or account identifiers; commit only a redacted minimal fixture and provenance note.

## Verification

```sh
./scripts/verify-squirreljme-network.sh
./scripts/verify-runtime-profiles.sh
./scripts/verify.sh --full
```

The first two commands are offline and loopback-only. `verify.sh --full` includes both. A SquirrelJME patch change must also be applied and built from a fresh checkout of the pinned base before checkpointing.
