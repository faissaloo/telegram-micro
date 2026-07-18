# Telegram feature surface and J2ME disposition

This document is an extensive planning inventory, not a parity claim. It separates Telegram's broad product surface from the subset that can be trustworthy on a Nokia C2-01-class J2ME phone.

The inventory was reviewed on 2026-07-17 against Telegram's official [API feature index](https://core.telegram.org/api), [method index](https://core.telegram.org/methods), [FAQ](https://telegram.org/faq), and [feature tour](https://telegram.org/tour). Telegram evolves continuously, so protocol work must still use the pinned local schemas and references for the layer this client declares.

## Labels

- `foundation`: production code exists and deterministic tests cover local invariants; this does not imply live interoperability.
- `partial`: a deliberately limited subset exists.
- `next`: high-value work that fits the current device budget.
- `later`: useful after P0 reliability and device evidence improve.
- `metadata`: parse and show a compact summary; do not attempt the full rich experience.
- `defer`: cost or risk is disproportionate until the core is device-proven.
- `platform-excluded`: the Nokia/J2ME platform cannot provide a credible implementation with the current runtime APIs.

Evidence remains independent: unit, target-VM, offline UI, live Telegram, and physical-device proof must be reported separately.

## Prioritization rule

Prefer work with high everyday frequency, recovery or compliance value, existing wire data, and deterministic proofability. Penalize resident heap, transient allocation, radio bytes, CPU/crypto cost, interaction depth, and schema churn. A feature that destabilizes login, recent history, text send, or recovery moves down regardless of desktop popularity.

## Authorization, account, and session

| Telegram surface | Current client state | J2ME disposition |
| --- | --- | --- |
| App API credentials and layer declaration | `foundation` | Keep generated credentials out of source and pin the declared layer. |
| Phone-number login | `foundation` | P0; retain retryable, keypad-safe input and explicit errors. |
| Login code delivery and entry | `foundation` | P0; preserve expiry/retry state across interruption. |
| Two-step-verification password | `foundation` | P0; retain bounded SRP/PBKDF work and transcript checks. |
| New-user sign-up | `partial` response classification only | `later`; requires names, terms, expiry, and error UX. |
| QR login | absent | `defer`; poor fit without a camera/scanner flow and second-device assumptions. |
| Passkeys/biometrics | absent | `platform-excluded` on MIDP 2.1. |
| DC discovery/config refresh | `foundation` | P0; refresh conservatively on `updateConfig`. |
| DC migration and auth export/import | `partial` | `next`; one bounded connection at a time, with rollback-safe session state. |
| Session restore | `foundation` | P0; continue corrupt/full-RMS and power-loss hardening. |
| Log out / start another session | `partial` reset path | `next`; explicit confirmation and clear scope of local deletion. |
| Active-session/device management | absent | `later`; list only a bounded recent page and confirm revocation. |
| Change phone number | absent | `defer`; high account-risk workflow needs live/device proof. |
| Account self-delete/delete | absent | `defer`; destructive and poor fit for an early constrained client. |
| Privacy/security settings | absent | `later`; begin with read-only summaries, then bounded common toggles. |
| Terms-of-service updates | absent | `next` when encountered; blocking account notices must not be silently ignored. |

## Core conversations and messages

