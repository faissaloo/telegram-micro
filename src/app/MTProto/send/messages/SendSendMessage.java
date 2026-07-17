package mtproto.send.messages;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

public class SendSendMessage extends SendEncrypted {
  public SendSendMessage(SendGetDialogs.DialogInputPeer peer, String message, long randomId) {
    this(peer, message, randomId, 0, false, false, false);
  }

  public SendSendMessage(
    SendGetDialogs.DialogInputPeer peer,
    String message,
    long randomId,
    int replyToMessageId
  ) {
    this(peer, message, randomId, replyToMessageId, false, false, false);
  }

  public SendSendMessage(
    SendGetDialogs.DialogInputPeer peer,
    String message,
    long randomId,
    int replyToMessageId,
    boolean noWebpage,
    boolean silent,
    boolean clearDraft
  ) {
    int flags = 0;

    if (peer == null) {
      throw new IllegalArgumentException("Send peer is required.");
    }
    if (message == null || message.length() == 0) {
      throw new IllegalArgumentException("Message text is required.");
    }
    if (randomId == 0) {
      throw new IllegalArgumentException("Random id is required.");
    }

    if (replyToMessageId > 0) {
      flags |= (1 << 0);
    }
    if (noWebpage) {
      flags |= (1 << 1);
    }
    if (silent) {
      flags |= (1 << 5);
    }
    if (clearDraft) {
      flags |= (1 << 7);
    }

    message_data
      .append_int(CombinatorIds.messages_send_message)
      .append_int(flags)
      .append_raw_bytes(peer.serialize());
    if (replyToMessageId > 0) {
      message_data
        .append_int(CombinatorIds.input_reply_to_message)
        .append_int(0)
        .append_int(replyToMessageId);
    }
    message_data
      .append_string(message)
      .append_long(randomId);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
