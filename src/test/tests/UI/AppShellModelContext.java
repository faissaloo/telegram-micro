package test;

import media.MediaReference;
import reader.DialogListModel;
import session.FailureLog;
import session.ResumeState;
import sync.SyncState;
import ui.AppShellModel;

public class AppShellModelContext {
  public static class AuthorizationMovesShellToDialogsTest extends Test {
    public String label() {
      return "App shell enters the dialogs route after authorization";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();
      shell.onAuthorized();

      expect(shell.authorized, true);
      expect(shell.router.currentScreen, AppShellModel.SCREEN_DIALOGS);
      expect(shell.authSummary, "AUTHORIZED");
      expect(shell.readerSummary, "EMPTY");
    }
  }

  public static class AppendingDialogsUpdatesSummaryAndBudgetTest extends Test {
    public String label() {
      return "App shell updates reader summary and budget after bounded dialog loads";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();
      shell.onAuthorized();
      shell.beginLocalDialogsLoad();
      shell.appendLocalDialog(DialogListModel.PEER_USER, 1, "Alice", "Hello", 2, false);
      shell.finishLocalDialogsLoad();

      expect(shell.reader.dialogs.count, 1);
      expect(shell.readerSummary, "1 DIALOGS");
      expect(shell.lastPlan != null, true);
      expect(shell.lastPlan.requiredBytes > 0, true);
    }
  }

  public static class DialogsUnchangedKeepsSummaryTest extends Test {
    public String label() {
      return "App shell keeps dialog state when Telegram reports dialogs unchanged";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();
      shell.onAuthorized();
      shell.appendLocalDialog(DialogListModel.PEER_USER, 1, "Alice", "Hello", 2, false);
      shell.beginDialogsLoad();
      shell.appendLocalDialog(DialogListModel.PEER_USER, 1, "Alice", "Hello", 2, false);
      shell.finishDialogsUnchanged();

      expect(shell.reader.dialogs.count, 1);
      expect(shell.readerSummary, "1 DIALOGS");
      expect(shell.activeDetail, "Dialogs unchanged.");
    }
  }

  public static class HistoryUnchangedKeepsSelectionDetailTest extends Test {
    public String label() {
      return "App shell keeps history detail when Telegram reports history unchanged";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();
      shell.onAuthorized();
      shell.appendLocalDialog(DialogListModel.PEER_USER, 1, "Alice", "Hello", 2, false);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);
      shell.beginHistoryLoadForSelected();
      shell.appendHistoryMessage(10, "Alice", "Hello", false, false, false);
      shell.finishHistoryUnchanged();

