package reader;

import media.MediaReference;
import session.WarmStateSnapshot;
import sync.ChannelSyncState;

public class ReaderStateModel {
  public static final int DEFAULT_HISTORY_SLOTS = 4;

  public static final class HistorySlot {
    public int peerKind = DialogListModel.PEER_UNKNOWN;
    public long peerId = 0;
    public boolean loaded = false;
    public int lastAccessTick = 0;
    public final MessageListModel messages;

    public HistorySlot(int historyCapacity) {
      messages = new MessageListModel(historyCapacity);
    }

    public void clear() {
      peerKind = DialogListModel.PEER_UNKNOWN;
      peerId = 0;
      loaded = false;
      lastAccessTick = 0;
      messages.clear();
    }
  }

  public final PeerDirectory peers;
  public final SecretChatDirectory secretChats;
  public final ChannelSyncState channelSync;
  public final DialogListModel dialogs;
  public MessageListModel history;
  public final MessageListModel emptyHistory;
  public final HistorySlot[] historySlots;

  public boolean loadingDialogs = false;
  public boolean loadingHistory = false;
  public boolean historyLoaded = false;
  public int historyPeerKind = DialogListModel.PEER_UNKNOWN;
  public long historyPeerId = 0;
  private HistorySlot currentHistorySlot = null;
  private HistorySlot pendingHistorySlot = null;
  private int historyAccessTick = 0;

  public ReaderStateModel() {
    this(DialogListModel.DEFAULT_CAPACITY, MessageListModel.DEFAULT_CAPACITY);
  }

  public ReaderStateModel(int dialogCapacity, int historyCapacity) {
    this(dialogCapacity, historyCapacity, DEFAULT_HISTORY_SLOTS);
  }

  public ReaderStateModel(int dialogCapacity, int historyCapacity, int historySlotCount) {
    peers = new PeerDirectory(dialogCapacity * 4);
    secretChats = new SecretChatDirectory(dialogCapacity);
    channelSync = new ChannelSyncState(dialogCapacity);
    dialogs = new DialogListModel(dialogCapacity);
    emptyHistory = new MessageListModel(historyCapacity);
    history = emptyHistory;

    if (historySlotCount <= 0) {
      throw new IllegalArgumentException("History slot count must be positive.");
    }
    historySlots = new HistorySlot[historySlotCount];
    for (int i = 0; i < historySlots.length; ++i) {
      historySlots[i] = new HistorySlot(historyCapacity);
    }
  }

  public void clearAll() {
    peers.clear();
    secretChats.clear();
    channelSync.clear();
    dialogs.clear();
    clearHistoryCache();
    loadingDialogs = false;
  }

  public void beginDialogsLoad() {
    peers.clear();
    channelSync.clear();
    dialogs.clear();
    clearHistoryCache();
    loadingDialogs = true;
  }

  public void finishDialogsLoad() {
    loadingDialogs = false;
  }

  public void clearHistory() {
    if (currentHistorySlot != null) {
      currentHistorySlot.clear();
    }
    if (pendingHistorySlot == currentHistorySlot) {
      pendingHistorySlot = null;
    }
    syncCurrentHistoryForSelectedDialog();
  }

  private void clearHistoryCache() {
    for (int i = 0; i < historySlots.length; ++i) {
      historySlots[i].clear();
    }
    currentHistorySlot = null;
    pendingHistorySlot = null;
    history = emptyHistory;
    loadingHistory = false;
    historyLoaded = false;
    historyPeerKind = DialogListModel.PEER_UNKNOWN;
    historyPeerId = 0;
  }

  public void beginHistoryLoad() {
    beginHistoryLoad(DialogListModel.PEER_UNKNOWN, 0);
  }

  public void beginHistoryLoad(int peerKind, long peerId) {
    pendingHistorySlot = prepareHistorySlot(peerKind, peerId);
    pendingHistorySlot.messages.clear();
    history = pendingHistorySlot.messages;
    currentHistorySlot = pendingHistorySlot;
    loadingHistory = true;
    historyLoaded = false;
    historyPeerKind = peerKind;
    historyPeerId = peerId;
  }

  public void finishHistoryLoad() {
    if (pendingHistorySlot != null) {
      pendingHistorySlot.loaded = true;
      pendingHistorySlot.messages.selectLast();
      touchHistorySlot(pendingHistorySlot);
    }
    loadingHistory = false;
    historyLoaded = true;
    pendingHistorySlot = null;
    syncCurrentHistoryForSelectedDialog();
  }

  public boolean appendDialog(int peerKind, long peerId, String title, String preview, int unreadCount, boolean hasMedia) {
    return dialogs.append(peerKind, peerId, title, preview, unreadCount, hasMedia);
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
    return appendDialog(
      peerKind,
      peerId,
      accessHash,
      hasAccessHash,
      topMessageId,
      0,
      0,
      title,
      preview,
      unreadCount,
      hasMedia
    );
  }

  public boolean appendDialog(
    int peerKind,
    long peerId,
    long accessHash,
    boolean hasAccessHash,
    int topMessageId,
    int readInboxMaxId,
    int readOutboxMaxId,
    String title,
    String preview,
    int unreadCount,
    boolean hasMedia
  ) {
    return dialogs.append(
      peerKind,
      peerId,
      accessHash,
      hasAccessHash,
      topMessageId,
      readInboxMaxId,
      readOutboxMaxId,
      title,
      preview,
      unreadCount,
      hasMedia
    );
  }

