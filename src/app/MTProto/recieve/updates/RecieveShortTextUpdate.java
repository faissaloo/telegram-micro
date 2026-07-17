package mtproto.recieve.updates;

import mtproto.CombinatorIds;
import mtproto.EncryptedResponse;
import reader.DialogListModel;
import support.Decode;
import mtproto.ProtocolLimits;

public final class RecieveShortTextUpdate {
  private static final int ALLOWED_FLAGS =
    (1 << 1) | (1 << 4) | (1 << 5) | (1 << 7) | (1 << 11) | (1 << 13);

  private static final int TG_VECTOR_ID = (int)0x1cb5c415L;
  private static final int TG_MESSAGE_ENTITY_UNKNOWN_ID = (int)0xbb92ba95L;
  private static final int TG_MESSAGE_ENTITY_MENTION_ID = (int)0xfa04579dL;
  private static final int TG_MESSAGE_ENTITY_HASHTAG_ID = (int)0x6f635b0dL;
  private static final int TG_MESSAGE_ENTITY_BOT_COMMAND_ID = (int)0x6cef8ac7L;
  private static final int TG_MESSAGE_ENTITY_URL_ID = (int)0x6ed02538L;
  private static final int TG_MESSAGE_ENTITY_EMAIL_ID = (int)0x64e475c2L;
  private static final int TG_MESSAGE_ENTITY_BOLD_ID = (int)0xbd610bc9L;
  private static final int TG_MESSAGE_ENTITY_ITALIC_ID = (int)0x826f8b60L;
  private static final int TG_MESSAGE_ENTITY_CODE_ID = (int)0x28a20571L;
  private static final int TG_MESSAGE_ENTITY_PRE_ID = (int)0x73924be0L;
  private static final int TG_MESSAGE_ENTITY_TEXT_URL_ID = (int)0x76a6d327L;
  private static final int TG_MESSAGE_ENTITY_MENTION_NAME_ID = (int)0xdc7b1140L;
  private static final int TG_INPUT_MESSAGE_ENTITY_MENTION_NAME_ID = (int)0x208e68c9L;
  private static final int TG_MESSAGE_ENTITY_PHONE_ID = (int)0x9b69e34bL;
  private static final int TG_MESSAGE_ENTITY_CASHTAG_ID = (int)0x4c4e743fL;
  private static final int TG_MESSAGE_ENTITY_UNDERLINE_ID = (int)0x9c4e7e8bL;
  private static final int TG_MESSAGE_ENTITY_STRIKE_ID = (int)0xbf0693d4L;
  private static final int TG_MESSAGE_ENTITY_BLOCKQUOTE_ID = (int)0xf1ccaaacL;
  private static final int TG_MESSAGE_ENTITY_BLOCKQUOTE_OLD_ID = (int)0x020df5d0L;
  private static final int TG_MESSAGE_ENTITY_BANK_CARD_ID = (int)0x761e6af4L;
  private static final int TG_MESSAGE_ENTITY_SPOILER_ID = (int)0x32ca960fL;
  private static final int TG_MESSAGE_ENTITY_CUSTOM_EMOJI_ID = (int)0xc8cf05f8L;

  public final int peerKind;
  public final long peerId;
  public final int senderPeerKind;
  public final long senderPeerId;
  public final int messageId;
  public final String text;
  public final boolean outgoing;
  public final int pts;
  public final int ptsCount;
  public final int date;

  public RecieveShortTextUpdate(
    int peerKind,
    long peerId,
    int senderPeerKind,
    long senderPeerId,
    int messageId,
    String text,
    boolean outgoing,
    int pts,
    int ptsCount,
    int date
  ) {
    this.peerKind = peerKind;
    this.peerId = peerId;
    this.senderPeerKind = senderPeerKind;
    this.senderPeerId = senderPeerId;
    this.messageId = messageId;
    this.text = text != null ? text : "";
    this.outgoing = outgoing;
    this.pts = pts;
    this.ptsCount = ptsCount;
    this.date = date;
  }

  public static RecieveShortTextUpdate from_encrypted_message(EncryptedResponse message) {
    if (message == null || message.data == null) {
      throw new IllegalArgumentException("Encrypted short update is required.");
    }
    return from_bytes(message.type, message.data);
  }

  public static RecieveShortTextUpdate from_bytes(int type, byte[] body) {
    TLReader reader;
    int flags;
    boolean outgoing;
    int messageId;
    long firstPeerId;
    long secondPeerId = 0;
    String text;
    int pts;
    int ptsCount;
    int date;

    if (body == null) {
      throw new IllegalArgumentException("Short update payload is missing.");
    }
    if (type != CombinatorIds.update_short_message &&
        type != CombinatorIds.update_short_chat_message) {
      throw new IllegalArgumentException("Expected a short text update.");
    }

    reader = new TLReader(body);
    flags = reader.readInt();
    if ((flags & ~ALLOWED_FLAGS) != 0) {
      throw new IllegalArgumentException("Short update uses unsupported optional fields.");
    }

    outgoing = ((flags >> 1) & 1) != 0;
    messageId = reader.readInt();
    firstPeerId = reader.readLong();
    if (type == CombinatorIds.update_short_chat_message) {
      secondPeerId = reader.readLong();
    }
    text = reader.readDisplayString();
    pts = reader.readInt();
    ptsCount = reader.readInt();
    date = reader.readInt();
    if (date < 0) {
      throw new IllegalArgumentException("Short update date must not be negative.");
    }

    if ((flags & (1 << 11)) != 0) {
      reader.readLong();
    }
    if ((flags & (1 << 7)) != 0) {
      skipMessageEntities(reader);
    }
    if (!reader.atEnd()) {
      throw new IllegalArgumentException("Short update has trailing data.");
    }

    if (type == CombinatorIds.update_short_message) {
      return new RecieveShortTextUpdate(
        DialogListModel.PEER_USER,
        firstPeerId,
        outgoing ? DialogListModel.PEER_UNKNOWN : DialogListModel.PEER_USER,
        outgoing ? 0 : firstPeerId,
        messageId,
        text,
        outgoing,
        pts,
        ptsCount,
        date
      );
    }

    return new RecieveShortTextUpdate(
      DialogListModel.PEER_CHAT,
      secondPeerId,
      DialogListModel.PEER_USER,
      firstPeerId,
      messageId,
      text,
      outgoing,
      pts,
      ptsCount,
      date
    );
  }

