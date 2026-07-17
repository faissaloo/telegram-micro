package ui;

import compose.ComposeStateModel;
import config.RuntimeDeviceProfile;
import discovery.DiscoveryStateModel;
import media.MediaPlanQueue;
import peer.PeerActionStateModel;
import reader.DialogListModel;
import reader.PeerDirectory;
import reader.ReaderStateModel;
import reader.SecretChatDirectory;
import session.FailureLog;
import session.OutboundRecoveryState;
import session.ResumeState;
import session.WarmStateSnapshot;
import support.WorkingSet;
import sync.SyncState;

public class AppShellModel {
  public static final int SCREEN_DIALOGS = 1;
  public static final int SCREEN_HISTORY = 2;
  public static final int SCREEN_STATUS = 3;
  public static final int SCREEN_COMPOSE = 4;
  public static final int SCREEN_DISCOVERY = 5;
  public static final int SCREEN_PEER = 6;
  public static final int SCREEN_MEDIA = 7;

  public static final int TARGET_RAM_BUDGET_BYTES = 256 * 1024;
  public static final int TARGET_RESERVED_BYTES = 80 * 1024;

  public final ScreenRouter router = new ScreenRouter(SCREEN_DIALOGS);
  public final ReaderStateModel reader = new ReaderStateModel();
  public final ComposeStateModel compose = new ComposeStateModel();
  public final DiscoveryStateModel discovery = new DiscoveryStateModel();
  public final PeerActionStateModel peer = new PeerActionStateModel();
  public final MediaPlanQueue mediaQueue = new MediaPlanQueue();
  public final OutboundRecoveryState outbound = new OutboundRecoveryState();
  public final WorkingSet workingSet = new WorkingSet();

  public boolean sessionReady = false;
  public boolean bootstrapReady = false;
  public boolean authorized = false;
  public int currentDc = 0;
  public int endpointCount = 0;
  public String endpoint = "";
  public String authSummary = "WAIT";
  public String readerSummary = "NO DIALOGS";
  public String deviceSummary = RuntimeDeviceProfile.DEFAULT_DEVICE_MODEL;
  public String sessionSummary = "HANDSHAKE PENDING";
  public String bootSummary = "BOOTSTRAP PENDING";
  public String resumeSummary = "IDLE";
  public String syncSummary = "NO STATE";
  public String syncDetail = "Updates catch-up state unavailable.";
  public String failureSummary = "NONE";
  public String failureDetail = "No queued failures.";
  public String mediaSummary = "NONE";
  public String mediaDetail = "No queued media actions.";
  public String outboundSummary = "NONE IDLE";
  public String outboundDetail = "No outbound recovery state.";
  public String activeDetail = "Login before opening the reader shell.";
  public WorkingSet.Plan lastPlan = null;

  public AppShellModel() {
    recomputeWorkingSet();
  }

  public void setRuntimeDeviceProfile(RuntimeDeviceProfile profile) {
    if (profile == null) {
      deviceSummary = RuntimeDeviceProfile.DEFAULT_DEVICE_MODEL;
      return;
    }
    deviceSummary = profile.deviceModel;
  }

  public void onSessionCreated(long elapsedMs) {
    sessionReady = true;
    sessionSummary = "SESSION " + Long.toString(elapsedMs) + "MS";
    activeDetail = "MTProto session established.";
    recomputeWorkingSet();
  }

  public void onBootstrapReady(int currentDc, String endpoint, int endpointCount) {
    bootstrapReady = true;
    this.currentDc = currentDc;
    this.endpoint = endpoint != null ? endpoint : "";
    this.endpointCount = endpointCount;
    bootSummary = "DC " + Integer.toString(currentDc) + " / " + Integer.toString(endpointCount);
    activeDetail = this.endpoint.length() > 0 ? this.endpoint : "Bootstrap completed.";
  }

  public void onAuthorized() {
    authorized = true;
    authSummary = "AUTHORIZED";
    router.navigate(SCREEN_DIALOGS);
    reader.clearAll();
    discovery.clear();
    peer.clear();
    mediaQueue.clear();
    compose.bindPeer(DialogListModel.PEER_UNKNOWN, 0, "No dialog selected");
    outbound.clear();
    updateMediaSummary();
    updateOutboundSummary();
    updateReaderSummary();
    activeDetail = "Reader shell is ready for bounded dialog sync.";
    recomputeWorkingSet();
  }

  public void onResumeAuthorized() {
    authorized = true;
    authSummary = "AUTHORIZED";
    sessionReady = true;
    updateMediaSummary();
    updateReaderSummary();
    updateOutboundSummary();
    activeDetail = "Saved authorization restored.";
    recomputeWorkingSet();
  }

  public void onResumeDetected() {
    authorized = true;
    authSummary = "RESTORING";
    sessionReady = false;
    sessionSummary = "RESTORING";
    router.navigate(SCREEN_STATUS);
    reader.clearAll();
    discovery.clear();
    peer.clear();
    mediaQueue.clear();
    compose.bindPeer(DialogListModel.PEER_UNKNOWN, 0, "No dialog selected");
    outbound.clear();
    updateMediaSummary();
    updateOutboundSummary();
    updateReaderSummary();
    activeDetail = "Restoring saved authorization.";
    recomputeWorkingSet();
  }

  public void onResumeFallbackToLogin(String detail) {
    authorized = false;
    authSummary = "WAIT";
    sessionReady = false;
    sessionSummary = "HANDSHAKE PENDING";
    router.navigate(SCREEN_STATUS);
    reader.clearAll();
    discovery.clear();
    peer.clear();
    mediaQueue.clear();
    compose.bindPeer(DialogListModel.PEER_UNKNOWN, 0, "No dialog selected");
    outbound.clear();
    updateMediaSummary();
    updateOutboundSummary();
    updateReaderSummary();
    activeDetail = detail != null ? detail : "Saved authorization was cleared.";
    recomputeWorkingSet();
  }

  public void onResumeStateChanged(ResumeState state) {
    StringBuffer summary = new StringBuffer();

    if (state == null) {
      resumeSummary = "IDLE";
      return;
    }

    summary.append(state.phaseName());
    if (state.dcId > 0) {
      summary.append(" DC ");
      summary.append(Integer.toString(state.dcId));
    }
    if (state.reconnectAttempts > 0) {
      summary.append(" x");
      summary.append(Integer.toString(state.reconnectAttempts));
    }
    resumeSummary = summary.toString();
  }

