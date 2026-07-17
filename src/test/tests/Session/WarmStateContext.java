package test;

import media.MediaReference;
import support.ByteArrayPlus;
import session.OutboundRecoveryState;
import session.WarmStateSnapshot;
import session.WarmStateSnapshotCodec;
import session.WarmStateStore;
import ui.AppShellModel;

public class WarmStateContext {
  private static AppShellModel buildWarmShell() {
    AppShellModel shell = new AppShellModel();

    shell.onAuthorized();
    shell.beginLocalDialogsLoad();
    shell.appendDialog(reader.DialogListModel.PEER_USER, 1, 0x1122334455667788L, true, 11, "Alice", "Ping", 2, false);
    shell.appendDialog(reader.DialogListModel.PEER_CHANNEL, 2, 0x0102030405060708L, true, 22, "Builders", "Pong", 0, false);
    shell.finishLocalDialogsLoad();
    shell.reader.peers.upsertIdentity(reader.DialogListModel.PEER_USER, 1, 0x1122334455667788L, true, false, "Alice", "alice");
    shell.reader.peers.upsertIdentity(reader.DialogListModel.PEER_CHANNEL, 2, 0x0102030405060708L, true, false, "Builders", "builders");
    shell.reader.secretChats.upsert(
      44,
      reader.DialogListModel.PEER_USER,
      1L,
      0x8877665544332211L,
      reader.SecretChatDirectory.STATE_WAITING,
      true,
      1700000000,
      0x1122334455667788L,
      new byte[] {1, 2, 3, 4},
      4
    );
    shell.selectDialog(0);
    shell.beginHistoryLoadForSelected();
    shell.appendHistoryMessage(11, "Alice", "older", false, false, false);
    shell.finishHistoryLoad();
    shell.openComposeForSelectedDialog();
    shell.updateComposeDraft("draft warm state");
    shell.beginComposeSend("draft warm state", 99L);
    return shell;
  }

  private static byte[] encodeLegacyV1(WarmStateSnapshot snapshot) {
    ByteArrayPlus out = new ByteArrayPlus();
    int i;
    int j;

    out.append_int(WarmStateSnapshotCodec.MAGIC);
    out.append_int(1);
    out.append_int(snapshot.route);
    out.append_int(snapshot.dialogCount);
    out.append_int(snapshot.selectedDialogIndex);
    out.append_int(snapshot.historySlotCount);

    for (i = 0; i < snapshot.dialogCount && i < snapshot.dialogs.length; ++i) {
      WarmStateSnapshot.DialogEntry dialog = snapshot.dialogs[i];

      out.append_int(dialog.peerKind);
      out.append_long(dialog.peerId);
      out.append_long(dialog.accessHash);
      out.append_int((dialog.hasAccessHash ? 1 : 0) | ((dialog.hasMedia ? 1 : 0) << 1));
      out.append_int(dialog.topMessageId);
      out.append_int(dialog.unreadCount);
      appendString(out, dialog.title);
      appendString(out, dialog.preview);
    }

    for (i = 0; i < snapshot.historySlotCount && i < snapshot.historySlots.length; ++i) {
      WarmStateSnapshot.HistorySlotEntry slot = snapshot.historySlots[i];

      out.append_int(slot.peerKind);
      out.append_long(slot.peerId);
      out.append_int(slot.loaded ? 1 : 0);
      out.append_int(slot.lastAccessTick);
      out.append_int(slot.messageCount);

      for (j = 0; j < slot.messageCount && j < slot.messages.length; ++j) {
        WarmStateSnapshot.MessageEntry message = slot.messages[j];

        out.append_int(message.messageId);
        out.append_int(
          (message.outgoing ? 1 : 0) |
          ((message.pending ? 1 : 0) << 1) |
          ((message.hasMedia ? 1 : 0) << 2)
        );
        appendString(out, message.sender);
        appendString(out, message.text);
      }
    }

    out.append_int(snapshot.composePeerKind);
    out.append_long(snapshot.composePeerId);
    out.append_int(snapshot.composeStatus);
    out.append_long(snapshot.composeLastRandomId);
    out.append_int(snapshot.composeLastMessageId);
    out.append_int(snapshot.composeLastErrorCode);
    out.append_int(snapshot.composeDirty ? 1 : 0);
    appendString(out, snapshot.composePeerTitle);
    appendString(out, snapshot.composeText);
    appendString(out, snapshot.composeLastErrorMessage);

    out.append_int(snapshot.outbound.sequence);
    out.append_int(snapshot.outbound.kind);
    out.append_int(snapshot.outbound.phase);
    out.append_int(snapshot.outbound.peerKind);
    out.append_long(snapshot.outbound.peerId);
    out.append_int(snapshot.outbound.code);
    out.append_int(snapshot.outbound.detail);
    appendString(out, snapshot.outbound.label);
    return out.toByteArray();
  }