  public boolean appendHistoryMessage(String sender, String text, boolean outgoing, boolean pending) {
    return appendHistoryMessage(0, sender, text, outgoing, pending, false);
  }

  public boolean appendHistoryMessage(int messageId, String sender, String text, boolean outgoing, boolean pending, boolean hasMedia) {
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
    MediaReference mediaRef
  ) {
    return appendHistoryMessage(
      messageId,
      sender,
      text,
      outgoing,
      pending,
      hasMedia,
      mediaLabel,
      mediaInfo,
      mediaRef,
      0
    );
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
    MediaReference mediaRef,
    int dateSeconds
  ) {
    HistorySlot target;
    int existingIndex;

    if (dateSeconds < 0) {
      return false;
    }
    target = appendTargetSlot();
    if (target == null) {
      return false;
    }

    history = target.messages;
    currentHistorySlot = target;
    existingIndex = messageIndexById(target.messages, messageId);
    if (existingIndex >= 0) {
      // A reconnect difference can reconcile the optimistic row while a
      // selected-history request is still in flight. Let the authoritative
      // history row replace it at the response's bounded ordering position.
      target.messages.removeAt(existingIndex);
    }
    return target.messages.append(
      messageId,
      sender,
      text,
      outgoing,
      pending,
      hasMedia,
      mediaLabel,
      mediaInfo,
      mediaRef,
      dateSeconds
    );
  }

  public boolean selectDialog(int index) {
    boolean changed = dialogs.selectIndex(index);
    if (changed) {
      syncCurrentHistoryForSelectedDialog();
    }
    return changed;
  }

  public boolean selectNextDialog() {
    boolean changed = dialogs.selectNext();
    if (changed) {
      syncCurrentHistoryForSelectedDialog();
    }
    return changed;
  }

  public boolean selectPreviousDialog() {
    boolean changed = dialogs.selectPrevious();
    if (changed) {
      syncCurrentHistoryForSelectedDialog();
    }
    return changed;
  }

  public DialogListModel.Item selectedDialog() {
    return dialogs.selected();
  }

  public int dialogIndexForPeer(int peerKind, long peerId) {
    return dialogs.indexOfPeer(peerKind, peerId);
  }

  public MessageListModel.Item selectedMessage() {
    return history.selected();
  }

  public boolean selectNextMessage() {
    return history.selectNext();
  }

  public boolean selectPreviousMessage() {
    return history.selectPrevious();
  }

  public boolean selectLastMessage() {
    return history.selectLast();
  }

  public boolean focusHistoryPeer(int peerKind, long peerId) {
    HistorySlot slot;

    if (peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0) {
      return false;
    }

    slot = findHistorySlot(peerKind, peerId);
    if (slot == null) {
      slot = prepareHistorySlot(peerKind, peerId);
    }

    currentHistorySlot = slot;
    pendingHistorySlot = null;
    history = slot.messages;
    loadingHistory = false;
    historyLoaded = slot.loaded;
    historyPeerKind = peerKind;
    historyPeerId = peerId;
    touchHistorySlot(slot);
    return true;
  }

  public boolean isHistoryLoadedForSelectedDialog() {
    DialogListModel.Item selected = selectedDialog();
    return selected != null &&
      historyLoaded &&
      historyPeerKind == selected.peerKind &&
      historyPeerId == selected.peerId;
  }

  public int historySlotCount() {
    return historySlots.length;
  }

  public int historyCapacityPerSlot() {
    return emptyHistory.capacity();
  }

  public int loadedHistorySlotCount() {
    int count = 0;

    for (int i = 0; i < historySlots.length; ++i) {
      if (historySlots[i].loaded) {
        count += 1;
      }
    }
    return count;
  }

  public boolean appendOutboundText(int peerKind, long peerId, String sender, String text) {
    return appendOutboundText(peerKind, peerId, sender, text, 0);
  }

  public boolean appendOutboundText(int peerKind, long peerId, String sender, String text, int dateSeconds) {
    return appendOutboundEntry(peerKind, peerId, sender, text, false, false, "", "", dateSeconds);
  }

  public boolean appendPendingOutboundText(int peerKind, long peerId, String sender, String text) {
    return appendOutboundEntry(peerKind, peerId, sender, text, true, false);
  }

  /**
   * Restores the one visible row owned by the single in-flight outbound job.
   * Reconnect dialog refreshes clear the bounded history cache, so retrying a
   * request must recreate this row without creating a second copy when the
   * cached row survived.
   */
  public boolean ensurePendingOutboundText(int peerKind, long peerId, String sender, String text) {
    HistorySlot slot;
    int index;
    int i;

    if (peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0 || text == null) {
      return false;
    }

    slot = findHistorySlot(peerKind, peerId);
    if (slot == null) {
      return appendPendingOutboundText(peerKind, peerId, sender, text);
    }

    index = latestPendingOutboundIndex(slot.messages);
    if (index < 0) {
      return appendPendingOutboundText(peerKind, peerId, sender, text);
    }

    // Older clients could leave more than one optimistic row. The runtime has
    // one outbound RPC lane, so retain only the newest row and repair it from
    // the durable compose job without allocating another message object.
    for (i = index - 1; i >= 0; --i) {
      if (slot.messages.items[i].outgoing && slot.messages.items[i].pending) {
        slot.messages.removeAt(i);
        index -= 1;
      }
    }
    slot.messages.items[index].set(0, sender, text, true, true, false);
    slot.messages.selectedIndex = index;
    slot.loaded = true;
    touchHistorySlot(slot);
    updateDialogPreviewFromSlot(peerKind, peerId, slot);
    if (isSelectedPeer(peerKind, peerId) ||
        (historyPeerKind == peerKind && historyPeerId == peerId)) {
      currentHistorySlot = slot;
      history = slot.messages;
      historyLoaded = true;
      loadingHistory = false;
      historyPeerKind = peerKind;
      historyPeerId = peerId;
    }
    return true;
  }

