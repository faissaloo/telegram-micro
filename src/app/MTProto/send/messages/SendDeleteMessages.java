package mtproto.send.messages;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

public final class SendDeleteMessages extends SendEncrypted {
  public SendDeleteMessages(int messageId, boolean revoke) {
    if (messageId == 0) {
      throw new IllegalArgumentException("Message id is required.");
    }

    message_data
      .append_int(CombinatorIds.messages_delete_messages)
      .append_int(revoke ? 1 : 0)
      .append_int(CombinatorIds.vector)
      .append_int(1)
      .append_int(messageId);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
