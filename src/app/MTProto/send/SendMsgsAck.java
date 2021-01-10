package mtproto.send;

import support.ByteArrayPlus;

import mtproto.CombinatorIds;
import mtproto.EncryptedRequest;
import mtproto.Deserialize;
import mtproto.MTProtoConnection;

//https://core.telegram.org/mtproto/service_messages#ping-messages-pingpong
public class SendMsgsAck extends SendEncrypted {
  public SendMsgsAck(long[] msg_ids) {
    message_data
      .append_int(CombinatorIds.ping)
      .append_vector_long(msg_ids);
  }
  
  public void send(MTProtoConnection sender) {
    super.send(sender);
    sender.seq_no += 1;
  }
}
