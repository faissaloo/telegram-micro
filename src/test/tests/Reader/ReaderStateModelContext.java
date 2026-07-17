package test;

import reader.DialogListModel;
import reader.PeerDirectory;
import reader.ReaderStateModel;
import session.WarmStateSnapshot;

public class ReaderStateModelContext {
  public static class SelectingDialogRestoresCachedHistoryTest extends Test {
    public String label() {
      return "Selecting a dialog restores bounded cached history when available";
    }

    public void test() throws TestFailureException {
      ReaderStateModel reader = new ReaderStateModel(4, 4, 2);
      reader.appendDialog(DialogListModel.PEER_USER, 1, "Alice", "Ping", 0, false);
      reader.appendDialog(DialogListModel.PEER_CHAT, 2, "Builders", "Pong", 0, false);
      reader.selectDialog(0);
      reader.beginHistoryLoad(DialogListModel.PEER_USER, 1);
      reader.appendHistoryMessage("Alice", "hello", false, false);
      reader.finishHistoryLoad();

      expect(reader.history.count, 1);
      expect(reader.selectDialog(1), true);
      expect(reader.history.count, 0);
      expect(reader.historyLoaded, false);
      expect(reader.selectDialog(0), true);
      expect(reader.historyLoaded, true);
      expect(reader.history.count, 1);
      expect(reader.history.items[0].text, "hello");
    }
  }

  public static class HistoryCapacityIsBoundedTest extends Test {
    public String label() {
      return "Reader history respects its fixed capacity";
    }

    public void test() throws TestFailureException {
      ReaderStateModel reader = new ReaderStateModel(2, 2);

      reader.appendDialog(DialogListModel.PEER_USER, 1, "Alice", "Ping", 0, false);
      reader.selectDialog(0);
      reader.beginHistoryLoad(DialogListModel.PEER_USER, 1);
      expect(reader.appendHistoryMessage("a", "1", false, false), true);
      expect(reader.appendHistoryMessage("b", "2", false, false), true);
      expect(reader.appendHistoryMessage("c", "3", false, false), false);
      expect(reader.history.count, 2);
    }
  }

  public static class HistorySlotsEvictLeastRecentPeerTest extends Test {
    public String label() {
      return "Reader history cache evicts the least-recent non-selected peer";
    }

    public void test() throws TestFailureException {
      ReaderStateModel reader = new ReaderStateModel(4, 2, 2);

      reader.appendDialog(DialogListModel.PEER_USER, 1, "Alice", "Ping", 0, false);
      reader.appendDialog(DialogListModel.PEER_CHAT, 2, "Builders", "Pong", 0, false);
      reader.appendDialog(DialogListModel.PEER_CHANNEL, 3, "Updates", "Zap", 0, false);

      reader.selectDialog(0);
      reader.beginHistoryLoad(DialogListModel.PEER_USER, 1);
      reader.appendHistoryMessage("Alice", "one", false, false);
      reader.finishHistoryLoad();

      reader.selectDialog(1);
      reader.beginHistoryLoad(DialogListModel.PEER_CHAT, 2);
      reader.appendHistoryMessage("Builders", "two", false, false);
      reader.finishHistoryLoad();

      reader.selectDialog(2);
      reader.beginHistoryLoad(DialogListModel.PEER_CHANNEL, 3);
      reader.appendHistoryMessage("Updates", "three", false, false);
      reader.finishHistoryLoad();

      expect(reader.loadedHistorySlotCount(), 2);
      expect(reader.selectDialog(0), true);
      expect(reader.historyLoaded, false);
      expect(reader.history.count, 0);
      expect(reader.selectDialog(1), true);
      expect(reader.historyLoaded, true);
      expect(reader.history.items[0].text, "two");
      expect(reader.selectDialog(2), true);
      expect(reader.historyLoaded, true);
      expect(reader.history.items[0].text, "three");
    }
  }

