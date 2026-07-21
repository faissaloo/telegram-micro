SquirrelJME Patch Set
==

This directory contains the TelegramMicro-specific SquirrelJME patch set used by the bootstrap scripts.

- Base commit: `14b4d02b42f8ee31fa40dcf07b8104f33c2d3b24`
- Patches:
  - `0001-squirreljme-mcp-and-direct-surface-capture.patch`
  - `0002-host-gcf-and-runtime-profiles.patch`

What it changes:
- adds an embedded ScritchUI MCP server with control discovery and automation tools
- adds direct in-process screenshots for ScritchUI components and windows
- enables GTK2 native ScritchUI loading in the standalone build while leaving the unfinished X11 driver disabled
- fixes a few CLDC/runtime issues that block TelegramMicro and its test harnesses
- implements enough of LCDUI `Form` and basic item rendering for TelegramMicro's startup status screen
- forwards ScritchUI/MCP launcher properties into hosted runs
- includes the small runtime safety fixes needed to keep hosted GTK/framebuffer runs alive

The second patch:

- adds the previously missing hosted Java SE IP provider for outbound `socket://host:port` connections
- enforces MIDP connection modes, one-shot streams, duplex half-close, and open-stream lifetime after `Connection.close()`
- maps the MIDP TCP socket options to the host socket implementation
- adds a validated, default-ideal runtime-profile namespace for Nokia KVM/JRT, Jbed, JBlend, J9, Jeode, and Sony Ericsson Java Platform investigation

The vendor profiles are identity-only until a focused probe and authoritative runtime/device evidence justify a behavioral difference. Hosted TCP proof does not establish SpringCoat-native networking, TLS, Telegram interoperability, or real-device behavior.

The scripts in [`scripts/common.sh`](../../scripts/common.sh) pin SquirrelJME to the base commit, apply the ordered patch stack, and record both patch-content and touched-file-state fingerprints before building the standalone JAR.
