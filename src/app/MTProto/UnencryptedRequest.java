package mtproto;

import java.io.IOException;

import support.Encode;
import support.ByteArrayPlus;

public class UnencryptedRequest {
  ByteArrayPlus message_data;

  public UnencryptedRequest(byte[] data) {
    message_data = new ByteArrayPlus();
    message_data.append_long(0L);
    message_data.append_long(message_id());
    message_data.append_int(data.length);
    message_data.append_raw_bytes(data);
  }

  private long message_id() {
    //https://core.telegram.org/mtproto/description#message-identifier-msg-id
    return (System.currentTimeMillis()/1000L)<<32;
  }

  public void send(MTProtoConnection sender) {
    (new TCPRequest(message_data.toByteArray())).send(sender);
  }
}