  public static class OutboundAppendUsesBoundedCachedHistoryTest extends Test {
    public String label() {
      return "Reader outbound appends update bounded cached history and preview";
    }

    public void test() throws TestFailureException {
      ReaderStateModel reader = new ReaderStateModel(2, 2, 2);

      reader.appendDialog(DialogListModel.PEER_USER, 1, "Alice", "Ping", 0, false);
      reader.selectDialog(0);
      expect(reader.appendOutboundText(DialogListModel.PEER_USER, 1, "YOU", "first"), true);
      expect(reader.appendOutboundText(DialogListModel.PEER_USER, 1, "YOU", "second"), true);
      expect(reader.appendOutboundFileNotice(DialogListModel.PEER_USER, 1, "YOU", "capture.png"), true);

      expect(reader.historyLoaded, true);
      expect(reader.history.count, 2);
      expect(reader.history.items[0].text, "second");
      expect(reader.history.items[1].text, "file: capture.png");
      expect(reader.dialogs.items[0].preview, "file: capture.png");
    }
  }

  public static class PendingOutboundLifecycleUpdatesCachedHistoryTest extends Test {
    public String label() {
      return "Reader pending outbound entries can be promoted or removed";
    }

    public void test() throws TestFailureException {
      ReaderStateModel reader = new ReaderStateModel(2, 3, 2);

      reader.appendDialog(DialogListModel.PEER_USER, 1, "Alice", "Ping", 0, false);
      reader.selectDialog(0);
      expect(reader.appendPendingOutboundText(DialogListModel.PEER_USER, 1, "YOU", "pending"), true);
      expect(reader.history.count, 1);
      expect(reader.history.items[0].pending, true);
      expect(reader.markLatestPendingOutboundSent(DialogListModel.PEER_USER, 1, 77, 1700000000), true);
      expect(reader.history.items[0].pending, false);
      expect(reader.history.items[0].messageId, 77);
      expect(reader.history.items[0].dateSeconds, 1700000000);

      expect(reader.appendPendingOutboundText(DialogListModel.PEER_USER, 1, "YOU", "retry"), true);
      expect(reader.history.count, 2);
      expect(reader.removeLatestPendingOutbound(DialogListModel.PEER_USER, 1), true);
      expect(reader.history.count, 1);
      expect(reader.history.items[0].text, "pending");
    }
  }

  public static class WarmStateRestorePreservesPeerIdentityTest extends Test {
    public String label() {
      return "Reader warm-state import preserves bounded peer identity and access hashes";
    }

    public void test() throws TestFailureException {
      ReaderStateModel source = new ReaderStateModel(4, 4, 2);
      ReaderStateModel restored = new ReaderStateModel(4, 4, 2);
      WarmStateSnapshot snapshot = new WarmStateSnapshot();
      PeerDirectory.Item user;
      PeerDirectory.Item channel;

      source.peers.upsertIdentity(DialogListModel.PEER_USER, 1, 0x1122334455667788L, true, false, "Alice", "alice");
      source.peers.upsertIdentity(DialogListModel.PEER_CHANNEL, 3, 0x0102030405060708L, true, false, "Updates", "updates");
      source.appendDialog(DialogListModel.PEER_USER, 1, 0x1122334455667788L, true, 11, "Alice", "Ping", 0, false);
      source.appendDialog(DialogListModel.PEER_CHANNEL, 3, 0x0102030405060708L, true, 22, "Updates", "Zap", 0, true);
      source.selectDialog(0);
      source.beginHistoryLoad(DialogListModel.PEER_USER, 1);
      source.appendHistoryMessage(11, "Alice", "hello", false, false, false);
      source.finishHistoryLoad();

      source.exportWarmState(snapshot);
      restored.importWarmState(snapshot);
      user = restored.peers.find(DialogListModel.PEER_USER, 1);
      channel = restored.peers.find(DialogListModel.PEER_CHANNEL, 3);

      expect(restored.peers.count, 2);
      expect(user != null, true);
      expect(user.accessHash, 0x1122334455667788L);
      expect(user.username, "alice");
      expect(channel != null, true);
      expect(channel.title, "Updates");
      expect(channel.username, "updates");
      expect(restored.dialogs.items[1].accessHash, 0x0102030405060708L);
      expect(restored.peers.titleOrFallback(DialogListModel.PEER_CHANNEL, 3), "Updates");
    }
  }