| Telegram surface | Current client state | J2ME disposition |
| --- | --- | --- |
| Bounded dialog list | `foundation` | P0; keep title, preview, unread count, and compact temporal state. |
| Private chats | `foundation` | P0 live/device proof remains. |
| Basic groups | `foundation` parser/model path | P0 interoperability hardening. |
| Supergroups | `partial` through channel peers | P0/P1 constructor and update hardening. |
| Broadcast channels | `foundation` read/join/leave paths | P0/P1; channel compliance features remain. |
| Recent history pagination | `foundation` bounded window | P0; add explicit older/newer slices without unbounded retention. |
| Message timestamps | `foundation` | Server epoch seconds survive history/live/send/warm-state paths and render as allocation-free `HH:MMZ`; local-zone/device proof remains. |
| Text send | `foundation` | One bounded compose job now keeps peer, text/reply/forward context, and its exact nonzero `random_id` immutable until acknowledgement; reconnect waits for dialog/sync reconciliation and retries idempotently. Fault-injected transport, live, and device proof remain. |
| Pending/sent state | `foundation` | Cache refresh/restart recreates at most one optimistic row; explicit failure retains the draft and unrelated navigation is not hijacked. Live/device proof remains. |
| Delivery/read state | `foundation` | Two monotonic integers per retained dialog (96 fixed bytes at capacity 12) drive compact `sent`/`read` labels; live/device semantics remain unproven. |
| Mark history read | `foundation` | Viewed bounded history emits serialized peer-appropriate read requests with optimistic rollback; PTS acknowledgements detect gaps before catch-up, while live/device proof remains. |
| Replies | `foundation` outbound context | P0/P1; render a bounded reply preview in history later. |
| Forward messages | `foundation` | P0/P1; honor protected-content failures explicitly. |
| Delete messages | `foundation` | P0/P1; add destructive confirmation and revoke-scope clarity. |
| Edit sent messages | live edits parsed; outbound edit absent | `later`; bounded text-only edit with preserved original timestamp. |
| Local compose draft | `foundation` | Draft and reply/forward context survive interruption; a single in-flight job temporarily locks mutation to avoid duplicating the payload in Nokia-class RAM. |
| Server-synced drafts | parsed/ignored | `later`; one bounded draft per retained dialog. |
| Message search within a chat | `foundation` | Explicit `messages.search` only: query is capped at 48 characters, one server page is capped at 6 pooled rows, results remain ephemeral and separate from cached history, failure differs from zero matches, and Back restores the chat. Pagination/context fetch, live interoperability, and device proof remain. |
| Global message search | absent | `later`; bounded server result page and clear scope label. |
| Peer discovery/search | `foundation` | P1; distinguish cached vs server results. |
| Styled-text entities | `partial` parsing/skipping | `later`; render only safe high-value styles/links without layout churn. |
| Mentions, hashtags, commands, URLs | `partial` text/link metadata | `later`; offer secondary actions only when recognized. |
| Message links/deep links | absent | `defer`; MIDP platform registration is inconsistent. |
| Pinned messages | ignored | `later`; compact banner plus bounded navigation. |
| Scheduled messages | absent | `defer`; requires another history surface and scheduling UX. |
| Silent send / send as | absent | `defer`; secondary options after ordinary send is proven. |
| Message effects | absent | `metadata`; never animate on the target. |
| Protected/no-forward content | parser flag not modeled | `next`; disable forward/save and explain why. |
| Saved Messages | may appear as an ordinary dialog | `later`; label self-chat and keep normal bounded behavior. |
| Imported messages/takeout/export | absent | `defer`; desktop-class storage workflow. |
| Fact checks | skipped | `metadata`; show a short marker and bounded text when safely parsed. |

## Groups, channels, forums, and moderation

| Telegram surface | Current client state | J2ME disposition |
| --- | --- | --- |
| Join/leave public channels | `foundation` | P1 live/device proof and confirmation copy. |
| Full user/chat/channel info | `foundation` bounded summaries | P1; keep details on a secondary screen. |
| Create groups/channels | absent | `defer`; admin complexity is not core reading/sending. |
| Member lists | participant count only | `later`; bounded page, no full roster cache. |
| Add/remove members | absent | `defer`; rights/error matrix is broad. |
| Invite links | absent | `later`; open/join one link with explicit destination and confirmation. |
| Join requests | absent | `defer`; admin workflow. |
| Admin/default/banned rights | skipped | `metadata` first; use rights only to prevent invalid actions. |
| Admin log | absent | `defer`; large specialized event surface. |
| Forum topics | payloads skipped | `later`; bounded topic list before topic history. |
| Message threads/channel comments | absent | `later`; bounded thread navigation and visible parent context. |
| Mentions/replies inbox | absent | `later`; bounded filtered server page. |
| Channel statistics | absent | `defer`; graph-heavy and admin-only. |
| Direct messages to channels | absent | `defer`; evolving paid/suggested-post semantics. |
| Sponsored channel messages | absent | `next` compliance investigation before claiming channel completeness. |
| Boosts, giveaways, revenue | absent | `defer`; commercial/admin surface. |