  public void onSyncStateChanged(SyncState state, boolean requestingState, boolean requestingDifference) {
    StringBuffer summary = new StringBuffer();
    StringBuffer detail = new StringBuffer();

    if (requestingState) {
      syncSummary = "STATE...";
    } else if (requestingDifference) {
      syncSummary = "DIFF...";
    } else if (state == null || !state.hasState) {
      syncSummary = "NO STATE";
    } else {
      summary.append("PTS ");
      summary.append(Integer.toString(state.pts));
      if (state.needsDifference) {
        summary.append(" DIRTY");
      }
      if (state.differenceTooLong) {
        summary.append(" LONG");
      }
      syncSummary = summary.toString();
    }

    if (state == null || !state.hasState) {
      syncDetail = requestingState
        ? "Requesting updates.State seed."
        : "Updates catch-up state unavailable.";
      return;
    }

    detail.append("qts ");
    detail.append(Integer.toString(state.qts));
    detail.append(" date ");
    detail.append(Integer.toString(state.date));
    detail.append(" seq ");
    detail.append(Integer.toString(state.seq));
    detail.append(" unread ");
    detail.append(Integer.toString(state.unreadCount));
    if (state.ptsLimitHint > 0 || state.ptsTotalLimitHint > 0 || state.qtsLimitHint > 0) {
      detail.append(" hints ");
      detail.append(Integer.toString(state.ptsLimitHint));
      detail.append("/");
      detail.append(Integer.toString(state.ptsTotalLimitHint));
      detail.append("/");
      detail.append(Integer.toString(state.qtsLimitHint));
    }
    syncDetail = detail.toString();
  }

  public void onFailureLogChanged(FailureLog log) {
    FailureLog.Entry latest;
    StringBuffer detail = new StringBuffer();

    if (log == null) {
      failureSummary = "NONE";
      failureDetail = "No queued failures.";
      return;
    }

    latest = log.latest();
    if (latest == null) {
      failureSummary = "NONE";
      failureDetail = "No queued failures.";
      return;
    }

    failureSummary = FailureLog.categoryName(latest.category) + " " + Integer.toString(latest.code);
    detail.append(FailureLog.severityName(latest.severity));
    detail.append(" #");
    detail.append(Integer.toString(latest.sequence));
    if (latest.detail != 0) {
      detail.append(" d");
      detail.append(Integer.toString(latest.detail));
    }
    if (log.count > 1) {
      detail.append(" (");
      detail.append(Integer.toString(log.count));
      detail.append(" stored)");
    }
    failureDetail = detail.toString();
  }

  public void onReaderLoading() {
    reader.loadingDialogs = true;
    updateReaderSummary();
  }

  public void onReaderEmpty() {
    reader.loadingDialogs = false;
    updateReaderSummary();
  }

  public void beginDialogsLoad() {
    reader.beginDialogsLoad();
    updateReaderSummary();
    activeDetail = "Loading dialogs...";
  }

  public boolean appendDialog(
    int peerKind,
    long peerId,
    long accessHash,
    boolean hasAccessHash,
    int topMessageId,
    String title,
    String preview,
    int unreadCount,
    boolean hasMedia
  ) {
    boolean appended = reader.appendDialog(
      peerKind,
      peerId,
      accessHash,
      hasAccessHash,
      topMessageId,
      title,
      preview,
      unreadCount,
      hasMedia
    );
    updateReaderSummary();
    recomputeWorkingSet();
    return appended;
  }

  public void finishDialogsLoad() {
    reader.finishDialogsLoad();
    updateReaderSummary();
    activeDetail = reader.dialogs.count > 0
      ? "Dialogs loaded."
      : "No dialogs returned.";
    recomputeWorkingSet();
  }

  public void finishDialogsUnchanged() {
    reader.loadingDialogs = false;
    updateReaderSummary();
    activeDetail = "Dialogs unchanged.";
    recomputeWorkingSet();
  }

  public void beginHistoryLoadForSelected() {
    DialogListModel.Item selected = reader.selectedDialog();

    if (selected == null) {
      reader.clearHistory();
      activeDetail = "No dialog selected.";
      return;
    }

    reader.beginHistoryLoad(selected.peerKind, selected.peerId);
    router.navigate(SCREEN_HISTORY);
    activeDetail = "Loading history for " + selected.title + "...";
    recomputeWorkingSet();
  }

  public boolean appendHistoryMessage(
    int messageId,
    String sender,
    String text,
    boolean outgoing,
    boolean pending,
    boolean hasMedia
  ) {
    return appendHistoryMessage(messageId, sender, text, outgoing, pending, hasMedia, "", "");
  }

  public boolean appendHistoryMessage(
    int messageId,
    String sender,
    String text,
    boolean outgoing,
    boolean pending,
    boolean hasMedia,
    String mediaLabel,
    String mediaInfo
  ) {
    return appendHistoryMessage(messageId, sender, text, outgoing, pending, hasMedia, mediaLabel, mediaInfo, null);
  }

  public boolean appendHistoryMessage(
    int messageId,
    String sender,
    String text,
    boolean outgoing,
    boolean pending,
    boolean hasMedia,
    String mediaLabel,
    String mediaInfo,
    media.MediaReference mediaRef
  ) {
    boolean appended = reader.appendHistoryMessage(
      messageId,
      sender,
      text,
      outgoing,
      pending,
      hasMedia,
      mediaLabel,
      mediaInfo,
      mediaRef
    );
    recomputeWorkingSet();
    return appended;
  }

  public void finishHistoryLoad() {
    reader.finishHistoryLoad();
    activeDetail = reader.history.count > 0
      ? "History loaded for " + selectedDialogTitle() + "."
      : "No recent messages for " + selectedDialogTitle() + ".";
    recomputeWorkingSet();
  }

  public void finishHistoryUnchanged() {
    reader.loadingHistory = false;
    reader.historyLoaded = true;
    activeDetail = "History unchanged for " + selectedDialogTitle() + ".";
    recomputeWorkingSet();
  }

  public void onReaderFailure(String detail) {
    reader.loadingDialogs = false;
    reader.loadingHistory = false;
    updateReaderSummary();
    activeDetail = detail != null ? detail : "Reader sync failed.";
  }

