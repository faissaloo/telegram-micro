package session;

import crypto.SHA256;

/** A compact, checksummed record for the ratcheted device entropy seed. */
public final class EntropySeedCodec {
  public static final int MAGIC = 0x544d4553;
  public static final int VERSION = 1;
  public static final int SEED_BYTES = SHA256.HASH_SIZE;
  public static final int TAG_BYTES = 16;
  public static final int ENCODED_SIZE = 4 + 1 + SEED_BYTES + TAG_BYTES;

  private EntropySeedCodec() {
  }

  public static byte[] encode(byte[] seed) {
    if (seed == null || seed.length != SEED_BYTES) {
      return null;
    }

    byte[] encoded = new byte[ENCODED_SIZE];
    putInt(encoded, 0, MAGIC);
    encoded[4] = VERSION;
    System.arraycopy(seed, 0, encoded, 5, SEED_BYTES);
    byte[] digest = new SHA256().digest(prefix(encoded));
    System.arraycopy(digest, 0, encoded, 5 + SEED_BYTES, TAG_BYTES);
    clear(digest);
    return encoded;
  }

  public static boolean decode(byte[] out, byte[] encoded) {
    if (out == null || out.length < SEED_BYTES || encoded == null ||
        encoded.length != ENCODED_SIZE || readInt(encoded, 0) != MAGIC ||
        (encoded[4] & 0xff) != VERSION) {
      return false;
    }

    byte[] digest = new SHA256().digest(prefix(encoded));
    int difference = 0;
    for (int i = 0; i < TAG_BYTES; ++i) {
      difference |= digest[i] ^ encoded[5 + SEED_BYTES + i];
    }
    clear(digest);
    if (difference != 0) {
      return false;
    }

    System.arraycopy(encoded, 5, out, 0, SEED_BYTES);
    return true;
  }

  private static byte[] prefix(byte[] encoded) {
    byte[] prefix = new byte[5 + SEED_BYTES];
    System.arraycopy(encoded, 0, prefix, 0, prefix.length);
    return prefix;
  }

  private static void putInt(byte[] out, int offset, int value) {
    out[offset] = (byte)(value >>> 24);
    out[offset + 1] = (byte)(value >>> 16);
    out[offset + 2] = (byte)(value >>> 8);
    out[offset + 3] = (byte)value;
  }

  private static int readInt(byte[] in, int offset) {
    return ((in[offset] & 0xff) << 24) |
      ((in[offset + 1] & 0xff) << 16) |
      ((in[offset + 2] & 0xff) << 8) |
      (in[offset + 3] & 0xff);
  }

  private static void clear(byte[] value) {
    for (int i = 0; i < value.length; ++i) {
      value[i] = 0;
    }
  }
}
