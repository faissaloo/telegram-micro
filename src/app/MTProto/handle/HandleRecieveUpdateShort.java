package mtproto.handle;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.Response;

public class HandleRecieveUpdateShort extends MTProtoCallback {
  public HandleRecieveUpdateShort(MTProtoConnection connection) {
    super(CombinatorIds.update_short, connection);
  }

  public void execute(Response response) {
  }
}
