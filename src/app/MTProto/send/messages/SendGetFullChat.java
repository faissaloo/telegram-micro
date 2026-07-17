package mtproto.send.messages;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

public final class SendGetFullChat extends SendEncrypted {
  public SendGetFullChat(long chatId) {
    if (chatId == 0) {
      throw new IllegalArgumentException("Chat id is required.");
    }

    message_data
      .append_int(CombinatorIds.messages_get_full_chat)
      .append_long(chatId);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