  public void beginLocalDialogsLoad() {
    beginDialogsLoad();
  }

  public boolean appendLocalDialog(int peerKind, long peerId, String title, String preview, int unreadCount, boolean hasMedia) {
    return appendDialog(peerKind, peerId, 0, false, 0, title, preview, unreadCount, hasMedia);
  }

  public void finishLocalDialogsLoad() {
    finishDialogsLoad();
  }

  public void navigateDialogs() {
    router.navigate(SCREEN_DIALOGS);
  }

  public void navigateHistory() {
    router.navigate(SCREEN_HISTORY);
  }

  public void navigateStatus() {
    router.navigate(SCREEN_STATUS);
  }

  public void navigateCompose() {
    router.navigate(SCREEN_COMPOSE);
  }

  public void navigateDiscovery() {
    router.navigate(SCREEN_DISCOVERY);
  }

  public void navigatePeer() {
    router.navigate(SCREEN_PEER);
  }

  public void navigateMedia() {
    router.navigate(SCREEN_MEDIA);
  }

  public void goBack() {
    router.navigate(router.previousScreen);
  }

  public boolean selectDialog(int index) {
    boolean changed = reader.selectDialog(index);
    if (changed) {
      DialogListModel.Item selected = reader.selectedDialog();

      if (selected != null) {
        compose.bindPeer(selected.peerKind, selected.peerId, selected.title);
      }
      noteOutboundSelected();
      updateReaderSummary();
      router.navigate(SCREEN_HISTORY);
      activeDetail = reader.historyLoaded
        ? "Showing cached history for " + selectedDialogTitle() + "."
        : "No cached history for " + selectedDialogTitle() + ".";
      recomputeWorkingSet();
    }
    return changed;
  }

  public boolean focusDialogIndex(int index) {
    boolean changed = reader.selectDialog(index);
    if (changed) {
      activeDetail = "Selected " + selectedDialogTitle() + ".";
    }
    return changed;
  }

  public boolean focusNextDialog() {
    boolean changed = reader.selectNextDialog();
    if (changed) {
      activeDetail = "Selected " + selectedDialogTitle() + ".";
    }
    return changed;
  }

  public boolean focusPreviousDialog() {
    boolean changed = reader.selectPreviousDialog();
    if (changed) {
      activeDetail = "Selected " + selectedDialogTitle() + ".";
    }
    return changed;
  }

  public String selectedDialogTitle() {
    DialogListModel.Item selected = reader.selectedDialog();
    if (selected != null) {
      return selected.title;
    }
    if (reader.historyPeerKind != DialogListModel.PEER_UNKNOWN &&
        reader.historyPeerId != 0) {
      return reader.peers.titleOrFallback(reader.historyPeerKind, reader.historyPeerId);
    }
    if (compose.peerKind != DialogListModel.PEER_UNKNOWN &&
        compose.peerId != 0) {
      return compose.peerTitle;
    }
    return "No dialog selected";
  }

  public DialogListModel.Item selectedDialog() {
    return reader.selectedDialog();
  }

  public reader.MessageListModel.Item selectedMessage() {
    return reader.selectedMessage();
  }

  public boolean canDeleteSelectedMessage() {
    reader.MessageListModel.Item message = reader.selectedMessage();
    return message != null && message.messageId != 0 && !message.pending;
  }

  public boolean canViewSelectedMessageMedia() {
    reader.MessageListModel.Item message = reader.selectedMessage();
    return message != null && message.hasMedia;
  }

  public String selectedMessageMediaSummary() {
    reader.MessageListModel.Item message = reader.selectedMessage();
    return message != null ? message.mediaSummary() : "";
  }

  public String selectedMessageMediaPolicy() {
    reader.MessageListModel.Item message = reader.selectedMessage();

    if (message == null || !message.hasMedia) {
      return "NONE";
    }
    if ("LINK".equals(message.mediaLabel) && selectedMessageMediaExternalTarget().length() > 0) {
      return "OPEN";
    }
    if (message.messageId == 0) {
      return "WAIT";
    }
    if (!message.mediaRef.usable()) {
      return "INFO";
    }
    return "SAVE";
  }

  public String selectedMessageMediaPolicyDetail() {
    reader.MessageListModel.Item message = reader.selectedMessage();
    String target;

    if (message == null || !message.hasMedia) {
      return "No media is selected.";
    }
    if ("LINK".equals(message.mediaLabel)) {
      target = selectedMessageMediaExternalTarget();
      if (target.length() > 0) {
        return "Open this link through the device browser.";
      }
      return "This link preview has no usable browser target yet.";
    }
    if (message.messageId == 0) {
      return "Wait until this media message is fully synced before saving it.";
    }
    if (!message.mediaRef.usable()) {
      return "This media type is metadata-only in the current low-memory build.";
    }
    if ("VOICE".equals(message.mediaLabel) || "AUDIO".equals(message.mediaLabel)) {
      return "Queue audio for explicit save only on low-memory devices.";
    }
    if ("VIDEO".equals(message.mediaLabel)) {
      return "Queue video for explicit save and fetch only on demand.";
    }
    if ("PHOTO".equals(message.mediaLabel) || "IMAGE".equals(message.mediaLabel)) {
      return "Queue image save explicitly instead of auto-fetching it.";
    }
    if ("STICKER".equals(message.mediaLabel) || "EMOJI".equals(message.mediaLabel)) {
      return "Show metadata now and save media payloads only when requested.";
    }
    return "Keep document saves explicit and bounded.";
  }

  public boolean canOpenSelectedMessageMediaExternally() {
    return selectedMessageMediaExternalTarget().length() > 0;
  }

  public String selectedMessageMediaExternalTarget() {
    reader.MessageListModel.Item message = reader.selectedMessage();

    if (message == null || !message.hasMedia || !"LINK".equals(message.mediaLabel)) {
      return "";
    }
    return normalizeExternalTarget(message.text);
  }

  public boolean canPlanSelectedMessageMedia() {
    reader.MessageListModel.Item message = reader.selectedMessage();

    return message != null &&
      message.hasMedia &&
      message.messageId != 0 &&
      message.mediaRef.usable() &&
      !"LINK".equals(message.mediaLabel);
  }

