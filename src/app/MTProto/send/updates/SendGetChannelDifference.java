package mtproto.send.updates;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;
import sync.ChannelSyncState;

public final class SendGetChannelDifference extends SendEncrypted {
  public SendGetChannelDifference(long channelId, long accessHash, ChannelSyncState.DifferenceArgs args) {
    int flags = 0;

    if (channelId == 0 || accessHash == 0) {
      throw new IllegalArgumentException("Channel input requires id and access hash.");
    }
    if (args == null) {
      throw new IllegalArgumentException("Channel difference args are required.");
    }
    if (args.force) {
      flags |= 1;
    }

    message_data
      .append_int(CombinatorIds.updates_get_channel_difference)
      .append_int(flags)
      .append_int(CombinatorIds.input_channel)
      .append_long(channelId)
      .append_long(accessHash)
      .append_int(CombinatorIds.channel_messages_filter_empty)
      .append_int(args.pts)
      .append_int(args.limit);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, false, true);
  }
}
