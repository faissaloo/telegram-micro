package mtproto.send;

import support.ByteArrayPlus;

import mtproto.CombinatorIds;
import mtproto.UnencryptedRequest;
import mtproto.Deserialize;

//https://core.telegram.org/mtproto/service_messages#ping-messages-pingpong
public class SendPing {
  ByteArrayPlus message_data;

  public SendPing(long ping_id) {
    message_data = new ByteArrayPlus();
    message_data.append_int(CombinatorIds.ping); //combinator_id
    message_data.append_long(ping_id);
  }
  
  public void send() {
    (new UnencryptedRequest(message_data.toByteArray())).send();
  }
}
