package session;

import support.Decode;
import support.Encode;

public final class FailureLogCodec {
  public static final int MAGIC = 0x4c464754;
  public static final int VERSION = 1;
  public static final int HEADER_SIZE = 16;
  public static final int ENTRY_SIZE = 16;
  public static final int ENCODED_SIZE = HEADER_SIZE + (FailureLog.MAX_ENTRIES * ENTRY_SIZE);

  private FailureLogCodec() {
  }

  public static byte[] encode(FailureLog log) {
    byte[] out = new byte[ENCODED_SIZE];

    if (!encode(log, out)) {
      return null;
    }
    return out;
  }

  public static boolean encode(FailureLog log, byte[] out) {
    int offset = 0;
    int i;

    if (log == null || out == null || out.length < ENCODED_SIZE) {
      return false;
    }

    System.arraycopy(Encode.int_encode(MAGIC), 0, out, offset, 4);
    offset += 4;
    System.arraycopy(Encode.int_encode(VERSION), 0, out, offset, 4);
    offset += 4;
    System.arraycopy(Encode.int_encode(log.nextSequence), 0, out, offset, 4);
    offset += 4;
    System.arraycopy(Encode.int_encode((log.count & 0xff) | ((log.head & 0xff) << 8)), 0, out, offset, 4);
    offset += 4;

    for (i = 0; i < FailureLog.MAX_ENTRIES; ++i) {
      FailureLog.Entry entry = log.entries[i];

      System.arraycopy(Encode.int_encode(entry.sequence), 0, out, offset, 4);
      offset += 4;
      System.arraycopy(Encode.int_encode((entry.category & 0xff) | ((entry.severity & 0xff) << 8)), 0, out, offset, 4);
      offset += 4;
      System.arraycopy(Encode.int_encode(entry.code), 0, out, offset, 4);
      offset += 4;
      System.arraycopy(Encode.int_encode(entry.detail), 0, out, offset, 4);
      offset += 4;
    }

    return true;
  }

  public static boolean decode(FailureLog out, byte[] data) {
    int offset = HEADER_SIZE;
    int meta;
    int i;

    if (out == null || data == null || data.length != ENCODED_SIZE) {
      return false;
    }
    if (Decode.Little.int_decode(data, 0) != MAGIC ||
        Decode.Little.int_decode(data, 4) != VERSION) {
      return false;
    }

    out.clear();
    out.nextSequence = Decode.Little.int_decode(data, 8);
    meta = Decode.Little.int_decode(data, 12);
    out.count = meta & 0xff;
    out.head = (meta >> 8) & 0xff;
    if (out.count > FailureLog.MAX_ENTRIES || out.head >= FailureLog.MAX_ENTRIES) {
      out.clear();
      return false;
    }

    for (i = 0; i < FailureLog.MAX_ENTRIES; ++i) {
      FailureLog.Entry entry = out.entries[i];
      int packed = Decode.Little.int_decode(data, offset + 4);

      entry.sequence = Decode.Little.int_decode(data, offset);
      entry.category = packed & 0xff;
      entry.severity = (packed >> 8) & 0xff;
      entry.code = Decode.Little.int_decode(data, offset + 8);
      entry.detail = Decode.Little.int_decode(data, offset + 12);
      offset += ENTRY_SIZE;
    }

    return true;
  }
}