  public static class LiveShortUpdateRefreshesDialogAndCachedHistoryTest extends Test {
    public String label() {
      return "Live short updates refresh bounded dialog activity and selected cached history";
    }

    public void test() throws TestFailureException {
      ReaderStateModel reader = new ReaderStateModel(4, 3, 2);

      reader.peers.upsertIdentity(DialogListModel.PEER_USER, 1, 0, false, false, "Alice", "alice");
      reader.peers.upsertIdentity(DialogListModel.PEER_USER, 2, 0, false, false, "Bob", "bob");
      reader.appendDialog(DialogListModel.PEER_USER, 1, "Alice", "older", 0, false);
      reader.appendDialog(DialogListModel.PEER_USER, 2, "Bob", "stale", 0, false);
      reader.selectDialog(1);

      expect(reader.applyLiveTextUpdate(
        DialogListModel.PEER_USER,
        2,
        DialogListModel.PEER_USER,
        2,
        99,
        "fresh ping",
        false,
        false,
        0
      ), true);

      expect(reader.dialogs.items[0].peerId, 2L);
      expect(reader.dialogs.items[0].preview, "fresh ping");
      expect(reader.historyLoaded, true);
      expect(reader.history.count, 1);
      expect(reader.history.items[0].sender, "Bob");
      expect(reader.history.items[0].text, "fresh ping");
      expect(reader.history.items[0].messageId, 99);
    }
  }

  public static class LiveShortUpdateFallsBackWhenPeerIsUnknownTest extends Test {
    public String label() {
      return "Live short updates require existing bounded peer context";
    }

    public void test() throws TestFailureException {
      ReaderStateModel reader = new ReaderStateModel(2, 2, 1);

      expect(reader.applyLiveTextUpdate(
        DialogListModel.PEER_USER,
        42,
        DialogListModel.PEER_USER,
        42,
        7,
        "who is this",
        false,
        false,
        1
      ), false);
      expect(reader.dialogs.count, 0);
      expect(reader.history.count, 0);
    }
  }

  public static class LiveEditUpdatesCachedMessageInPlaceTest extends Test {
    public String label() {
      return "Live edits update cached messages in place without duplicating history";
    }

    public void test() throws TestFailureException {
      ReaderStateModel reader = new ReaderStateModel(3, 4, 2);

      reader.peers.upsertIdentity(DialogListModel.PEER_USER, 2, 0, false, false, "Bob", "bob");
      reader.appendDialog(DialogListModel.PEER_USER, 2, "Bob", "old", 0, false);
      reader.selectDialog(0);
      expect(reader.applyLiveTextUpdate(
        DialogListModel.PEER_USER,
        2,
        DialogListModel.PEER_USER,
        2,
        77,
        "old",
        false,
        false,
        0
      ), true);

      expect(reader.applyLiveEditedTextUpdate(
        DialogListModel.PEER_USER,
        2,
        DialogListModel.PEER_USER,
        2,
        77,
        "edited",
        false,
        false
      ), true);

      expect(reader.history.count, 1);
      expect(reader.history.items[0].messageId, 77);
      expect(reader.history.items[0].text, "edited");
      expect(reader.dialogs.items[0].preview, "edited");
    }
  }

  public static class MessageDatesSurviveBoundedLiveUpdatesTest extends Test {
    public String label() {
      return "Message dates survive edits and bounded eviction while invalid dates are atomic";
    }

