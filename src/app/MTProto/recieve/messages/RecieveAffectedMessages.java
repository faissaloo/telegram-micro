package mtproto.recieve.messages;

import mtproto.CombinatorIds;
import mtproto.RPCResponse;
import support.Decode;

public final class RecieveAffectedMessages {
  public final int pts;
  public final int ptsCount;

  public RecieveAffectedMessages(int pts, int ptsCount) {
    this.pts = pts;
    this.ptsCount = ptsCount;
  }

  public static RecieveAffectedMessages from_rpc_response(RPCResponse response) {
    if (response == null ||
        response.type != CombinatorIds.messages_affected_messages ||
        response.body == null) {
      throw new IllegalArgumentException("messages.AffectedMessages response is required.");
    }
    if (response.body.length < 8) {
      throw new IllegalArgumentException("messages.AffectedMessages body is truncated.");
    }

    return new RecieveAffectedMessages(
      Decode.Little.int_decode(response.body, 0),
      Decode.Little.int_decode(response.body, 4)
    );
  }
}
