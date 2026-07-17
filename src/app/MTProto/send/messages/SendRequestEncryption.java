package mtproto.send.messages;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

public final class SendRequestEncryption extends SendEncrypted {
  public SendRequestEncryption(long userId, long accessHash, int randomId, byte[] ga) {
    if (userId == 0) {
      throw new IllegalArgumentException("User id is required.");
    }
    if (accessHash == 0) {
      throw new IllegalArgumentException("User access hash is required.");
    }
    if (randomId == 0) {
      throw new IllegalArgumentException("Secret chat random id is required.");
    }
    if (ga == null || ga.length == 0) {
      throw new IllegalArgumentException("Secret chat g_a bytes are required.");
    }

    message_data
      .append_int(CombinatorIds.messages_request_encryption)
      .append_int(CombinatorIds.input_user)
      .append_long(userId)
      .append_long(accessHash)
      .append_int(randomId)
      .append_byte_string(ga);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