  public boolean isSelectedMessageMediaPlanned() {
    DialogListModel.Item dialog = reader.selectedDialog();
    reader.MessageListModel.Item message = reader.selectedMessage();

    if (dialog == null || message == null || message.messageId == 0) {
      return false;
    }
    return mediaQueue.contains(dialog.peerKind, dialog.peerId, message.messageId);
  }

  public boolean planSelectedMessageMedia() {
    DialogListModel.Item dialog = reader.selectedDialog();
    reader.MessageListModel.Item message = reader.selectedMessage();
    String summary;

    if (dialog == null || message == null || !message.hasMedia) {
      activeDetail = "Select a media message before planning it.";
      return false;
    }
    if ("LINK".equals(message.mediaLabel)) {
      activeDetail = "Links should be opened externally instead of queued.";
      return false;
    }
    if (message.messageId == 0) {
      activeDetail = "This media item is not fully synced yet.";
      return false;
    }
    if (!message.mediaRef.usable()) {
      activeDetail = "This media item does not have a downloadable reference yet.";
      return false;
    }

    summary = message.mediaSummary();
    if (!mediaQueue.enqueue(
        dialog.peerKind,
        dialog.peerId,
        message.messageId,
        dialog.title,
        message.mediaLabel,
        summary.length() > 0 ? summary : message.text,
        message.mediaRef)) {
      activeDetail = "This media item cannot be queued.";
      return false;
    }

    updateMediaSummary();
    activeDetail = isSelectedMessageMediaPlanned()
      ? "Queued " + dialog.title + " media for explicit save."
      : "Media queue unchanged.";
    recomputeWorkingSet();
    return true;
  }

  public boolean canClearMediaQueue() {
    return mediaQueue.count > 0;
  }

  public void clearMediaQueue() {
    mediaQueue.clear();
    updateMediaSummary();
    activeDetail = "Cleared queued media actions.";
    recomputeWorkingSet();
  }

  public String selectedMessageMediaQueueState() {
    DialogListModel.Item dialog = reader.selectedDialog();
    reader.MessageListModel.Item message = reader.selectedMessage();
    MediaPlanQueue.Entry entry;

    if (dialog == null || message == null || message.messageId == 0) {
      return "no";
    }
    entry = mediaQueue.entryFor(dialog.peerKind, dialog.peerId, message.messageId);
    if (entry == null) {
      return "no";
    }
    if (entry.status == MediaPlanQueue.STATUS_QUEUED) {
      return "queued";
    }
    if (entry.status == MediaPlanQueue.STATUS_LOADING) {
      return "loading";
    }
    if (entry.status == MediaPlanQueue.STATUS_READY) {
      return "ready";
    }
    if (entry.status == MediaPlanQueue.STATUS_FAILED) {
      return "failed";
    }
    return "no";
  }

  public String selectedMessageMediaQueueDetail() {
    DialogListModel.Item dialog = reader.selectedDialog();
    reader.MessageListModel.Item message = reader.selectedMessage();
    MediaPlanQueue.Entry entry;

    if (dialog == null || message == null || message.messageId == 0) {
      return "";
    }
    entry = mediaQueue.entryFor(dialog.peerKind, dialog.peerId, message.messageId);
    if (entry == null) {
      return "";
    }
    if (entry.status == MediaPlanQueue.STATUS_LOADING) {
      return Integer.toString(entry.loadedBytes) +
        (entry.expectedBytes > 0 ? "/" + Integer.toString(entry.expectedBytes) : "") +
        " bytes";
    }
    if (entry.status == MediaPlanQueue.STATUS_READY) {
      return Integer.toString(entry.loadedBytes) + " bytes cached";
    }
    return entry.detail != null ? entry.detail : "";
  }

  private static String normalizeExternalTarget(String text) {
    String target = text != null ? text.trim() : "";

    if (target.length() == 0) {
      return "";
    }
    if (startsWithIgnoreCase(target, "http://") || startsWithIgnoreCase(target, "https://")) {
      return target;
    }
    if (startsWithIgnoreCase(target, "www.")) {
      return "https://" + target;
    }
    return "";
  }

  private static boolean startsWithIgnoreCase(String text, String prefix) {
    if (text == null || prefix == null || text.length() < prefix.length()) {
      return false;
    }
    return text.substring(0, prefix.length()).toLowerCase().equals(prefix.toLowerCase());
  }

  public boolean focusNextMessage() {
    boolean changed = reader.selectNextMessage();
    if (changed) {
      activeDetail = "Selected message " +
        Integer.toString(reader.history.selectedIndex + 1) +
        " of " + Integer.toString(reader.history.count) + ".";
    }
    return changed;
  }

  public boolean focusPreviousMessage() {
    boolean changed = reader.selectPreviousMessage();
    if (changed) {
      activeDetail = "Selected message " +
        Integer.toString(reader.history.selectedIndex + 1) +
        " of " + Integer.toString(reader.history.count) + ".";
    }
    return changed;
  }

  public boolean selectLatestMessage() {
    return reader.selectLastMessage();
  }

  public void openDiscovery() {
    discovery.search(reader);
    router.navigate(SCREEN_DISCOVERY);
    activeDetail = discovery.detail;
    recomputeWorkingSet();
  }

  public boolean searchDiscovery(String query) {
    if (!discovery.replaceQuery(query)) {
      activeDetail = "Search query exceeds the bounded discovery limit.";
      return false;
    }
    discovery.search(reader);
    router.navigate(SCREEN_DISCOVERY);
    activeDetail = discovery.detail;
    recomputeWorkingSet();
    return true;
  }

  public boolean selectDiscoveryIndex(int index) {
    boolean changed = discovery.selectIndex(index);

    if (changed) {
      DiscoveryStateModel.Result selected = discovery.selected();
      activeDetail = selected != null
        ? "Selected " + selected.title + "."
        : discovery.detail;
    }
    return changed;
  }

  public DiscoveryStateModel.Result selectedDiscoveryResult() {
    return discovery.selected();
  }

