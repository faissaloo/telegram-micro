package mtproto.handle;

import mtproto.MTProtoConnection;
import mtproto.Response;

public class HandleRecieveAuxiliaryUpdates extends MTProtoCallback {
  public HandleRecieveAuxiliaryUpdates(int combinatorId, MTProtoConnection connection) {
    super(combinatorId, connection);
  }

  public void execute(Response response) {
  }
}
