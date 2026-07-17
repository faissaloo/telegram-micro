package compose;

import reader.DialogListModel;

public class ComposeStateModel {
  public static final int STATUS_IDLE = 0;
  public static final int STATUS_READY = 1;
  public static final int STATUS_SENDING = 2;
  public static final int STATUS_SENT = 3;
  public static final int STATUS_FAILED = 4;
  public static final int CONTEXT_NONE = 0;
  public static final int CONTEXT_REPLY = 1;
  public static final int CONTEXT_FORWARD = 2;

  public static final int TEXT_MAX = 160;
  public static final int CONTEXT_LABEL_MAX = 40;
  public static final int CONTEXT_PREVIEW_MAX = 64;

  public int peerKind = DialogListModel.PEER_UNKNOWN;
  public long peerId = 0;
  public String peerTitle = "No dialog selected";
  public String text = "";
  public int status = STATUS_IDLE;
  public long lastRandomId = 0;
  public int lastMessageId = 0;
  public int lastErrorCode = 0;
  public String lastErrorMessage = "";
  public boolean dirty = false;
  public int contextMode = CONTEXT_NONE;
  public int replyToMessageId = 0;
  public String contextLabel = "";
  public String contextPreview = "";
  public int forwardFromPeerKind = DialogListModel.PEER_UNKNOWN;
  public long forwardFromPeerId = 0;
  public int forwardMessageId = 0;

  public void bindPeer(int peerKind, long peerId, String peerTitle) {
    rebindPeer(peerKind, peerId, peerTitle, false, false);
  }

  public void rebindPeer(
    int peerKind,
    long peerId,
    String peerTitle,
    boolean preserveDraft,
    boolean preserveContext
  ) {
    String previousText = preserveDraft ? text : "";
    int previousContextMode = preserveContext ? contextMode : CONTEXT_NONE;
    int previousReplyToMessageId = preserveContext ? replyToMessageId : 0;
    String previousContextLabel = preserveContext ? contextLabel : "";
    String previousContextPreview = preserveContext ? contextPreview : "";
    int previousForwardFromPeerKind = preserveContext ? forwardFromPeerKind : DialogListModel.PEER_UNKNOWN;
    long previousForwardFromPeerId = preserveContext ? forwardFromPeerId : 0;
    int previousForwardMessageId = preserveContext ? forwardMessageId : 0;

    if (this.peerKind == peerKind && this.peerId == peerId) {
      if (peerTitle != null && peerTitle.length() > 0) {
        this.peerTitle = DialogListModel.limit(peerTitle, DialogListModel.TITLE_MAX);
      }
      return;
    }

    this.peerKind = peerKind;
    this.peerId = peerId;
    this.peerTitle = peerTitle != null && peerTitle.length() > 0
      ? DialogListModel.limit(peerTitle, DialogListModel.TITLE_MAX)
      : "No dialog selected";
    clearDraft();
    clearContext();
    if (preserveDraft) {
      text = previousText != null ? previousText : "";
    }
    if (preserveContext) {
      contextMode = previousContextMode;
      replyToMessageId = previousReplyToMessageId;
      contextLabel = previousContextLabel != null ? previousContextLabel : "";
      contextPreview = previousContextPreview != null ? previousContextPreview : "";
      forwardFromPeerKind = previousForwardFromPeerKind;
      forwardFromPeerId = previousForwardFromPeerId;
      forwardMessageId = previousForwardMessageId;
    }
    dirty = text.length() > 0 || hasForwardSource();
    status = dirty ? STATUS_READY : STATUS_IDLE;
    lastRandomId = 0;
    lastMessageId = 0;
    lastErrorCode = 0;
    lastErrorMessage = "";
  }

  public void clearDraft() {
    text = "";
    status = STATUS_IDLE;
    dirty = false;
  }