  public boolean appendOutboundFileNotice(int peerKind, long peerId, String sender, String fileName) {
    if (fileName == null) {
      return false;
    }
    return appendOutboundText(peerKind, peerId, sender, "file: " + fileName);
  }

  public boolean applyLiveTextUpdate(
    int peerKind,
    long peerId,
    int senderPeerKind,
    long senderPeerId,
    int messageId,
    String text,
    boolean outgoing,
    boolean hasMedia,
    int unreadDelta
  ) {
    return applyLiveTextUpdate(
      peerKind,
      peerId,
      senderPeerKind,
      senderPeerId,
      messageId,
      text,
      outgoing,
      hasMedia,
      "",
      "",
      unreadDelta
    );
  }

  public boolean applyLiveTextUpdate(
    int peerKind,
    long peerId,
    int senderPeerKind,
    long senderPeerId,
    int messageId,
    String text,
    boolean outgoing,
    boolean hasMedia,
    String mediaLabel,
    String mediaInfo,
    int unreadDelta
  ) {
    return applyLiveTextUpdate(
      peerKind,
      peerId,
      senderPeerKind,
      senderPeerId,
      messageId,
      text,
      outgoing,
      hasMedia,
      mediaLabel,
      mediaInfo,
      null,
      unreadDelta
    );
  }

  public boolean applyLiveTextUpdate(
    int peerKind,
    long peerId,
    int senderPeerKind,
    long senderPeerId,
    int messageId,
    String text,
    boolean outgoing,
    boolean hasMedia,
    String mediaLabel,
    String mediaInfo,
    MediaReference mediaRef,
    int unreadDelta
  ) {
    return applyLiveTextUpdate(
      peerKind,
      peerId,
      senderPeerKind,
      senderPeerId,
      messageId,
      text,
      outgoing,
      hasMedia,
      mediaLabel,
      mediaInfo,
      mediaRef,
      0,
      unreadDelta
    );
  }

  public boolean applyLiveTextUpdate(
    int peerKind,
    long peerId,
    int senderPeerKind,
    long senderPeerId,
    int messageId,
    String text,
    boolean outgoing,
    boolean hasMedia,
    String mediaLabel,
    String mediaInfo,
    MediaReference mediaRef,
    int dateSeconds,
    int unreadDelta
  ) {
    DialogListModel.Item dialog;
    PeerDirectory.Item peer;
    HistorySlot slot;
    String title;
    String sender;
    String preview;
    int unreadCount = 0;
    int dialogIndex;
    int messageIndex;

    if (peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0 || text == null || dateSeconds < 0) {
      return false;
    }

    dialogIndex = dialogs.indexOfPeer(peerKind, peerId);
    dialog = dialogIndex >= 0 ? dialogs.items[dialogIndex] : null;
    peer = peers.find(peerKind, peerId);
    if (dialog == null && peer == null) {
      return false;
    }

    if (dialog != null) {
      title = dialog.title;
      unreadCount = dialog.unreadCount;
      if (unreadDelta > 0) {
        unreadCount += unreadDelta;
      }
    } else {
      title = peer.title;
      unreadCount = unreadDelta > 0 ? unreadDelta : 0;
    }

    if ((title == null || title.length() == 0) && peer != null) {
      title = peer.title;
    }
    if (title == null || title.length() == 0) {
      title = peers.titleOrFallback(peerKind, peerId);
    }
    preview = previewText(text, hasMedia, mediaLabel, mediaInfo);

    if (!dialogs.upsertActivity(
        peerKind,
        peerId,
        dialog != null && dialog.hasAccessHash ? dialog.accessHash : (peer != null ? peer.accessHash : 0),
        dialog != null ? dialog.hasAccessHash : (peer != null && peer.hasAccessHash),
        messageId,
        title,
        preview,
        unreadCount,
        hasMedia)) {
      return false;
    }

    slot = findHistorySlot(peerKind, peerId);
    if (slot == null &&
        !isSelectedPeer(peerKind, peerId) &&
        !(historyPeerKind == peerKind && historyPeerId == peerId)) {
      return true;
    }
    if (slot == null) {
      slot = prepareHistorySlot(peerKind, peerId);
    }

    sender = outgoing
      ? "YOU"
      : peers.titleOrFallback(
        senderPeerKind != DialogListModel.PEER_UNKNOWN ? senderPeerKind : peerKind,
        senderPeerId != 0 ? senderPeerId : peerId
      );

    slot.loaded = true;
    touchHistorySlot(slot);
    messageIndex = messageIndexById(slot.messages, messageId);
    if (messageIndex >= 0) {
      int appliedDateSeconds = dateSeconds > 0
        ? dateSeconds
        : slot.messages.items[messageIndex].dateSeconds;

      slot.messages.items[messageIndex].set(
        messageId,
        sender,
        text,
        outgoing,
        false,
        hasMedia,
        mediaLabel,
        mediaInfo,
        mediaRef,
        appliedDateSeconds
      );
      updateDialogPreviewFromSlot(peerKind, peerId, slot);
      slot.messages.selectLast();
      if (isSelectedPeer(peerKind, peerId) ||
          (historyPeerKind == peerKind && historyPeerId == peerId)) {
        currentHistorySlot = slot;
        history = slot.messages;
        historyLoaded = true;
        loadingHistory = false;
        historyPeerKind = peerKind;
        historyPeerId = peerId;
      }
      return true;
    }
    if (!slot.messages.pushBounded(
        messageId,
        sender,
        text,
        outgoing,
        false,
        hasMedia,
        mediaLabel,
        mediaInfo,
        mediaRef,
        dateSeconds)) {
      return false;
    }
    slot.messages.selectLast();

    if (isSelectedPeer(peerKind, peerId) ||
        (historyPeerKind == peerKind && historyPeerId == peerId)) {
      currentHistorySlot = slot;
      history = slot.messages;
      historyLoaded = true;
      loadingHistory = false;
      historyPeerKind = peerKind;
      historyPeerId = peerId;
    }
    return true;
  }