  public boolean openSelectedDiscoveryResult() {
    DiscoveryStateModel.Result selected = discovery.selected();

    if (selected == null) {
      activeDetail = "Select a peer before opening it.";
      return false;
    }
    if (selected.hasDialog()) {
      return selectDialog(selected.dialogIndex);
    }
    if (openComposeForPeer(selected.peerKind, selected.peerId, selected.title)) {
      return true;
    }
    return openPeerFor(
      selected.peerKind,
      selected.peerId,
      selected.dialogIndex,
      selected.title,
      selected.username
    );
  }

  public boolean openComposeForSelectedDiscoveryResult() {
    DiscoveryStateModel.Result selected = discovery.selected();

    if (selected == null) {
      activeDetail = "Select a peer before composing.";
      return false;
    }
    return openComposeForPeer(selected.peerKind, selected.peerId, selected.title);
  }

  public boolean openComposeForPeer(int peerKind, long peerId, String fallbackTitle) {
    DialogListModel.Item dialog;
    PeerDirectory.Item identity;
    int dialogIndex;
    String title = resolvePeerTitle(peerKind, peerId, fallbackTitle);
    boolean preserveForward = compose.hasForwardSource();

    if (peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0) {
      activeDetail = "Select a peer before composing.";
      return false;
    }
    dialogIndex = reader.dialogIndexForPeer(peerKind, peerId);
    dialog = dialogIndex >= 0 ? reader.dialogs.items[dialogIndex] : null;
    identity = reader.peers.find(peerKind, peerId);
    if (peerKind != DialogListModel.PEER_CHAT &&
        (dialog == null || !dialog.hasAccessHash) &&
        (identity == null || !identity.hasAccessHash)) {
      activeDetail = "This peer is missing a usable access hash.";
      return false;
    }

    compose.rebindPeer(peerKind, peerId, title, preserveForward, preserveForward);
    if (!preserveForward) {
      compose.clearContext();
    }
    noteOutboundSelected();
    reader.focusHistoryPeer(peerKind, peerId);
    router.navigate(SCREEN_COMPOSE);
    activeDetail = preserveForward
      ? "Forward the selected message to " + title + "."
      : "Compose a bounded outbound message for " + title + ".";
    recomputeWorkingSet();
    return true;
  }

  public boolean openPeerForSelectedDialog() {
    DialogListModel.Item selected = reader.selectedDialog();

    if (selected != null) {
      return openPeerFor(selected.peerKind, selected.peerId, reader.dialogs.selectedIndex, selected.title, "");
    }
    if (reader.historyPeerKind != DialogListModel.PEER_UNKNOWN && reader.historyPeerId != 0) {
      return openPeerFor(
        reader.historyPeerKind,
        reader.historyPeerId,
        reader.dialogIndexForPeer(reader.historyPeerKind, reader.historyPeerId),
        resolvePeerTitle(reader.historyPeerKind, reader.historyPeerId, compose.peerTitle),
        ""
      );
    }
    if (compose.peerKind != DialogListModel.PEER_UNKNOWN && compose.peerId != 0) {
      return openPeerFor(
        compose.peerKind,
        compose.peerId,
        reader.dialogIndexForPeer(compose.peerKind, compose.peerId),
        compose.peerTitle,
        ""
      );
    }
    activeDetail = "Select a dialog before opening peer actions.";
    return false;
  }

  public boolean openPeerForSelectedDiscoveryResult() {
    DiscoveryStateModel.Result selected = discovery.selected();

    if (selected == null) {
      activeDetail = "Select a peer before opening peer actions.";
      return false;
    }
    return openPeerFor(
      selected.peerKind,
      selected.peerId,
      selected.dialogIndex,
      selected.title,
      selected.username
    );
  }

  public boolean openPeerDialog() {
    if (peer.dialogIndex < 0) {
      activeDetail = "No dialog is cached for this peer yet.";
      return false;
    }
    return selectDialog(peer.dialogIndex);
  }

  public boolean openComposeForSelectedPeer() {
    if (peer.peerKind == DialogListModel.PEER_UNKNOWN || peer.peerId == 0) {
      activeDetail = "Select a peer before composing.";
      return false;
    }
    return openComposeForPeer(peer.peerKind, peer.peerId, peer.title);
  }

  public void notePeerAction(String summary, String detail) {
    if (peer.peerKind == DialogListModel.PEER_UNKNOWN || peer.peerId == 0) {
      return;
    }
    peer.noteAction(summary, detail);
    activeDetail = peer.detail;
  }

  public void notePeerReady(String summary, String detail) {
    if (peer.peerKind == DialogListModel.PEER_UNKNOWN || peer.peerId == 0) {
      return;
    }
    peer.noteReady(summary, detail);
    activeDetail = peer.detail;
  }

  public void notePeerError(String detail) {
    if (peer.peerKind == DialogListModel.PEER_UNKNOWN || peer.peerId == 0) {
      return;
    }
    peer.noteError(detail);
    activeDetail = peer.detail;
  }

  public void noteSecretChatPending(String detail) {
    if (peer.peerKind == DialogListModel.PEER_UNKNOWN || peer.peerId == 0) {
      return;
    }
    peer.noteSecretPending(detail);
    activeDetail = peer.detail;
  }

  public void notePeerMembershipChanged(boolean joined, String detail) {
    if (peer.peerKind == DialogListModel.PEER_UNKNOWN || peer.peerId == 0) {
      return;
    }
    peer.noteMembershipChanged(joined, detail);
    activeDetail = peer.detail;
  }

  public void noteSecretChatReady(int chatId, int secretState, String detail) {
    if (peer.peerKind == DialogListModel.PEER_UNKNOWN || peer.peerId == 0) {
      return;
    }
    peer.noteSecretReady(chatId, secretState, detail);
    activeDetail = peer.detail;
  }

  public void applyPeerInfo(String about, int participantCount, boolean participantCountKnown, String detail) {
    if (peer.peerKind == DialogListModel.PEER_UNKNOWN || peer.peerId == 0) {
      return;
    }
    peer.applyInfo(about, participantCount, participantCountKnown, detail);
    activeDetail = peer.detail;
  }

