package test;

import reader.DialogListModel;

public class DialogListModelContext {
  public static class CapacityStopsAtFixedBoundTest extends Test {
    public String label() {
      return "Dialog lists stop at their fixed capacity";
    }

    public void test() throws TestFailureException {
      DialogListModel dialogs = new DialogListModel(2);

      expect(dialogs.append(DialogListModel.PEER_USER, 1, "Alice", "One", 0, false), true);
      expect(dialogs.append(DialogListModel.PEER_USER, 2, "Bob", "Two", 0, false), true);
      expect(dialogs.append(DialogListModel.PEER_USER, 3, "Carol", "Three", 0, false), false);
      expect(dialogs.count, 2);
    }
  }

  public static class SelectionTracksCurrentDialogTest extends Test {
    public String label() {
      return "Dialog selection tracks the current pooled item";
    }

    public void test() throws TestFailureException {
      DialogListModel dialogs = new DialogListModel(3);

      dialogs.append(DialogListModel.PEER_USER, 1, "Alice", "One", 0, false);
      dialogs.append(DialogListModel.PEER_CHAT, 2, "Builders", "Two", 3, true);
      dialogs.selectNext();

      expect(dialogs.selected().title, "Builders");
      expect(dialogs.selected().unreadCount, 3);
      expect(dialogs.selected().hasMedia, true);
    }
  }

  public static class ActivityUpsertMovesUpdatedDialogToFrontTest extends Test {
    public String label() {
      return "Dialog activity updates move the hot dialog to the front without losing selection";
    }

    public void test() throws TestFailureException {
      DialogListModel dialogs = new DialogListModel(3);

      dialogs.append(DialogListModel.PEER_USER, 1, "Alice", "One", 0, false);
      dialogs.append(DialogListModel.PEER_CHAT, 2, "Builders", "Two", 1, false);
      dialogs.append(DialogListModel.PEER_CHANNEL, 3, "Updates", "Three", 0, false);
      dialogs.selectIndex(2);

      expect(dialogs.upsertActivity(
        DialogListModel.PEER_CHAT,
        2,
        0,
        false,
        55,
        "Builders",
        "Fresh",
        2,
        false
      ), true);

      expect(dialogs.items[0].peerKind, DialogListModel.PEER_CHAT);
      expect(dialogs.items[0].peerId, 2L);
      expect(dialogs.items[0].preview, "Fresh");
      expect(dialogs.items[0].topMessageId, 55);
      expect(dialogs.selected().peerKind, DialogListModel.PEER_CHANNEL);
      expect(dialogs.selected().peerId, 3L);
    }
  }
}