  public boolean applyLiveEditedTextUpdate(
    int peerKind,
    long peerId,
    int senderPeerKind,
    long senderPeerId,
    int messageId,
    String text,
    boolean outgoing,
    boolean hasMedia
  ) {
    return applyLiveEditedTextUpdate(
      peerKind,
      peerId,
      senderPeerKind,
      senderPeerId,
      messageId,
      text,
      outgoing,
      hasMedia,
      "",
      ""
    );
  }

  public boolean applyLiveEditedTextUpdate(
    int peerKind,
    long peerId,
    int senderPeerKind,
    long senderPeerId,
    int messageId,
    String text,
    boolean outgoing,
    boolean hasMedia,
    String mediaLabel,
    String mediaInfo
  ) {
    return applyLiveEditedTextUpdate(
      peerKind,
      peerId,
      senderPeerKind,
      senderPeerId,
      messageId,
      text,
      outgoing,
      hasMedia,
      mediaLabel,
      mediaInfo,
      null
    );
  }

  public boolean applyLiveEditedTextUpdate(
    int peerKind,
    long peerId,
    int senderPeerKind,
    long senderPeerId,
    int messageId,
    String text,
    boolean outgoing,
    boolean hasMedia,
    String mediaLabel,
    String mediaInfo,
    MediaReference mediaRef
  ) {
    return applyLiveEditedTextUpdate(
      peerKind,
      peerId,
      senderPeerKind,
      senderPeerId,
      messageId,
      text,
      outgoing,
      hasMedia,
      mediaLabel,
      mediaInfo,
      mediaRef,
      0
    );
  }

  public boolean applyLiveEditedTextUpdate(
    int peerKind,
    long peerId,
    int senderPeerKind,
    long senderPeerId,
    int messageId,
    String text,
    boolean outgoing,
    boolean hasMedia,
    String mediaLabel,
    String mediaInfo,
    MediaReference mediaRef,
    int dateSeconds
  ) {
    DialogListModel.Item dialog;
    PeerDirectory.Item peer;
    HistorySlot slot;
    String sender;
    int dialogIndex;
    int messageIndex;

    if (peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0 || text == null || dateSeconds < 0) {
      return false;
    }

    dialogIndex = dialogs.indexOfPeer(peerKind, peerId);
    dialog = dialogIndex >= 0 ? dialogs.items[dialogIndex] : null;
    peer = peers.find(peerKind, peerId);
    slot = findHistorySlot(peerKind, peerId);
    if (dialog == null && peer == null && slot == null) {
      return false;
    }

    if (dialog != null && dialog.topMessageId == messageId) {
      dialog.preview = DialogListModel.limit(
        previewText(text, hasMedia, mediaLabel, mediaInfo),
        DialogListModel.PREVIEW_MAX
      );
      dialog.hasMedia = hasMedia;
    }

    if (slot != null) {
      messageIndex = messageIndexById(slot.messages, messageId);
      if (messageIndex >= 0) {
        sender = outgoing
          ? "YOU"
          : peers.titleOrFallback(
            senderPeerKind != DialogListModel.PEER_UNKNOWN ? senderPeerKind : peerKind,
            senderPeerId != 0 ? senderPeerId : peerId
          );
        slot.messages.items[messageIndex].set(
          messageId,
          sender,
          text,
          outgoing,
          slot.messages.items[messageIndex].pending,
          hasMedia,
          mediaLabel,
          mediaInfo,
          mediaRef,
          dateSeconds > 0 ? dateSeconds : slot.messages.items[messageIndex].dateSeconds
        );
        updateDialogPreviewFromSlot(peerKind, peerId, slot);
        if (isSelectedPeer(peerKind, peerId) ||
            (historyPeerKind == peerKind && historyPeerId == peerId)) {
          currentHistorySlot = slot;
          history = slot.messages;
          historyLoaded = slot.loaded;
          loadingHistory = false;
          historyPeerKind = peerKind;
          historyPeerId = peerId;
        }
        return true;
      }
    }

    return dialog != null;
  }

