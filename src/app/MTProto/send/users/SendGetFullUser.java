package mtproto.send.users;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

public final class SendGetFullUser extends SendEncrypted {
  public SendGetFullUser(long userId, long accessHash) {
    if (userId == 0) {
      throw new IllegalArgumentException("User id is required.");
    }
    if (accessHash == 0) {
      throw new IllegalArgumentException("User access hash is required.");
    }

    message_data
      .append_int(CombinatorIds.users_get_full_user)
      .append_int(CombinatorIds.input_user)
      .append_long(userId)
      .append_long(accessHash);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
