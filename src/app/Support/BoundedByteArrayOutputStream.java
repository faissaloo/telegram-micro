package support;

import java.io.IOException;
import java.io.OutputStream;

/** A growable byte sink that refuses to exceed an explicit heap budget. */
public final class BoundedByteArrayOutputStream extends OutputStream {
  private byte[] buffer;
  private int count;
  private final int maximum;

  public BoundedByteArrayOutputStream(int initialCapacity, int maximum) {
    if (initialCapacity < 0 || maximum <= 0 || initialCapacity > maximum) {
      throw new IllegalArgumentException("Invalid bounded stream capacity.");
    }
    buffer = new byte[initialCapacity];
    this.maximum = maximum;
  }

  public void write(int value) throws IOException {
    ensureCapacity(count + 1);
    buffer[count++] = (byte)value;
  }

  public void write(byte[] value, int offset, int length) throws IOException {
    if (value == null) {
      throw new NullPointerException("Input bytes are required.");
    }
    if (offset < 0 || length < 0 || offset > value.length - length) {
      throw new IndexOutOfBoundsException();
    }
    if (length > maximum - count) {
      throw new IOException("Decompressed payload exceeds the device budget.");
    }
    ensureCapacity(count + length);
    System.arraycopy(value, offset, buffer, count, length);
    count += length;
  }

  public int size() {
    return count;
  }

  public byte[] toByteArray() {
    byte[] out = new byte[count];
    System.arraycopy(buffer, 0, out, 0, count);
    return out;
  }

  private void ensureCapacity(int required) throws IOException {
    if (required < 0 || required > maximum) {
      throw new IOException("Decompressed payload exceeds the device budget.");
    }
    if (required <= buffer.length) {
      return;
    }

    int next = buffer.length == 0 ? 256 : buffer.length * 2;
    if (next < required) {
      next = required;
    }
    if (next > maximum) {
      next = maximum;
    }
    byte[] replacement = new byte[next];
    System.arraycopy(buffer, 0, replacement, 0, count);
    clear(buffer);
    buffer = replacement;
  }

  private static void clear(byte[] value) {
    for (int i = 0; i < value.length; ++i) {
      value[i] = 0;
    }
  }
}
