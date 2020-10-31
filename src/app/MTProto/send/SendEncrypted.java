package mtproto.send;

import support.ByteArrayPlus;
import mtproto.Serializer;
import mtproto.EncryptedRequest;
import mtproto.SendRequestThread;
import mtproto.MTProtoConnection;

public class SendEncrypted {
  public Serializer message_data = new Serializer();

  public void send(MTProtoConnection sender) {
    (new EncryptedRequest(message_data.end())).send(sender);
  }
}
