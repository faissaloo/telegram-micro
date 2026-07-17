package session;

import compose.ComposeStateModel;
import media.MediaReference;
import reader.DialogListModel;
import reader.MessageListModel;
import reader.PeerDirectory;
import reader.ReaderStateModel;
import reader.SecretChatDirectory;

public final class WarmStateSnapshot {
  public static final int VERSION = 9;
  public static final int COMPOSE_ERROR_MAX = 96;
  public static final int COMPOSE_CONTEXT_LABEL_MAX = ComposeStateModel.CONTEXT_LABEL_MAX;
  public static final int COMPOSE_CONTEXT_PREVIEW_MAX = ComposeStateModel.CONTEXT_PREVIEW_MAX;
  public static final int LOGIN_PHONE_MAX = 32;
  public static final int LOGIN_CODE_HASH_MAX = 96;

  public static final class PeerEntry {
    public int peerKind = DialogListModel.PEER_UNKNOWN;
    public long peerId = 0;
    public long accessHash = 0;
    public boolean hasAccessHash = false;
    public boolean minAccessHash = false;
    public String title = "";
    public String username = "";

    public void clear() {
      peerKind = DialogListModel.PEER_UNKNOWN;
      peerId = 0;
      accessHash = 0;
      hasAccessHash = false;
      minAccessHash = false;
      title = "";
      username = "";
    }
  }

  public static final class DialogEntry {
    public int peerKind = DialogListModel.PEER_UNKNOWN;
    public long peerId = 0;
    public long accessHash = 0;
    public boolean hasAccessHash = false;
    public int topMessageId = 0;
    public String title = "";
    public String preview = "";
    public int unreadCount = 0;
    public boolean hasMedia = false;

    public void clear() {
      peerKind = DialogListModel.PEER_UNKNOWN;
      peerId = 0;
      accessHash = 0;
      hasAccessHash = false;
      topMessageId = 0;
      title = "";
      preview = "";
      unreadCount = 0;
      hasMedia = false;
    }
  }

  public static final class SecretChatEntry {
    public int chatId = 0;
    public int peerKind = DialogListModel.PEER_UNKNOWN;
    public long peerId = 0;
    public long accessHash = 0;
    public int state = SecretChatDirectory.STATE_UNKNOWN;
    public boolean outbound = false;
    public int date = 0;
    public long keyFingerprint = 0;
    public byte[] pendingKeyData = null;

    public void clear() {
      chatId = 0;
      peerKind = DialogListModel.PEER_UNKNOWN;
      peerId = 0;
      accessHash = 0;
      state = SecretChatDirectory.STATE_UNKNOWN;
      outbound = false;
      date = 0;
      keyFingerprint = 0;
      pendingKeyData = null;
    }
  }

  public static final class MessageEntry {
    public int messageId = 0;
    public String sender = "";
    public String text = "";
    public String mediaLabel = "";
    public String mediaInfo = "";
    public final MediaReference mediaRef = new MediaReference();
    public int dateSeconds = 0;
    public boolean outgoing = false;
    public boolean pending = false;
    public boolean hasMedia = false;

    public void clear() {
      messageId = 0;
      sender = "";
      text = "";
      mediaLabel = "";
      mediaInfo = "";
      mediaRef.clear();
      dateSeconds = 0;
      outgoing = false;
      pending = false;
      hasMedia = false;
    }
  }

  public static final class HistorySlotEntry {
    public int peerKind = DialogListModel.PEER_UNKNOWN;
    public long peerId = 0;
    public boolean loaded = false;
    public int lastAccessTick = 0;
    public int messageCount = 0;
    public final MessageEntry[] messages;

    public HistorySlotEntry() {
      messages = new MessageEntry[MessageListModel.DEFAULT_CAPACITY];
      for (int i = 0; i < messages.length; ++i) {
        messages[i] = new MessageEntry();
      }
    }

    public void clear() {
      peerKind = DialogListModel.PEER_UNKNOWN;
      peerId = 0;
      loaded = false;
      lastAccessTick = 0;
      messageCount = 0;
      for (int i = 0; i < messages.length; ++i) {
        messages[i].clear();
      }
    }
  }

  public final DialogEntry[] dialogs;
  public final PeerEntry[] peers;
  public final SecretChatEntry[] secretChats;
  public final HistorySlotEntry[] historySlots;
  public final OutboundRecoveryState outbound;