      expect(shell.reader.history.count, 1);
      expect(shell.reader.historyLoaded, true);
      expect(shell.activeDetail, "History unchanged for Alice.");
    }
  }

  public static class ResumeDetectedMovesShellToStatusTest extends Test {
    public String label() {
      return "Saved authorization resumes into the observable status shell first";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onResumeDetected();

      expect(shell.authorized, true);
      expect(shell.authSummary, "RESTORING");
      expect(shell.sessionSummary, "RESTORING");
      expect(shell.router.currentScreen, AppShellModel.SCREEN_STATUS);
      expect(shell.readerSummary, "EMPTY");
    }
  }

  public static class ResumeFallbackLocksReaderTest extends Test {
    public String label() {
      return "Clearing a saved session returns the shell to a locked reader";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onResumeDetected();
      shell.onResumeFallbackToLogin("Saved auth rejected.");

      expect(shell.authorized, false);
      expect(shell.authSummary, "WAIT");
      expect(shell.readerSummary, "LOCKED");
      expect(shell.sessionSummary, "HANDSHAKE PENDING");
      expect(shell.activeDetail, "Saved auth rejected.");
    }
  }

  public static class ResumeSummaryReflectsRuntimeStateTest extends Test {
    public String label() {
      return "App shell exposes compact resume state status";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();
      ResumeState state = new ResumeState();

      state.dcId = 4;
      state.noteDisconnect(401);
      shell.onResumeStateChanged(state);

      expect(shell.resumeSummary, "RECONNECT DC 4 x1");
    }
  }

  public static class FailureSummaryUsesLatestFailureTest extends Test {
    public String label() {
      return "App shell surfaces the latest bounded failure entry";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();
      FailureLog log = new FailureLog();

      log.push(FailureLog.CATEGORY_STORAGE, FailureLog.SEVERITY_WARN, -303, 1);
      log.push(FailureLog.CATEGORY_READER, FailureLog.SEVERITY_ERROR, -401, 214);
      shell.onFailureLogChanged(log);

      expect(shell.failureSummary, "READER -401");
      expect(shell.failureDetail, "ERROR #1 d214 (2 stored)");
    }
  }

  public static class SyncSummaryReflectsUpdatesCatchUpStateTest extends Test {
    public String label() {
      return "App shell exposes compact updates sync state";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();
      SyncState state = new SyncState();

      state.applyUpdatesState(new mtproto.recieve.updates.RecieveUpdatesState(100, 20, 30, 40, 1));
      state.markNeedsDifference();
      shell.onSyncStateChanged(state, false, false);

      expect(shell.syncSummary, "PTS 100 DIRTY");
      expect(shell.syncDetail.indexOf("qts 20") >= 0, true);
    }
  }

  public static class OpeningComposeBindsSelectedDialogTest extends Test {
    public String label() {
      return "App shell opens compose on the selected dialog";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendDialog(DialogListModel.PEER_USER, 1, 0x1122334455667788L, true, 11, "Alice", "Hello", 0, false);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);

      expect(shell.openComposeForSelectedDialog(), true);
      expect(shell.router.currentScreen, AppShellModel.SCREEN_COMPOSE);
      expect(shell.compose.peerId, 1L);
      expect(shell.compose.peerTitle, "Alice");
    }
  }

  public static class BeginComposeSendStagesPendingHistoryTest extends Test {
    public String label() {
      return "App shell stages a pending outbound history row when compose send begins";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendLocalDialog(DialogListModel.PEER_USER, 1, "Alice", "Hello", 0, false);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);

      expect(shell.beginComposeSend("hello", 99L), true);
      expect(shell.router.currentScreen, AppShellModel.SCREEN_HISTORY);
      expect(shell.compose.statusName(), "SENDING");
      expect(shell.reader.history.count, 1);
      expect(shell.reader.history.items[0].pending, true);
      expect(shell.reader.history.items[0].text, "hello");
    }
  }

  public static class ResumeAuthorizationPreservesWarmShellStateTest extends Test {
    public String label() {
      return "Resume authorization preserves the restored warm shell state";
    }

    public void test() throws TestFailureException {
      AppShellModel source = new AppShellModel();
      AppShellModel shell = new AppShellModel();
      session.WarmStateSnapshot snapshot;

      source.onAuthorized();
      source.appendDialog(DialogListModel.PEER_USER, 1, 0x1122334455667788L, true, 11, "Alice", "Hello", 0, false);
      source.finishLocalDialogsLoad();
      source.reader.peers.upsertIdentity(DialogListModel.PEER_USER, 1, 0x1122334455667788L, true, false, "Alice", "alice");
      source.selectDialog(0);
      source.beginComposeSend("draft", 99L);
      snapshot = source.captureWarmState();

      shell.onResumeDetected();
      shell.applyWarmState(snapshot, true);
      shell.onResumeAuthorized();

      expect(shell.authSummary, "AUTHORIZED");
      expect(shell.reader.dialogs.count, 1);
      expect(shell.reader.peers.find(DialogListModel.PEER_USER, 1).username, "alice");
      expect(shell.reader.history.count, 1);
      expect(shell.compose.text, "draft");
      expect(shell.outboundSummary, "TEXT SEND");
    }
  }

  public static class InterruptedComposeCanBeResumedTest extends Test {
    public String label() {
      return "App shell can keep an interrupted outbound message in resumable state";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendLocalDialog(DialogListModel.PEER_USER, 1, "Alice", "Hello", 0, false);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);
      shell.beginComposeSend("draft", 99L);

      expect(shell.hasRecoverableOutboundSend(), true);
      expect(shell.resumeInterruptedComposeSend(99L), true);
      expect(shell.compose.statusName(), "SENDING");
      expect(shell.outboundSummary, "TEXT SEND");
      expect(shell.activeDetail, "Retrying interrupted outbound message for Alice.");
    }
  }

  public static class InFlightSendKeepsStableComposeWhileBrowsingTest extends Test {
    public String label() {
      return "In-flight send identity survives browsing and blocks compose mutation";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendDialog(DialogListModel.PEER_USER, 1, 0x1111222233334444L, true, 11, "Alice", "Hello", 0, false);
      shell.appendDialog(DialogListModel.PEER_USER, 2, 0x5555666677778888L, true, 12, "Bob", "Hi", 0, false);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);
      expect(shell.beginComposeSend("stable payload", 99L), true);

      expect(shell.selectDialog(1), true);
      expect(shell.selectedDialog().peerId, 2L);
      expect(shell.compose.peerId, 1L);
      expect(shell.compose.text, "stable payload");
      expect(shell.compose.lastRandomId, 99L);
      expect(shell.outbound.peerId, 1L);
      expect(shell.openComposeForSelectedDialog(), false);
      expect(shell.updateComposeDraft("replacement"), false);
      expect(shell.prepareReplyToSelectedMessage(), false);
      expect(shell.prepareForwardSelectedMessage(), false);
      expect(shell.beginComposeSend("second", 100L), false);
      expect(shell.beginComposeForward(100L), false);
      expect(shell.compose.peerId, 1L);
      expect(shell.compose.text, "stable payload");
      expect(shell.compose.lastRandomId, 99L);
      expect(shell.activeDetail, AppShellModel.OUTBOUND_WAIT_DETAIL);
    }
  }

  public static class InterruptedSendResumeIsIdempotentTest extends Test {
    public String label() {
      return "Interrupted reply reuses its identity and recreates one pending row";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendDialog(DialogListModel.PEER_USER, 1, 0x1111222233334444L, true, 11, "Alice", "Hello", 0, false);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);
      shell.beginHistoryLoadForSelected();
      shell.appendHistoryMessage(10, "Alice", "Original", false, false, false);
      shell.finishHistoryLoad();
      expect(shell.prepareReplyToSelectedMessage(), true);
      expect(shell.beginComposeSend("stable payload", 99L), true);

      shell.beginDialogsLoad();
      shell.appendDialog(DialogListModel.PEER_USER, 1, 0x1111222233334444L, true, 11, "Alice", "Hello", 0, false);
      shell.finishDialogsLoad();
      shell.selectDialog(0);
      expect(shell.reader.history.count, 0);
      expect(shell.resumeInterruptedComposeSend(100L), false);
      expect(shell.reader.history.count, 0);
      expect(shell.resumeInterruptedComposeSend(99L), true);
      expect(shell.resumeInterruptedComposeSend(99L), true);
      expect(shell.reader.history.count, 1);
      expect(shell.reader.history.items[0].pending, true);
      expect(shell.reader.history.items[0].text, "stable payload");
      expect(shell.compose.lastRandomId, 99L);
      expect(shell.compose.replyToMessageId, 10);
      expect(shell.reconcileOutboundMessageId(99L, 77), true);
      expect(shell.reconcileOutboundMessageId(99L, 77), false);
      expect(shell.reader.history.count, 1);
      expect(shell.reader.history.items[0].pending, false);
      expect(shell.reader.history.items[0].messageId, 77);
    }
  }

  public static class CatchUpReconcilesInterruptedSendBeforeRetryTest extends Test {
    public String label() {
      return "Catch-up can reconcile an interrupted send before any resend";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendDialog(DialogListModel.PEER_USER, 1, 0x1111222233334444L, true, 11, "Alice", "Hello", 0, false);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);
      expect(shell.beginComposeSend("accepted before disconnect", 99L), true);

      shell.beginDialogsLoad();
      shell.appendDialog(DialogListModel.PEER_USER, 1, 0x1111222233334444L, true, 11, "Alice", "Hello", 0, false);
      shell.finishDialogsLoad();
      shell.selectDialog(0);
      expect(shell.reader.history.count, 0);
      expect(shell.reconcileOutboundMessageId(100L, 77), false);
      expect(shell.reconcileOutboundMessageId(99L, 77), true);
      expect(shell.reader.history.count, 1);
      expect(shell.reader.history.items[0].messageId, 77);
      expect(shell.reader.history.items[0].pending, false);
      expect(shell.reader.history.items[0].text, "accepted before disconnect");
      expect(shell.compose.status, compose.ComposeStateModel.STATUS_SENT);
      expect(shell.outbound.phase, session.OutboundRecoveryState.PHASE_SENT);
    }
  }

  public static class InterruptedForwardResumeIsIdempotentTest extends Test {
    public String label() {
      return "Interrupted forward keeps source, destination, and retry identity";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendDialog(DialogListModel.PEER_USER, 1, 0x1111222233334444L, true, 11, "Alice", "Hello", 0, false);
      shell.appendDialog(DialogListModel.PEER_USER, 2, 0x5555666677778888L, true, 12, "Bob", "Hi", 0, false);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);
      shell.beginHistoryLoadForSelected();
      shell.appendHistoryMessage(10, "Alice", "Forward me", false, false, false);
      shell.finishHistoryLoad();
      expect(shell.prepareForwardSelectedMessage(), true);
      expect(shell.openComposeForPeer(DialogListModel.PEER_USER, 2, "Bob"), true);
      expect(shell.beginComposeForward(99L), true);

      shell.beginDialogsLoad();
      shell.appendDialog(DialogListModel.PEER_USER, 1, 0x1111222233334444L, true, 11, "Alice", "Hello", 0, false);
      shell.appendDialog(DialogListModel.PEER_USER, 2, 0x5555666677778888L, true, 12, "Bob", "Hi", 0, false);
      shell.finishDialogsLoad();
      shell.selectDialog(1);
      expect(shell.resumeInterruptedComposeSend(99L), true);
      expect(shell.resumeInterruptedComposeSend(99L), true);
      expect(shell.compose.peerId, 2L);
      expect(shell.compose.forwardFromPeerId, 1L);
      expect(shell.compose.forwardMessageId, 10);
      expect(shell.compose.lastRandomId, 99L);
      expect(shell.outbound.kind, session.OutboundRecoveryState.KIND_FORWARD);
      expect(shell.reader.history.count, 1);
      expect(shell.reader.history.items[0].pending, true);
      expect(shell.reader.history.items[0].text, "Fwd: Forward me");
    }
  }

  public static class SendCompletionPreservesUnrelatedRouteTest extends Test {
    public String label() {
      return "Outbound completion does not hijack an unrelated user route";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendDialog(DialogListModel.PEER_USER, 1, 0x1111222233334444L, true, 11, "Alice", "Hello", 0, false);
      shell.appendDialog(DialogListModel.PEER_USER, 2, 0x5555666677778888L, true, 12, "Bob", "Hi", 0, false);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);
      shell.beginComposeSend("stable payload", 99L);
      shell.selectDialog(1);
      shell.navigateStatus();

      shell.onComposeSent(77);

      expect(shell.router.currentScreen, AppShellModel.SCREEN_STATUS);
      expect(shell.selectedDialog().peerId, 2L);
      expect(shell.compose.status, compose.ComposeStateModel.STATUS_SENT);
      expect(shell.outbound.phase, session.OutboundRecoveryState.PHASE_SENT);
      expect(shell.outbound.peerId, 1L);
    }
  }

  public static class ReplyActionMovesShellIntoComposeTest extends Test {
    public String label() {
      return "App shell can open compose with reply context for the selected message";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendLocalDialog(DialogListModel.PEER_USER, 1, "Alice", "Hello", 0, false);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);
      shell.beginHistoryLoadForSelected();
      shell.appendHistoryMessage(10, "Alice", "Hello there", false, false, false);
      shell.finishHistoryLoad();

      expect(shell.prepareReplyToSelectedMessage(), true);
      expect(shell.router.currentScreen, AppShellModel.SCREEN_COMPOSE);
      expect(shell.compose.contextMode, compose.ComposeStateModel.CONTEXT_REPLY);
      expect(shell.compose.replyToMessageId, 10);
      expect(shell.compose.contextLabel, "Alice");
    }
  }

  public static class ForwardActionStagesRealForwardContextTest extends Test {
    public String label() {
      return "App shell can stage a real forward context from the selected message";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendLocalDialog(DialogListModel.PEER_USER, 1, "Alice", "Hello", 0, false);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);
      shell.beginHistoryLoadForSelected();
      shell.appendHistoryMessage(10, "Alice", "Hello there", false, false, false);
      shell.finishHistoryLoad();

      expect(shell.prepareForwardSelectedMessage(), true);
      expect(shell.router.currentScreen, AppShellModel.SCREEN_COMPOSE);
      expect(shell.compose.contextMode, compose.ComposeStateModel.CONTEXT_FORWARD);
      expect(shell.compose.forwardMessageId, 10);
      expect(shell.compose.contextPreview, "Hello there");
      expect(shell.compose.canSend(), true);
    }
  }

  public static class ForwardContextSurvivesComposePeerSwitchTest extends Test {
    public String label() {
      return "App shell keeps forward context when switching the destination peer";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendDialog(DialogListModel.PEER_USER, 1, 0x1111222233334444L, true, 11, "Alice", "Hello", 0, false);
      shell.finishLocalDialogsLoad();
      shell.reader.peers.upsertIdentity(DialogListModel.PEER_CHANNEL, 9, 0x9999888877776666L, true, false, "News", "news");
      shell.selectDialog(0);
      shell.beginHistoryLoadForSelected();
      shell.appendHistoryMessage(10, "Alice", "Hello there", false, false, false);
      shell.finishHistoryLoad();

      expect(shell.prepareForwardSelectedMessage(), true);
      expect(shell.openComposeForPeer(DialogListModel.PEER_CHANNEL, 9, "News"), true);
      expect(shell.compose.peerKind, DialogListModel.PEER_CHANNEL);
      expect(shell.compose.peerId, 9L);
      expect(shell.compose.contextMode, compose.ComposeStateModel.CONTEXT_FORWARD);
      expect(shell.compose.forwardMessageId, 10);
      expect(shell.activeDetail, "Forward the selected message to News.");
    }
  }

  public static class BeginComposeForwardStagesPendingHistoryTest extends Test {
    public String label() {
      return "App shell stages a pending bounded history row when forwarding";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendDialog(DialogListModel.PEER_USER, 1, 0x1111222233334444L, true, 11, "Alice", "Hello", 0, false);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);
      shell.beginHistoryLoadForSelected();
      shell.appendHistoryMessage(10, "Alice", "Hello there", false, false, false);
      shell.finishHistoryLoad();

      expect(shell.prepareForwardSelectedMessage(), true);
      expect(shell.beginComposeForward(99L), true);
      expect(shell.router.currentScreen, AppShellModel.SCREEN_HISTORY);
      expect(shell.compose.statusName(), "SENDING");
      expect(shell.reader.history.count, 2);
      expect(shell.reader.history.items[1].pending, true);
      expect(shell.reader.history.items[1].text, "Fwd: Hello there");
      expect(shell.outboundSummary, "FORWARD SEND");
    }
  }

  public static class LinkMediaUsesExternalTargetPolicyTest extends Test {
    public String label() {
      return "App shell exposes external-open policy for selected link media";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendLocalDialog(DialogListModel.PEER_USER, 1, "Alice", "LINK example.com", 0, true);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);
      shell.beginHistoryLoadForSelected();
      shell.appendHistoryMessage(11, "Alice", "www.example.com/test", false, false, true, "LINK", "example.com/test");
      shell.finishHistoryLoad();

      expect(shell.canViewSelectedMessageMedia(), true);
      expect(shell.selectedMessageMediaPolicy(), "OPEN");
      expect(shell.canOpenSelectedMessageMediaExternally(), true);
      expect(shell.selectedMessageMediaExternalTarget(), "https://www.example.com/test");
    }
  }

  public static class MediaPlanningQueuesSelectedMessageTest extends Test {
    public String label() {
      return "App shell can queue selected media for explicit save";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendLocalDialog(DialogListModel.PEER_USER, 1, "Alice", "PHOTO 640x400", 0, true);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);
      shell.beginHistoryLoadForSelected();
      MediaReference ref = new MediaReference();
      ref.setPhoto(0x12345678L, 0x1122334455667788L, 2, new byte[] {1, 2, 3, 4}, 4, "x");
      shell.appendHistoryMessage(11, "Alice", "", false, false, true, "PHOTO", "640x400", ref);
      shell.finishHistoryLoad();

      expect(shell.canPlanSelectedMessageMedia(), true);
      expect(shell.selectedMessageMediaPolicy(), "SAVE");
      expect(shell.planSelectedMessageMedia(), true);
      expect(shell.mediaQueue.count, 1);
      expect(shell.mediaSummary, "1 QUEUED");
      expect(shell.isSelectedMessageMediaPlanned(), true);
      expect(shell.mediaDetail.indexOf("PHOTO") >= 0, true);
    }
  }

  public static class MediaQueueCanBeClearedTest extends Test {
    public String label() {
      return "App shell can clear queued media actions";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendLocalDialog(DialogListModel.PEER_USER, 1, "Alice", "PHOTO 640x400", 0, true);
      shell.finishLocalDialogsLoad();
      shell.selectDialog(0);
      shell.beginHistoryLoadForSelected();
      MediaReference ref = new MediaReference();
      ref.setPhoto(0x12345678L, 0x1122334455667788L, 2, new byte[] {1, 2, 3, 4}, 4, "x");
      shell.appendHistoryMessage(11, "Alice", "", false, false, true, "PHOTO", "640x400", ref);
      shell.finishHistoryLoad();

      expect(shell.planSelectedMessageMedia(), true);
      expect(shell.canClearMediaQueue(), true);

      shell.clearMediaQueue();

      expect(shell.mediaQueue.count, 0);
      expect(shell.mediaSummary, "NONE");
      expect(shell.canClearMediaQueue(), false);
    }
  }

  public static class DiscoverySearchFindsCachedPeerTest extends Test {
    public String label() {
      return "App shell discovery can find cached peers without dialogs";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.reader.peers.upsertIdentity(DialogListModel.PEER_USER, 7, 0x1122334455667788L, true, false, "Bob", "bob");

      expect(shell.searchDiscovery("bob"), true);
      expect(shell.router.currentScreen, AppShellModel.SCREEN_DISCOVERY);
      expect(shell.discovery.count, 1);
      expect(shell.discovery.selected().peerId, 7L);
      expect(shell.discovery.selected().hasDialog(), false);
    }
  }

  public static class DiscoveryComposeOpensPeerWithoutDialogTest extends Test {
    public String label() {
      return "App shell discovery can compose to a cached peer without an existing dialog";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.reader.peers.upsertIdentity(DialogListModel.PEER_USER, 7, 0x1122334455667788L, true, false, "Bob", "bob");
      shell.searchDiscovery("bob");

      expect(shell.openComposeForSelectedDiscoveryResult(), true);
      expect(shell.router.currentScreen, AppShellModel.SCREEN_COMPOSE);
      expect(shell.compose.peerKind, DialogListModel.PEER_USER);
      expect(shell.compose.peerId, 7L);
      expect(shell.compose.peerTitle, "Bob");
    }
  }

  public static class BeginComposeSendSupportsPeerWithoutDialogTest extends Test {
    public String label() {
      return "App shell can stage outbound history for a composed peer without an existing dialog";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.reader.peers.upsertIdentity(DialogListModel.PEER_USER, 7, 0x1122334455667788L, true, false, "Bob", "bob");
      shell.openComposeForPeer(DialogListModel.PEER_USER, 7, "Bob");

      expect(shell.beginComposeSend("hello", 99L), true);
      expect(shell.router.currentScreen, AppShellModel.SCREEN_HISTORY);
      expect(shell.reader.historyPeerKind, DialogListModel.PEER_USER);
      expect(shell.reader.historyPeerId, 7L);
      expect(shell.reader.history.count, 1);
      expect(shell.selectedDialogTitle(), "Bob");
    }
  }

  public static class PeerScreenBindsChannelActionsTest extends Test {
    public String label() {
      return "App shell peer screen exposes channel join and leave capabilities from cached state";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendDialog(DialogListModel.PEER_CHANNEL, 88, 0x1122334455667788L, true, 11, "News", "Latest", 0, false);
      shell.finishLocalDialogsLoad();
      shell.reader.peers.upsertIdentity(DialogListModel.PEER_CHANNEL, 88, 0x1122334455667788L, true, false, "News", "news");
      shell.selectDialog(0);

      expect(shell.openPeerForSelectedDialog(), true);
      expect(shell.router.currentScreen, AppShellModel.SCREEN_PEER);
      expect(shell.peer.peerId, 88L);
      expect(shell.peer.hasDialog, true);
      expect(shell.peer.canLeave, true);
      expect(shell.peer.canJoin, false);
      expect(shell.peer.title, "News");
    }
  }

  public static class PeerMembershipChangeUpdatesChannelCapabilitiesTest extends Test {
    public String label() {
      return "App shell peer state updates join and leave capabilities explicitly";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendDialog(DialogListModel.PEER_CHANNEL, 88, 0x1122334455667788L, true, 11, "News", "Latest", 0, false);
      shell.finishLocalDialogsLoad();
      shell.reader.peers.upsertIdentity(DialogListModel.PEER_CHANNEL, 88, 0x1122334455667788L, true, false, "News", "news");
      shell.selectDialog(0);
      shell.openPeerForSelectedDialog();

      shell.notePeerMembershipChanged(false, "Left channel.");
      expect(shell.peer.hasDialog, false);
      expect(shell.peer.canJoin, true);
      expect(shell.peer.canLeave, false);
      expect(shell.peer.summary, "LEFT");

      shell.notePeerMembershipChanged(true, "Joined channel.");
      expect(shell.peer.hasDialog, true);
      expect(shell.peer.canJoin, false);
      expect(shell.peer.canLeave, true);
      expect(shell.peer.summary, "JOINED");
    }
  }

  public static class PeerSecretReadyTracksChatIdTest extends Test {
    public String label() {
      return "App shell peer state tracks secret chat request results";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.reader.peers.upsertIdentity(DialogListModel.PEER_USER, 7, 0x1122334455667788L, true, false, "Bob", "bob");
      shell.searchDiscovery("bob");
      shell.openPeerForSelectedDiscoveryResult();
      shell.noteSecretChatPending("Requesting secret chat...");
      shell.noteSecretChatReady(55, 2, "Secret chat requested.");

      expect(shell.peer.secretChatId, 55);
      expect(shell.peer.secretSummary, "REQUESTED #55");
      expect(shell.peer.canSecretChat, false);
      expect(shell.peer.summary, "SECRET");
    }
  }

  public static class OpeningPeerAppliesExistingSecretRegistryStateTest extends Test {
    public String label() {
      return "App shell peer route restores existing secret chat state from bounded cache";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.reader.peers.upsertIdentity(DialogListModel.PEER_USER, 7, 0x1122334455667788L, true, false, "Bob", "bob");
      shell.reader.secretChats.upsert(55, DialogListModel.PEER_USER, 7L, 0x9988776655443322L, reader.SecretChatDirectory.STATE_WAITING, true, 1700000000);
      shell.searchDiscovery("bob");

      expect(shell.openPeerForSelectedDiscoveryResult(), true);
      expect(shell.peer.secretChatId, 55);
      expect(shell.peer.secretSummary, "WAITING #55");
      expect(shell.peer.canSecretChat, false);
      expect(shell.peer.canAcceptSecretChat, false);
    }
  }

  public static class OpeningPeerAppliesInboundSecretRequestStateTest extends Test {
    public String label() {
      return "App shell peer route exposes accepting inbound secret chat requests";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.reader.peers.upsertIdentity(DialogListModel.PEER_USER, 7, 0x1122334455667788L, true, false, "Bob", "bob");
      shell.reader.secretChats.upsert(
        56,
        DialogListModel.PEER_USER,
        7L,
        0x9988776655443322L,
        reader.SecretChatDirectory.STATE_REQUESTED,
        false,
        1700000001,
        0,
        new byte[] {1, 2, 3},
        3
      );
      shell.searchDiscovery("bob");

      expect(shell.openPeerForSelectedDiscoveryResult(), true);
      expect(shell.peer.secretChatId, 56);
      expect(shell.peer.secretSummary, "REQUESTED #56");
      expect(shell.peer.canAcceptSecretChat, true);
      expect(shell.peer.canDiscardSecretChat, true);
    }
  }

  public static class OpeningPeerAppliesClosedSecretStateTest extends Test {
    public String label() {
      return "App shell peer route re-allows starting secret chats after a tracked close";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.reader.peers.upsertIdentity(DialogListModel.PEER_USER, 7, 0x1122334455667788L, true, false, "Bob", "bob");
      shell.reader.secretChats.upsert(
        57,
        DialogListModel.PEER_USER,
        7L,
        0x9988776655443322L,
        reader.SecretChatDirectory.STATE_CLOSED,
        true,
        1700000002
      );
      shell.searchDiscovery("bob");

      expect(shell.openPeerForSelectedDiscoveryResult(), true);
      expect(shell.peer.secretChatId, 57);
      expect(shell.peer.secretSummary, "CLOSED #57");
      expect(shell.peer.canSecretChat, true);
      expect(shell.peer.canAcceptSecretChat, false);
      expect(shell.peer.canDiscardSecretChat, false);
    }
  }

  public static class ComposeRequiresAccessHashForCachedUserPeerTest extends Test {
    public String label() {
      return "App shell blocks composing to cached user peers without access hashes";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.reader.peers.upsertIdentity(DialogListModel.PEER_USER, 7, 0, false, false, "Bob", "bob");

      expect(shell.openComposeForPeer(DialogListModel.PEER_USER, 7, "Bob"), false);
      expect(shell.activeDetail, "This peer is missing a usable access hash.");
    }
  }

  public static class PeerInfoAppliesAboutAndCountTest extends Test {
    public String label() {
      return "App shell can apply bounded Telegram peer info to the peer route";
    }

    public void test() throws TestFailureException {
      AppShellModel shell = new AppShellModel();

      shell.onAuthorized();
      shell.appendDialog(DialogListModel.PEER_CHANNEL, 88, 0x1122334455667788L, true, 11, "News", "Latest", 0, false);
      shell.finishLocalDialogsLoad();
      shell.reader.peers.upsertIdentity(DialogListModel.PEER_CHANNEL, 88, 0x1122334455667788L, true, false, "News", "news");
      shell.selectDialog(0);
      shell.openPeerForSelectedDialog();
      shell.applyPeerInfo("World events and updates.", 1234, true, "Fetched Telegram peer details (1234).");

      expect(shell.peer.about, "World events and updates.");
      expect(shell.peer.participantCountKnown, true);
      expect(shell.peer.participantCount, 1234);
      expect(shell.activeDetail, "Fetched Telegram peer details (1234).");
    }
  }
}