  public boolean deleteMessageForPeer(int peerKind, long peerId, int messageId) {
    HistorySlot slot;
    DialogListModel.Item dialog;
    int dialogIndex;
    int messageIndex;
    boolean removed = false;

    if (peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0 || messageId == 0) {
      return false;
    }

    slot = findHistorySlot(peerKind, peerId);
    if (slot != null) {
      messageIndex = messageIndexById(slot.messages, messageId);
      if (messageIndex >= 0 && slot.messages.removeAt(messageIndex)) {
        updateDialogPreviewFromSlot(peerKind, peerId, slot);
        if (isSelectedPeer(peerKind, peerId) ||
            (historyPeerKind == peerKind && historyPeerId == peerId)) {
          currentHistorySlot = slot;
          history = slot.messages;
          historyLoaded = slot.loaded;
          loadingHistory = false;
          historyPeerKind = peerKind;
          historyPeerId = peerId;
        }
        removed = true;
      }
    }

    dialogIndex = dialogs.indexOfPeer(peerKind, peerId);
    dialog = dialogIndex >= 0 ? dialogs.items[dialogIndex] : null;
    if (dialog != null && dialog.topMessageId == messageId) {
      if (slot == null || slot.messages.count <= 0) {
        dialog.topMessageId = 0;
        dialog.preview = "";
        dialog.hasMedia = false;
      } else {
        updateDialogPreviewFromSlot(peerKind, peerId, slot);
      }
      removed = true;
    }

    return removed;
  }

  public int deleteMessageGlobally(int messageId) {
    int removed = 0;

    if (messageId == 0) {
      return 0;
    }

    for (int i = 0; i < historySlots.length; ++i) {
      HistorySlot slot = historySlots[i];

      if (slot.peerKind == DialogListModel.PEER_UNKNOWN || slot.peerId == 0) {
        continue;
      }
      if (deleteMessageForPeer(slot.peerKind, slot.peerId, messageId)) {
        removed += 1;
      }
    }

    for (int i = 0; i < dialogs.count; ++i) {
      DialogListModel.Item dialog = dialogs.items[i];

      if (dialog.topMessageId == messageId &&
          !deleteMessageForPeer(dialog.peerKind, dialog.peerId, messageId)) {
        dialog.topMessageId = 0;
        dialog.preview = "";
        dialog.hasMedia = false;
        removed += 1;
      }
    }

    return removed;
  }

  public boolean applyLiveUnreadCount(int peerKind, long peerId, int unreadCount) {
    return applyLiveInboxRead(peerKind, peerId, 0, unreadCount);
  }

  public boolean applyLiveInboxRead(int peerKind, long peerId, int maxMessageId, int unreadCount) {
    int dialogIndex;

    if (peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0 || maxMessageId < 0) {
      return false;
    }

    dialogIndex = dialogs.indexOfPeer(peerKind, peerId);
    if (dialogIndex < 0) {
      return false;
    }

    if (maxMessageId > 0 && maxMessageId < dialogs.items[dialogIndex].readInboxMaxId) {
      return true;
    }
    if (maxMessageId > dialogs.items[dialogIndex].readInboxMaxId) {
      dialogs.items[dialogIndex].readInboxMaxId = maxMessageId;
    }
    dialogs.items[dialogIndex].unreadCount = unreadCount > 0 ? unreadCount : 0;
    return true;
  }

  public boolean applyLiveOutboxRead(int peerKind, long peerId, int maxMessageId) {
    int dialogIndex;

    if (peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0 || maxMessageId < 0) {
      return false;
    }

    dialogIndex = dialogs.indexOfPeer(peerKind, peerId);
    if (dialogIndex < 0) {
      return false;
    }

    if (maxMessageId > dialogs.items[dialogIndex].readOutboxMaxId) {
      dialogs.items[dialogIndex].readOutboxMaxId = maxMessageId;
    }
    return true;
  }

  public int latestIncomingMessageIdForPeer(int peerKind, long peerId) {
    HistorySlot slot = findHistorySlot(peerKind, peerId);
    int latest = 0;

    if (slot == null) {
      return 0;
    }
    for (int i = 0; i < slot.messages.count; ++i) {
      if (!slot.messages.items[i].outgoing && slot.messages.items[i].messageId > latest) {
        latest = slot.messages.items[i].messageId;
      }
    }
    return latest;
  }

  public boolean markLatestPendingOutboundSent(int peerKind, long peerId, int messageId) {
    return markLatestPendingOutboundSent(peerKind, peerId, messageId, 0);
  }

  public boolean markLatestPendingOutboundSent(int peerKind, long peerId, int messageId, int dateSeconds) {
    HistorySlot slot;
    int index;

    if (dateSeconds < 0) {
      return false;
    }
    slot = findHistorySlot(peerKind, peerId);
    if (slot == null) {
      return false;
    }

    index = latestPendingOutboundIndex(slot.messages);
    if (index < 0) {
      return false;
    }

    slot.messages.items[index].messageId = messageId;
    slot.messages.items[index].pending = false;
    slot.messages.items[index].dateSeconds = dateSeconds;
    updateDialogPreviewFromSlot(peerKind, peerId, slot);
    if (isSelectedPeer(peerKind, peerId)) {
      currentHistorySlot = slot;
      history = slot.messages;
      historyLoaded = true;
      loadingHistory = false;
      historyPeerKind = peerKind;
      historyPeerId = peerId;
    }
    return true;
  }

