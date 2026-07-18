package test;

import media.MediaReference;
import reader.SecretChatDirectory;
import support.ByteArrayPlus;
import support.Decode;
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
    shell.reader.applyLiveInboxRead(reader.DialogListModel.PEER_USER, 1, 10, 2);
    shell.reader.applyLiveOutboxRead(reader.DialogListModel.PEER_USER, 1, 11);
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
    shell.appendHistoryMessage(
      11, "Alice", "older", false, false, false, "", "", null, 1700000000
    );
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

  private static byte[] encodeLegacyV8WithMessage() {
    ByteArrayPlus out = new ByteArrayPlus();

    out.append_int(WarmStateSnapshotCodec.MAGIC);
    out.append_int(8);
    out.append_int(0);
    out.append_int(0);
    out.append_int(0);
    out.append_int(0);
    out.append_int(0);
    out.append_int(1);

    out.append_int(reader.DialogListModel.PEER_USER);
    out.append_long(1L);
    out.append_int(1);
    out.append_int(1);
    out.append_int(1);
    out.append_int(11);
    out.append_int(0);
    appendString(out, "Alice");
    appendString(out, "legacy");
    appendString(out, "");
    appendString(out, "");
    out.append_int(0);
    out.append_long(0L);
    out.append_long(0L);
    out.append_int(0);
    out.append_int(0);
    appendString(out, "");
    appendString(out, "");
    out.append_int(0);

    out.append_int(reader.DialogListModel.PEER_UNKNOWN);
    out.append_long(0L);
    out.append_int(compose.ComposeStateModel.STATUS_IDLE);
    out.append_long(0L);
    out.append_int(0);
    out.append_int(0);
    out.append_int(0);
    appendString(out, "");
    appendString(out, "");
    appendString(out, "");
    out.append_int(compose.ComposeStateModel.CONTEXT_NONE);
    out.append_int(0);
    out.append_int(reader.DialogListModel.PEER_UNKNOWN);
    out.append_long(0L);
    out.append_int(0);
    appendString(out, "");
    appendString(out, "");

    out.append_int(0);
    out.append_int(0);
    out.append_int(0);
    out.append_int(reader.DialogListModel.PEER_UNKNOWN);
    out.append_long(0L);
    out.append_int(0);
    out.append_int(0);
    appendString(out, "");
    appendString(out, "");
    appendString(out, "");
    out.append_long(0L);
    return out.toByteArray();
  }

  private static byte[] encodeCurrentWithMessageDate(int dateSeconds) {
    byte[] legacy = encodeLegacyV8WithMessage();
    byte[] current = new byte[legacy.length + 4];
    int dateOffset = 64;

    System.arraycopy(legacy, 0, current, 0, dateOffset);
    current[4] = (byte)WarmStateSnapshotCodec.VERSION;
    current[dateOffset] = (byte)dateSeconds;
    current[dateOffset + 1] = (byte)(dateSeconds >> 8);
    current[dateOffset + 2] = (byte)(dateSeconds >> 16);
    current[dateOffset + 3] = (byte)(dateSeconds >> 24);
    System.arraycopy(
      legacy,
      dateOffset,
      current,
      dateOffset + 4,
      legacy.length - dateOffset
    );
    return current;
  }

  private static byte[] downgradeCurrentToV9(byte[] current) {
    int dialogCount = Decode.Little.int_decode(current, 12);
    byte[] legacy = new byte[current.length - (dialogCount * 8)];
    int sourceStart = 0;
    int targetOffset = 0;
    int dialogOffset = 32;

    for (int i = 0; i < dialogCount; ++i) {
      int readWatermarkOffset = dialogOffset + 28;
      int segmentLength = readWatermarkOffset - sourceStart;
      int titleOffset = dialogOffset + 40;
      int titleLength = Decode.Little.int_decode(current, titleOffset);
      int previewOffset = titleOffset + 4 + (titleLength * 4);
      int previewLength = Decode.Little.int_decode(current, previewOffset);

      System.arraycopy(current, sourceStart, legacy, targetOffset, segmentLength);
      targetOffset += segmentLength;
      sourceStart = readWatermarkOffset + 8;
      dialogOffset = previewOffset + 4 + (previewLength * 4);
    }
    System.arraycopy(
      current,
      sourceStart,
      legacy,
      targetOffset,
      current.length - sourceStart
    );
    legacy[4] = 9;
    return legacy;
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
      expect(decoded.dialogs[0].readInboxMaxId, 10);
      expect(decoded.dialogs[0].readOutboxMaxId, 11);
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
      expect(decoded.historySlots[0].messages[0].dateSeconds, 1700000000);
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
      expect(restored.reader.history.items[0].dateSeconds, 1700000000);
      expect(restored.reader.history.items[1].pending, true);
      expect(restored.compose.text, "draft warm state");
      expect(restored.outbound.phase, OutboundRecoveryState.PHASE_IN_FLIGHT);
      expect(restored.outboundSummary, "TEXT SEND");
      expect(restored.activeDetail, "Warm reader cache restored while reconnecting.");
    }
  }

  public static class IncoherentWarmOutboundFailsClosedTest extends Test {
    public String label() {
      return "Warm outbound retry requires matching peer and nonzero random id";
    }

    public void test() throws TestFailureException {
      WarmStateSnapshot mismatchedPeer = buildWarmShell().captureWarmState();
      WarmStateSnapshot missingRandomId = buildWarmShell().captureWarmState();
      WarmStateSnapshot mismatchedPayload = buildWarmShell().captureWarmState();
      AppShellModel restoredPeer = new AppShellModel();
      AppShellModel restoredRandom = new AppShellModel();
      AppShellModel restoredPayload = new AppShellModel();

      mismatchedPeer.outbound.peerId = 2L;
      restoredPeer.applyWarmState(mismatchedPeer, true);
      expect(restoredPeer.hasRecoverableOutboundSend(), false);
      expect(restoredPeer.isOutboundSendLocked(), false);
      expect(restoredPeer.compose.status, compose.ComposeStateModel.STATUS_FAILED);
      expect(restoredPeer.compose.text, "draft warm state");
      expect(restoredPeer.outbound.phase, OutboundRecoveryState.PHASE_FAILED);
      expect(restoredPeer.reader.history.count, 1);
      expect(restoredPeer.activeDetail, AppShellModel.OUTBOUND_RECOVERY_INVALID_DETAIL);

      missingRandomId.composeLastRandomId = 0;
      restoredRandom.applyWarmState(missingRandomId, true);
      expect(restoredRandom.hasRecoverableOutboundSend(), false);
      expect(restoredRandom.isOutboundSendLocked(), false);
      expect(restoredRandom.compose.status, compose.ComposeStateModel.STATUS_FAILED);
      expect(restoredRandom.compose.text, "draft warm state");
      expect(restoredRandom.outbound.phase, OutboundRecoveryState.PHASE_FAILED);
      expect(restoredRandom.reader.history.count, 1);
      expect(restoredRandom.activeDetail, AppShellModel.OUTBOUND_RECOVERY_INVALID_DETAIL);

      mismatchedPayload.outbound.detail += 1;
      restoredPayload.applyWarmState(mismatchedPayload, true);
      expect(restoredPayload.hasRecoverableOutboundSend(), false);
      expect(restoredPayload.isOutboundSendLocked(), false);
      expect(restoredPayload.compose.status, compose.ComposeStateModel.STATUS_FAILED);
      expect(restoredPayload.compose.text, "draft warm state");
      expect(restoredPayload.outbound.phase, OutboundRecoveryState.PHASE_FAILED);
      expect(restoredPayload.reader.history.count, 1);
      expect(restoredPayload.activeDetail, AppShellModel.OUTBOUND_RECOVERY_INVALID_DETAIL);
    }
  }

  public static class OutboundResumeWaitsForBothRefreshCallbacksTest extends Test {
    public String label() {
      return "Outbound resume waits for dialog, history, and sync callbacks in either order";
    }

    public void test() throws TestFailureException {
      expect(OutboundRecoveryState.resumePrerequisitesReady(
        false, false, false, false, false, false, false, false
      ), false);
      expect(OutboundRecoveryState.resumePrerequisitesReady(
        true, true, false, false, false, false, false, false
      ), false);
      expect(OutboundRecoveryState.resumePrerequisitesReady(
        true, false, true, false, false, false, false, false
      ), false);
      expect(OutboundRecoveryState.resumePrerequisitesReady(
        true, false, false, true, false, false, false, false
      ), false);
      expect(OutboundRecoveryState.resumePrerequisitesReady(
        true, false, false, false, true, false, false, false
      ), false);
      expect(OutboundRecoveryState.resumePrerequisitesReady(
        true, false, false, false, false, true, false, false
      ), false);
      expect(OutboundRecoveryState.resumePrerequisitesReady(
        true, false, false, false, false, false, true, false
      ), false);
      expect(OutboundRecoveryState.resumePrerequisitesReady(
        true, false, false, false, false, false, false, true
      ), false);
      expect(OutboundRecoveryState.resumePrerequisitesReady(
        true, false, false, false, false, false, false, false
      ), true);
      expect(OutboundRecoveryState.shouldRequestDifferenceBeforeResume(true, true), true);
      expect(OutboundRecoveryState.shouldRequestDifferenceBeforeResume(true, false), false);
      expect(OutboundRecoveryState.shouldRequestDifferenceBeforeResume(false, true), false);
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
      expect(decoded.historySlots[0].messages[0].dateSeconds, 0);
      expect(decoded.composeText, "draft warm state");
      expect(decoded.outbound.phase, OutboundRecoveryState.PHASE_IN_FLIGHT);
      expect(decoded.loginPhoneNumber, "");
      expect(decoded.loginCodeSentAtMillis, 0L);
    }
  }

  public static class PreviousWarmStateVersionDefaultsMessageDateTest extends Test {
    public String label() {
      return "Warm state version 8 remains readable with unknown message dates";
    }

    public void test() throws TestFailureException {
      WarmStateSnapshot decoded = new WarmStateSnapshot();

      expect(WarmStateSnapshotCodec.decode(decoded, encodeLegacyV8WithMessage()), true);
      expect(decoded.version, 8);
      expect(decoded.historySlotCount, 1);
      expect(decoded.historySlots[0].messageCount, 1);
      expect(decoded.historySlots[0].messages[0].messageId, 11);
      expect(decoded.historySlots[0].messages[0].text, "legacy");
      expect(decoded.historySlots[0].messages[0].dateSeconds, 0);
    }
  }

  public static class Version9WarmStateDefaultsReadWatermarksTest extends Test {
    public String label() {
      return "Warm state version 9 remains readable with unknown read watermarks";
    }

    public void test() throws TestFailureException {
      WarmStateSnapshot decoded = new WarmStateSnapshot();
      byte[] current = WarmStateSnapshotCodec.encode(buildWarmShell().captureWarmState());

      expect(WarmStateSnapshotCodec.decode(
        decoded,
        downgradeCurrentToV9(current)
      ), true);
      expect(decoded.version, 9);
      expect(decoded.dialogCount, 2);
      expect(decoded.dialogs[0].peerId, 1L);
      expect(decoded.dialogs[0].readInboxMaxId, 0);
      expect(decoded.dialogs[0].readOutboxMaxId, 0);
      expect(decoded.historySlots[0].messages[0].dateSeconds, 1700000000);
    }
  }

  public static class TruncatedCurrentWarmStateIsRejectedTest extends Test {
    public String label() {
      return "Warm state version 10 rejects a truncated record";
    }

    public void test() throws TestFailureException {
      byte[] encoded = WarmStateSnapshotCodec.encode(buildWarmShell().captureWarmState());
      byte[] truncated = new byte[encoded.length - 1];

      System.arraycopy(encoded, 0, truncated, 0, truncated.length);
      expect(WarmStateSnapshotCodec.decode(new WarmStateSnapshot(), truncated), false);
    }
  }

  public static class CorruptCurrentWarmStateDateIsRejectedTest extends Test {
    public String label() {
      return "Warm state version 10 rejects a corrupt negative message date atomically";
    }

    public void test() throws TestFailureException {
      WarmStateSnapshot decoded = new WarmStateSnapshot();

      expect(WarmStateSnapshotCodec.decode(decoded, encodeCurrentWithMessageDate(-1)), false);
      expect(decoded.historySlotCount, 0);
      expect(decoded.historySlots[0].messageCount, 0);
    }
  }

  public static class CorruptCurrentDialogReadWatermarkIsRejectedTest extends Test {
    public String label() {
      return "Warm state version 10 rejects a corrupt dialog read watermark atomically";
    }

    public void test() throws TestFailureException {
      WarmStateSnapshot decoded = new WarmStateSnapshot();
      byte[] encoded = WarmStateSnapshotCodec.encode(buildWarmShell().captureWarmState());
      int readOutboxOffset = 64;

      encoded[readOutboxOffset] = (byte)0xff;
      encoded[readOutboxOffset + 1] = (byte)0xff;
      encoded[readOutboxOffset + 2] = (byte)0xff;
      encoded[readOutboxOffset + 3] = (byte)0xff;
      expect(WarmStateSnapshotCodec.decode(decoded, encoded), false);
      expect(decoded.dialogCount, 0);
      expect(decoded.historySlotCount, 0);
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

  public static class WarmStateStoreRoundTripTest extends Test {
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

  public static class OversizedWarmStateIsRejectedTest extends Test {
    public String label() {
      return "Warm state rejects oversized records before parsing";
    }

    public void test() throws TestFailureException {
      expect(
        WarmStateSnapshotCodec.decode(
          new WarmStateSnapshot(),
          new byte[WarmStateSnapshotCodec.MAX_ENCODED_BYTES + 1]
        ),
        false
      );
    }
  }

  public static class NegativeMessageDateIsNotEncodedTest extends Test {
    public String label() {
      return "Warm state refuses to encode a negative message date";
    }

    public void test() throws TestFailureException {
      WarmStateSnapshot snapshot = buildWarmShell().captureWarmState();

      snapshot.historySlots[0].messages[0].dateSeconds = -1;
      expect(WarmStateSnapshotCodec.encode(snapshot) == null, true);
    }
  }

  public static class NegativeDialogReadWatermarkIsNotEncodedTest extends Test {
    public String label() {
      return "Warm state refuses to encode a negative dialog read watermark";
    }

    public void test() throws TestFailureException {
      WarmStateSnapshot snapshot = buildWarmShell().captureWarmState();

      snapshot.dialogs[0].readOutboxMaxId = -1;
      expect(WarmStateSnapshotCodec.encode(snapshot) == null, true);
    }
  }

  public static class SecretChatPendingKeyIsBoundedTest extends Test {
    public String label() {
      return "Secret-chat warm state rejects oversized pending key material";
    }

    public void test() throws TestFailureException {
      SecretChatDirectory directory = new SecretChatDirectory(1);
      expect(
        directory.upsert(
          1,
          reader.DialogListModel.PEER_USER,
          2,
          3,
          SecretChatDirectory.STATE_WAITING,
          true,
          4,
          0,
          new byte[SecretChatDirectory.PENDING_KEY_BYTES_MAX + 1],
          SecretChatDirectory.PENDING_KEY_BYTES_MAX + 1
        ),
        false
      );
      expect(directory.count, 0);
    }
  }
}
