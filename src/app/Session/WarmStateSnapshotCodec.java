package session;

import compose.ComposeStateModel;
import reader.DialogListModel;
import reader.MessageListModel;
import reader.PeerDirectory;
import reader.SecretChatDirectory;
import support.ByteArrayPlus;
import support.Decode;

public final class WarmStateSnapshotCodec {
  public static final int MAGIC = 0x53574d54;
  public static final int VERSION = 8;
  public static final int MAX_ENCODED_BYTES = 128 * 1024;
  private static final int VERSION_V1 = 1;
  private static final int VERSION_V2 = 2;
  private static final int VERSION_V3 = 3;
  private static final int VERSION_V4 = 4;
  private static final int VERSION_V5 = 5;
  private static final int VERSION_V6 = 6;

  private WarmStateSnapshotCodec() {
  }

  public static byte[] encode(WarmStateSnapshot snapshot) {
    ByteArrayPlus out = new ByteArrayPlus();

    if (snapshot == null) {
      return null;
    }

    out.append_int(MAGIC);
    out.append_int(VERSION);
    out.append_int(snapshot.route);
    out.append_int(snapshot.dialogCount);
    out.append_int(snapshot.selectedDialogIndex);
    out.append_int(snapshot.peerCount);
    out.append_int(snapshot.secretChatCount);
    out.append_int(snapshot.historySlotCount);

    for (int i = 0; i < snapshot.dialogCount && i < snapshot.dialogs.length; ++i) {
      WarmStateSnapshot.DialogEntry dialog = snapshot.dialogs[i];

      out.append_int(dialog.peerKind);
      out.append_long(dialog.peerId);
      out.append_long(dialog.accessHash);
      out.append_int(
        (dialog.hasAccessHash ? 1 : 0) |
        ((dialog.hasMedia ? 1 : 0) << 1)
      );
      out.append_int(dialog.topMessageId);
      out.append_int(dialog.unreadCount);
      appendString(out, dialog.title, DialogListModel.TITLE_MAX);
      appendString(out, dialog.preview, DialogListModel.PREVIEW_MAX);
    }

    for (int i = 0; i < snapshot.peerCount && i < snapshot.peers.length; ++i) {
      WarmStateSnapshot.PeerEntry peer = snapshot.peers[i];

      out.append_int(peer.peerKind);
      out.append_long(peer.peerId);
      out.append_long(peer.accessHash);
      out.append_int(
        (peer.hasAccessHash ? 1 : 0) |
        ((peer.minAccessHash ? 1 : 0) << 1)
      );
      appendString(out, peer.title, PeerDirectory.TITLE_MAX);
      appendString(out, peer.username, PeerDirectory.USERNAME_MAX);
    }

    for (int i = 0; i < snapshot.secretChatCount && i < snapshot.secretChats.length; ++i) {
      WarmStateSnapshot.SecretChatEntry secretChat = snapshot.secretChats[i];

      if (secretChat.pendingKeyData != null &&
          secretChat.pendingKeyData.length > SecretChatDirectory.PENDING_KEY_BYTES_MAX) {
        return null;
      }

      out.append_int(secretChat.chatId);
      out.append_int(secretChat.peerKind);
      out.append_long(secretChat.peerId);
      out.append_long(secretChat.accessHash);
      out.append_int(secretChat.state);
      out.append_int(secretChat.outbound ? 1 : 0);
      out.append_int(secretChat.date);
      out.append_long(secretChat.keyFingerprint);
      appendBytes(out, secretChat.pendingKeyData);
    }

    for (int i = 0; i < snapshot.historySlotCount && i < snapshot.historySlots.length; ++i) {
      WarmStateSnapshot.HistorySlotEntry slot = snapshot.historySlots[i];

      out.append_int(slot.peerKind);
      out.append_long(slot.peerId);
      out.append_int(slot.loaded ? 1 : 0);
      out.append_int(slot.lastAccessTick);
      out.append_int(slot.messageCount);

      for (int j = 0; j < slot.messageCount && j < slot.messages.length; ++j) {
        WarmStateSnapshot.MessageEntry message = slot.messages[j];

        out.append_int(message.messageId);
        out.append_int(
          (message.outgoing ? 1 : 0) |
          ((message.pending ? 1 : 0) << 1) |
          ((message.hasMedia ? 1 : 0) << 2)
        );
        appendString(out, message.sender, MessageListModel.SENDER_MAX);
        appendString(out, message.text, MessageListModel.TEXT_MAX);
        appendString(out, message.mediaLabel, MessageListModel.MEDIA_LABEL_MAX);
        appendString(out, message.mediaInfo, MessageListModel.MEDIA_INFO_MAX);
        appendMediaReference(out, message.mediaRef);
      }
    }

    out.append_int(snapshot.composePeerKind);
    out.append_long(snapshot.composePeerId);
    out.append_int(snapshot.composeStatus);
    out.append_long(snapshot.composeLastRandomId);
    out.append_int(snapshot.composeLastMessageId);
    out.append_int(snapshot.composeLastErrorCode);
    out.append_int(snapshot.composeDirty ? 1 : 0);
    appendString(out, snapshot.composePeerTitle, DialogListModel.TITLE_MAX);
    appendString(out, snapshot.composeText, ComposeStateModel.TEXT_MAX);
    appendString(out, snapshot.composeLastErrorMessage, WarmStateSnapshot.COMPOSE_ERROR_MAX);
    out.append_int(snapshot.composeContextMode);
    out.append_int(snapshot.composeReplyToMessageId);
    out.append_int(snapshot.composeForwardFromPeerKind);
    out.append_long(snapshot.composeForwardFromPeerId);
    out.append_int(snapshot.composeForwardMessageId);
    appendString(out, snapshot.composeContextLabel, WarmStateSnapshot.COMPOSE_CONTEXT_LABEL_MAX);
    appendString(out, snapshot.composeContextPreview, WarmStateSnapshot.COMPOSE_CONTEXT_PREVIEW_MAX);

    out.append_int(snapshot.outbound.sequence);
    out.append_int(snapshot.outbound.kind);
    out.append_int(snapshot.outbound.phase);
    out.append_int(snapshot.outbound.peerKind);
    out.append_long(snapshot.outbound.peerId);
    out.append_int(snapshot.outbound.code);
    out.append_int(snapshot.outbound.detail);
    appendString(out, snapshot.outbound.label, OutboundRecoveryState.LABEL_MAX);
    appendString(out, snapshot.loginPhoneNumber, WarmStateSnapshot.LOGIN_PHONE_MAX);
    appendString(out, snapshot.loginPhoneCodeHash, WarmStateSnapshot.LOGIN_CODE_HASH_MAX);
    out.append_long(snapshot.loginCodeSentAtMillis);
    byte[] encoded = out.toByteArray();
    return encoded.length <= MAX_ENCODED_BYTES ? encoded : null;
  }

