SquirrelJME Patch Set
==

This directory contains the TelegramMicro-specific SquirrelJME patch set used by the bootstrap scripts.

- Base commit: `14b4d02b42f8ee31fa40dcf07b8104f33c2d3b24`
- Patch file: `0001-squirreljme-mcp-and-headless-capture.patch`

What it changes:
- adds an embedded ScritchUI MCP server with control discovery and automation tools
- adds direct in-process screenshots for ScritchUI components and windows
- enables GTK2 and X11 native ScritchUI loading in the standalone build
- fixes a few CLDC/runtime issues that block TelegramMicro and its test harnesses
- implements enough of LCDUI `Form` and basic item rendering for TelegramMicro's startup status screen
- adds hosted Java SE `socket://` support and forwards ScritchUI/MCP launcher properties into hosted runs
- includes the small runtime safety fixes needed to keep hosted GTK/framebuffer runs alive

The scripts in [scripts/common.sh](/home/faissaloo/j2mewtk/2.5.2/apps/TelegramMicro/scripts/common.sh) pin SquirrelJME to the base commit and apply this patch idempotently before building the standalone jar.