  public int version = VERSION;
  public int route = 0;
  public int dialogCount = 0;
  public int selectedDialogIndex = 0;
  public int peerCount = 0;
  public int secretChatCount = 0;
  public int historySlotCount = 0;

  public int composePeerKind = DialogListModel.PEER_UNKNOWN;
  public long composePeerId = 0;
  public String composePeerTitle = "";
  public String composeText = "";
  public int composeStatus = ComposeStateModel.STATUS_IDLE;
  public long composeLastRandomId = 0;
  public int composeLastMessageId = 0;
  public int composeLastErrorCode = 0;
  public String composeLastErrorMessage = "";
  public boolean composeDirty = false;
  public int composeContextMode = ComposeStateModel.CONTEXT_NONE;
  public int composeReplyToMessageId = 0;
  public String composeContextLabel = "";
  public String composeContextPreview = "";
  public int composeForwardFromPeerKind = DialogListModel.PEER_UNKNOWN;
  public long composeForwardFromPeerId = 0;
  public int composeForwardMessageId = 0;
  public String loginPhoneNumber = "";
  public String loginPhoneCodeHash = "";
  public long loginCodeSentAtMillis = 0;

  public WarmStateSnapshot() {
    dialogs = new DialogEntry[DialogListModel.DEFAULT_CAPACITY];
    for (int i = 0; i < dialogs.length; ++i) {
      dialogs[i] = new DialogEntry();
    }

    peers = new PeerEntry[PeerDirectory.DEFAULT_CAPACITY];
    for (int i = 0; i < peers.length; ++i) {
      peers[i] = new PeerEntry();
    }

    secretChats = new SecretChatEntry[SecretChatDirectory.DEFAULT_CAPACITY];
    for (int i = 0; i < secretChats.length; ++i) {
      secretChats[i] = new SecretChatEntry();
    }

    historySlots = new HistorySlotEntry[ReaderStateModel.DEFAULT_HISTORY_SLOTS];
    for (int i = 0; i < historySlots.length; ++i) {
      historySlots[i] = new HistorySlotEntry();
    }

    outbound = new OutboundRecoveryState();
    clear();
  }

  public void clear() {
    version = VERSION;
    route = 0;
    dialogCount = 0;
    selectedDialogIndex = 0;
    peerCount = 0;
    secretChatCount = 0;
    historySlotCount = 0;
    composePeerKind = DialogListModel.PEER_UNKNOWN;
    composePeerId = 0;
    composePeerTitle = "";
    composeText = "";
    composeStatus = ComposeStateModel.STATUS_IDLE;
    composeLastRandomId = 0;
    composeLastMessageId = 0;
    composeLastErrorCode = 0;
    composeLastErrorMessage = "";
    composeDirty = false;
    composeContextMode = ComposeStateModel.CONTEXT_NONE;
    composeReplyToMessageId = 0;
    composeContextLabel = "";
    composeContextPreview = "";
    composeForwardFromPeerKind = DialogListModel.PEER_UNKNOWN;
    composeForwardFromPeerId = 0;
    composeForwardMessageId = 0;
    loginPhoneNumber = "";
    loginPhoneCodeHash = "";
    loginCodeSentAtMillis = 0;
    outbound.clear();

    for (int i = 0; i < dialogs.length; ++i) {
      dialogs[i].clear();
    }
    for (int i = 0; i < peers.length; ++i) {
      peers[i].clear();
    }
    for (int i = 0; i < secretChats.length; ++i) {
      secretChats[i].clear();
    }
    for (int i = 0; i < historySlots.length; ++i) {
      historySlots[i].clear();
    }
  }

  public boolean hasUsefulShellState() {
    return dialogCount > 0 ||
      peerCount > 0 ||
      secretChatCount > 0 ||
      historySlotCount > 0 ||
      (composeText != null && composeText.length() > 0) ||
      composeContextMode != ComposeStateModel.CONTEXT_NONE ||
      outbound.hasUsefulState();
  }

  public boolean hasUsefulLoginState() {
    return loginPhoneNumber != null &&
      loginPhoneNumber.length() > 0 &&
      loginPhoneCodeHash != null &&
      loginPhoneCodeHash.length() > 0 &&
      loginCodeSentAtMillis > 0;
  }

  public boolean hasUsefulState() {
    return hasUsefulShellState() || hasUsefulLoginState();
  }
}