  private static void skipMessageEntities(TLReader reader) {
    int count = reader.readVectorCount();
    int i;

    for (i = 0; i < count; ++i) {
      skipMessageEntity(reader);
    }
  }

  private static void skipMessageEntity(TLReader reader) {
    int constructor = reader.readInt();
    int flags;

    switch (constructor) {
      case TG_MESSAGE_ENTITY_BLOCKQUOTE_ID:
        flags = reader.readInt();
        reader.readInt();
        reader.readInt();
        if ((flags & ~(1 << 0)) != 0) {
          throw new IllegalArgumentException("Unsupported blockquote entity flags.");
        }
        return;
      case TG_MESSAGE_ENTITY_BLOCKQUOTE_OLD_ID:
      case TG_MESSAGE_ENTITY_UNKNOWN_ID:
      case TG_MESSAGE_ENTITY_MENTION_ID:
      case TG_MESSAGE_ENTITY_HASHTAG_ID:
      case TG_MESSAGE_ENTITY_BOT_COMMAND_ID:
      case TG_MESSAGE_ENTITY_URL_ID:
      case TG_MESSAGE_ENTITY_EMAIL_ID:
      case TG_MESSAGE_ENTITY_BOLD_ID:
      case TG_MESSAGE_ENTITY_ITALIC_ID:
      case TG_MESSAGE_ENTITY_CODE_ID:
      case TG_MESSAGE_ENTITY_PHONE_ID:
      case TG_MESSAGE_ENTITY_CASHTAG_ID:
      case TG_MESSAGE_ENTITY_UNDERLINE_ID:
      case TG_MESSAGE_ENTITY_STRIKE_ID:
      case TG_MESSAGE_ENTITY_BANK_CARD_ID:
      case TG_MESSAGE_ENTITY_SPOILER_ID:
        reader.readInt();
        reader.readInt();
        return;
      case TG_MESSAGE_ENTITY_PRE_ID:
      case TG_MESSAGE_ENTITY_TEXT_URL_ID:
        reader.readInt();
        reader.readInt();
        reader.readDisplayString();
        return;
      case TG_MESSAGE_ENTITY_MENTION_NAME_ID:
        reader.readInt();
        reader.readInt();
        reader.readLong();
        return;
      case TG_INPUT_MESSAGE_ENTITY_MENTION_NAME_ID:
        throw new IllegalArgumentException("Input message entities are not supported.");
      case TG_MESSAGE_ENTITY_CUSTOM_EMOJI_ID:
        reader.readInt();
        reader.readInt();
        reader.readLong();
        return;
      default:
        throw new IllegalArgumentException("Unsupported message entity.");
    }
  }

  private static final class TLReader {
    private final byte[] data;
    private final int length;
    private int offset;

    TLReader(byte[] data) {
      this.data = data;
      this.length = data != null ? data.length : 0;
      this.offset = 0;
    }

    boolean atEnd() {
      return offset == length;
    }

    int readInt() {
      require(4);
      int value = Decode.Little.int_decode(data, offset);
      offset += 4;
      return value;
    }

    long readLong() {
      require(8);
      long value = Decode.Little.long_decode(data, offset);
      offset += 8;
      return value;
    }

    String readDisplayString() {
      int byteLength = readByteLength();
      int paddedLength = paddedByteLength(byteLength);
      ProtocolLimits.requireLength(
        byteLength,
        ProtocolLimits.MAX_DISPLAY_STRING_BYTES,
        "Display string"
      );
      StringBuffer buffer = new StringBuffer(byteLength);
      int i;

      require(byteLength);
      for (i = 0; i < byteLength; ++i) {
        int value = data[offset + i] & 0xFF;
        if (value == '\n' || value == '\r' || value == '\t') {
          buffer.append(' ');
        } else if (value >= 0x20 && value < 0x7F) {
          buffer.append((char)value);
        } else {
          buffer.append('?');
        }
      }
      offset += paddedLength;
      return buffer.toString();
    }

    int readVectorCount() {
      int constructor = readInt();

      if (constructor != TG_VECTOR_ID) {
        throw new IllegalArgumentException("Expected vector constructor.");
      }
      return ProtocolLimits.requireVectorCount(readInt(), length - offset);
    }

    private int readByteLength() {
      int byteLength;

      require(1);
      byteLength = data[offset] & 0xFF;
      offset += 1;
      if (byteLength >= 254) {
        require(3);
        byteLength = Decode.Little.int24_decode(data, offset);
        offset += 3;
      }
      return byteLength;
    }

    private int paddedByteLength(int byteLength) {
      int payloadStart = offset;
      int paddedEnd = payloadStart + byteLength;

      while ((paddedEnd & 3) != 0) {
        paddedEnd += 1;
      }
      require(paddedEnd - payloadStart);
      return paddedEnd - payloadStart;
    }

    private void require(int amount) {
      if (amount < 0 || offset > length - amount) {
        throw new IllegalArgumentException("TL payload underflow.");
      }
    }
  }
}
