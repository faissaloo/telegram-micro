# Release readiness and feature completeness

## Decision

TelegramMicro 0.1.0 is suitable for a public developer-preview release. It is not a stable, everyday Telegram client and is not yet device verified. The release exists to make the bounded J2ME implementation installable, reproducible, and easier to test on more runtimes and handsets without overstating interoperability.

## Feature-surface snapshot

The planning inventory currently lists 131 Telegram surfaces. Of those, 33 are labelled `foundation`, 12 are `partial`, 53 are explicitly absent, and 33 use more specific states such as parsed/skipped or metadata-only. Those counts are an inventory, not a weighted completeness score: reliable login, read, send, and recovery matter more than dozens of desktop-only features.

| Area | Current release state | Release interpretation |
| --- | --- | --- |
| Build and install | Reproducible JAR/JAD, pinned patched SquirrelJME, target artifact budget | Preview-ready; physical handset installation remains unproved. |
| Authentication | MTProto handshake, phone/code/password models, bounded SRP work, durable session, request timeout/reconnect recovery | Strong local foundation; the post-timeout recovery change has target-VM proof but not a completed live login trace. |
| Dialogs and history | Fixed-capacity dialogs, recent bounded history, timestamps, sender/media summaries, read watermarks | Useful foundation; constructor breadth and live/device behavior remain incomplete. |
| Text messaging | Bounded compose state, reply/forward/delete request paths, stable `random_id`, optimistic-row repair, reconnect reconciliation | Strong deterministic foundation; live radio-loss and real-device send proof remain outstanding. |
| Search and discovery | Bounded peer discovery and six-row in-chat server search | Useful narrow slice; paging and global search are absent. |
| Updates and recovery | State/difference foundations, PTS gap detection, bounded channel difference, warm restore | P0 foundation; needs long-session and radio-loss soak evidence. |
| Media | Metadata plus limited same-DC download planning/chunks | Partial; no general upload, cross-DC/CDN completion, thumbnail pipeline, or broad playback. |
| Secret chats | Request/accept/discard and bounded key-state foundations | Not encrypted-message support; sequence, rekey, TTL, and encrypted payload handling are absent. |
| Hosted usability | 240x320 keypad-first screens, character input fix, semantic softkey footer patch | Native/Java builds and static routing checks pass; the new footer still needs visual proof in a real graphical session. |
| Runtime profiles | Ideal plus named Nokia/Jbed/JBlend/J9/Jeode/Sony Ericsson identities | Profiles are identity-only until authoritative device probes justify quirks. |

## Important release limitations

- The downloadable JAR intentionally contains no Telegram API ID/hash. It will not send `auth.sendCode`; operators must rebuild locally with their own credentials.
- No physical Nokia or other feature phone has completed the documented release checklist with this exact artifact.
- The latest authentication timeout/reconnect fix is target-VM verified, and the separate host crypto vector gate passes, but a complete production-account login was not rerun for this release.
- Hosted softkey placement compiles and is checked semantically, but the release environment had no real X display for a fresh visual capture. Existing README screenshots show the direct application surface and predate that host-chrome change.
- General media transfer, cross-DC authorization, many current Telegram constructors, notifications, moderation/settings breadth, calls, stories, mini apps, payments, multi-account use, and encrypted secret messages are absent or deliberately deferred.
- Vendor runtime profiles do not yet emulate folklore-based quirks. They remain identity-only until documented probes and authoritative evidence exist.

## Evidence required before beta

1. Complete fresh and restored live login, dialog load, history, send, acknowledgement, reconnect, and logout flows without exposing account material.
2. Run the exact artifact through the Nokia checklist, including RMS pressure, radio loss, interruption, low-memory behavior, and keypad usability.
3. Capture the hosted semantic softkey footer from a real X display and exercise left, right, and Options actions.
4. Add a bounded long-session soak and a larger constructor/error fixture corpus.
5. Prove DC migration/export-import and clarify the supported media subset.

The detailed disposition remains in [telegram-feature-matrix.md](telegram-feature-matrix.md); the ordered engineering backlog remains in [parity-roadmap.md](parity-roadmap.md).
