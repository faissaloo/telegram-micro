package mtproto.recieve.service;

import mtproto.CombinatorIds;
import mtproto.RPCResponse;
import support.Decode;

public class RecieveUpdateShortSentMessage {
  public final boolean outgoing;
  public final int messageId;
  public final int pts;
  public final int ptsCount;
  public final int date;

  public RecieveUpdateShortSentMessage(boolean outgoing, int messageId, int pts, int ptsCount, int date) {
    this.outgoing = outgoing;
    this.messageId = messageId;
    this.pts = pts;
    this.ptsCount = ptsCount;
    this.date = date;
  }

  public static RecieveUpdateShortSentMessage from_rpc_response(RPCResponse response) {
    int offset = 0;
    int flags;
    int date;

    if (response == null || response.type != CombinatorIds.update_short_sent_message || response.body == null) {
      throw new IllegalArgumentException("updateShortSentMessage response is required.");
    }
    if (response.body.length < 20) {
      throw new IllegalArgumentException("updateShortSentMessage body is truncated.");
    }

    flags = Decode.Little.int_decode(response.body, offset);
    offset += 4;

    if ((flags & (1 << 7)) != 0 || (flags & (1 << 9)) != 0 || (flags & (1 << 25)) != 0) {
      throw new IllegalArgumentException("updateShortSentMessage has unsupported optional fields.");
    }

    date = Decode.Little.int_decode(response.body, offset + 12);
    if (date < 0) {
      throw new IllegalArgumentException("updateShortSentMessage date must not be negative.");
    }

    return new RecieveUpdateShortSentMessage(
      ((flags >> 1) & 1) != 0,
      Decode.Little.int_decode(response.body, offset),
      Decode.Little.int_decode(response.body, offset + 4),
      Decode.Little.int_decode(response.body, offset + 8),
      date
    );
  }
}
