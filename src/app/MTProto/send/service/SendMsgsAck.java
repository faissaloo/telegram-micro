package mtproto.send.service;

import mtproto.send.SendEncrypted;
import support.ByteArrayPlus;

import mtproto.CombinatorIds;
import mtproto.EncryptedRequest;
import mtproto.Deserialize;
import mtproto.MTProtoConnection;

public class SendMsgsAck extends SendEncrypted {
  public SendMsgsAck(long[] msg_ids) {
    message_data
      .append_int(CombinatorIds.msgs_ack)
      .append_vector_long(msg_ids);
  }
  
  public void send(MTProtoConnection sender) {
    super.send(sender);
    sender.seq_no += 1;
  }
}
