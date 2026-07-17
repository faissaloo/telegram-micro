package crypto;

/**
 * Fixed-size mixer for first-install key timing samples.
 *
 * Event count is a collection threshold, not a formal entropy estimate. The
 * caller must collect human-generated timing before extracting the seed.
 */
public final class EntropyPool {
  public static final int REQUIRED_EVENTS = 128;

  private final SHA256 hashEngine = new SHA256();
  private final byte[] state = new byte[SHA256.HASH_SIZE];
  private final byte[] sample = new byte[SHA256.HASH_SIZE + 25];
  private int eventCount;
  private long lastTimestamp = Long.MIN_VALUE;
  private boolean consumed;

  public EntropyPool(byte[] initialState) {
    if (initialState != null) {
      int length = initialState.length < state.length ? initialState.length : state.length;
      System.arraycopy(initialState, 0, state, 0, length);
    }
  }

  public boolean recordEvent(long timestamp, int keyCode, int eventKind, long freeMemory) {
    if (consumed || eventCount >= REQUIRED_EVENTS || timestamp == lastTimestamp) {
      return false;
    }

    System.arraycopy(state, 0, sample, 0, state.length);
    putLong(sample, state.length, timestamp);
    putInt(sample, state.length + 8, keyCode);
    putInt(sample, state.length + 12, eventKind);
    putLong(sample, state.length + 16, freeMemory);
    sample[sample.length - 1] = (byte)eventCount;

    byte[] mixed = hashEngine.digest(sample);
    System.arraycopy(mixed, 0, state, 0, state.length);
    clear(mixed);
    lastTimestamp = timestamp;
    ++eventCount;
    return true;
  }

  public int eventCount() {
    return eventCount;
  }

  public int remainingEvents() {
    return REQUIRED_EVENTS - eventCount;
  }

  public boolean ready() {
    return !consumed && eventCount >= REQUIRED_EVENTS;
  }

  public byte[] extractSeed() {
    if (!ready()) {
      throw new IllegalStateException("More timing samples are required.");
    }

    System.arraycopy(state, 0, sample, 0, state.length);
    for (int i = state.length; i < sample.length; ++i) {
      sample[i] = 0;
    }
    sample[sample.length - 1] = 0x53;
    byte[] seed = hashEngine.digest(sample);
    clear(state);
    clear(sample);
    consumed = true;
    return seed;
  }

  private static void putInt(byte[] out, int offset, int value) {
    out[offset] = (byte)(value >>> 24);
    out[offset + 1] = (byte)(value >>> 16);
    out[offset + 2] = (byte)(value >>> 8);
    out[offset + 3] = (byte)value;
  }

  private static void putLong(byte[] out, int offset, long value) {
    for (int i = 7; i >= 0; --i) {
      out[offset + i] = (byte)(value & 0xff);
      value >>>= 8;
    }
  }

  private static void clear(byte[] value) {
    for (int i = 0; i < value.length; ++i) {
      value[i] = 0;
    }
  }
}
