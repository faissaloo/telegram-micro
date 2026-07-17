package peer;

import reader.DialogListModel;
import reader.SecretChatDirectory;

public final class PeerActionStateModel {
  public static final int ABOUT_MAX = 160;
  public static final int DETAIL_MAX = 96;

  public static final int STATUS_IDLE = 0;
  public static final int STATUS_READY = 1;
  public static final int STATUS_ACTION = 2;
  public static final int STATUS_ERROR = 3;
  public static final int STATUS_PENDING_SECRET = 4;

  public int peerKind = DialogListModel.PEER_UNKNOWN;
  public long peerId = 0;
  public int dialogIndex = -1;
  public boolean hasDialog = false;
  public boolean hasAccessHash = false;
  public boolean minAccessHash = false;
  public boolean canCompose = false;
  public boolean canJoin = false;
  public boolean canLeave = false;
  public boolean canSecretChat = false;
  public boolean canAcceptSecretChat = false;
  public boolean canDiscardSecretChat = false;
  public int status = STATUS_IDLE;
  public int participantCount = 0;
  public boolean participantCountKnown = false;
  public int secretChatId = 0;
  public String title = "No peer selected";
  public String username = "";
  public String summary = "IDLE";
  public String detail = "Open a dialog or discovery result to inspect peer actions.";
  public String about = "No peer selected.";
  public String secretSummary = "UNAVAILABLE";

  public void clear() {
    peerKind = DialogListModel.PEER_UNKNOWN;
    peerId = 0;
    dialogIndex = -1;
    hasDialog = false;
    hasAccessHash = false;
    minAccessHash = false;
    canCompose = false;
    canJoin = false;
    canLeave = false;
    canSecretChat = false;
    canAcceptSecretChat = false;
    canDiscardSecretChat = false;
    status = STATUS_IDLE;
    participantCount = 0;
    participantCountKnown = false;
    secretChatId = 0;
    title = "No peer selected";
    username = "";
    summary = "IDLE";
    detail = "Open a dialog or discovery result to inspect peer actions.";
    about = "No peer selected.";
    secretSummary = "UNAVAILABLE";
  }

  public void bind(
    int peerKind,
    long peerId,
    int dialogIndex,
    boolean hasDialog,
    boolean hasAccessHash,
    boolean minAccessHash,
    String title,
    String username
  ) {
    this.peerKind = peerKind;
    this.peerId = peerId;
    this.dialogIndex = dialogIndex;
    this.hasDialog = hasDialog;
    this.hasAccessHash = hasAccessHash;
    this.minAccessHash = minAccessHash;
    this.title = title != null && title.length() > 0
      ? DialogListModel.limit(title, DialogListModel.TITLE_MAX)
      : fallbackTitle(peerKind, peerId);
    this.username = username != null ? DialogListModel.limit(username, 24) : "";
    this.canCompose = peerId != 0 &&
      (peerKind == DialogListModel.PEER_CHAT || hasAccessHash);
    this.canJoin = peerKind == DialogListModel.PEER_CHANNEL && !hasDialog && hasAccessHash;
    this.canLeave = peerKind == DialogListModel.PEER_CHANNEL && hasDialog;
    this.canSecretChat = peerKind == DialogListModel.PEER_USER && hasAccessHash && !minAccessHash;
    this.canAcceptSecretChat = false;
    this.canDiscardSecretChat = false;
    this.status = STATUS_READY;
    this.summary = kindName();
    this.detail = hasDialog
      ? "Dialog-linked peer actions are available."
      : "Cached peer actions are available.";
    this.about = this.username.length() > 0
      ? "@" + this.username
      : "No cached username.";
    this.secretSummary = canSecretChat
      ? "AVAILABLE"
      : "UNAVAILABLE";
    this.participantCount = 0;
    this.participantCountKnown = false;
    this.secretChatId = 0;
  }

  public void noteAction(String summary, String detail) {
    status = STATUS_ACTION;
    if (summary != null && summary.length() > 0) {
      this.summary = summary;
    }
    this.detail = detail != null && detail.length() > 0
      ? DialogListModel.limit(detail, DETAIL_MAX)
      : "Peer action in progress.";
  }

  public void noteReady(String summary, String detail) {
    status = STATUS_READY;
    if (summary != null && summary.length() > 0) {
      this.summary = summary;
    } else {
      this.summary = kindName();
    }
    this.detail = detail != null && detail.length() > 0
      ? DialogListModel.limit(detail, DETAIL_MAX)
      : "Peer action completed.";
  }

  public void noteError(String detail) {
    status = STATUS_ERROR;
    summary = "ERROR";
    this.detail = detail != null && detail.length() > 0
      ? DialogListModel.limit(detail, DETAIL_MAX)
      : "Peer action failed.";
  }

