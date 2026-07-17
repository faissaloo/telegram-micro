package mtproto.send.messages;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

public class SendGetDialogs extends SendEncrypted {
  public SendGetDialogs(int limit) {
    this(false, 0, 0, DialogInputPeer.empty(), limit, 0L);
  }

  public SendGetDialogs(
    boolean excludePinned,
    int offsetDate,
    int offsetId,
    DialogInputPeer offsetPeer,
    int limit,
    long hash
  ) {
    int flags = excludePinned ? 1 : 0;

    if (offsetPeer == null) {
      offsetPeer = DialogInputPeer.empty();
    }

    message_data
      .append_int(CombinatorIds.messages_get_dialogs)
      .append_int(flags)
      .append_int(offsetDate)
      .append_int(offsetId)
      .append_raw_bytes(offsetPeer.serialize())
      .append_int(limit)
      .append_long(hash);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }

  public static final class DialogInputPeer {
    public final int peerKind;
    public final long peerId;
    public final long accessHash;
    public final boolean hasAccessHash;

    private DialogInputPeer(int peerKind, long peerId, long accessHash, boolean hasAccessHash) {
      this.peerKind = peerKind;
      this.peerId = peerId;
      this.accessHash = accessHash;
      this.hasAccessHash = hasAccessHash;
    }

    public static DialogInputPeer empty() {
      return new DialogInputPeer(0, 0, 0, false);
    }

    public static DialogInputPeer user(long peerId, long accessHash) {
      return new DialogInputPeer(1, peerId, accessHash, true);
    }

    public static DialogInputPeer chat(long peerId) {
      return new DialogInputPeer(2, peerId, 0, false);
    }

    public static DialogInputPeer channel(long peerId, long accessHash) {
      return new DialogInputPeer(3, peerId, accessHash, true);
    }

    public byte[] serialize() {
      if (peerKind == 0) {
        return (new mtproto.Serializer())
          .append_int(CombinatorIds.input_peer_empty)
          .end();
      }

      if (peerKind == 1 && hasAccessHash) {
        return (new mtproto.Serializer())
          .append_int(CombinatorIds.input_peer_user)
          .append_long(peerId)
          .append_long(accessHash)
          .end();
      }

      if (peerKind == 2) {
        return (new mtproto.Serializer())
          .append_int(CombinatorIds.input_peer_chat)
          .append_long(peerId)
          .end();
      }

      if (peerKind == 3 && hasAccessHash) {
        return (new mtproto.Serializer())
          .append_int(CombinatorIds.input_peer_channel)
          .append_long(peerId)
          .append_long(accessHash)
          .end();
      }

      throw new IllegalArgumentException("Input peer is incomplete.");
    }
  }
}
