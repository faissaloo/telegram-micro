package mtproto.send.messages;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

public final class SendSendEncryptedService extends SendEncrypted {
  public SendSendEncryptedService(int chatId, long accessHash, long randomId, byte[] data) {
    if (chatId == 0) {
      throw new IllegalArgumentException("Secret chat id is required.");
    }
    if (accessHash == 0) {
      throw new IllegalArgumentException("Secret chat access hash is required.");
    }
    if (randomId == 0) {
      throw new IllegalArgumentException("Secret chat random id is required.");
    }
    if (data == null || data.length == 0) {
      throw new IllegalArgumentException("Secret service payload is required.");
    }

    message_data
      .append_int(CombinatorIds.messages_send_encrypted_service)
      .append_int(CombinatorIds.input_encrypted_chat)
      .append_int(chatId)
      .append_long(accessHash)
      .append_long(randomId)
      .append_byte_string(data);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
