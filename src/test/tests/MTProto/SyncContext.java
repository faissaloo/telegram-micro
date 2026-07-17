package test;

import mtproto.CombinatorIds;
import mtproto.RPCResponse;
import mtproto.send.updates.SendGetChannelDifference;
import mtproto.send.updates.SendGetDifference;
import mtproto.send.updates.SendGetState;
import mtproto.recieve.updates.RecieveUpdatesDifference;
import mtproto.recieve.updates.RecieveUpdatesState;
import support.Decode;
import sync.ChannelSyncState;
import sync.SyncState;

public class SyncContext {
  public static class BuildUpdatesGetStateRequestTest extends Test {
    public String label() {
      return "Can build updates.getState requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendGetState()).message_data.end();

      expect(body.length, 4);
      expect(Decode.Little.int_decode(body, 0), CombinatorIds.updates_get_state);
    }
  }

  public static class BuildUpdatesGetDifferenceRequestTest extends Test {
    public String label() {
      return "Can build updates.getDifference requests";
    }

    public void test() throws TestFailureException {
      SyncState state = new SyncState();
      SyncState.DifferenceArgs args;
      byte[] body;
      int offset = 0;

      state.applyUpdatesState(new RecieveUpdatesState(10, 20, 30, 40, 2));
      args = state.buildDifferenceArgs();
      body = (new SendGetDifference(args)).message_data.end();

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.updates_get_difference);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 7);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 10);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), SyncState.DEFAULT_PTS_LIMIT_HINT);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), SyncState.DEFAULT_PTS_TOTAL_LIMIT_HINT);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 30);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 20);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), SyncState.DEFAULT_QTS_LIMIT_HINT);
      offset += 4;
      expect(offset, body.length);
    }
  }

  public static class BuildUpdatesGetChannelDifferenceRequestTest extends Test {
    public String label() {
      return "Can build updates.getChannelDifference requests";
    }

    public void test() throws TestFailureException {
      ChannelSyncState state = new ChannelSyncState(4);
      ChannelSyncState.DifferenceArgs args;
      byte[] body;
      int offset = 0;

      state.seedFromDialog(555L, 321);
      args = state.buildDifferenceArgs(555L, 24);
      body = (new SendGetChannelDifference(555L, 777L, args)).message_data.end();

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.updates_get_channel_difference);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 0);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_channel);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 555L);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 777L);
      offset += 8;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.channel_messages_filter_empty);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 321);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 24);
      offset += 4;
      expect(offset, body.length);
    }
  }

  public static class ParseUpdatesStatePayloadTest extends Test {
    public String label() {
      return "Can parse updates.State payloads";
    }

    public void test() throws TestFailureException {
      RPCResponse response = new RPCResponse(
        CombinatorIds.updates_state,
        (new mtproto.Serializer())
          .append_int(11)
          .append_int(22)
          .append_int(33)
          .append_int(44)
          .append_int(5)
          .end()
      );
      RecieveUpdatesState state = RecieveUpdatesState.from_rpc_response(response);

      expect(state.pts, 11);
      expect(state.qts, 22);
      expect(state.date, 33);
      expect(state.seq, 44);
      expect(state.unreadCount, 5);
    }
  }

  public static class ParseUpdatesDifferenceEmptyPayloadTest extends Test {
    public String label() {
      return "Can parse updates.differenceEmpty payloads";
    }

    public void test() throws TestFailureException {
      RPCResponse response = new RPCResponse(
        CombinatorIds.updates_difference_empty,
        (new mtproto.Serializer())
          .append_int(99)
          .append_int(77)
          .end()
      );
      RecieveUpdatesDifference difference = RecieveUpdatesDifference.from_rpc_response(response);

      expect(difference.type, RecieveUpdatesDifference.TYPE_EMPTY);
      expect(difference.date, 99);
      expect(difference.seq, 77);
    }
  }

  public static class ParseUpdatesDifferenceTooLongPayloadTest extends Test {
    public String label() {
      return "Can parse updates.differenceTooLong payloads";
    }

    public void test() throws TestFailureException {
      RPCResponse response = new RPCResponse(
        CombinatorIds.updates_difference_too_long,
        (new mtproto.Serializer())
          .append_int(1234)
          .end()
      );
      RecieveUpdatesDifference difference = RecieveUpdatesDifference.from_rpc_response(response);

      expect(difference.type, RecieveUpdatesDifference.TYPE_TOO_LONG);
      expect(difference.pts, 1234);
    }
  }

  public static class ParseUpdatesDifferenceSlicePayloadTest extends Test {
    public String label() {
      return "Can parse updates.differenceSlice payloads";
    }

    public void test() throws TestFailureException {
      RPCResponse response = new RPCResponse(
        CombinatorIds.updates_difference_slice,
        (new mtproto.Serializer())
          .append_int(CombinatorIds.vector).append_int(0)
          .append_int(CombinatorIds.vector).append_int(0)
          .append_int(CombinatorIds.vector).append_int(0)
          .append_int(CombinatorIds.vector).append_int(0)
          .append_int(CombinatorIds.vector).append_int(0)
          .append_int(CombinatorIds.updates_state)
          .append_int(200)
          .append_int(300)
          .append_int(400)
          .append_int(500)
          .append_int(6)
          .end()
      );
      RecieveUpdatesDifference difference = RecieveUpdatesDifference.from_rpc_response(response);

      expect(difference.type, RecieveUpdatesDifference.TYPE_SLICE);
      expect(difference.pts, 200);
      expect(difference.qts, 300);
      expect(difference.date, 400);
      expect(difference.seq, 500);
      expect(difference.unreadCount, 6);
    }
  }

  public static class SyncStateBuildsDifferenceQueriesFromStateTest extends Test {
    public String label() {
      return "Sync state builds bounded difference queries from updates.State";
    }

    public void test() throws TestFailureException {
      SyncState state = new SyncState();
      SyncState.DifferenceArgs args;

      state.applyUpdatesState(new RecieveUpdatesState(10, 20, 30, 40, 2));
      args = state.buildDifferenceArgs();

      expect(state.hasState, true);
      expect(state.needsDifference, false);
      expect(args.pts, 10);
      expect(args.date, 30);
      expect(args.qts, 20);
      expect(args.hasPtsLimit, true);
      expect(args.ptsLimit, SyncState.DEFAULT_PTS_LIMIT_HINT);
      expect(args.hasPtsTotalLimit, true);
      expect(args.ptsTotalLimit, SyncState.DEFAULT_PTS_TOTAL_LIMIT_HINT);
      expect(args.hasQtsLimit, true);
      expect(args.qtsLimit, SyncState.DEFAULT_QTS_LIMIT_HINT);
    }
  }

  public static class SyncStateMarksDifferenceTooLongTest extends Test {
    public String label() {
      return "Sync state marks updatesTooLong as a catch-up condition";
    }

    public void test() throws TestFailureException {
      SyncState state = new SyncState();

      state.noteDifferenceTooLong(777);

      expect(state.hasState, true);
      expect(state.pts, 777);
      expect(state.needsDifference, true);
      expect(state.differenceTooLong, true);
    }
  }

  public static class ChannelSyncStateBuildsForcedBootstrapRequestTest extends Test {
    public String label() {
      return "Channel sync state can force a bounded bootstrap request";
    }

    public void test() throws TestFailureException {
      ChannelSyncState state = new ChannelSyncState(4);
      ChannelSyncState.DifferenceArgs args;

      args = state.buildDifferenceArgs(777L, 12);

      expect(args.force, true);
      expect(args.pts, 1);
      expect(args.limit, 12);

      expect(state.seedFromDialog(777L, 456), true);
      args = state.buildDifferenceArgs(777L, 12);

      expect(args.force, false);
      expect(args.pts, 456);
      expect(args.limit, 12);
    }
  }
}
