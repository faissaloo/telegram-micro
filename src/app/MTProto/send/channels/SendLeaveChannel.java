package mtproto.send.channels;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;

public final class SendLeaveChannel extends SendEncrypted {
  public SendLeaveChannel(long channelId, long accessHash) {
    if (channelId == 0) {
      throw new IllegalArgumentException("Channel id is required.");
    }
    if (accessHash == 0) {
      throw new IllegalArgumentException("Channel access hash is required.");
    }

    message_data
      .append_int(CombinatorIds.channels_leave_channel)
      .append_int(CombinatorIds.input_channel)
      .append_long(channelId)
      .append_long(accessHash);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }
}
