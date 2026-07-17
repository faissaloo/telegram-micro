package crypto;

import support.RandomPlus;

/**
 * A small SHA-256 counter generator seeded by entropy collected outside this
 * class. There is deliberately no zero-argument constructor: protocol code
 * must not silently fall back to java.util.Random.
 */
public final class SecureRandomPlus extends RandomPlus {
  public static final int SEED_BYTES = SHA256.HASH_SIZE;

  private final SHA256 hashEngine = new SHA256();
  private final byte[] key = new byte[SEED_BYTES];
  private final byte[] input = new byte[SEED_BYTES + 9];
  private byte[] generatedBuffer = new byte[SEED_BYTES];
  private int bufferIndex = SEED_BYTES;
  private long counter = 1;

  public SecureRandomPlus(byte[] seed) {
    super();
    if (seed == null || seed.length < SEED_BYTES) {
      throw new IllegalArgumentException("A 32-byte entropy seed is required.");
    }
    System.arraycopy(seed, 0, key, 0, key.length);
  }

  private void generateBuffer() {
    System.arraycopy(key, 0, input, 0, key.length);
    putLong(input, key.length, counter++);
    input[input.length - 1] = 0x47;
    generatedBuffer = hashEngine.digest(input);

    /* Rekey after every block so old state cannot reconstruct later output. */
    System.arraycopy(generatedBuffer, 0, input, 0, generatedBuffer.length);
    putLong(input, generatedBuffer.length, counter++);
    input[input.length - 1] = 0x4b;
    byte[] nextKey = hashEngine.digest(input);
    System.arraycopy(nextKey, 0, key, 0, key.length);
    clear(nextKey);
    bufferIndex = 0;
  }

  public synchronized byte nextByte() {
    if (bufferIndex >= generatedBuffer.length) {
      generateBuffer();
    }
    return generatedBuffer[bufferIndex++];
  }

  public synchronized void nextBytes(byte[] out) {
    if (out == null) {
      throw new NullPointerException("Output buffer is required.");
    }
    for (int i = 0; i < out.length; ++i) {
      out[i] = nextByte();
    }
  }

  public synchronized byte[] nextBytes(int length) {
    if (length < 0) {
      throw new IllegalArgumentException("Length cannot be negative.");
    }
    byte[] out = new byte[length];
    nextBytes(out);
    return out;
  }

  protected synchronized int next(int bits) {
    if (bits <= 0 || bits > 32) {
      throw new IllegalArgumentException("Bit count must be between 1 and 32.");
    }

    int value = ((nextByte() & 0xff) << 24) |
      ((nextByte() & 0xff) << 16) |
      ((nextByte() & 0xff) << 8) |
      (nextByte() & 0xff);
    return bits == 32 ? value : value >>> (32 - bits);
  }

  private static void putLong(byte[] out, int offset, long value) {
    for (int i = 7; i >= 0; --i) {
      out[offset + i] = (byte)(value & 0xff);
      value >>>= 8;
    }
  }

  private static void clear(byte[] value) {
    if (value == null) {
      return;
    }
    for (int i = 0; i < value.length; ++i) {
      value[i] = 0;
    }
  }
}