  public boolean removeLatestPendingOutbound(int peerKind, long peerId) {
    HistorySlot slot;
    int index;

    slot = findHistorySlot(peerKind, peerId);
    if (slot == null) {
      return false;
    }

    index = latestPendingOutboundIndex(slot.messages);
    if (index < 0) {
      return false;
    }

    if (!slot.messages.removeAt(index)) {
      return false;
    }
    updateDialogPreviewFromSlot(peerKind, peerId, slot);
    if (isSelectedPeer(peerKind, peerId)) {
      currentHistorySlot = slot;
      history = slot.messages;
      historyLoaded = true;
      loadingHistory = false;
      historyPeerKind = peerKind;
      historyPeerId = peerId;
    }
    return true;
  }

  private boolean appendOutboundEntry(int peerKind, long peerId, String sender, String text, boolean pending, boolean hasMedia) {
    return appendOutboundEntry(peerKind, peerId, sender, text, pending, hasMedia, "", "", 0);
  }

  private boolean appendOutboundEntry(
    int peerKind,
    long peerId,
    String sender,
    String text,
    boolean pending,
    boolean hasMedia,
    String mediaLabel,
    String mediaInfo,
    int dateSeconds
  ) {
    HistorySlot slot;

    if (peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0 || text == null || dateSeconds < 0) {
      return false;
    }

    slot = prepareHistorySlot(peerKind, peerId);
    slot.loaded = true;
    touchHistorySlot(slot);
    if (!slot.messages.pushBounded(0, sender, text, true, pending, hasMedia, mediaLabel, mediaInfo, null, dateSeconds)) {
      return false;
    }
    slot.messages.selectLast();

    updateDialogPreviewFromSlot(peerKind, peerId, slot);
    if (isSelectedPeer(peerKind, peerId)) {
      currentHistorySlot = slot;
      history = slot.messages;
      historyLoaded = true;
      loadingHistory = false;
      historyPeerKind = peerKind;
      historyPeerId = peerId;
    }
    return true;
  }

  private int latestPendingOutboundIndex(MessageListModel messages) {
    int i;

    if (messages == null) {
      return -1;
    }

    for (i = messages.count - 1; i >= 0; --i) {
      if (messages.items[i].outgoing && messages.items[i].pending) {
        return i;
      }
    }
    return -1;
  }

  private int messageIndexById(MessageListModel messages, int messageId) {
    int i;

    if (messages == null || messageId == 0) {
      return -1;
    }

    for (i = messages.count - 1; i >= 0; --i) {
      if (messages.items[i].messageId == messageId) {
        return i;
      }
    }
    return -1;
  }

  private HistorySlot appendTargetSlot() {
    DialogListModel.Item selected;

    if (pendingHistorySlot != null) {
      return pendingHistorySlot;
    }
    if (currentHistorySlot != null && currentHistorySlot.peerKind != DialogListModel.PEER_UNKNOWN) {
      return currentHistorySlot;
    }
    if (historyPeerKind != DialogListModel.PEER_UNKNOWN && historyPeerId != 0) {
      currentHistorySlot = prepareHistorySlot(historyPeerKind, historyPeerId);
      return currentHistorySlot;
    }

    selected = selectedDialog();
    if (selected == null) {
      return null;
    }
    historyPeerKind = selected.peerKind;
    historyPeerId = selected.peerId;
    currentHistorySlot = prepareHistorySlot(selected.peerKind, selected.peerId);
    return currentHistorySlot;
  }

  private void syncCurrentHistoryForSelectedDialog() {
    DialogListModel.Item selected = selectedDialog();
    HistorySlot slot;

    if (selected == null) {
      if (pendingHistorySlot != null) {
        currentHistorySlot = pendingHistorySlot;
        history = pendingHistorySlot.messages;
        loadingHistory = true;
        historyLoaded = false;
        historyPeerKind = pendingHistorySlot.peerKind;
        historyPeerId = pendingHistorySlot.peerId;
        return;
      }
      if (currentHistorySlot != null && currentHistorySlot.peerKind != DialogListModel.PEER_UNKNOWN) {
        history = currentHistorySlot.messages;
        loadingHistory = false;
        historyLoaded = currentHistorySlot.loaded;
        historyPeerKind = currentHistorySlot.peerKind;
        historyPeerId = currentHistorySlot.peerId;
        return;
      }
      currentHistorySlot = null;
      history = emptyHistory;
      loadingHistory = false;
      historyLoaded = false;
      historyPeerKind = DialogListModel.PEER_UNKNOWN;
      historyPeerId = 0;
      return;
    }

    historyPeerKind = selected.peerKind;
    historyPeerId = selected.peerId;
    slot = findHistorySlot(selected.peerKind, selected.peerId);
    currentHistorySlot = slot;
    if (slot == null) {
      history = emptyHistory;
      loadingHistory = false;
      historyLoaded = false;
      return;
    }

    history = slot.messages;
    loadingHistory = pendingHistorySlot == slot && !slot.loaded;
    historyLoaded = slot.loaded;
    touchHistorySlot(slot);
  }

  private HistorySlot findHistorySlot(int peerKind, long peerId) {
    for (int i = 0; i < historySlots.length; ++i) {
      if (historySlots[i].peerKind == peerKind &&
          historySlots[i].peerId == peerId) {
        return historySlots[i];
      }
    }
    return null;
  }

  private HistorySlot prepareHistorySlot(int peerKind, long peerId) {
    HistorySlot slot = findHistorySlot(peerKind, peerId);

    if (slot != null) {
      touchHistorySlot(slot);
      return slot;
    }

    slot = reusableEmptyHistorySlot();
    if (slot == null) {
      slot = evictableHistorySlot();
      if (slot == null) {
        slot = historySlots[0];
      }
    }

    slot.clear();
    slot.peerKind = peerKind;
    slot.peerId = peerId;
    touchHistorySlot(slot);
    return slot;
  }