  private static void appendString(ByteArrayPlus out, String value) {
    String text = value != null ? value : "";
    char[] chars = new char[text.length()];

    out.append_int(text.length());
    text.getChars(0, text.length(), chars, 0);
    for (int i = 0; i < chars.length; ++i) {
      out.append_int(chars[i] & 0xffff);
    }
  }

  public static class CodecRoundTripPreservesWarmStateTest extends Test {
    public String label() {
      return "Warm state codec preserves bounded reader, compose, and outbound state";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = buildWarmShell();
      WarmStateSnapshot decoded = new WarmStateSnapshot();
      WarmStateSnapshot snapshot = shell.captureWarmState();
      byte[] encoded;

      snapshot.loginPhoneNumber = "+15555550123";
      snapshot.loginPhoneCodeHash = "hash123";
      snapshot.loginCodeSentAtMillis = 123456789L;
      encoded = WarmStateSnapshotCodec.encode(snapshot);

      expect(encoded != null, true);
      expect(WarmStateSnapshotCodec.decode(decoded, encoded), true);
      expect(decoded.version, WarmStateSnapshotCodec.VERSION);
      expect(decoded.dialogCount, 2);
      expect(decoded.selectedDialogIndex, 0);
      expect(decoded.peerCount, 2);
      expect(decoded.secretChatCount, 1);
      expect(decoded.peers[0].accessHash, 0x1122334455667788L);
      expect(decoded.peers[0].username, "alice");
      expect(decoded.secretChats[0].chatId, 44);
      expect(decoded.secretChats[0].state, reader.SecretChatDirectory.STATE_WAITING);
      expect(decoded.secretChats[0].keyFingerprint, 0x1122334455667788L);
      expect(decoded.secretChats[0].pendingKeyData, new byte[] {1, 2, 3, 4});
      expect(decoded.historySlotCount, 1);
      expect(decoded.historySlots[0].messageCount, 2);
      expect(decoded.historySlots[0].messages[1].pending, true);
      expect(decoded.composeText, "draft warm state");
      expect(decoded.composeStatus, compose.ComposeStateModel.STATUS_SENDING);
      expect(decoded.outbound.phase, OutboundRecoveryState.PHASE_IN_FLIGHT);
      expect(decoded.loginPhoneNumber, "+15555550123");
      expect(decoded.loginPhoneCodeHash, "hash123");
      expect(decoded.loginCodeSentAtMillis, 123456789L);
    }
  }

  public static class AppShellRestoresWarmStateTest extends Test {
    public String label() {
      return "App shell restores cached reader and compose state from a warm snapshot";
    }

    public void test() throws TestFailureException {
      AppShellModel source = buildWarmShell();
      AppShellModel restored = new AppShellModel();
      WarmStateSnapshot snapshot = source.captureWarmState();

      restored.onResumeDetected();
      restored.applyWarmState(snapshot, true);

      expect(restored.reader.dialogs.count, 2);
      expect(restored.reader.dialogs.items[0].title, "Alice");
      expect(restored.reader.peers.count, 2);
      expect(restored.reader.secretChats.count, 1);
      expect(restored.reader.peers.find(reader.DialogListModel.PEER_CHANNEL, 2).username, "builders");
      expect(restored.reader.secretChats.findByPeer(reader.DialogListModel.PEER_USER, 1).chatId, 44);
      expect(restored.reader.secretChats.findByPeer(reader.DialogListModel.PEER_USER, 1).keyFingerprint, 0x1122334455667788L);
      expect(restored.reader.secretChats.findByPeer(reader.DialogListModel.PEER_USER, 1).pendingKeyData, new byte[] {1, 2, 3, 4});
      expect(restored.reader.history.count, 2);
      expect(restored.reader.history.items[1].pending, true);
      expect(restored.compose.text, "draft warm state");
      expect(restored.outbound.phase, OutboundRecoveryState.PHASE_IN_FLIGHT);
      expect(restored.outboundSummary, "TEXT SEND");
      expect(restored.activeDetail, "Warm reader cache restored while reconnecting.");
    }
  }