  public void clearContext() {
    contextMode = CONTEXT_NONE;
    replyToMessageId = 0;
    contextLabel = "";
    contextPreview = "";
    forwardFromPeerKind = DialogListModel.PEER_UNKNOWN;
    forwardFromPeerId = 0;
    forwardMessageId = 0;
    if (text.length() == 0 && status != STATUS_SENDING) {
      status = STATUS_IDLE;
      dirty = false;
    }
  }

  public void prepareReplyContext(int messageId, String sender, String text) {
    contextMode = CONTEXT_REPLY;
    replyToMessageId = messageId > 0 ? messageId : 0;
    contextLabel = DialogListModel.limit(
      sender != null && sender.length() > 0 ? sender : "message",
      CONTEXT_LABEL_MAX
    );
    contextPreview = DialogListModel.limit(
      text != null && text.length() > 0 ? text : "(empty)",
      CONTEXT_PREVIEW_MAX
    );
  }

  public boolean prepareForwardContext(
    int fromPeerKind,
    long fromPeerId,
    int messageId,
    String sender,
    String text
  ) {
    String body = text != null && text.length() > 0 ? text : "(empty)";

    if (fromPeerKind == DialogListModel.PEER_UNKNOWN || fromPeerId == 0 || messageId == 0) {
      return false;
    }

    clearDraft();
    clearContext();
    contextMode = CONTEXT_FORWARD;
    forwardFromPeerKind = fromPeerKind;
    forwardFromPeerId = fromPeerId;
    forwardMessageId = messageId;
    contextLabel = DialogListModel.limit(
      sender != null && sender.length() > 0 ? sender : "message",
      CONTEXT_LABEL_MAX
    );
    contextPreview = DialogListModel.limit(body, CONTEXT_PREVIEW_MAX);
    dirty = true;
    status = STATUS_READY;
    return true;
  }

  public boolean hasForwardSource() {
    return contextMode == CONTEXT_FORWARD &&
      forwardFromPeerKind != DialogListModel.PEER_UNKNOWN &&
      forwardFromPeerId != 0 &&
      forwardMessageId != 0;
  }

  public String contextName() {
    switch (contextMode) {
      case CONTEXT_REPLY:
        return "REPLY";
      case CONTEXT_FORWARD:
        return "FORWARD";
      default:
        return "NONE";
    }
  }

  public boolean replaceText(String text) {
    String next = text != null ? text : "";

    if (next.length() > TEXT_MAX) {
      return false;
    }

    this.text = next;
    dirty = next.length() > 0 || hasForwardSource();
    if (status != STATUS_SENDING) {
      status = dirty ? STATUS_READY : STATUS_IDLE;
    }
    if (status != STATUS_FAILED) {
      lastErrorCode = 0;
      lastErrorMessage = "";
    }
    return true;
  }

  public boolean canSend() {
    return peerKind != DialogListModel.PEER_UNKNOWN &&
      peerId != 0 &&
      (text.length() > 0 || hasForwardSource()) &&
      status != STATUS_SENDING;
  }

  public void markSending(long randomId) {
    status = STATUS_SENDING;
    lastRandomId = randomId;
    lastErrorCode = 0;
    lastErrorMessage = "";
  }

  public void markSent(int messageId) {
    status = STATUS_SENT;
    lastMessageId = messageId;
    lastErrorCode = 0;
    lastErrorMessage = "";
    clearDraft();
    clearContext();
    status = STATUS_SENT;
  }

  public void markFailed(int errorCode, String errorMessage) {
    status = STATUS_FAILED;
    lastErrorCode = errorCode;
    lastErrorMessage = errorMessage != null ? errorMessage : "";
  }

  public String statusName() {
    switch (status) {
      case STATUS_READY:
        return "READY";
      case STATUS_SENDING:
        return "SENDING";
      case STATUS_SENT:
        return "SENT";
      case STATUS_FAILED:
        return "FAILED";
      default:
        return "IDLE";
    }
  }
}
