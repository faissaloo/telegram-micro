package mtproto.send.messages;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

public class SendGetHistory extends SendEncrypted {
  public SendGetHistory(SendGetDialogs.DialogInputPeer peer, int limit) {
    this(peer, 0, 0, 0, limit, 0, 0, 0L);
  }

  public SendGetHistory(
    SendGetDialogs.DialogInputPeer peer,
    int offsetId,
    int offsetDate,
    int addOffset,
    int limit,
    int maxId,
    int minId,
    long hash
  ) {
    if (peer == null) {
      throw new IllegalArgumentException("History peer is required.");
    }

    message_data
      .append_int(CombinatorIds.messages_get_history)
      .append_raw_bytes(peer.serialize())
      .append_int(offsetId)
      .append_int(offsetDate)
      .append_int(addOffset)
      .append_int(limit)
      .append_int(maxId)
      .append_int(minId)
      .append_long(hash);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
