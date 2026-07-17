package mtproto.send.updates;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;
import mtproto.send.SendEncrypted;
import sync.SyncState;

public final class SendGetDifference extends SendEncrypted {
  public SendGetDifference(SyncState.DifferenceArgs args) {
    int flags = 0;

    if (args == null) {
      throw new IllegalArgumentException("Difference args are required.");
    }
    if (args.hasPtsTotalLimit) {
      flags |= 1;
    }
    if (args.hasPtsLimit) {
      flags |= (1 << 1);
    }
    if (args.hasQtsLimit) {
      flags |= (1 << 2);
    }

    message_data
      .append_int(CombinatorIds.updates_get_difference)
      .append_int(flags)
      .append_int(args.pts);
    if (args.hasPtsLimit) {
      message_data.append_int(args.ptsLimit);
    }
    if (args.hasPtsTotalLimit) {
      message_data.append_int(args.ptsTotalLimit);
    }
    message_data
      .append_int(args.date)
      .append_int(args.qts);
    if (args.hasQtsLimit) {
      message_data.append_int(args.qtsLimit);
    }
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, false, true);
  }
}
