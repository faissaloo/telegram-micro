# Keypad-first UX guidance

TelegramMicro targets a 240x320 non-touch phone with directional keys, numeric keys, and softkeys. Desktop Telegram density and touch-first navigation are not appropriate defaults.

These rules translate Nielsen Norman Group guidance on [ten usability heuristics](https://www.nngroup.com/articles/ten-usability-heuristics/), [progressive disclosure](https://www.nngroup.com/articles/progressive-disclosure/), [system-status visibility](https://www.nngroup.com/articles/visibility-system-status/), and [list-entry information hierarchy](https://www.nngroup.com/articles/list-entries/) into repository-specific constraints.

## Primary interaction model

- Make `Dialogs -> History -> Compose` the shortest path. Discovery, peer details, media actions, diagnostics, and account controls remain secondary screens.
- Keep Back/Cancel available from every non-terminal interaction. Preserve the draft or pending outcome when leaving a network operation.
- Use the directional pad for selection and paging. Numeric shortcuts may accelerate expert use, but the visible command menu or footer must expose the same action.
- Keep command names stable across screens: `Open`, `Back`/`Dialogs`, `Compose`, `Reply`, `Refresh`, `Send`, and `Cancel` must not change meaning.
- Restore focus and scroll context after returning from a secondary screen whenever the bounded model still contains the item.

## Visibility of system status

- Show immediate local feedback after every accepted key action that starts work.
- Distinguish `pending locally`, `sent to Telegram`, `read`, `retrying`, `offline`, and `failed`; never collapse them into one ambiguous state.
- For operations longer than a perceptible moment, show a short verb-led state such as `Loading history`, `Sending`, or `Reconnecting`.
- Preserve a failed item/draft and provide a constructive next action. Do not silently remove messages or state after an error.
- Keep protocol codes and internal constructor names on the Status/diagnostic surface; primary error copy uses plain language.

## Recognition over recall

- Keep the selected peer title visible in history and compose.
- Show message sender, text/media summary, timestamp, and delivery state together when available.
- Label time zones when device-local conversion is not trustworthy.
- Show the current selection count/index where a long list cannot reveal position spatially.
- Put short keypad hints in the footer, but never require users to memorize a hidden key map.
- Keep reply/forward context visible until send or explicit cancellation.

## Dialog-list information hierarchy

Treat each row as a consistent miniature information architecture:

1. Peer title is primary and occupies the top/left emphasis position.
2. Last-message preview is secondary and truncated predictably.
3. Unread count and recent time/status are compact differentiators.
4. Media or error markers appear only when they change the decision to open the row.

Use the same placement and styling for every row. Avoid avatars until measured device evidence shows that a tiny bounded cache improves recognition more than it costs in heap, decode time, and visible text density.

## History information hierarchy

1. Sender/context identifies who produced the message.
2. Text is the primary content and wraps within a fixed bubble width.
3. Media is a compact semantic label, not an implicit download.
4. Timestamp and delivery state share one short metadata line.
5. Reply, forward, delete, peer, and media actions use progressive disclosure through commands.

Do not animate delivery indicators, stickers, emoji, or loading decoration. Preserve CPU and repaint time for navigation feedback.

## In-chat search

- Label the chat scope, query, and retained/total result count; users should never have to infer whether a search is local, global, or still running.
- Fetch only after the explicit `Search` action. Keep one small server page separate from cached history; do not build a resident local index.
- Keep sender, compact preview/media label, and timestamp in every result row. Open longer detail through progressive disclosure.
- Make Back from the result page restore the current chat and its retained selection. Search results must not imply that history was viewed or mark messages read.
- Distinguish `no matches` from `request failed`, retain the query after failure, and offer `Edit` and `Refresh` as constructive recovery actions.

## Error prevention and recovery

- Omit or disable actions that cannot succeed with the known peer, rights, access hash, media DC, or message flags.
- Confirm destructive/account-wide actions and state whether the effect is local or remote.
- Validate bounded input before network, allocation, or durable mutation.
- Prevent duplicate send through visible in-flight state and idempotent random IDs; never ask a user to guess whether a send happened.
- Keep retry/cancel paths explicit. Retrying must reuse or deliberately replace the operation identity according to protocol semantics.
- On storage pressure, sacrifice optional cache/media before authorization or an unsent draft.

## Progressive disclosure

The first screen or command set exposes frequent core actions. Secondary screens hold:

- peer biography, membership, and secret-chat lifecycle;
- media download planning and storage details;
- sync, failure, memory, and protocol diagnostics;
- infrequent account and privacy controls;
- admin, topic, and commercial features if they are ever added.

Secondary navigation must have a clear label with strong information scent. `Status`, `Peer`, and `Media` are preferable to a generic `More` when command capacity permits.

## Resource-aware usability

- Prefer fixed rows, reusable layout arrays, and direct `Graphics` drawing over rebuilding many LCDUI objects.
- Do not allocate strings, dates, collections, or images inside paint loops when a reusable buffer or precomputed scalar works.
- Fetch only after explicit user action unless the data is required for the current text screen.
- Never background-prefetch media. Show byte/dimension limits before beginning expensive work.
- Keep working-state and failure-state text short enough to remain legible at 176- and 240-pixel widths.
- Treat a predictable bounded refusal as better UX than an optimistic action that risks OOM or corrupt state.

## Heuristic review checklist

For every new visible feature, verify:

1. What does the user see immediately after invoking it?
2. Can they leave or cancel without losing unrelated state?
3. Are terms ordinary Telegram/user language rather than MTProto jargon?
4. Is the primary action visible without remembering a key?
5. Are dangerous or impossible states prevented before commitment?
6. Does the layout emphasize only the information needed for the current decision?
7. Does an error explain what happened and what the user can do next?
8. Does the same action behave consistently on dialogs, history, compose, and secondary screens?
9. What is the exact heap, storage, wire, repaint, and interaction bound?
10. Which evidence lane proves the result, and which remains unproved?

## Physical-device usability tasks

The final authority is the device checklist, supplemented by timed task observation:

- find and open a recent unread conversation;
- identify when and by whom the latest message was sent;
- type, correct, send, and confirm a short message;
- cancel a reply/forward without losing the ordinary draft;
- recover from a failed send and a radio interruption;
- understand a media refusal without opening diagnostics;
- return to the prior dialog and selection without hunting.

Record mis-keying, backtracking, uncertainty, clipped text, and task time against the exact artifact hash. Emulator aesthetics alone cannot validate keypad ergonomics.
