package mtproto.send.messages;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

public final class SendDiscardEncryption extends SendEncrypted {
  public SendDiscardEncryption(int chatId, boolean deleteHistory) {
    if (chatId == 0) {
      throw new IllegalArgumentException("Secret chat id is required.");
    }

    message_data
      .append_int(CombinatorIds.messages_discard_encryption)
      .append_int(deleteHistory ? 1 : 0)
      .append_int(chatId);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