  private HistorySlot reusableEmptyHistorySlot() {
    for (int i = 0; i < historySlots.length; ++i) {
      if (historySlots[i].peerKind == DialogListModel.PEER_UNKNOWN &&
          historySlots[i].peerId == 0) {
        return historySlots[i];
      }
    }
    return null;
  }

  private HistorySlot evictableHistorySlot() {
    HistorySlot best = null;

    for (int i = 0; i < historySlots.length; ++i) {
      HistorySlot candidate = historySlots[i];

      if (candidate == pendingHistorySlot) {
        continue;
      }
      if (candidate.loaded && isSelectedPeer(candidate.peerKind, candidate.peerId)) {
        continue;
      }
      if (best == null || candidate.lastAccessTick < best.lastAccessTick) {
        best = candidate;
      }
    }

    return best;
  }

  private boolean isSelectedPeer(int peerKind, long peerId) {
    DialogListModel.Item selected = selectedDialog();

    return selected != null &&
      selected.peerKind == peerKind &&
      selected.peerId == peerId;
  }

  private void touchHistorySlot(HistorySlot slot) {
    if (slot == null) {
      return;
    }
    historyAccessTick += 1;
    slot.lastAccessTick = historyAccessTick;
  }

  private void updateDialogPreviewFromSlot(int peerKind, long peerId, HistorySlot slot) {
    DialogListModel.Item dialog;
    MessageListModel.Item latest;

    for (int i = 0; i < dialogs.count; ++i) {
      dialog = dialogs.items[i];
      if (dialog.peerKind == peerKind && dialog.peerId == peerId) {
        if (slot == null || slot.messages.count <= 0) {
          dialog.topMessageId = 0;
          dialog.preview = "";
          dialog.hasMedia = false;
          return;
        }

        latest = slot.messages.items[slot.messages.count - 1];
        if (latest.messageId > 0) {
          dialog.topMessageId = latest.messageId;
        }
        dialog.preview = DialogListModel.limit(latest.previewText(), DialogListModel.PREVIEW_MAX);
        dialog.hasMedia = latest.hasMedia;
        return;
      }
    }
  }

  private static String previewText(String text, boolean hasMedia, String mediaLabel, String mediaInfo) {
    if (text != null && text.length() > 0) {
      return text;
    }
    if (hasMedia) {
      if (mediaLabel != null && mediaLabel.length() > 0) {
        if (mediaInfo != null && mediaInfo.length() > 0) {
          return DialogListModel.limit(
            mediaLabel + " " + mediaInfo,
            DialogListModel.PREVIEW_MAX
          );
        }
        return mediaLabel;
      }
      if (mediaInfo != null && mediaInfo.length() > 0) {
        return mediaInfo;
      }
      return "MEDIA";
    }
    return "";
  }

  public void exportWarmState(WarmStateSnapshot out) {
    int dialogLimit;
    int peerLimit;
    int slotCount = 0;

    if (out == null) {
      return;
    }

    out.dialogCount = dialogs.count <= out.dialogs.length ? dialogs.count : out.dialogs.length;
    out.selectedDialogIndex = dialogs.count > 0 ? dialogs.selectedIndex : 0;
    dialogLimit = out.dialogCount;
    for (int i = 0; i < dialogLimit; ++i) {
      WarmStateSnapshot.DialogEntry target = out.dialogs[i];
      DialogListModel.Item source = dialogs.items[i];

      target.peerKind = source.peerKind;
      target.peerId = source.peerId;
      target.accessHash = source.accessHash;
      target.hasAccessHash = source.hasAccessHash;
      target.topMessageId = source.topMessageId;
      target.readInboxMaxId = source.readInboxMaxId;
      target.readOutboxMaxId = source.readOutboxMaxId;
      target.title = source.title;
      target.preview = source.preview;
      target.unreadCount = source.unreadCount;
      target.hasMedia = source.hasMedia;
    }

    out.peerCount = peers.count <= out.peers.length ? peers.count : out.peers.length;
    peerLimit = out.peerCount;
    for (int i = 0; i < peerLimit; ++i) {
      WarmStateSnapshot.PeerEntry target = out.peers[i];
      PeerDirectory.Item source = peers.items[i];

      target.peerKind = source.peerKind;
      target.peerId = source.peerId;
      target.accessHash = source.accessHash;
      target.hasAccessHash = source.hasAccessHash;
      target.minAccessHash = source.minAccessHash;
      target.title = source.title;
      target.username = source.username;
    }

    out.secretChatCount = secretChats.count <= out.secretChats.length
      ? secretChats.count
      : out.secretChats.length;
    for (int i = 0; i < out.secretChatCount; ++i) {
      WarmStateSnapshot.SecretChatEntry target = out.secretChats[i];
      SecretChatDirectory.Item source = secretChats.items[i];

      target.chatId = source.chatId;
      target.peerKind = source.peerKind;
      target.peerId = source.peerId;
      target.accessHash = source.accessHash;
      target.state = source.state;
      target.outbound = source.outbound;
      target.date = source.date;
      target.keyFingerprint = source.keyFingerprint;
      if (source.pendingKeyData != null && source.pendingKeyData.length > 0) {
        target.pendingKeyData = new byte[source.pendingKeyData.length];
        System.arraycopy(source.pendingKeyData, 0, target.pendingKeyData, 0, source.pendingKeyData.length);
      } else {
        target.pendingKeyData = null;
      }
    }

    for (int i = 0; i < historySlots.length && slotCount < out.historySlots.length; ++i) {
      HistorySlot sourceSlot = historySlots[i];
      WarmStateSnapshot.HistorySlotEntry targetSlot;
      int messageCount;

      if (sourceSlot.peerKind == DialogListModel.PEER_UNKNOWN &&
          sourceSlot.peerId == 0 &&
          !sourceSlot.loaded &&
          sourceSlot.messages.count == 0) {
        continue;
      }

      targetSlot = out.historySlots[slotCount];
      targetSlot.clear();
      targetSlot.peerKind = sourceSlot.peerKind;
      targetSlot.peerId = sourceSlot.peerId;
      targetSlot.loaded = sourceSlot.loaded;
      targetSlot.lastAccessTick = sourceSlot.lastAccessTick;
      messageCount = sourceSlot.messages.count <= targetSlot.messages.length
        ? sourceSlot.messages.count
        : targetSlot.messages.length;
      targetSlot.messageCount = messageCount;

      for (int j = 0; j < messageCount; ++j) {
        WarmStateSnapshot.MessageEntry targetMessage = targetSlot.messages[j];
        MessageListModel.Item sourceMessage = sourceSlot.messages.items[j];

        targetMessage.messageId = sourceMessage.messageId;
        targetMessage.sender = sourceMessage.sender;
        targetMessage.text = sourceMessage.text;
        targetMessage.mediaLabel = sourceMessage.mediaLabel;
        targetMessage.mediaInfo = sourceMessage.mediaInfo;
        targetMessage.outgoing = sourceMessage.outgoing;
        targetMessage.pending = sourceMessage.pending;
        targetMessage.hasMedia = sourceMessage.hasMedia;
        targetMessage.dateSeconds = sourceMessage.dateSeconds;
        targetMessage.mediaRef.copyFrom(sourceMessage.mediaRef);
      }
      slotCount += 1;
    }

    out.historySlotCount = slotCount;
  }

