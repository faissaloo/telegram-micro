package mtproto.recieve.updates;

import mtproto.CombinatorIds;
import mtproto.RPCResponse;
import support.Decode;

public final class RecieveUpdatesState {
  public final int pts;
  public final int qts;
  public final int date;
  public final int seq;
  public final int unreadCount;

  public RecieveUpdatesState(int pts, int qts, int date, int seq, int unreadCount) {
    this.pts = pts;
    this.qts = qts;
    this.date = date;
    this.seq = seq;
    this.unreadCount = unreadCount;
  }

  public static RecieveUpdatesState from_rpc_response(RPCResponse response) {
    byte[] body;

    if (response == null || response.type != CombinatorIds.updates_state || response.body == null) {
      throw new IllegalArgumentException("updates.State response is required.");
    }

    body = response.body;
    if (body.length < 20) {
      throw new IllegalArgumentException("updates.State body is truncated.");
    }

    return new RecieveUpdatesState(
      Decode.Little.int_decode(body, 0),
      Decode.Little.int_decode(body, 4),
      Decode.Little.int_decode(body, 8),
      Decode.Little.int_decode(body, 12),
      Decode.Little.int_decode(body, 16)
    );
  }
}
