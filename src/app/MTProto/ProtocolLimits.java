package mtproto;

/**
 * Hard protocol budgets sized for the Nokia C2-01 target.
 *
 * These limits are security boundaries: network-controlled lengths must be
 * checked before loops or allocations. Raise them only with device evidence.
 */
public final class ProtocolLimits {
  public static final int MAX_WIRE_FRAME_BYTES = 128 * 1024;
  public static final int MAX_INFLATED_BYTES = 128 * 1024;
  public static final int MAX_TL_BYTES = 64 * 1024;
  public static final int MAX_DISPLAY_STRING_BYTES = 4096;
  public static final int MAX_VECTOR_ITEMS = 512;
  public static final int MAX_CONTAINER_MESSAGES = 64;
  public static final int MAX_PENDING_REQUESTS = 2;
  public static final int MAX_PENDING_RESPONSES = 2;
  public static final int MAX_DC_OPTIONS = 32;
  public static final int MAX_FUTURE_SALTS = 16;
  public static final int MAX_MEDIA_CHUNK_BYTES = 16 * 1024;
  public static final int MAX_AUTH_DH_BYTES = 1024;

  private ProtocolLimits() {
  }

  public static int requireLength(int length, int maximum, String label) {
    if (length < 0 || length > maximum) {
      throw new IllegalArgumentException(label + " exceeds the device budget.");
    }
    return length;
  }

  public static int requireVectorCount(int count, int remainingBytes) {
    requireLength(count, MAX_VECTOR_ITEMS, "TL vector");
    if (count > remainingBytes / 4) {
      throw new IllegalArgumentException("TL vector count exceeds its payload.");
    }
    return count;
  }
}
