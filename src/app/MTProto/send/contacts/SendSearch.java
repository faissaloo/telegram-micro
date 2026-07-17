package mtproto.send.contacts;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

public final class SendSearch extends SendEncrypted {
  public SendSearch(String query, int limit) {
    String trimmed = query != null ? query.trim() : "";

    if (trimmed.length() == 0) {
      throw new IllegalArgumentException("Search query is required.");
    }
    if (limit <= 0) {
      throw new IllegalArgumentException("Search limit must be positive.");
    }

    message_data
      .append_int(CombinatorIds.contacts_search)
      .append_string(trimmed)
      .append_int(limit);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
