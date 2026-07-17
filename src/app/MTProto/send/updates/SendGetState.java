package mtproto.send.updates;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

public final class SendGetState extends SendEncrypted {
  public SendGetState() {
    message_data.append_int(CombinatorIds.updates_get_state);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, false, true);
  }
}
