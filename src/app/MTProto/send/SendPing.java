package mtproto.send;

import support.ByteArrayPlus;

import mtproto.CombinatorIds;
import mtproto.UnencryptedRequest;
import mtproto.Deserialize;

//https://core.telegram.org/mtproto/service_messages#ping-messages-pingpong
public class SendPing extends SendUnencrypted {
  public SendPing(long ping_id) {
    message_data 
      .append_int(CombinatorIds.ping)
      .append_long(ping_id);
  }
}
