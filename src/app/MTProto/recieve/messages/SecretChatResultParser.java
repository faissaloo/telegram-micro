package mtproto.recieve.messages;

import mtproto.CombinatorIds;
import mtproto.RPCResponse;
import mtproto.secret.SecretChatBootstrap;

public final class SecretChatResultParser {
  public static final int STATE_UNKNOWN = 0;
  public static final int STATE_WAITING = 1;
  public static final int STATE_REQUESTED = 2;
  public static final int STATE_READY = 3;
  public static final int STATE_CLOSED = 4;
  public static final int STATE_ACTIVITY = 5;

  public static final class Result {
    public int state = STATE_UNKNOWN;
    public int chatId = 0;
    public long accessHash = 0;
    public int date = 0;
    public long adminId = 0;
    public long participantId = 0;
    public long keyFingerprint = 0;
    public long randomId = 0;
    public boolean service = false;
    public final byte[] exchangeBytes = new byte[SecretChatBootstrap.EXPONENT_BYTES];
    public int exchangeLength = 0;

    public void clear() {
      state = STATE_UNKNOWN;
      chatId = 0;
      accessHash = 0;
      date = 0;
      adminId = 0;
      participantId = 0;
      keyFingerprint = 0;
      randomId = 0;
      service = false;
      exchangeLength = 0;
    }
  }

  private String lastError = "";

  public String lastError() {
    return lastError;
  }

  public boolean parse(RPCResponse response, Result out) {
    TLReader reader;

    if (response == null || out == null) {
      lastError = "Secret chat response is missing.";
      return false;
    }

    out.clear();
    reader = new TLReader(response.body);

    try {
      if (!parseBody(response.type, reader, out)) {
        return false;
      }
      if (!reader.atEnd()) {
        lastError = "Unexpected secret chat trailing bytes.";
        return false;
      }
    } catch (IllegalArgumentException e) {
      lastError = e.getMessage();
      return false;
    }

    lastError = "";
    return true;
  }

  boolean parseBody(int constructor, TLReader reader, Result out) {
    if (reader == null || out == null) {
      lastError = "Secret chat parse target is missing.";
      return false;
    }

    out.clear();
    try {
      if (constructor == CombinatorIds.encrypted_chat_waiting) {
        out.state = STATE_WAITING;
        parseCore(reader, out, false);
      } else if (constructor == CombinatorIds.encrypted_chat_requested) {
        out.state = STATE_REQUESTED;
        parseCore(reader, out, true);
        out.exchangeLength = reader.readBytesInto(out.exchangeBytes);
      } else if (constructor == CombinatorIds.encrypted_chat) {
        out.state = STATE_READY;
        parseCore(reader, out, false);
        out.exchangeLength = reader.readBytesInto(out.exchangeBytes);
        out.keyFingerprint = reader.readLong();
      } else if (constructor == CombinatorIds.encrypted_chat_discarded) {
        parseDiscarded(reader, out);
      } else {
        lastError = "Unexpected secret chat constructor: 0x" + Integer.toHexString(constructor);
        return false;
      }
    } catch (IllegalArgumentException e) {
      lastError = e.getMessage();
      return false;
    }

    lastError = "";
    return true;
  }

  private void parseCore(TLReader reader, Result out, boolean hasFlags) {
    int flags;

    if (hasFlags) {
      flags = reader.readInt();
      if ((flags & 1) != 0) {
        reader.readInt();
      }
    }

    out.chatId = reader.readInt();
    out.accessHash = reader.readLong();
    out.date = reader.readInt();
    out.adminId = reader.readLong();
    out.participantId = reader.readLong();
  }

  private void parseDiscarded(TLReader reader, Result out) {
    int flags = reader.readInt();

    out.state = STATE_CLOSED;
    out.chatId = reader.readInt();
    if ((flags & ~1) != 0) {
      throw new IllegalArgumentException("Unsupported encryptedChatDiscarded flags.");
    }
  }
}
