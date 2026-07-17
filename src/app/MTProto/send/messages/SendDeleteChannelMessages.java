package mtproto.send.messages;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

public final class SendDeleteChannelMessages extends SendEncrypted {
  public SendDeleteChannelMessages(long channelId, long accessHash, int messageId) {
    if (channelId == 0) {
      throw new IllegalArgumentException("Channel id is required.");
    }
    if (accessHash == 0) {
      throw new IllegalArgumentException("Channel access hash is required.");
    }
    if (messageId == 0) {
      throw new IllegalArgumentException("Message id is required.");
    }

    message_data
      .append_int(CombinatorIds.channels_delete_messages)
      .append_int(CombinatorIds.input_channel)
      .append_long(channelId)
      .append_long(accessHash)
      .append_int(CombinatorIds.vector)
      .append_int(1)
      .append_int(messageId);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
