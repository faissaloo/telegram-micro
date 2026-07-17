package mtproto.handle;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.Response;

public class HandleRecieveMsgsAck extends MTProtoCallback {
  public HandleRecieveMsgsAck(MTProtoConnection connection) {
    super(CombinatorIds.msgs_ack, connection);
  }

  public void execute(Response response) {
  }
}