## Media and rich content

| Telegram surface | Current client state | J2ME disposition |
| --- | --- | --- |
| Media metadata in history | `foundation` for selected photo/document/web metadata | P1; unknown shapes fail boundedly. |
| Same-DC file download | `partial` bounded chunks/queue | `next`; stream to bounded durable storage where possible. |
| Cross-DC file download | explicit unsupported state | `next` after auth export/import; one media DC at a time. |
| CDN download/redirect | explicit unsupported state | `later`; hash verification and bounded reupload-token handling required. |
| Photo thumbnails | metadata only | `later`; decode only declared small dimensions/bytes, never prefetch. |
| Full photos | absent | `defer`; explicit user action and durable streaming only. |
| Generic files/documents | metadata + download planning | `later`; bounded save path, progress, cancel, checksum/error handling. |
| Upload attachments | absent | `defer`; chunking, retry, file picker, and storage pressure first. |
| Voice messages/audio | metadata only | `later` if MMAPI codec/device tests pass; never transcode. |
| Video/video notes | metadata only | `defer`; device codec, heap, and radio cost are high. |
| GIFs/animations | metadata only | `metadata`; static label or first frame at most. |
| Static stickers | document metadata | `later`; tiny raster budget only. |
| Animated/video stickers | metadata only | `platform-excluded` for credible playback. |
| Standard emoji | device-font dependent | `partial`; preserve text even if glyphs are missing. |
| Custom/animated emoji | metadata/skipped | `metadata`; show fallback text/marker. |
| Contact cards | skipped | `later`; compact name/phone prompt with confirmation before saving. |
| Locations/live locations | skipped | `metadata` first; maps/live tracking are `defer`. |
| Polls/quizzes | skipped | `later`; text-only question/options with bounded counts. |
| Collaborative checklists | skipped | `defer`; new Premium surface with broad mutation semantics. |
| Link previews/web pages | `partial` compact metadata | `later`; no browser engine or cached Instant View tree. |
| Instant View | parser can skip rich pages | `defer`; tree/layout cost is disproportionate. |
| Translation/transcription/AI summaries | absent | `defer`; server/Premium surface and unsuitable primary priority. |

## Contacts, identity, discovery, and organization

| Telegram surface | Current client state | J2ME disposition |
| --- | --- | --- |
| Username/public peer search | `foundation` | P1; bounded result count and explicit server state. |
| Cached peer directory | `foundation` | P0/P1; fixed capacity and access-hash correctness. |
| Address-book sync/import | absent | `defer`; JSR-75/PIM availability and privacy vary by handset. |
| Add/delete Telegram contact | absent | `later`; explicit confirmation and phone visibility rules. |
| Usernames and collectible usernames | primary username only | `metadata`; one display name/username is enough. |
| Profile/about | `foundation` bounded summary | P1. |
| Profile photos/avatars | skipped | `later`; tiny on-demand thumbnail only. |
| Presence/last seen/online | parser skip | `later`; one compact status with privacy-aware wording. |
| Blocklist/block/unblock | absent | `later`; high-value safety action with confirmation. |
| Granular privacy controls | absent | `defer`; large rule editor. |
| Similar channels/bots | absent | `defer`; discovery is secondary. |
| Chat folders/filters | absent | `later`; a small server-backed folder list, not duplicated caches. |
| Top peers/frequent contacts | absent | `defer`; avoid ranking/background state until measured. |
| Nearby people | absent | `platform-excluded` without reliable location APIs and privacy UX. |

## Updates, notifications, and reliability