  public static boolean decode(WarmStateSnapshot out, byte[] data) {
    int version;

    if (out == null || data == null || data.length < 8 ||
        data.length > MAX_ENCODED_BYTES) {
      return false;
    }
    if (Decode.Little.int_decode(data, 0) != MAGIC) {
      return false;
    }

    version = Decode.Little.int_decode(data, 4);
    if (version == VERSION_V1) {
      return decodeV1(out, data, 8);
    }
    if (version == VERSION_V2) {
      return decodeV2(out, data, 8);
    }
    if (version == VERSION_V3) {
      return decodeV3(out, data, 8);
    }
    if (version == VERSION_V4) {
      return decodeV4(out, data, 8);
    }
    if (version == VERSION_V5) {
      return decodeV5(out, data, 8);
    }
    if (version == VERSION_V6) {
      return decodeV6(out, data, 8);
    }
    if (version == 7) {
      return decodeV7(out, data, 8);
    }
    if (version == VERSION) {
      return decodeV8(out, data, 8);
    }
    return false;
  }

  private static boolean decodeV1(WarmStateSnapshot out, byte[] data, int offset) {
    if (offset + 16 > data.length) {
      return false;
    }

    out.clear();
    out.version = VERSION_V1;
    out.route = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.dialogCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.selectedDialogIndex = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.historySlotCount = Decode.Little.int_decode(data, offset);
    offset += 4;

    if (!countsValid(out, false)) {
      out.clear();
      return false;
    }

    offset = decodeDialogs(out, data, offset);
    if (offset < 0) {
      out.clear();
      return false;
    }

    offset = decodeHistorySlots(out, data, offset);
    if (offset < 0) {
      out.clear();
      return false;
    }

    offset = decodeComposeAndOutbound(out, data, offset);
    if (offset < 0) {
      out.clear();
      return false;
    }

    return offset == data.length;
  }

  private static boolean decodeV2(WarmStateSnapshot out, byte[] data, int offset) {
    offset = decodeV2Body(out, data, offset);
    return offset >= 0 && offset == data.length;
  }

  private static boolean decodeV3(WarmStateSnapshot out, byte[] data, int offset) {
    offset = decodeV2Body(out, data, offset);
    if (offset < 0) {
      out.clear();
      return false;
    }
    out.version = VERSION_V3;

    out.loginPhoneNumber = readString(data, WarmStateSnapshot.LOGIN_PHONE_MAX, offset);
    if (out.loginPhoneNumber == null) {
      out.clear();
      return false;
    }
    offset = stringNextOffset(data, offset);
    out.loginPhoneCodeHash = readString(data, WarmStateSnapshot.LOGIN_CODE_HASH_MAX, offset);
    if (out.loginPhoneCodeHash == null) {
      out.clear();
      return false;
    }
    offset = stringNextOffset(data, offset);
    if (offset + 8 > data.length) {
      out.clear();
      return false;
    }
    out.loginCodeSentAtMillis = Decode.Little.long_decode(data, offset);
    offset += 8;
    return offset == data.length;
  }

