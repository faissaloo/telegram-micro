package mtproto.send.messages;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

public final class SendAcceptEncryption extends SendEncrypted {
  public SendAcceptEncryption(int chatId, long accessHash, byte[] gb, long keyFingerprint) {
    if (chatId == 0) {
      throw new IllegalArgumentException("Secret chat id is required.");
    }
    if (accessHash == 0) {
      throw new IllegalArgumentException("Secret chat access hash is required.");
    }
    if (gb == null || gb.length == 0) {
      throw new IllegalArgumentException("Secret chat g_b bytes are required.");
    }

    message_data
      .append_int(CombinatorIds.messages_accept_encryption)
      .append_int(CombinatorIds.input_encrypted_chat)
      .append_int(chatId)
      .append_long(accessHash)
      .append_byte_string(gb)
      .append_long(keyFingerprint);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
