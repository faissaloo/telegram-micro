package mtproto.send;

import support.ByteArrayPlus;
import mtproto.Serializer;
import mtproto.EncryptedRequest;
import mtproto.SendRequestThread;
import mtproto.MTProtoConnection;

public class SendEncrypted {
  public Serializer message_data = new Serializer();
  //come up with a nice way to insert the MTProto header into these requests, maybe create a class that can be inherited from that contains it
  //or maybe have it set via a function in the MTProtoConnection and then get it and stick it at the beginning before the message is encrypted

  public void send(MTProtoConnection sender) {
    send(sender, false, false);
  }

  protected void send(MTProtoConnection sender, boolean trackRetry, boolean contentRelated) {
    send(sender, trackRetry, contentRelated, true);
  }

  protected void send(MTProtoConnection sender, boolean trackRetry, boolean contentRelated, boolean includeHeader) {
    sender.sendEncryptedPayload(message_data.end(), trackRetry, contentRelated, includeHeader);
  }
}
