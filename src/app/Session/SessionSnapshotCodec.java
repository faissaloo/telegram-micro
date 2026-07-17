package session;

import support.Decode;
import support.Encode;

public class SessionSnapshotCodec {
  public static final int MAGIC = 0x4d534754;
  public static final int FLAG_HAS_AUTH_STATE = 1;
  public static final int FLAG_AUTHORIZED = 2;
  public static final int LEGACY_ENCODED_SIZE =
    4 + 4 + 4 + 4 + 8 + 8 + 8 + 4 + SessionSnapshot.AUTH_KEY_BYTES;
  public static final int ENCODED_SIZE =
    LEGACY_ENCODED_SIZE + 4 + 8;

  public static byte[] encode(SessionSnapshot snapshot) {
    byte[] out = new byte[ENCODED_SIZE];
    if (!encode(snapshot, out)) {
      return null;
    }
    return out;
  }

  public static boolean encode(SessionSnapshot snapshot, byte[] out) {
    int flags = 0;
    int offset = 0;

    if (snapshot == null || out == null || out.length < ENCODED_SIZE) {
      return false;
    }

    if (snapshot.hasAuthState) {
      flags |= FLAG_HAS_AUTH_STATE;
    }
    if (snapshot.authorized) {
      flags |= FLAG_AUTHORIZED;
    }

    System.arraycopy(Encode.int_encode(MAGIC), 0, out, offset, 4);
    offset += 4;
    System.arraycopy(Encode.int_encode(SessionSnapshot.VERSION), 0, out, offset, 4);
    offset += 4;
    System.arraycopy(Encode.int_encode(flags), 0, out, offset, 4);
    offset += 4;
    System.arraycopy(Encode.int_encode(snapshot.dcId), 0, out, offset, 4);
    offset += 4;
    System.arraycopy(Encode.long_encode(snapshot.authKeyId), 0, out, offset, 8);
    offset += 8;
    System.arraycopy(Encode.long_encode(snapshot.serverSalt), 0, out, offset, 8);
    offset += 8;
    System.arraycopy(Encode.long_encode(snapshot.sessionId), 0, out, offset, 8);
    offset += 8;
    System.arraycopy(Encode.int_encode(snapshot.seqNo), 0, out, offset, 4);
    offset += 4;
    System.arraycopy(snapshot.authKey, 0, out, offset, SessionSnapshot.AUTH_KEY_BYTES);
    offset += SessionSnapshot.AUTH_KEY_BYTES;
    System.arraycopy(Encode.int_encode(snapshot.timeOffsetSeconds), 0, out, offset, 4);
    offset += 4;
    System.arraycopy(Encode.long_encode(snapshot.lastMessageId), 0, out, offset, 8);
    return true;
  }

  public static boolean decode(SessionSnapshot out, byte[] data) {
    int flags;
    int version;
    int offset = 0;

    if (out == null || data == null || data.length < LEGACY_ENCODED_SIZE) {
      return false;
    }

    if (Decode.Little.int_decode(data, offset) != MAGIC) {
      return false;
    }
    offset += 4;

    version = Decode.Little.int_decode(data, offset);
    if (version != SessionSnapshot.LEGACY_VERSION &&
        version != SessionSnapshot.VERSION) {
      return false;
    }
    offset += 4;

    if (version == SessionSnapshot.VERSION && data.length < ENCODED_SIZE) {
      return false;
    }

    flags = Decode.Little.int_decode(data, offset);
    offset += 4;

    out.clear();
    out.version = version;
    out.hasAuthState = (flags & FLAG_HAS_AUTH_STATE) != 0;
    out.authorized = (flags & FLAG_AUTHORIZED) != 0;
    out.dcId = Decode.Little.int_decode(data, offset);
    offset += 4;
    out.authKeyId = Decode.Little.long_decode(data, offset);
    offset += 8;
    out.serverSalt = Decode.Little.long_decode(data, offset);
    offset += 8;
    out.sessionId = Decode.Little.long_decode(data, offset);
    offset += 8;
    out.seqNo = Decode.Little.int_decode(data, offset);
    offset += 4;
    System.arraycopy(data, offset, out.authKey, 0, SessionSnapshot.AUTH_KEY_BYTES);
    offset += SessionSnapshot.AUTH_KEY_BYTES;

    if (version >= SessionSnapshot.VERSION) {
      out.timeOffsetSeconds = Decode.Little.int_decode(data, offset);
      offset += 4;
      out.lastMessageId = Decode.Little.long_decode(data, offset);
    }

    if (out.hasAuthState && out.authKeyId == 0) {
      out.clear();
      return false;
    }
    if (!out.hasAuthState) {
      out.authorized = false;
    }
    return true;
  }
}