  private boolean openPeerFor(
    int peerKind,
    long peerId,
    int dialogIndex,
    String fallbackTitle,
    String fallbackUsername
  ) {
    PeerDirectory.Item identity;
    DialogListModel.Item dialog = null;
    boolean hasAccessHash = false;
    boolean minAccessHash = false;
    boolean hasDialog = false;
    String title;
    String username;
    SecretChatDirectory.Item secretChat;

    if (peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0) {
      activeDetail = "Select a peer before opening peer actions.";
      return false;
    }

    if (dialogIndex >= 0 && dialogIndex < reader.dialogs.count) {
      dialog = reader.dialogs.items[dialogIndex];
      hasDialog = dialog.peerKind == peerKind && dialog.peerId == peerId;
    }
    if (!hasDialog) {
      dialogIndex = reader.dialogIndexForPeer(peerKind, peerId);
      if (dialogIndex >= 0) {
        dialog = reader.dialogs.items[dialogIndex];
        hasDialog = true;
      }
    }

    identity = reader.peers.find(peerKind, peerId);
    if (dialog != null && dialog.hasAccessHash) {
      hasAccessHash = true;
    } else if (identity != null && identity.hasAccessHash) {
      hasAccessHash = true;
      minAccessHash = identity.minAccessHash;
    }

    title = resolvePeerTitle(peerKind, peerId, fallbackTitle);
    username = identity != null && identity.username.length() > 0
      ? identity.username
      : (fallbackUsername != null ? fallbackUsername : "");
    peer.bind(
      peerKind,
      peerId,
      dialogIndex,
      hasDialog,
      hasAccessHash,
      minAccessHash,
      title,
      username
    );
    secretChat = reader.secretChats.findByPeer(peerKind, peerId);
    if (secretChat != null) {
      peer.applyExistingSecret(secretChat.chatId, secretChat.state, secretChat.outbound, secretChat.keyFingerprint);
    }
    router.navigate(SCREEN_PEER);
    activeDetail = "Peer actions for " + title + ".";
    recomputeWorkingSet();
    return true;
  }

  private String resolvePeerTitle(int peerKind, long peerId, String fallbackTitle) {
    DialogListModel.Item dialog;
    PeerDirectory.Item identity;
    int dialogIndex;

    dialogIndex = reader.dialogIndexForPeer(peerKind, peerId);
    dialog = dialogIndex >= 0 ? reader.dialogs.items[dialogIndex] : null;
    if (dialog != null && dialog.title != null && dialog.title.length() > 0) {
      return dialog.title;
    }
    identity = reader.peers.find(peerKind, peerId);
    if (identity != null && identity.title != null && identity.title.length() > 0) {
      return identity.title;
    }
    if (fallbackTitle != null && fallbackTitle.length() > 0) {
      return DialogListModel.limit(fallbackTitle, DialogListModel.TITLE_MAX);
    }
    return reader.peers.titleOrFallback(peerKind, peerId);
  }

  public boolean openComposeForSelectedDialog() {
    DialogListModel.Item selected = reader.selectedDialog();

    if (selected == null) {
      activeDetail = "Select a dialog before composing.";
      return false;
    }
    return openComposeForPeer(selected.peerKind, selected.peerId, selected.title);
  }

  public boolean prepareReplyToSelectedMessage() {
    DialogListModel.Item dialog = reader.selectedDialog();
    reader.MessageListModel.Item message = reader.selectedMessage();

    if (dialog == null || message == null) {
      activeDetail = "Select a message before replying.";
      return false;
    }

    compose.bindPeer(dialog.peerKind, dialog.peerId, dialog.title);
    compose.prepareReplyContext(message.messageId, message.sender, message.text);
    router.navigate(SCREEN_COMPOSE);
    activeDetail = "Replying to " + message.sender + ".";
    recomputeWorkingSet();
    return true;
  }

  public boolean prepareForwardSelectedMessage() {
    DialogListModel.Item dialog = reader.selectedDialog();
    reader.MessageListModel.Item message = reader.selectedMessage();
    StringBuffer preview = new StringBuffer();

    if (dialog == null || message == null) {
      activeDetail = "Select a message before forwarding.";
      return false;
    }
    if (message.messageId == 0) {
      activeDetail = "This message cannot be forwarded yet.";
      return false;
    }

    compose.bindPeer(dialog.peerKind, dialog.peerId, dialog.title);
    if (message.text != null && message.text.length() > 0) {
      preview.append(message.text);
    } else if (message.hasMedia) {
      preview.append(message.previewText());
    } else {
      preview.append("(empty)");
    }
    if (!compose.prepareForwardContext(
        dialog.peerKind,
        dialog.peerId,
        message.messageId,
        message.sender,
        preview.toString())) {
      activeDetail = "This message cannot be forwarded yet.";
      return false;
    }
    noteOutboundSelected();
    router.navigate(SCREEN_COMPOSE);
    activeDetail = "Select a destination or send to forward " + message.sender + ".";
    recomputeWorkingSet();
    return true;
  }

  public boolean updateComposeDraft(String text) {
    if (!compose.replaceText(text)) {
      activeDetail = "Draft exceeds the bounded compose limit.";
      return false;
    }
    noteOutboundSelected();
    if (compose.hasForwardSource()) {
      activeDetail = "Forward the selected message to " + compose.peerTitle + ".";
    } else {
      activeDetail = compose.text.length() > 0
        ? "Draft ready for " + compose.peerTitle + "."
        : "Compose a bounded outbound message for " + compose.peerTitle + ".";
    }
    recomputeWorkingSet();
    return true;
  }

  public boolean beginComposeSend(String text, long randomId) {
    int targetPeerKind = compose.peerKind;
    long targetPeerId = compose.peerId;
    String targetTitle = compose.peerTitle;
    int dialogIndex;

    if (targetPeerKind == DialogListModel.PEER_UNKNOWN || targetPeerId == 0) {
      DialogListModel.Item selected = reader.selectedDialog();
      if (selected == null) {
        activeDetail = "Select a dialog or peer before sending.";
        return false;
      }
      targetPeerKind = selected.peerKind;
      targetPeerId = selected.peerId;
      targetTitle = selected.title;
      compose.bindPeer(targetPeerKind, targetPeerId, targetTitle);
    }

    if (!compose.replaceText(text)) {
      activeDetail = "Draft exceeds the bounded compose limit.";
      return false;
    }
    if (!compose.canSend()) {
      activeDetail = "Compose text is empty.";
      return false;
    }
    if (!reader.appendPendingOutboundText(targetPeerKind, targetPeerId, "YOU", compose.text)) {
      activeDetail = "Unable to stage the bounded outbound history entry.";
      return false;
    }
    reader.focusHistoryPeer(targetPeerKind, targetPeerId);
    dialogIndex = reader.dialogIndexForPeer(targetPeerKind, targetPeerId);
    if (dialogIndex >= 0) {
      reader.selectDialog(dialogIndex);
    }

    compose.markSending(randomId);
    outbound.note(
      OutboundRecoveryState.KIND_TEXT,
      OutboundRecoveryState.PHASE_IN_FLIGHT,
      targetPeerKind,
      targetPeerId,
      0,
      compose.text.length(),
      compose.text
    );
    updateOutboundSummary();
    router.navigate(SCREEN_HISTORY);
    activeDetail = "Sending message to " + targetTitle + "...";
    recomputeWorkingSet();
    return true;
  }

