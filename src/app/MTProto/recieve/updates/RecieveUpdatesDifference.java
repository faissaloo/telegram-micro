package mtproto.recieve.updates;

import mtproto.CombinatorIds;
import mtproto.RPCResponse;
import support.Decode;

public final class RecieveUpdatesDifference {
  public static final int TYPE_EMPTY = 1;
  public static final int TYPE_FULL = 2;
  public static final int TYPE_SLICE = 3;
  public static final int TYPE_TOO_LONG = 4;

  public final int type;
  public final int pts;
  public final int qts;
  public final int date;
  public final int seq;
  public final int unreadCount;

  public RecieveUpdatesDifference(int type, int pts, int qts, int date, int seq, int unreadCount) {
    this.type = type;
    this.pts = pts;
    this.qts = qts;
    this.date = date;
    this.seq = seq;
    this.unreadCount = unreadCount;
  }

  public static RecieveUpdatesDifference from_rpc_response(RPCResponse response) {
    byte[] body;
    int nestedOffset;

    if (response == null || response.body == null) {
      throw new IllegalArgumentException("updates.Difference response is required.");
    }

    body = response.body;
    if (response.type == CombinatorIds.updates_difference_empty) {
      if (body.length < 8) {
        throw new IllegalArgumentException("updates.differenceEmpty body is truncated.");
      }
      return new RecieveUpdatesDifference(
        TYPE_EMPTY,
        0,
        0,
        Decode.Little.int_decode(body, 0),
        Decode.Little.int_decode(body, 4),
        0
      );
    }
    if (response.type == CombinatorIds.updates_difference_too_long) {
      if (body.length < 4) {
        throw new IllegalArgumentException("updates.differenceTooLong body is truncated.");
      }
      return new RecieveUpdatesDifference(
        TYPE_TOO_LONG,
        Decode.Little.int_decode(body, 0),
        0,
        0,
        0,
        0
      );
    }
    if (response.type != CombinatorIds.updates_difference &&
        response.type != CombinatorIds.updates_difference_slice) {
      throw new IllegalArgumentException("Unsupported updates.Difference response.");
    }
    if (body.length < 24) {
      throw new IllegalArgumentException("updates.Difference body is truncated.");
    }

    nestedOffset = body.length - 24;
    if (Decode.Little.int_decode(body, nestedOffset) != CombinatorIds.updates_state) {
      throw new IllegalArgumentException("updates.Difference does not end with updates.State.");
    }
    nestedOffset += 4;
    return new RecieveUpdatesDifference(
      response.type == CombinatorIds.updates_difference_slice ? TYPE_SLICE : TYPE_FULL,
      Decode.Little.int_decode(body, nestedOffset),
      Decode.Little.int_decode(body, nestedOffset + 4),
      Decode.Little.int_decode(body, nestedOffset + 8),
      Decode.Little.int_decode(body, nestedOffset + 12),
      Decode.Little.int_decode(body, nestedOffset + 16)
    );
  }

  public boolean hasState() {
    return type == TYPE_FULL || type == TYPE_SLICE;
  }
}
