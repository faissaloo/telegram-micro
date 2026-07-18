package mtproto.send.messages;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

/** Marks the visible bounded history slice read for a cloud dialog. */
public final class SendReadHistory extends SendEncrypted {
  public SendReadHistory(SendGetDialogs.DialogInputPeer peer, int maxMessageId) {
    if (peer == null || peer.peerKind == 0) {
      throw new IllegalArgumentException("Read-history peer is required.");
    }
    if (maxMessageId <= 0) {
      throw new IllegalArgumentException("Read-history max message id is required.");
    }

    if (peer.peerKind == 3) {
      if (!peer.hasAccessHash) {
        throw new IllegalArgumentException("Channel read history requires an access hash.");
      }
      message_data
        .append_int(CombinatorIds.channels_read_history)
        .append_int(CombinatorIds.input_channel)
        .append_long(peer.peerId)
        .append_long(peer.accessHash)
        .append_int(maxMessageId);
      return;
    }

    message_data
      .append_int(CombinatorIds.messages_read_history)
      .append_raw_bytes(peer.serialize())
      .append_int(maxMessageId);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