  public static class ForwardComposeContextRoundTripTest extends Test {
    public String label() {
      return "Warm state codec preserves forward compose context";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();
      WarmStateSnapshot decoded = new WarmStateSnapshot();
      WarmStateSnapshot snapshot;
      byte[] encoded;

      shell.onAuthorized();
      shell.beginLocalDialogsLoad();
      shell.appendDialog(reader.DialogListModel.PEER_USER, 1, 0x1122334455667788L, true, 11, "Alice", "Ping", 0, false);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);
      shell.beginHistoryLoadForSelected();
      shell.appendHistoryMessage(11, "Alice", "forward body", false, false, false);
      shell.finishHistoryLoad();
      shell.prepareForwardSelectedMessage();
      snapshot = shell.captureWarmState();
      encoded = WarmStateSnapshotCodec.encode(snapshot);

      expect(WarmStateSnapshotCodec.decode(decoded, encoded), true);
      expect(decoded.composeContextMode, compose.ComposeStateModel.CONTEXT_FORWARD);
      expect(decoded.composeForwardFromPeerKind, reader.DialogListModel.PEER_USER);
      expect(decoded.composeForwardFromPeerId, 1L);
      expect(decoded.composeForwardMessageId, 11);
      expect(decoded.composeContextPreview, "forward body");
    }
  }

  public static class LegacyWarmStateDecodeRemainsSupportedTest extends Test {
    public String label() {
      return "Warm state codec still decodes legacy version-1 snapshots";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = buildWarmShell();
      WarmStateSnapshot decoded = new WarmStateSnapshot();
      byte[] encoded = encodeLegacyV1(shell.captureWarmState());

      expect(WarmStateSnapshotCodec.decode(decoded, encoded), true);
      expect(decoded.version, 1);
      expect(decoded.dialogCount, 2);
      expect(decoded.peerCount, 0);
      expect(decoded.secretChatCount, 0);
      expect(decoded.historySlotCount, 1);
      expect(decoded.composeText, "draft warm state");
      expect(decoded.outbound.phase, OutboundRecoveryState.PHASE_IN_FLIGHT);
      expect(decoded.loginPhoneNumber, "");
      expect(decoded.loginCodeSentAtMillis, 0L);
    }
  }

  public static class MediaWarmStateRoundTripTest extends Test {
    public String label() {
      return "Warm state codec preserves compact media metadata for cached history";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();
      WarmStateSnapshot decoded = new WarmStateSnapshot();
      WarmStateSnapshot snapshot;
      byte[] encoded;

      shell.onAuthorized();
      shell.beginLocalDialogsLoad();
      shell.appendDialog(reader.DialogListModel.PEER_USER, 1, 0x1122334455667788L, true, 11, "Alice", "PHOTO 640x400", 0, true);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);
      shell.beginHistoryLoadForSelected();
      MediaReference ref = new MediaReference();
      ref.setPhoto(0x12345678L, 0x1122334455667788L, 2, new byte[] {1, 2, 3, 4}, 4, "x");
      shell.appendHistoryMessage(11, "Alice", "", false, false, true, "PHOTO", "640x400", ref);
      shell.finishHistoryLoad();

      snapshot = shell.captureWarmState();
      encoded = WarmStateSnapshotCodec.encode(snapshot);

      expect(WarmStateSnapshotCodec.decode(decoded, encoded), true);
      expect(decoded.historySlotCount, 1);
      expect(decoded.historySlots[0].messageCount, 1);
      expect(decoded.historySlots[0].messages[0].hasMedia, true);
      expect(decoded.historySlots[0].messages[0].mediaLabel, "PHOTO");
      expect(decoded.historySlots[0].messages[0].mediaInfo, "640x400");
      expect(decoded.historySlots[0].messages[0].mediaRef.kind, MediaReference.KIND_PHOTO);
      expect(decoded.historySlots[0].messages[0].mediaRef.fileReferenceLength, 4);
    }
  }

  public static class WarmStateStoreRoundTripTest extends SkippedTest {
    public String label() {
      return "Warm state snapshots round-trip through RMS";
    }

    public void test() throws TestFailureException {
      WarmStateStore store = new WarmStateStore("TelegramMicroWarmStateTest");
      WarmStateSnapshot loaded = new WarmStateSnapshot();
      AppShellModel shell = buildWarmShell();
      WarmStateSnapshot source = shell.captureWarmState();

      expect(store.clear(), true);
      expect(store.save(source), true);
      expect(store.load(loaded), true);
      expect(loaded.dialogCount, source.dialogCount);
      expect(loaded.composeText, source.composeText);
      expect(store.clear(), true);
    }
  }
}
