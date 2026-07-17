package mtproto.send.auth;

import mtproto.send.SendEncrypted;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;

public class SendSignIn extends SendEncrypted {
  public SendSignIn(String phone_number, String phone_code_hash, String phone_code) {
    message_data
      .append_int(CombinatorIds.sign_in)
      .append_int(1)
      .append_string(phone_number)
      .append_string(phone_code_hash)
      .append_string(phone_code);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