  private static boolean decodeV4(WarmStateSnapshot out, byte[] data, int offset) {
    offset = decodeV4Body(out, data, offset);
    if (offset < 0) {
      out.clear();
      return false;
    }
    out.version = VERSION_V4;

    out.loginPhoneNumber = readString(data, WarmStateSnapshot.LOGIN_PHONE_MAX, offset);
    if (out.loginPhoneNumber == null) {
      out.clear();
      return false;
    }
    offset = stringNextOffset(data, offset);
    out.loginPhoneCodeHash = readString(data, WarmStateSnapshot.LOGIN_CODE_HASH_MAX, offset);
    if (out.loginPhoneCodeHash == null) {
      out.clear();
      return false;
    }
    offset = stringNextOffset(data, offset);
    if (offset + 8 > data.length) {
      out.clear();
      return false;
    }
    out.loginCodeSentAtMillis = Decode.Little.long_decode(data, offset);
    offset += 8;
    return offset == data.length;
  }

  private static boolean decodeV5(WarmStateSnapshot out, byte[] data, int offset) {
    offset = decodeV5Body(out, data, offset);
    if (offset < 0) {
      out.clear();
      return false;
    }
    out.version = VERSION_V5;

    out.loginPhoneNumber = readString(data, WarmStateSnapshot.LOGIN_PHONE_MAX, offset);
    if (out.loginPhoneNumber == null) {
      out.clear();
      return false;
    }
    offset = stringNextOffset(data, offset);
    out.loginPhoneCodeHash = readString(data, WarmStateSnapshot.LOGIN_CODE_HASH_MAX, offset);
    if (out.loginPhoneCodeHash == null) {
      out.clear();
      return false;
    }
    offset = stringNextOffset(data, offset);
    if (offset + 8 > data.length) {
      out.clear();
      return false;
    }
    out.loginCodeSentAtMillis = Decode.Little.long_decode(data, offset);
    offset += 8;
    return offset == data.length;
  }

  private static boolean decodeV6(WarmStateSnapshot out, byte[] data, int offset) {
    offset = decodeV6Body(out, data, offset);
    if (offset < 0) {
      out.clear();
      return false;
    }
    out.version = VERSION_V6;

    out.loginPhoneNumber = readString(data, WarmStateSnapshot.LOGIN_PHONE_MAX, offset);
    if (out.loginPhoneNumber == null) {
      out.clear();
      return false;
    }
    offset = stringNextOffset(data, offset);
    out.loginPhoneCodeHash = readString(data, WarmStateSnapshot.LOGIN_CODE_HASH_MAX, offset);
    if (out.loginPhoneCodeHash == null) {
      out.clear();
      return false;
    }
    offset = stringNextOffset(data, offset);
    if (offset + 8 > data.length) {
      out.clear();
      return false;
    }
    out.loginCodeSentAtMillis = Decode.Little.long_decode(data, offset);
    offset += 8;
    return offset == data.length;
  }

  private static boolean decodeV7(WarmStateSnapshot out, byte[] data, int offset) {
    offset = decodeV7Body(out, data, offset);
    if (offset < 0) {
      out.clear();
      return false;
    }
    out.version = 7;

    out.loginPhoneNumber = readString(data, WarmStateSnapshot.LOGIN_PHONE_MAX, offset);
    if (out.loginPhoneNumber == null) {
      out.clear();
      return false;
    }
    offset = stringNextOffset(data, offset);
    out.loginPhoneCodeHash = readString(data, WarmStateSnapshot.LOGIN_CODE_HASH_MAX, offset);
    if (out.loginPhoneCodeHash == null) {
      out.clear();
      return false;
    }
    offset = stringNextOffset(data, offset);
    if (offset + 8 > data.length) {
      out.clear();
      return false;
    }
    out.loginCodeSentAtMillis = Decode.Little.long_decode(data, offset);
    offset += 8;
    return offset == data.length;
  }

  private static boolean decodeV8(WarmStateSnapshot out, byte[] data, int offset) {
    offset = decodeV8Body(out, data, offset);
    if (offset < 0) {
      out.clear();
      return false;
    }
    out.version = VERSION;

    out.loginPhoneNumber = readString(data, WarmStateSnapshot.LOGIN_PHONE_MAX, offset);
    if (out.loginPhoneNumber == null) {
      out.clear();
      return false;
    }
    offset = stringNextOffset(data, offset);
    out.loginPhoneCodeHash = readString(data, WarmStateSnapshot.LOGIN_CODE_HASH_MAX, offset);
    if (out.loginPhoneCodeHash == null) {
      out.clear();
      return false;
    }
    offset = stringNextOffset(data, offset);
    if (offset + 8 > data.length) {
      out.clear();
      return false;
    }
    out.loginCodeSentAtMillis = Decode.Little.long_decode(data, offset);
    offset += 8;
    return offset == data.length;
  }

