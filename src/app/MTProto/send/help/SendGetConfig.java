package mtproto.send.help;

import mtproto.send.SendEncrypted;
import support.ByteArrayPlus;

import mtproto.CombinatorIds;
import mtproto.EncryptedRequest;
import mtproto.Deserialize;
import mtproto.MTProtoConnection;

public class SendGetConfig extends SendEncrypted {
  public SendGetConfig() {
    //mfw this gives us gzip_packed as a response...
    message_data
      .append_int(CombinatorIds.help_get_config);
  }
  
  public void send(MTProtoConnection sender) {
    super.send(sender);
    sender.seq_no += 1;
  }
}
