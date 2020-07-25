package mtproto.send;
import support.ByteArrayPlus;
import mtproto.UnencryptedRequest;

public class SendUnencrypted {
  public ByteArrayPlus message_data;

  public void send() {
    (new UnencryptedRequest(message_data.toByteArray())).send();
  }
}