  public void noteSecretPending(String detail) {
    status = STATUS_PENDING_SECRET;
    summary = "SECRET";
    secretSummary = "PENDING";
    secretChatId = 0;
    canAcceptSecretChat = false;
    canDiscardSecretChat = false;
    this.detail = detail != null && detail.length() > 0
      ? DialogListModel.limit(detail, DETAIL_MAX)
      : "Secret chat bootstrap is pending.";
  }

  public void noteMembershipChanged(boolean joined, String detail) {
    status = STATUS_READY;
    if (peerKind == DialogListModel.PEER_CHANNEL) {
      hasDialog = joined;
      if (!joined) {
        dialogIndex = -1;
      }
      canJoin = !joined && hasAccessHash;
      canLeave = joined;
    }
    summary = joined ? "JOINED" : "LEFT";
    canAcceptSecretChat = false;
    canDiscardSecretChat = false;
    this.detail = detail != null && detail.length() > 0
      ? DialogListModel.limit(detail, DETAIL_MAX)
      : (joined ? "Peer action completed." : "Peer action completed.");
  }

  public void noteSecretReady(int chatId, int secretState, String detail) {
    status = STATUS_READY;
    summary = "SECRET";
    secretChatId = chatId;
    canSecretChat = false;
    canAcceptSecretChat = false;
    canDiscardSecretChat = true;
    secretSummary = formatSecretSummary(chatId, secretState);
    this.detail = detail != null && detail.length() > 0
      ? DialogListModel.limit(detail, DETAIL_MAX)
      : "Secret chat bootstrap completed.";
  }

  public void applyExistingSecret(int chatId, int secretState, boolean outbound, long keyFingerprint) {
    if (chatId == 0) {
      return;
    }
    secretChatId = chatId;
    canSecretChat = secretState == SecretChatDirectory.STATE_CLOSED &&
      peerKind == DialogListModel.PEER_USER &&
      hasAccessHash &&
      !minAccessHash;
    canAcceptSecretChat = secretState == SecretChatDirectory.STATE_REQUESTED && !outbound;
    canDiscardSecretChat = secretState != SecretChatDirectory.STATE_CLOSED;
    secretSummary = formatSecretSummary(chatId, secretState);
    if (status == STATUS_READY) {
      if (canAcceptSecretChat) {
        detail = "Inbound secret chat can be accepted.";
      } else if (secretState == SecretChatDirectory.STATE_CLOSED) {
        detail = DialogListModel.limit("Tracked secret chat was closed locally.", DETAIL_MAX);
      } else if (secretState == SecretChatDirectory.STATE_READY && keyFingerprint != 0) {
        detail = DialogListModel.limit("Tracked secret chat fingerprint is ready.", DETAIL_MAX);
      } else {
        detail = DialogListModel.limit(
          outbound
            ? "Existing outbound secret chat is tracked locally."
            : "Existing inbound secret chat is tracked locally.",
          DETAIL_MAX
        );
      }
    }
  }

  public void applyInfo(String about, int participantCount, boolean participantCountKnown, String detail) {
    if (about != null && about.length() > 0) {
      this.about = DialogListModel.limit(about, ABOUT_MAX);
    } else if (peerKind == DialogListModel.PEER_USER) {
      this.about = "No cached bio.";
    } else {
      this.about = "No cached description.";
    }
    if (participantCountKnown) {
      noteParticipantCount(participantCount);
    }
    noteReady(kindName(), detail);
  }

  public void noteParticipantCount(int participantCount) {
    if (participantCount < 0) {
      return;
    }
    this.participantCount = participantCount;
    this.participantCountKnown = true;
  }

  public String statusName() {
    switch (status) {
      case STATUS_READY:
        return "READY";
      case STATUS_ACTION:
        return "ACTION";
      case STATUS_ERROR:
        return "ERROR";
      case STATUS_PENDING_SECRET:
        return "SECRET";
      default:
        return "IDLE";
    }
  }

  public String kindName() {
    switch (peerKind) {
      case DialogListModel.PEER_USER:
        return "USER";
      case DialogListModel.PEER_CHAT:
        return "GROUP";
      case DialogListModel.PEER_CHANNEL:
        return "CHANNEL";
      default:
        return "UNKNOWN";
    }
  }

  private static String fallbackTitle(int peerKind, long peerId) {
    switch (peerKind) {
      case DialogListModel.PEER_USER:
        return "USER " + Long.toString(peerId);
      case DialogListModel.PEER_CHAT:
        return "GROUP " + Long.toString(peerId);
      case DialogListModel.PEER_CHANNEL:
        return "CHANNEL " + Long.toString(peerId);
      default:
        return "PEER " + Long.toString(peerId);
    }
  }

  private static String formatSecretSummary(int chatId, int secretState) {
    if (secretState == 1) {
      return "WAITING #" + Integer.toString(chatId);
    }
    if (secretState == 2) {
      return "REQUESTED #" + Integer.toString(chatId);
    }
    if (secretState == 4) {
      return "CLOSED #" + Integer.toString(chatId);
    }
    return "READY #" + Integer.toString(chatId);
  }
}
