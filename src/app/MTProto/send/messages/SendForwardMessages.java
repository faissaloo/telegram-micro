package mtproto.send.messages;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

public final class SendForwardMessages extends SendEncrypted {
  public SendForwardMessages(
    SendGetDialogs.DialogInputPeer fromPeer,
    int messageId,
    long randomId,
    SendGetDialogs.DialogInputPeer toPeer
  ) {
    if (fromPeer == null) {
      throw new IllegalArgumentException("Forward source peer is required.");
    }
    if (messageId == 0) {
      throw new IllegalArgumentException("Forward source message id is required.");
    }
    if (randomId == 0) {
      throw new IllegalArgumentException("Forward random id is required.");
    }
    if (toPeer == null) {
      throw new IllegalArgumentException("Forward destination peer is required.");
    }

    message_data
      .append_int(CombinatorIds.messages_forward_messages)
      .append_int(0)
      .append_raw_bytes(fromPeer.serialize())
      .append_int(CombinatorIds.vector)
      .append_int(1)
      .append_int(messageId)
      .append_int(CombinatorIds.vector)
      .append_int(1)
      .append_long(randomId)
      .append_raw_bytes(toPeer.serialize());
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