  public void importWarmState(WarmStateSnapshot snapshot) {
    int maxAccessTick = 0;

    clearAll();
    clearHistoryCache();

    if (snapshot == null) {
      return;
    }

    for (int i = 0; i < snapshot.dialogCount && i < dialogs.items.length; ++i) {
      WarmStateSnapshot.DialogEntry source = snapshot.dialogs[i];
      appendDialog(
        source.peerKind,
        source.peerId,
        source.accessHash,
        source.hasAccessHash,
        source.topMessageId,
        source.readInboxMaxId,
        source.readOutboxMaxId,
        source.title,
        source.preview,
        source.unreadCount,
        source.hasMedia
      );
    }

    for (int i = 0; i < snapshot.peerCount && i < peers.items.length; ++i) {
      WarmStateSnapshot.PeerEntry source = snapshot.peers[i];

      peers.upsertIdentity(
        source.peerKind,
        source.peerId,
        source.accessHash,
        source.hasAccessHash,
        source.minAccessHash,
        source.title,
        source.username
      );
    }

    for (int i = 0; i < snapshot.secretChatCount && i < secretChats.items.length; ++i) {
      WarmStateSnapshot.SecretChatEntry source = snapshot.secretChats[i];

      secretChats.upsert(
        source.chatId,
        source.peerKind,
        source.peerId,
        source.accessHash,
        source.state,
        source.outbound,
        source.date,
        source.keyFingerprint,
        source.pendingKeyData,
        source.pendingKeyData != null ? source.pendingKeyData.length : 0
      );
    }

    if (dialogs.count > 0) {
      if (snapshot.selectedDialogIndex >= 0 && snapshot.selectedDialogIndex < dialogs.count) {
        dialogs.selectedIndex = snapshot.selectedDialogIndex;
      } else {
        dialogs.selectedIndex = 0;
      }
    }

    for (int i = 0; i < snapshot.historySlotCount && i < historySlots.length; ++i) {
      WarmStateSnapshot.HistorySlotEntry sourceSlot = snapshot.historySlots[i];
      HistorySlot targetSlot = historySlots[i];

      targetSlot.clear();
      targetSlot.peerKind = sourceSlot.peerKind;
      targetSlot.peerId = sourceSlot.peerId;
      targetSlot.loaded = sourceSlot.loaded;
      targetSlot.lastAccessTick = sourceSlot.lastAccessTick;
      if (targetSlot.lastAccessTick > maxAccessTick) {
        maxAccessTick = targetSlot.lastAccessTick;
      }

      for (int j = 0; j < sourceSlot.messageCount && j < targetSlot.messages.items.length; ++j) {
        WarmStateSnapshot.MessageEntry sourceMessage = sourceSlot.messages[j];
        targetSlot.messages.append(
          sourceMessage.messageId,
          sourceMessage.sender,
          sourceMessage.text,
          sourceMessage.outgoing,
          sourceMessage.pending,
          sourceMessage.hasMedia,
          sourceMessage.mediaLabel,
          sourceMessage.mediaInfo,
          sourceMessage.mediaRef,
          sourceMessage.dateSeconds
        );
      }
    }

    historyAccessTick = maxAccessTick;
    loadingDialogs = false;
    loadingHistory = false;
    pendingHistorySlot = null;
    currentHistorySlot = null;
    syncCurrentHistoryForSelectedDialog();
  }
}
