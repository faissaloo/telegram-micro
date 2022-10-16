package mtproto.send.auth;

import mtproto.send.SendEncrypted;
import support.ByteArrayPlus;

import mtproto.CombinatorIds;
import mtproto.EncryptedRequest;
import mtproto.Deserialize;
import mtproto.MTProtoConnection;

public class SendSendCode extends SendEncrypted {
  public SendSendCode(String phone_number, int api_id, byte[] api_hash) {
    message_data
      .append_int(CombinatorIds.send_code)
      .append_byte_string(phone_number.getBytes())
      .append_int(api_id)
      .append_byte_string(api_hash) //api_hash, not sure what format this needs
      .append_int(CombinatorIds.code_settings)
      .append_int(0); //just set the flags to be empty for now
      //.append_vector_byte_strings(new byte[0][]); //logout tokens
  }
  
  public void send(MTProtoConnection sender) {
    super.send(sender);
    sender.seq_no += 1;
  }
}
