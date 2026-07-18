package mtproto.send.messages;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

/** Builds one bounded, unfiltered messages.search page for a single peer. */
public final class SendSearchMessages extends SendEncrypted {
  public static final int QUERY_MAX = 48;
  public static final int LIMIT_MAX = 6;

  public SendSearchMessages(SendGetDialogs.DialogInputPeer peer, String query, int limit) {
    this(peer, query, 0, limit);
  }

  public SendSearchMessages(
    SendGetDialogs.DialogInputPeer peer,
    String query,
    int offsetId,
    int limit
  ) {
    String trimmed = query != null ? query.trim() : "";

    if (peer == null) {
      throw new IllegalArgumentException("Search peer is required.");
    }
    if (trimmed.length() == 0 || trimmed.length() > QUERY_MAX) {
      throw new IllegalArgumentException("Search query must be 1-48 characters.");
    }
    if (offsetId < 0) {
      throw new IllegalArgumentException("Search offset must not be negative.");
    }
    if (limit <= 0 || limit > LIMIT_MAX) {
      throw new IllegalArgumentException("Search limit exceeds the bounded page.");
    }

    message_data
      .append_int(CombinatorIds.messages_search)
      .append_int(0)
      .append_raw_bytes(peer.serialize())
      .append_string(trimmed)
      .append_int(CombinatorIds.input_messages_filter_empty)
      .append_int(0)
      .append_int(Integer.MAX_VALUE)
      .append_int(offsetId)
      .append_int(0)
      .append_int(limit)
      .append_int(Integer.MAX_VALUE)
      .append_int(0)
      .append_long(0L);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
