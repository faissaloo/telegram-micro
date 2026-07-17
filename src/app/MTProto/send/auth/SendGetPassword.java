package mtproto.send.auth;

import mtproto.send.SendEncrypted;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;

public class SendGetPassword extends SendEncrypted {
  public SendGetPassword() {
    message_data.append_int(CombinatorIds.account_get_password);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