  public boolean beginComposeForward(long randomId) {
    int targetPeerKind = compose.peerKind;
    long targetPeerId = compose.peerId;
    String targetTitle = compose.peerTitle;
    StringBuffer label = new StringBuffer();
    int dialogIndex;

    if (!compose.hasForwardSource()) {
      activeDetail = "No forward source is selected.";
      return false;
    }
    if (targetPeerKind == DialogListModel.PEER_UNKNOWN || targetPeerId == 0) {
      activeDetail = "Select a peer before forwarding.";
      return false;
    }

    label.append("Fwd: ");
    label.append(compose.contextPreview != null && compose.contextPreview.length() > 0
      ? compose.contextPreview
      : "(empty)");
    if (!reader.appendPendingOutboundText(targetPeerKind, targetPeerId, "YOU", label.toString())) {
      activeDetail = "Unable to stage the bounded forward entry.";
      return false;
    }
    reader.focusHistoryPeer(targetPeerKind, targetPeerId);
    dialogIndex = reader.dialogIndexForPeer(targetPeerKind, targetPeerId);
    if (dialogIndex >= 0) {
      reader.selectDialog(dialogIndex);
    }

    compose.markSending(randomId);
    outbound.note(
      OutboundRecoveryState.KIND_FORWARD,
      OutboundRecoveryState.PHASE_IN_FLIGHT,
      targetPeerKind,
      targetPeerId,
      0,
      compose.forwardMessageId,
      compose.contextPreview
    );
    updateOutboundSummary();
    router.navigate(SCREEN_HISTORY);
    activeDetail = "Forwarding message to " + targetTitle + "...";
    recomputeWorkingSet();
    return true;
  }

  public void onComposeSent(int messageId) {
    boolean wasForward = compose.hasForwardSource();
    int peerKind = compose.peerKind;
    long peerId = compose.peerId;
    String peerTitle = compose.peerTitle;

    compose.markSent(messageId);
    outbound.note(
      wasForward ? OutboundRecoveryState.KIND_FORWARD : OutboundRecoveryState.KIND_TEXT,
      OutboundRecoveryState.PHASE_SENT,
      peerKind,
      peerId,
      messageId,
      0,
      peerTitle
    );
    updateOutboundSummary();
    activeDetail = wasForward
      ? "Message forwarded to " + peerTitle + "."
      : "Message sent to " + peerTitle + ".";
    router.navigate(SCREEN_HISTORY);
    recomputeWorkingSet();
  }

  public void onComposeFailed(int errorCode, String detail) {
    boolean wasForward = compose.hasForwardSource();

    compose.markFailed(errorCode, detail);
    outbound.note(
      wasForward ? OutboundRecoveryState.KIND_FORWARD : OutboundRecoveryState.KIND_TEXT,
      OutboundRecoveryState.PHASE_FAILED,
      compose.peerKind,
      compose.peerId,
      errorCode,
      compose.text.length(),
      compose.text
    );
    updateOutboundSummary();
    activeDetail = detail != null && detail.length() > 0
      ? detail
      : "Message send failed.";
    router.navigate(SCREEN_COMPOSE);
    recomputeWorkingSet();
  }

  public boolean hasRecoverableOutboundSend() {
    return (outbound.kind == OutboundRecoveryState.KIND_TEXT ||
      outbound.kind == OutboundRecoveryState.KIND_FORWARD) &&
      outbound.phase == OutboundRecoveryState.PHASE_IN_FLIGHT &&
      compose.peerKind != DialogListModel.PEER_UNKNOWN &&
      compose.peerId != 0 &&
      ((compose.text != null && compose.text.length() > 0) || compose.hasForwardSource());
  }

  public boolean resumeInterruptedComposeSend(long randomId) {
    if (!hasRecoverableOutboundSend()) {
      return false;
    }

    compose.markSending(randomId);
    outbound.note(
      compose.hasForwardSource() ? OutboundRecoveryState.KIND_FORWARD : OutboundRecoveryState.KIND_TEXT,
      OutboundRecoveryState.PHASE_IN_FLIGHT,
      compose.peerKind,
      compose.peerId,
      0,
      compose.hasForwardSource() ? compose.forwardMessageId : compose.text.length(),
      compose.hasForwardSource() ? compose.contextPreview : compose.text
    );
    updateOutboundSummary();
    activeDetail = compose.hasForwardSource()
      ? "Retrying interrupted forward for " + compose.peerTitle + "."
      : "Retrying interrupted outbound message for " + compose.peerTitle + ".";
    recomputeWorkingSet();
    return true;
  }

  public void updateReaderSummary() {
    if (!authorized) {
      readerSummary = "LOCKED";
      return;
    }
    if (reader.loadingDialogs) {
      readerSummary = "LOADING";
      return;
    }
    if (reader.dialogs.count == 0) {
      readerSummary = "EMPTY";
      return;
    }
    readerSummary = Integer.toString(reader.dialogs.count) + " DIALOGS";
  }

  public void updateMediaSummary() {
    MediaPlanQueue.Entry latest = mediaQueue.latest();

    if (latest == null) {
      mediaSummary = "NONE";
      mediaDetail = "No queued media actions.";
      return;
    }

    mediaSummary = Integer.toString(mediaQueue.count) + " QUEUED";
    mediaDetail = latest.label + " for " + latest.peerTitle + " | " + mediaQueueStateLabel(latest) +
      (latest.detail != null && latest.detail.length() > 0 ? " | " + latest.detail : "");
  }

