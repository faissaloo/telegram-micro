package mtproto.send;
import support.ByteArrayPlus;
import mtproto.Serializer;
import mtproto.UnencryptedRequest;
import mtproto.SendRequestThread;

public class SendUnencrypted {
  public Serializer message_data = new Serializer();

  public void send(SendRequestThread sender) {
    (new UnencryptedRequest(message_data.end())).send(sender);
  }
}