    public void test() throws TestFailureException {
      ReaderStateModel reader = new ReaderStateModel(2, 2, 1);

      reader.peers.upsertIdentity(DialogListModel.PEER_USER, 2, 0, false, false, "Bob", "bob");
      reader.appendDialog(DialogListModel.PEER_USER, 2, "Bob", "old", 0, false);
      reader.selectDialog(0);

      expect(reader.applyLiveTextUpdate(
        DialogListModel.PEER_USER, 2,
        DialogListModel.PEER_USER, 2,
        1, "one", false, false, "", "", null, 1700000000, 0
      ), true);
      expect(reader.applyLiveTextUpdate(
        DialogListModel.PEER_USER, 2,
        DialogListModel.PEER_USER, 2,
        2, "two", false, false, "", "", null, 1700000060, 0
      ), true);
      expect(reader.applyLiveEditedTextUpdate(
        DialogListModel.PEER_USER, 2,
        DialogListModel.PEER_USER, 2,
        2, "two edited", false, false, "", "", null, 0
      ), true);
      expect(reader.history.items[1].dateSeconds, 1700000060);

      expect(reader.applyLiveTextUpdate(
        DialogListModel.PEER_USER, 2,
        DialogListModel.PEER_USER, 2,
        3, "three", false, false, "", "", null, 1700000120, 0
      ), true);
      expect(reader.history.count, 2);
      expect(reader.history.items[0].messageId, 2);
      expect(reader.history.items[0].dateSeconds, 1700000060);
      expect(reader.history.items[1].dateSeconds, 1700000120);

      expect(reader.applyLiveTextUpdate(
        DialogListModel.PEER_USER, 2,
        DialogListModel.PEER_USER, 2,
        4, "invalid", false, false, "", "", null, -1, 1
      ), false);
      expect(reader.history.count, 2);
      expect(reader.dialogs.items[0].topMessageId, 3);
      expect(reader.dialogs.items[0].preview, "three");
      expect(reader.dialogs.items[0].unreadCount, 0);
    }
  }

  public static class LiveDeleteRemovesCachedMessageAndClearsPreviewTest extends Test {
    public String label() {
      return "Live deletes remove cached messages and clear dialog preview when needed";
    }

    public void test() throws TestFailureException {
      ReaderStateModel reader = new ReaderStateModel(3, 4, 2);

      reader.peers.upsertIdentity(DialogListModel.PEER_USER, 2, 0, false, false, "Bob", "bob");
      reader.appendDialog(DialogListModel.PEER_USER, 2, "Bob", "old", 0, false);
      reader.selectDialog(0);
      expect(reader.applyLiveTextUpdate(
        DialogListModel.PEER_USER,
        2,
        DialogListModel.PEER_USER,
        2,
        77,
        "gone",
        false,
        false,
        0
      ), true);

      expect(reader.deleteMessageForPeer(DialogListModel.PEER_USER, 2, 77), true);
      expect(reader.history.count, 0);
      expect(reader.dialogs.items[0].preview, "");
      expect(reader.dialogs.items[0].topMessageId, 0);
    }
  }

  public static class LiveUnreadUpdatesDialogBadgeTest extends Test {
    public String label() {
      return "Live read-state updates change the bounded unread badge";
    }

    public void test() throws TestFailureException {
      ReaderStateModel reader = new ReaderStateModel(3, 3, 1);

      reader.appendDialog(DialogListModel.PEER_USER, 9, "Alice", "Ping", 4, false);
      expect(reader.applyLiveUnreadCount(DialogListModel.PEER_USER, 9, 1), true);
      expect(reader.dialogs.items[0].unreadCount, 1);
      expect(reader.applyLiveUnreadCount(DialogListModel.PEER_USER, 9, 0), true);
      expect(reader.dialogs.items[0].unreadCount, 0);
    }
  }
}