| Telegram surface | Current client state | J2ME disposition |
| --- | --- | --- |
| `updates.getState/getDifference` | `foundation` | P0; read/send/delete acknowledgements now preserve PTS gaps for catch-up; expand the fixture corpus. |
| Channel difference | `foundation` | P0/P1; channel mutation acknowledgements preserve PTS gaps; continue bounded slice and too-long fallback hardening. |
| Live new/edit/delete updates | `partial` | P0/P1; preserve temporal/read state and unknown-update recovery. |
| Reconnect/session resume | `foundation` | Lost volatile RPC slots are cleared; a live reconnect requests the difference from its last applied update checkpoint before resend, while a cold restore without that volatile checkpoint falls back to the same durable `random_id`. Dialog/history and update callbacks settle before the outbound job resumes. Radio-loss/device soak remains. |
| Message-ID reconciliation | `foundation` | Matching `updateMessageID` can complete the durable job during catch-up before resend, or after retry, using the original peer plus `random_id`; live trace proof remains. |
| Background push notifications | absent | `platform-excluded` as a reliable always-on service on this target. |
| Foreground polling | `partial` update connection | P0; throttle and make radio state visible. |
| Local alerts/vibration/sound | absent | `later`; obey profiles and avoid background assumptions. |
| Per-chat mute/notification settings | parsed/skipped | `later`; common bounded toggles only. |
| Typing/upload actions | absent | `defer`; ephemeral traffic and UI churn. |
| Long-session bounded-state soak | absent | `next`; assert no capacity or retained-growth drift. |
| Multiple simultaneous DC connections | absent by design | Keep excluded; serialize migrations/media work to bound heap. |

## Secret chats and calls

| Telegram surface | Current client state | J2ME disposition |
| --- | --- | --- |
| Secret-chat request/accept/discard lifecycle | `foundation` | Do not label secret messaging supported. |
| Secret-chat key material/fingerprint | `partial` bounded persistence | Continue hostile DH/key-state proof. |
| Encrypted secret messages | absent | `defer` until P0 cloud messaging is live/device proven. |
| Secret-chat sequence/PFS/rekey | absent | `defer`; mandatory before encrypted-message claims. |
| TTL/self-destruct/screenshot events | parser action summaries only | `defer`; deletion timing and device lifecycle are safety-critical. |
| One-to-one voice/video calls | absent | `platform-excluded` for a credible low-latency encrypted stack. |
| Group calls/live streams | absent | `platform-excluded`. |

## Bots, stories, commercial, and platform ecosystems

| Telegram surface | Current client state | J2ME disposition |
| --- | --- | --- |
| Bots as ordinary text peers | `partial` | Keep text fallback; clearly expose unsupported interaction controls. |
| Bot commands | plain text only | `later`; compact command list and text insertion. |
| Reply/inline keyboards | explicitly unsupported | `later` for bounded URL/callback/text buttons only. |
| Inline queries | absent | `defer`; complex result/media surface. |
| Mini apps, games, web events | absent | `platform-excluded` without an HTML/JS runtime. |
| Payments/Telegram Passport | absent | `platform-excluded` for a safe credential/payment UI on this client. |
| Stories | absent | `defer`; media-first, ephemeral, and high-bandwidth. |
| Premium features | absent | `metadata`; explain server errors without upsell-heavy UI. |
| Telegram Business | absent | `defer`; secondary account-management surface. |
| Stars, subscriptions, gifts, paid media/messages | absent | `defer`; evolving commercial semantics and payment risk. |
| AI compose/translation/summaries | absent | `defer`; not part of the trustworthy core. |
| Themes/wallpapers/accent colors | fixed local theme | `later`; one high-contrast alternative before remote themes. |
| Multi-account | absent | `defer`; doubles durable secrets and resident state. |

## Current ordered implementation queue

1. Model protected-content restrictions before offering forward/save actions.
2. Add bounded DC auth export/import, then cross-DC media download.
3. Add a small constructor/fixture corpus and long-session capacity soak.
4. Investigate sponsored-message requirements before stronger channel claims.
5. Add explicit older search/history pages only after interaction and heap measurements confirm a safe bounded replacement policy.

Reorder this queue only when local references, a live interoperability failure, device evidence, or user research demonstrates higher core value.