  private static String mediaQueueStateLabel(MediaPlanQueue.Entry entry) {
    if (entry == null) {
      return "none";
    }
    if (entry.status == MediaPlanQueue.STATUS_QUEUED) {
      return "queued";
    }
    if (entry.status == MediaPlanQueue.STATUS_LOADING) {
      return "loading";
    }
    if (entry.status == MediaPlanQueue.STATUS_READY) {
      return "ready";
    }
    if (entry.status == MediaPlanQueue.STATUS_FAILED) {
      return "failed";
    }
    return "none";
  }

  public void updateOutboundSummary() {
    outboundSummary = outbound.summary();
    outboundDetail = outbound.detail();
  }

  private void noteOutboundSelected() {
    outbound.note(
      compose.hasForwardSource() ? OutboundRecoveryState.KIND_FORWARD : OutboundRecoveryState.KIND_TEXT,
      (compose.text.length() > 0 || compose.hasForwardSource())
        ? OutboundRecoveryState.PHASE_SELECTED
        : OutboundRecoveryState.PHASE_IDLE,
      compose.peerKind,
      compose.peerId,
      0,
      compose.hasForwardSource() ? compose.forwardMessageId : compose.text.length(),
      compose.hasForwardSource()
        ? compose.contextPreview
        : (compose.text.length() > 0 ? compose.text : compose.peerTitle)
    );
    updateOutboundSummary();
  }

  public WarmStateSnapshot captureWarmState() {
    WarmStateSnapshot snapshot = new WarmStateSnapshot();

    snapshot.route = router.currentScreen;
    reader.exportWarmState(snapshot);
    snapshot.composePeerKind = compose.peerKind;
    snapshot.composePeerId = compose.peerId;
    snapshot.composePeerTitle = compose.peerTitle;
    snapshot.composeText = compose.text;
    snapshot.composeStatus = compose.status;
    snapshot.composeLastRandomId = compose.lastRandomId;
    snapshot.composeLastMessageId = compose.lastMessageId;
    snapshot.composeLastErrorCode = compose.lastErrorCode;
    snapshot.composeLastErrorMessage = compose.lastErrorMessage;
    snapshot.composeDirty = compose.dirty;
    snapshot.composeContextMode = compose.contextMode;
    snapshot.composeReplyToMessageId = compose.replyToMessageId;
    snapshot.composeContextLabel = compose.contextLabel;
    snapshot.composeContextPreview = compose.contextPreview;
    snapshot.composeForwardFromPeerKind = compose.forwardFromPeerKind;
    snapshot.composeForwardFromPeerId = compose.forwardFromPeerId;
    snapshot.composeForwardMessageId = compose.forwardMessageId;
    snapshot.outbound.copyFrom(outbound);
    return snapshot;
  }

  public void applyWarmState(WarmStateSnapshot snapshot, boolean resumePending) {
    if (snapshot == null) {
      return;
    }

    reader.importWarmState(snapshot);
    compose.bindPeer(snapshot.composePeerKind, snapshot.composePeerId, snapshot.composePeerTitle);
    compose.text = snapshot.composeText != null ? snapshot.composeText : "";
    compose.status = snapshot.composeStatus;
    compose.lastRandomId = snapshot.composeLastRandomId;
    compose.lastMessageId = snapshot.composeLastMessageId;
    compose.lastErrorCode = snapshot.composeLastErrorCode;
    compose.lastErrorMessage = snapshot.composeLastErrorMessage != null ? snapshot.composeLastErrorMessage : "";
    compose.dirty = snapshot.composeDirty;
    compose.contextMode = snapshot.composeContextMode;
    compose.replyToMessageId = snapshot.composeReplyToMessageId;
    compose.contextLabel = snapshot.composeContextLabel != null ? snapshot.composeContextLabel : "";
    compose.contextPreview = snapshot.composeContextPreview != null ? snapshot.composeContextPreview : "";
    compose.forwardFromPeerKind = snapshot.composeForwardFromPeerKind;
    compose.forwardFromPeerId = snapshot.composeForwardFromPeerId;
    compose.forwardMessageId = snapshot.composeForwardMessageId;
    mediaQueue.clear();
    updateMediaSummary();
    outbound.copyFrom(snapshot.outbound);
    updateOutboundSummary();
    updateReaderSummary();

    if (snapshot.route >= SCREEN_DIALOGS && snapshot.route <= SCREEN_MEDIA) {
      router.init(snapshot.route);
    } else {
      router.init(SCREEN_DIALOGS);
    }

    activeDetail = resumePending
      ? "Warm reader cache restored while reconnecting."
      : "Warm reader cache restored.";
    recomputeWorkingSet();
  }

  public void recomputeWorkingSet() {
    int historyBytes = reader.historyCapacityPerSlot() * 128;
    int cachedHistoryBytes = (reader.historySlotCount() - 1) * reader.historyCapacityPerSlot() * 96;
    int composeBytes = compose.text.length() > 0 ? compose.text.length() * 2 : 0;
    int mediaQueueBytes = mediaQueue.count * 96 + mediaQueue.totalStoredBytes();

    workingSet.init(TARGET_RAM_BUDGET_BYTES, TARGET_RESERVED_BYTES);
    workingSet.add(WorkingSet.WORK_AUTH, 12 * 1024, true);
    workingSet.add(WorkingSet.WORK_SESSION, 8 * 1024, true);
    workingSet.add(WorkingSet.WORK_UI, 24 * 1024, true);
    if (composeBytes > 0) {
      workingSet.add(WorkingSet.WORK_UI, composeBytes, false);
    }
    if (mediaQueueBytes > 0) {
      workingSet.add(WorkingSet.WORK_UI, mediaQueueBytes, false);
    }
    workingSet.add(WorkingSet.WORK_READER_DIALOGS, reader.dialogs.capacity() * 160, true);
    workingSet.add(WorkingSet.WORK_READER_HISTORY, historyBytes, true);
    if (cachedHistoryBytes > 0) {
      workingSet.add(WorkingSet.WORK_READER_HISTORY, cachedHistoryBytes, false);
    }
    workingSet.add(WorkingSet.WORK_MEDIA_QUEUE, 2 * 1024, false);
    workingSet.add(WorkingSet.WORK_DEVICE_STATE, 2 * 1024, true);
    lastPlan = workingSet.plan();
  }
}
