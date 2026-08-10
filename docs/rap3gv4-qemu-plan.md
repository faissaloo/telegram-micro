# RAP3Gv4 deep-emulation plan

## Purpose and evidence boundary

This is a research and implementation plan for a QEMU machine capable of
progressively booting the Nokia C2-01 RM-721/RM-722 software 11.40 stack. It is
not a claim that the supplied firmware boots today. The goal is to complement,
not replace, hosted SquirrelJME:

- SquirrelJME remains the fast, deterministic MIDP/CLDC and TelegramMicro test
  lane.
- QEMU should eventually exercise Nokia's actual Series 40 Java runtime,
  lifecycle, storage, LCDUI, and device services on a measured RAP3Gv4 board
  model.
- A real RM-721/RM-722 remains the authority for device behavior. QEMU evidence
  is labelled `deep-emulator verified`, never `device verified`.

QEMU requires a board model because Arm firmware is tied to the SoC, memory
map, and peripherals rather than to a PC-like standard platform. Its own Arm
documentation warns that an image for one Arm machine will generally not run
on another. The implementation must therefore introduce a dedicated
`nokia-c2-01` machine instead of loading the files into `virt` or an unrelated
phone model. See QEMU's [Arm machine guidance](https://gitlab.com/qemu-project/qemu/-/blob/master/docs/system/target-arm.rst)
and [device-model terminology](https://qemu.readthedocs.io/en/v9.2.4/system/device-emulation.html).

## Findings available now

### Board facts

The Nokia service schematic identifies board `3my_30c`, a RAP3Gv4 device
(`D2800`), and stacked combo memory (`D3000`) containing 64 MB DDR and 128 MB
flash. It also identifies the PEARL_J energy-management/peripheral ASIC,
240x320 display path, camera CCP interface, full-speed USB, keyboard matrix,
SIM, MMC, audio, Bluetooth/FM, and RF connections. The Level 1/2 service manual
independently confirms the QVGA display, Series 40 software, Bluetooth,
micro-USB, and the RM-721/RM-722 radio-band distinction.

Sources:

- [Nokia C2-01 RM-721/RM-722 service schematic](https://usermanual.wiki/Document/nokiac201rm721rm722serviceschematicsv1.1366791010.pdf)
- [Nokia C2-01 RM-721/RM-722 Level 1/2 service manual](https://altehandys.de/downloads/ser-man-no-c2-01.pdf)
- [local firmware inspection](firmware-evidence/rm722-v11.40.md)

The public schematic is connectivity evidence, not a programming manual. It
does not provide the RAP3Gv4 memory map, interrupt numbers, reset vectors,
register semantics, DMA behavior, timing, DSP interface, or BootROM contents.
The generic RAP3G family is reported to contain a processor subsystem, DSP, MCU
peripherals, and DSP peripherals, but the exact RAP3Gv4 CPU revision and QEMU
CPU model remain unconfirmed. `arm926` is only a bring-up hypothesis until an
instruction trace or authoritative RAP3Gv4 document proves it.

### Firmware-container facts

The three supplied payloads are Nokia BB5/XSR flash containers, not flat ROM
images. Each starts with a binary envelope containing `BB5` and `XSR 1.6`.
Additional static observations are:

| File | Evidence | Consequence |
| --- | --- | --- |
| `rm721__11.40.mcusw` | `Elf2flash 09.07.000`, `PASUBTOC`, `PAPUBKEYS`, `RAP3NAND`, `GENIO_INIT`, `ISA+DYNSW`, `ISA-USER` | Build a bounded container inventory and reconstruct flash placement before attempting execution. |
| `rm721__11.40.mcusw` | Boot diagnostics reference authenticated `Naloext`, SDRAM discovery, and `MuxOneNAND (CS0)` manufacturer/device/revision IDs | Model the secure boot boundary and the exact multiplexed OneNAND behavior; a raw `-bios` load is invalid. |
| `rm721__11.40.ppm_f` | `Elf2flash 09.07.000`, software 11.40 and language-pack metadata | Treat PPM as a separately placed flash component, not an executable boot image. |
| `rm721__11.40.image_f_059F5P7` | `Elf2flash 11.51.000`, `CMT PI 299`, then a FAT16 content volume | Preserve both the BB5 placement envelope and extracted filesystem; the FAT volume alone cannot boot the handset. |

The MCU strings also expose startup branches for missing SDRAM, missing
OneNAND, energy-management initialization, clocks, and authentication failure.
Those messages provide useful progress markers but do not reveal register
addresses or prove which branches execute on this handset.

The bundle does **not** contain a verified flat flash reconstruction, the
on-chip BootROM, per-device OTP/root-key state, calibration/permanent-memory
data, SIM identity, or a hardware execution trace. No committed tool should
invent those values or redistribute extracted proprietary payloads.

### Reusable QEMU capabilities

QEMU already supplies the useful framework pieces: Arm TCG execution, QOM/qdev
device composition, memory regions, IRQ wiring, block backends, graphical/input
backends, QMP, qtest, tracing, and a GDB stub. The
[GDB stub](https://qemu.readthedocs.io/en/v10.0.3/system/gdb.html) can stop at
reset and inspect registers and memory; deterministic
[record/replay](https://qemu.readthedocs.io/en/v10.0.3/system/replay.html) is a
later diagnostic option once every nondeterministic input is modelled.

Historical QEMU includes a Nokia-authored
[OneNAND device model](https://gitlab.com/qemu-project/qemu/-/blob/v2.6.1/hw/block/onenand.c).
It is useful specification and implementation evidence, but it must be audited
against the selected modern QEMU release and the observed `MuxOneNAND` IDs and
bus behavior. It is not evidence that current QEMU can attach the C2-01 flash
unchanged.

## Architecture

Keep the machine as small and observable as possible:

```text
BB5 container decoder -> immutable reconstructed flash + manifest
                                      |
                                      v
QEMU nokia-c2-01 machine
  RAP3Gv4 CPU/reset/IRQ/clock/DMA shell
  64 MB DDR       multiplexed OneNAND       synthetic OTP/security state
  PEARL_J          display/keypad            USB/MMC
  RF/service boundary -> off | measured stub | explicit lab network backend
                                      |
                                      v
                         Series 40 -> Nokia JVM -> TelegramMicro
```

The decoder should be a host tool separate from QEMU device code. Its output is
a deterministic manifest of source hashes, bounded regions, addresses, flags,
checksums, and overlaps plus a sparse flash image. It must reject unknown block
types, overflow, truncation, overlap, or an unauthenticated transformation.
Generated flash images, device identifiers, traces, and firmware remain ignored
local evidence.

The machine should use QOM devices for major boundaries rather than one giant
board file. Start with CPU/reset, address space, RAM, interrupt controller,
timers/clocks, and OneNAND. Add PEARL_J, display, keyboard, and other peripherals
only when a trace or document defines their observable contract. Unknown MMIO
must log a bounded first-seen record and fail or return a configurable poison
value; silent all-zero devices are forbidden because they hide missing models.

## Modes and safety

Provide distinct modes from the start:

| Mode | Behavior | Permitted claim |
| --- | --- | --- |
| `strict` | Only evidence-backed devices and values; unknown MMIO stops or reports deterministically; radio off | Firmware/model research only |
| `probe-replay` | Replays a versioned, sanitized handset trace at a defined device boundary | Compatibility with that exact trace |
| `lab` | May provide synthetic battery/SIM state and a host packet-data bridge | TelegramMicro workflow testing, explicitly not hardware fidelity |

Default to `strict`. Never expose a physical modem, transmit RF, clone an IMEI,
or load real SIM/account credentials. Any future packet-data backend should
operate above the RF/DSP boundary and use QEMU's normal user/TAP networking. It
must be visibly labelled synthetic in the guest-facing evidence and disabled in
hardware-conformance runs.

Security state needs the same separation. Model a deterministic, synthetic
device identity and document which checks it satisfies. Do not patch out
authentication merely to advance the program counter: if a development bypass
is temporarily necessary, keep it in an opt-in `lab` device and retain a strict
failure test.

## Milestones and gates

### 0. Research workspace and provenance

- Pin an upstream QEMU release and commit in a separate GPL-compatible fork or
  patch stack; do not vendor a QEMU tree into the TelegramMicro production JAR.
- Add ignored paths and scripts that require the operator to supply the firmware
  and verify its existing SHA-256.
- Record schematic/manual revisions, handset board/firmware identity, and every
  inferred fact in a machine-readable evidence ledger.
- Decide redistribution rules before publishing derived flash layouts or traces.

Gate: a clean checkout builds QEMU without firmware; supplying the known bundle
reproduces only hashes and metadata, and no proprietary payload is tracked.

### 1. Bounded BB5/XSR inventory and flash reconstruction

- Specify the outer `BB5`/`XSR 1.6` records, checksums, placement records, and
  erase/write regions from independent samples and flashing traces.
- Decode MCU, PPM, and content envelopes without executing embedded material.
- Emit a sparse 128 MB candidate flash plus JSON/text manifest, never a silently
  concatenated image.
- Keep protected/per-device regions blank and explicit.

Gate: hostile truncated, oversized, overlapping, and unknown records fail before
allocation or output mutation; reconstruction is byte-reproducible; every output
extent points back to an input extent and verified checksum. This gate does not
claim bootability.

### 2. Physical reset and bus trace

- Acquire a sacrificial RM-721/RM-722 11.40 handset and non-destructive service
  access. Capture power-on and Nokia USB flashing exchanges before considering
  invasive probing.
- Record CPU mode, reset PC, early instruction bytes, OneNAND IDs, memory aliases,
  first MMIO accesses, clocks, and interrupts where tooling permits.
- Capture separately: cold boot, no-SIM boot, offline/flight mode, keypad input,
  display start, Java launch, RMS write, suspend, and resume.
- Sanitize serials, public IDs, IMEI, SIM, certificates, and user data at capture
  time. Never commit raw protected traces.

Gate: two cold boots agree on the stable event sequence and clearly label
per-device/nondeterministic fields. Address-map entries require either two
independent observations or one observation plus authoritative documentation.

### 3. Minimal RAP3Gv4 QEMU machine

- Register `-M nokia-c2-01`, 64 MB RAM, candidate Arm CPU, reset vector, immutable
  BootROM input, and a first-seen MMIO tracer.
- Implement only enough interrupt, timer, clock, and reset state to reproduce the
  captured prefix. Keep DSP/RF held in a documented quiescent state.
- Expose QMP queries for boot stage, unknown MMIO count, IRQ state, and flash
  identity.

Gate: qtests prove reset state, aliases, alignment, endianness, IRQ transitions,
and unknown-MMIO bounds. The emulated PC trace matches a measured prefix; a CPU
model mismatch fails visibly rather than being papered over.

### 4. BootROM, security, DDR, and multiplexed OneNAND

- Require an operator-supplied, lawfully acquired BootROM or a clean-room reset
  model derived from observable behavior.
- Implement synthetic OTP/root-key state, authenticated stage transitions, DDR
  setup, OneNAND IDs, BootRAM/buffer behavior, ECC/status, bad blocks, and
  read/program/erase timing actually used by 11.40.
- Audit and port the historical QEMU OneNAND model where it matches; add qtests
  before modifying semantics.

Gate: strict mode reaches the same authenticated loader milestone as hardware,
and corruption of every boot-stage input fails at the corresponding boundary.
Snapshots cannot mutate the operator's source firmware.

### 5. Kernel/OS services and first framebuffer

- Add discovered interrupt-controller, timers, DMA, watchdog, GPIO/GENIO, CBUS,
  PEARL_J power/reset/RTC/battery behavior, and the minimum DSP/RF mailbox stubs.
- Model the display controller and panel timing only far enough to produce the
  measured 240x320 surface; initially ignore camera, audio, Bluetooth, and RF
  payload paths while returning truthful unavailable state.

Gate: deterministic boot reaches a framebuffer; unknown MMIO trends downward
with every device addition; screenshot and boot-event checkpoints match the
physical handset within documented timing/colour tolerances.

### 6. Keypad, storage, lifecycle, and Java

- Implement the measured keyboard matrix, three softkeys, D-pad/select, Send,
  End, digits, `*`, and `#`.
- Add the minimum MMC/USB and writable OneNAND behavior needed for suite install,
  RMS, and clean shutdown using copy-on-write storage.
- Exercise Nokia JVM discovery, JAD/JAR install, launch, text entry, background,
  resume, RMS corruption, and uninstall with TelegramMicro's exact release files.

Gate: scripted QMP/input flows reproduce physical key events; the Nokia JVM
installs and launches TelegramMicro; clean and interrupted RMS round trips have
bounded, repeatable outcomes. Label this `deep-emulator verified`, not device
proof.

### 7. Synthetic packet data and TelegramMicro workflow

- First boot and test in radio-off/no-SIM state.
- Locate the narrowest measured packet-data service boundary above RF/DSP and
  implement a `lab` backend to QEMU user networking. Do not emulate GSM/UMTS RF.
- Keep network transcripts bounded and secret-safe. Retain TelegramMicro's live
  fail-closed acknowledgement and explicit operator state.

Gate: offline DNS/TCP tests pass through the Nokia network stack, then an
explicitly authorized live Telegram login can be attempted. A lab-network pass
does not prove cellular interoperability, RF behavior, or real-device support.

### 8. Differential regression suite

- Turn sanitized boot, MMIO, key, display, RMS, and lifecycle transcripts into
  versioned fixtures with source/evidence labels.
- Run qtests for each device, deterministic boot checkpoints under TCG, QMP
  automation for UI flows, and record/replay only after nondeterministic sources
  are controlled.
- Compare SquirrelJME, QEMU Nokia JVM, and physical-device outcomes without
  treating agreement between two emulators as hardware proof.

Gate: CI builds without proprietary inputs; operator-supplied integration runs
produce a compact evidence report containing exact QEMU commit, firmware hashes,
mode, model coverage, unknown-MMIO count, screenshots, and unproved lanes.

## Initial backlog

1. Write a read-only BB5 envelope dumper with strict size and overlap checks.
2. Inventory all record tags and candidate placement fields in the three 11.40
   payloads; do not yet emit a flash image.
3. Obtain a second known-good RAP3Gv4 firmware set and compare container grammar.
4. Acquire an RM-721/RM-722 test handset and capture a redacted USB dead-flash
   transcript, including reported ASIC, ROM, flash, and XSR values.
5. Identify lawful BootROM acquisition and debugging options before writing the
   QEMU machine.
6. Prototype a QEMU `nokia-c2-01` skeleton that boots a free test ROM into RAM and
   proves reset/IRQ/MMIO observability; do not load Nokia code yet.
7. Audit the historical OneNAND model against the observed memory part and modern
   QEMU APIs.

## Principal risks

| Risk | Impact | Response |
| --- | --- | --- |
| Undocumented RAP3Gv4 register map and CPU details | Highest; execution may stop immediately | Hardware traces and fail-loud incremental modelling; never infer from another Nokia ASIC without a profile boundary. |
| Missing BootROM/OTP/per-device state | Secure chain may be impossible to reproduce from the public bundle alone | Operator-supplied lawful dump or clean-room behavioral model; synthetic identity; strict versus lab separation. |
| Integrated modem/DSP dependencies | Series 40 may not reach UI without service responses | Quiescent/offline mode first, then trace-backed mailbox stubs; no RF emulation in initial scope. |
| Proprietary container and firmware licensing | Limits CI and redistribution | Commit parsers, manifests, tests, and synthetic fixtures only; require local firmware by hash. |
| False confidence from permissive stubs | Emulator appears to boot while violating hardware | Unknown-MMIO budget, poison/fail mode, evidence ledger, differential physical traces. |
| Project scale | Full fidelity is a multi-stage reverse-engineering program | Define useful intermediate products: container inventory, boot-prefix runner, framebuffer boot, then JVM workflow. |

## Success definition

The first useful release is not “full C2-01 emulation.” It is a reproducible,
non-redistributing research harness that decodes the known containers, boots a
measured prefix under a dedicated QEMU machine, reports every unknown hardware
interaction, and can be extended without inventing behavior. The product goal is
reached only when the unmodified, exact Nokia 11.40 image reaches its JVM and
runs the exact TelegramMicro JAR through install, input, RMS, lifecycle, and
network workflows with limitations stated separately from real-device evidence.
