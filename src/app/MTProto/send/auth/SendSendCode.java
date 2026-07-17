package mtproto.send.auth;

import mtproto.send.SendEncrypted;
import support.ByteArrayPlus;

import mtproto.CombinatorIds;
import mtproto.EncryptedRequest;
import mtproto.Deserialize;
import mtproto.MTProtoConnection;

public class SendSendCode extends SendEncrypted {
  public SendSendCode(String phone_number, int api_id, String api_hash) {
    message_data
      .append_int(CombinatorIds.send_code)
      .append_string(phone_number)
      .append_int(api_id)
      .append_string(api_hash)
      .append_int(CombinatorIds.code_settings)
      .append_int(0);
  }
  
  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
