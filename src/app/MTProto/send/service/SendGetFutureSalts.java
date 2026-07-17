package mtproto.send.service;

import mtproto.send.SendEncrypted;
import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;

public class SendGetFutureSalts extends SendEncrypted {
  public SendGetFutureSalts(int num) {
    message_data
      .append_int(CombinatorIds.get_future_salts)
      .append_int(num);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true, false);
  }
}