  private static boolean countsValid(WarmStateSnapshot out, boolean withPeers) {
    if (out.dialogCount < 0 || out.dialogCount > out.dialogs.length) {
      return false;
    }
    if (out.selectedDialogIndex < 0 || out.selectedDialogIndex > out.dialogCount) {
      return false;
    }
    if (withPeers && (out.peerCount < 0 || out.peerCount > out.peers.length)) {
      return false;
    }
    if (withPeers && (out.secretChatCount < 0 || out.secretChatCount > out.secretChats.length)) {
      return false;
    }
    if (out.historySlotCount < 0 || out.historySlotCount > out.historySlots.length) {
      return false;
    }
    return true;
  }

  private static int decodeV2Body(WarmStateSnapshot out, byte[] data, int offset) {
    if (offset + 20 > data.length) {
      return -1;
    }

    out.clear();
    out.version = VERSION_V2;
    out.route = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.dialogCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.selectedDialogIndex = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.peerCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.historySlotCount = Decode.Little.int_decode(data, offset);
    offset += 4;

    if (!countsValid(out, true)) {
      out.clear();
      return -1;
    }

    offset = decodeDialogs(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodePeers(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeHistorySlots(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeComposeAndOutbound(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    return offset;
  }

  private static int decodeV4Body(WarmStateSnapshot out, byte[] data, int offset) {
    if (offset + 20 > data.length) {
      return -1;
    }

    out.clear();
    out.version = VERSION_V4;
    out.route = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.dialogCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.selectedDialogIndex = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.peerCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.secretChatCount = 0;
    out.historySlotCount = Decode.Little.int_decode(data, offset);
    offset += 4;

    if (!countsValid(out, true)) {
      out.clear();
      return -1;
    }

    offset = decodeDialogs(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodePeers(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeHistorySlots(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeComposeAndOutboundV4(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    return offset;
  }

  private static int decodeV5Body(WarmStateSnapshot out, byte[] data, int offset) {
    if (offset + 24 > data.length) {
      return -1;
    }

    out.clear();
    out.version = VERSION_V5;
    out.route = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.dialogCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.selectedDialogIndex = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.peerCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.secretChatCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.historySlotCount = Decode.Little.int_decode(data, offset);
    offset += 4;

    if (!countsValid(out, true)) {
      out.clear();
      return -1;
    }

    offset = decodeDialogs(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodePeers(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeSecretChats(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeHistorySlots(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeComposeAndOutboundV4(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    return offset;
  }

  private static int decodeV6Body(WarmStateSnapshot out, byte[] data, int offset) {
    if (offset + 24 > data.length) {
      return -1;
    }

    out.clear();
    out.version = VERSION_V6;
    out.route = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.dialogCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.selectedDialogIndex = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.peerCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.secretChatCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.historySlotCount = Decode.Little.int_decode(data, offset);
    offset += 4;

    if (!countsValid(out, true)) {
      out.clear();
      return -1;
    }

    offset = decodeDialogs(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodePeers(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeSecretChatsV6(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeHistorySlots(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeComposeAndOutboundV4(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    return offset;
  }

  private static int decodeV7Body(WarmStateSnapshot out, byte[] data, int offset) {
    if (offset + 24 > data.length) {
      return -1;
    }

    out.clear();
    out.version = VERSION;
    out.route = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.dialogCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.selectedDialogIndex = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.peerCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.secretChatCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.historySlotCount = Decode.Little.int_decode(data, offset);
    offset += 4;

    if (!countsValid(out, true)) {
      out.clear();
      return -1;
    }

    offset = decodeDialogs(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodePeers(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeSecretChatsV6(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeHistorySlotsV7(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeComposeAndOutboundV4(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    return offset;
  }

  private static int decodeV8Body(WarmStateSnapshot out, byte[] data, int offset) {
    if (offset + 24 > data.length) {
      return -1;
    }

    out.clear();
    out.version = VERSION;
    out.route = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.dialogCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.selectedDialogIndex = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.peerCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.secretChatCount = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.historySlotCount = Decode.Little.int_decode(data, offset);
    offset += 4;

    if (!countsValid(out, true)) {
      out.clear();
      return -1;
    }

    offset = decodeDialogs(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodePeers(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeSecretChatsV6(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeHistorySlotsV8(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    offset = decodeComposeAndOutboundV4(out, data, offset);
    if (offset < 0) {
      out.clear();
      return -1;
    }

    return offset;
  }

  private static int decodeDialogs(WarmStateSnapshot out, byte[] data, int offset) {
    for (int i = 0; i < out.dialogCount; ++i) {
      WarmStateSnapshot.DialogEntry dialog = out.dialogs[i];
      int flags;

      if (offset + 32 > data.length) {
        return -1;
      }
      dialog.peerKind = Decode.Little.int_decode(data, offset);
      offset += 4;
      dialog.peerId = Decode.Little.long_decode(data, offset);
      offset += 8;
      dialog.accessHash = Decode.Little.long_decode(data, offset);
      offset += 8;
      flags = Decode.Little.int_decode(data, offset);
      offset += 4;
      dialog.hasAccessHash = (flags & 1) != 0;
      dialog.hasMedia = (flags & 2) != 0;
      dialog.topMessageId = Decode.Little.int_decode(data, offset);
      offset += 4;
      dialog.unreadCount = Decode.Little.int_decode(data, offset);
      offset += 4;
      dialog.title = readString(data, DialogListModel.TITLE_MAX, offset);
      if (dialog.title == null) {
        return -1;
      }
      offset = stringNextOffset(data, offset);
      dialog.preview = readString(data, DialogListModel.PREVIEW_MAX, offset);
      if (dialog.preview == null) {
        return -1;
      }
      offset = stringNextOffset(data, offset);
    }
    return offset;
  }

  private static int decodePeers(WarmStateSnapshot out, byte[] data, int offset) {
    for (int i = 0; i < out.peerCount; ++i) {
      WarmStateSnapshot.PeerEntry peer = out.peers[i];
      int flags;

      if (offset + 24 > data.length) {
        return -1;
      }
      peer.peerKind = Decode.Little.int_decode(data, offset);
      offset += 4;
      peer.peerId = Decode.Little.long_decode(data, offset);
      offset += 8;
      peer.accessHash = Decode.Little.long_decode(data, offset);
      offset += 8;
      flags = Decode.Little.int_decode(data, offset);
      offset += 4;
      peer.hasAccessHash = (flags & 1) != 0;
      peer.minAccessHash = (flags & 2) != 0;
      peer.title = readString(data, PeerDirectory.TITLE_MAX, offset);
      if (peer.title == null) {
        return -1;
      }
      offset = stringNextOffset(data, offset);
      peer.username = readString(data, PeerDirectory.USERNAME_MAX, offset);
      if (peer.username == null) {
        return -1;
      }
      offset = stringNextOffset(data, offset);
    }
    return offset;
  }

  private static int decodeSecretChats(WarmStateSnapshot out, byte[] data, int offset) {
    for (int i = 0; i < out.secretChatCount; ++i) {
      WarmStateSnapshot.SecretChatEntry secretChat = out.secretChats[i];

      if (offset + 36 > data.length) {
        return -1;
      }
      secretChat.chatId = Decode.Little.int_decode(data, offset);
      offset += 4;
      secretChat.peerKind = Decode.Little.int_decode(data, offset);
      offset += 4;
      secretChat.peerId = Decode.Little.long_decode(data, offset);
      offset += 8;
      secretChat.accessHash = Decode.Little.long_decode(data, offset);
      offset += 8;
      secretChat.state = Decode.Little.int_decode(data, offset);
      offset += 4;
      secretChat.outbound = Decode.Little.int_decode(data, offset) != 0;
      offset += 4;
      secretChat.date = Decode.Little.int_decode(data, offset);
      offset += 4;
    }
    return offset;
  }

  private static int decodeSecretChatsV6(WarmStateSnapshot out, byte[] data, int offset) {
    for (int i = 0; i < out.secretChatCount; ++i) {
      WarmStateSnapshot.SecretChatEntry secretChat = out.secretChats[i];
      int pendingLength;

      if (offset + 44 > data.length) {
        return -1;
      }
      secretChat.chatId = Decode.Little.int_decode(data, offset);
      offset += 4;
      secretChat.peerKind = Decode.Little.int_decode(data, offset);
      offset += 4;
      secretChat.peerId = Decode.Little.long_decode(data, offset);
      offset += 8;
      secretChat.accessHash = Decode.Little.long_decode(data, offset);
      offset += 8;
      secretChat.state = Decode.Little.int_decode(data, offset);
      offset += 4;
      secretChat.outbound = Decode.Little.int_decode(data, offset) != 0;
      offset += 4;
      secretChat.date = Decode.Little.int_decode(data, offset);
      offset += 4;
      secretChat.keyFingerprint = Decode.Little.long_decode(data, offset);
      offset += 8;
      pendingLength = Decode.Little.int_decode(data, offset);
      offset += 4;
      if (pendingLength < 0 ||
          pendingLength > SecretChatDirectory.PENDING_KEY_BYTES_MAX ||
          pendingLength > data.length - offset) {
        return -1;
      }
      if (pendingLength > 0) {
        secretChat.pendingKeyData = new byte[pendingLength];
        System.arraycopy(data, offset, secretChat.pendingKeyData, 0, pendingLength);
        offset += pendingLength;
      } else {
        secretChat.pendingKeyData = null;
      }
    }
    return offset;
  }

  private static int decodeHistorySlots(WarmStateSnapshot out, byte[] data, int offset) {
    for (int i = 0; i < out.historySlotCount; ++i) {
      WarmStateSnapshot.HistorySlotEntry slot = out.historySlots[i];
      int loaded;

      if (offset + 24 > data.length) {
        return -1;
      }
      slot.peerKind = Decode.Little.int_decode(data, offset);
      offset += 4;
      slot.peerId = Decode.Little.long_decode(data, offset);
      offset += 8;
      loaded = Decode.Little.int_decode(data, offset);
      offset += 4;
      slot.loaded = loaded != 0;
      slot.lastAccessTick = Decode.Little.int_decode(data, offset);
      offset += 4;
      slot.messageCount = Decode.Little.int_decode(data, offset);
      offset += 4;

      if (slot.messageCount < 0 || slot.messageCount > slot.messages.length) {
        return -1;
      }

      for (int j = 0; j < slot.messageCount; ++j) {
        WarmStateSnapshot.MessageEntry message = slot.messages[j];
        int flags;

        if (offset + 8 > data.length) {
          return -1;
        }
        message.messageId = Decode.Little.int_decode(data, offset);
        offset += 4;
        flags = Decode.Little.int_decode(data, offset);
        offset += 4;
        message.outgoing = (flags & 1) != 0;
        message.pending = (flags & 2) != 0;
        message.hasMedia = (flags & 4) != 0;
        message.sender = readString(data, MessageListModel.SENDER_MAX, offset);
        if (message.sender == null) {
          return -1;
        }
        offset = stringNextOffset(data, offset);
        message.text = readString(data, MessageListModel.TEXT_MAX, offset);
        if (message.text == null) {
          return -1;
        }
        offset = stringNextOffset(data, offset);
      }
    }
    return offset;
  }

  private static int decodeHistorySlotsV7(WarmStateSnapshot out, byte[] data, int offset) {
    for (int i = 0; i < out.historySlotCount; ++i) {
      WarmStateSnapshot.HistorySlotEntry slot = out.historySlots[i];
      int loaded;

      if (offset + 24 > data.length) {
        return -1;
      }
      slot.peerKind = Decode.Little.int_decode(data, offset);
      offset += 4;
      slot.peerId = Decode.Little.long_decode(data, offset);
      offset += 8;
      loaded = Decode.Little.int_decode(data, offset);
      offset += 4;
      slot.loaded = loaded != 0;
      slot.lastAccessTick = Decode.Little.int_decode(data, offset);
      offset += 4;
      slot.messageCount = Decode.Little.int_decode(data, offset);
      offset += 4;

      if (slot.messageCount < 0 || slot.messageCount > slot.messages.length) {
        return -1;
      }

      for (int j = 0; j < slot.messageCount; ++j) {
        WarmStateSnapshot.MessageEntry message = slot.messages[j];
        int flags;

        if (offset + 8 > data.length) {
          return -1;
        }
        message.messageId = Decode.Little.int_decode(data, offset);
        offset += 4;
        flags = Decode.Little.int_decode(data, offset);
        offset += 4;
        message.outgoing = (flags & 1) != 0;
        message.pending = (flags & 2) != 0;
        message.hasMedia = (flags & 4) != 0;
        message.sender = readString(data, MessageListModel.SENDER_MAX, offset);
        if (message.sender == null) {
          return -1;
        }
        offset = stringNextOffset(data, offset);
        message.text = readString(data, MessageListModel.TEXT_MAX, offset);
        if (message.text == null) {
          return -1;
        }
        offset = stringNextOffset(data, offset);
        message.mediaLabel = readString(data, MessageListModel.MEDIA_LABEL_MAX, offset);
        if (message.mediaLabel == null) {
          return -1;
        }
        offset = stringNextOffset(data, offset);
        message.mediaInfo = readString(data, MessageListModel.MEDIA_INFO_MAX, offset);
        if (message.mediaInfo == null) {
          return -1;
        }
        offset = stringNextOffset(data, offset);
      }
    }
    return offset;
  }

  private static int decodeHistorySlotsV8(WarmStateSnapshot out, byte[] data, int offset) {
    for (int i = 0; i < out.historySlotCount; ++i) {
      WarmStateSnapshot.HistorySlotEntry slot = out.historySlots[i];
      int loaded;

      if (offset + 24 > data.length) {
        return -1;
      }
      slot.peerKind = Decode.Little.int_decode(data, offset);
      offset += 4;
      slot.peerId = Decode.Little.long_decode(data, offset);
      offset += 8;
      loaded = Decode.Little.int_decode(data, offset);
      offset += 4;
      slot.loaded = loaded != 0;
      slot.lastAccessTick = Decode.Little.int_decode(data, offset);
      offset += 4;
      slot.messageCount = Decode.Little.int_decode(data, offset);
      offset += 4;

      if (slot.messageCount < 0 || slot.messageCount > slot.messages.length) {
        return -1;
      }

      for (int j = 0; j < slot.messageCount; ++j) {
        WarmStateSnapshot.MessageEntry message = slot.messages[j];
        int flags;

        if (offset + 8 > data.length) {
          return -1;
        }
        message.messageId = Decode.Little.int_decode(data, offset);
        offset += 4;
        flags = Decode.Little.int_decode(data, offset);
        offset += 4;
        message.outgoing = (flags & 1) != 0;
        message.pending = (flags & 2) != 0;
        message.hasMedia = (flags & 4) != 0;
        message.sender = readString(data, MessageListModel.SENDER_MAX, offset);
        if (message.sender == null) {
          return -1;
        }
        offset = stringNextOffset(data, offset);
        message.text = readString(data, MessageListModel.TEXT_MAX, offset);
        if (message.text == null) {
          return -1;
        }
        offset = stringNextOffset(data, offset);
        message.mediaLabel = readString(data, MessageListModel.MEDIA_LABEL_MAX, offset);
        if (message.mediaLabel == null) {
          return -1;
        }
        offset = stringNextOffset(data, offset);
        message.mediaInfo = readString(data, MessageListModel.MEDIA_INFO_MAX, offset);
        if (message.mediaInfo == null) {
          return -1;
        }
        offset = stringNextOffset(data, offset);
        offset = readMediaReference(message.mediaRef, data, offset);
        if (offset < 0) {
          return -1;
        }
      }
    }
    return offset;
  }

  private static int decodeComposeAndOutbound(WarmStateSnapshot out, byte[] data, int offset) {
    if (offset + 36 > data.length) {
      return -1;
    }
    out.composePeerKind = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.composePeerId = Decode.Little.long_decode(data, offset);
    offset += 8;
    out.composeStatus = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.composeLastRandomId = Decode.Little.long_decode(data, offset);
    offset += 8;
    out.composeLastMessageId = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.composeLastErrorCode = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.composeDirty = Decode.Little.int_decode(data, offset) != 0;
    offset += 4;

    out.composePeerTitle = readString(data, DialogListModel.TITLE_MAX, offset);
    if (out.composePeerTitle == null) {
      return -1;
    }
    offset = stringNextOffset(data, offset);
    out.composeText = readString(data, ComposeStateModel.TEXT_MAX, offset);
    if (out.composeText == null) {
      return -1;
    }
    offset = stringNextOffset(data, offset);
    out.composeLastErrorMessage = readString(data, WarmStateSnapshot.COMPOSE_ERROR_MAX, offset);
    if (out.composeLastErrorMessage == null) {
      return -1;
    }
    offset = stringNextOffset(data, offset);
    out.composeContextMode = ComposeStateModel.CONTEXT_NONE;
    out.composeReplyToMessageId = 0;
    out.composeContextLabel = "";
    out.composeContextPreview = "";
    out.composeForwardFromPeerKind = DialogListModel.PEER_UNKNOWN;
    out.composeForwardFromPeerId = 0;
    out.composeForwardMessageId = 0;

    if (offset + 32 > data.length) {
      return -1;
    }
    out.outbound.sequence = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.outbound.kind = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.outbound.phase = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.outbound.peerKind = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.outbound.peerId = Decode.Little.long_decode(data, offset);
    offset += 8;
    out.outbound.code = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.outbound.detail = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.outbound.label = readString(data, OutboundRecoveryState.LABEL_MAX, offset);
    if (out.outbound.label == null) {
      return -1;
    }
    offset = stringNextOffset(data, offset);
    return offset;
  }

  private static int decodeComposeAndOutboundV4(WarmStateSnapshot out, byte[] data, int offset) {
    if (offset + 36 > data.length) {
      return -1;
    }
    out.composePeerKind = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.composePeerId = Decode.Little.long_decode(data, offset);
    offset += 8;
    out.composeStatus = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.composeLastRandomId = Decode.Little.long_decode(data, offset);
    offset += 8;
    out.composeLastMessageId = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.composeLastErrorCode = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.composeDirty = Decode.Little.int_decode(data, offset) != 0;
    offset += 4;

    out.composePeerTitle = readString(data, DialogListModel.TITLE_MAX, offset);
    if (out.composePeerTitle == null) {
      return -1;
    }
    offset = stringNextOffset(data, offset);
    out.composeText = readString(data, ComposeStateModel.TEXT_MAX, offset);
    if (out.composeText == null) {
      return -1;
    }
    offset = stringNextOffset(data, offset);
    out.composeLastErrorMessage = readString(data, WarmStateSnapshot.COMPOSE_ERROR_MAX, offset);
    if (out.composeLastErrorMessage == null) {
      return -1;
    }
    offset = stringNextOffset(data, offset);

    if (offset + 20 > data.length) {
      return -1;
    }
    out.composeContextMode = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.composeReplyToMessageId = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.composeForwardFromPeerKind = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.composeForwardFromPeerId = Decode.Little.long_decode(data, offset);
    offset += 8;
    out.composeForwardMessageId = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.composeContextLabel = readString(data, WarmStateSnapshot.COMPOSE_CONTEXT_LABEL_MAX, offset);
    if (out.composeContextLabel == null) {
      return -1;
    }
    offset = stringNextOffset(data, offset);
    out.composeContextPreview = readString(data, WarmStateSnapshot.COMPOSE_CONTEXT_PREVIEW_MAX, offset);
    if (out.composeContextPreview == null) {
      return -1;
    }
    offset = stringNextOffset(data, offset);

    if (offset + 32 > data.length) {
      return -1;
    }
    out.outbound.sequence = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.outbound.kind = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.outbound.phase = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.outbound.peerKind = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.outbound.peerId = Decode.Little.long_decode(data, offset);
    offset += 8;
    out.outbound.code = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.outbound.detail = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.outbound.label = readString(data, OutboundRecoveryState.LABEL_MAX, offset);
    if (out.outbound.label == null) {
      return -1;
    }
    offset = stringNextOffset(data, offset);
    return offset;
  }

  private static void appendString(ByteArrayPlus out, String value, int maxChars) {
    String text = value != null ? value : "";
    int length;
    char[] chars;

    if (text.length() > maxChars) {
      text = text.substring(0, maxChars);
    }

    length = text.length();
    out.append_int(length);
    chars = new char[length];
    text.getChars(0, length, chars, 0);
    for (int i = 0; i < length; ++i) {
      out.append_int(chars[i] & 0xffff);
    }
  }

  private static void appendBytes(ByteArrayPlus out, byte[] value) {
    int length = value != null ? value.length : 0;

    out.append_int(length);
    if (length > 0) {
      out.append_raw_bytes(value);
    }
  }

  private static void appendMediaReference(ByteArrayPlus out, media.MediaReference ref) {
    if (ref == null) {
      out.append_int(0);
      out.append_long(0);
      out.append_long(0);
      out.append_int(0);
      out.append_int(0);
      appendString(out, "", media.MediaReference.THUMB_SIZE_MAX);
      appendString(out, "", media.MediaReference.MIME_TYPE_MAX);
      appendBytes(out, null);
      return;
    }
    out.append_int(ref.kind);
    out.append_long(ref.id);
    out.append_long(ref.accessHash);
    out.append_int(ref.dcId);
    out.append_int(ref.expectedSize);
    appendString(out, ref.thumbSize, media.MediaReference.THUMB_SIZE_MAX);
    appendString(out, ref.mimeType, media.MediaReference.MIME_TYPE_MAX);
    if (ref.fileReferenceLength > 0) {
      byte[] bytes = new byte[ref.fileReferenceLength];
      System.arraycopy(ref.fileReference, 0, bytes, 0, ref.fileReferenceLength);
      appendBytes(out, bytes);
    } else {
      appendBytes(out, null);
    }
  }

  private static int readMediaReference(media.MediaReference out, byte[] data, int offset) {
    int fileReferenceLength;
    byte[] fileReference;

    if (offset + 28 > data.length || out == null) {
      return -1;
    }
    out.clear();
    out.kind = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.id = Decode.Little.long_decode(data, offset);
    offset += 8;
    out.accessHash = Decode.Little.long_decode(data, offset);
    offset += 8;
    out.dcId = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.expectedSize = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.thumbSize = readString(data, media.MediaReference.THUMB_SIZE_MAX, offset);
    if (out.thumbSize == null) {
      return -1;
    }
    offset = stringNextOffset(data, offset);
    out.mimeType = readString(data, media.MediaReference.MIME_TYPE_MAX, offset);
    if (out.mimeType == null) {
      return -1;
    }
    offset = stringNextOffset(data, offset);
    if (offset + 4 > data.length) {
      return -1;
    }
    fileReferenceLength = Decode.Little.int_decode(data, offset);
    offset += 4;
    if (fileReferenceLength < 0 || fileReferenceLength > media.MediaReference.FILE_REFERENCE_MAX ||
        offset + fileReferenceLength > data.length) {
      return -1;
    }
    if (fileReferenceLength > 0) {
      fileReference = new byte[fileReferenceLength];
      System.arraycopy(data, offset, fileReference, 0, fileReferenceLength);
      offset += fileReferenceLength;
      out.fileReferenceLength = fileReferenceLength;
      System.arraycopy(fileReference, 0, out.fileReference, 0, fileReferenceLength);
    }
    return offset;
  }

  private static String readString(byte[] data, int maxChars, int offset) {
    int length;
    char[] chars;

    if (offset + 4 > data.length) {
      return null;
    }
    length = Decode.Little.int_decode(data, offset);
    if (length < 0 || length > maxChars || offset + 4 + (length * 4) > data.length) {
      return null;
    }

    chars = new char[length];
    offset += 4;
    for (int i = 0; i < length; ++i) {
      chars[i] = (char)(Decode.Little.int_decode(data, offset) & 0xffff);
      offset += 4;
    }
    return new String(chars);
  }

  private static int stringNextOffset(byte[] data, int offset) {
    int length = Decode.Little.int_decode(data, offset);
    return offset + 4 + (length * 4);
  }
}
