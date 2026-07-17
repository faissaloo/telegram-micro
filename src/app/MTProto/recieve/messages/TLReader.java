package mtproto.recieve.messages;

import support.Decode;

final class TLReader {
  private final byte[] data;
  private final int length;
  private int offset;

  TLReader(byte[] data) {
    this.data = data;
    this.length = data != null ? data.length : 0;
    this.offset = 0;
  }

  int offset() {
    return offset;
  }

  int length() {
    return length;
  }

  boolean atEnd() {
    return offset == length;
  }

  String hexWindow(int start, int count) {
    int safeStart = start < 0 ? 0 : start;
    int safeEnd = safeStart + count;
    StringBuffer out = new StringBuffer(count * 3);
    int i;

    if (safeEnd > length) {
      safeEnd = length;
    }

    for (i = safeStart; i < safeEnd; ++i) {
      int value = data[i] & 0xFF;
      if (out.length() > 0) {
        out.append(' ');
      }
      if (value < 0x10) {
        out.append('0');
      }
      out.append(Integer.toHexString(value));
    }
    return out.toString();
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

  void skip(int amount) {
    require(amount);
    offset += amount;
  }

  byte[] readBytes() {
    int byteLength = readByteLength();
    int paddedLength = paddedByteLength(byteLength);
    byte[] result = new byte[byteLength];

    require(byteLength);
    System.arraycopy(data, offset, result, 0, byteLength);
    offset += paddedLength;
    return result;
  }

  int readBytesInto(byte[] out) {
    int byteLength = readByteLength();
    int paddedLength = paddedByteLength(byteLength);

    if (out == null || byteLength > out.length) {
      throw new IllegalArgumentException("Target buffer is too small for TL bytes.");
    }

    require(byteLength);
    System.arraycopy(data, offset, out, 0, byteLength);
    offset += paddedLength;
    return byteLength;
  }

  String readDisplayString() {
    support.Utf8String value = new support.Utf8String(readBytes());
    return value.string();
  }

  void skipBytes() {
    int byteLength = readByteLength();
    int paddedLength = paddedByteLength(byteLength);
    require(byteLength);
    offset += paddedLength;
  }

  int readVectorCount() {
    int constructor = readInt();
    if (constructor != 0x1cb5c415) {
      throw new IllegalArgumentException("Expected vector constructor.");
    }
    return readInt();
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
    if (amount < 0 || offset + amount > length) {
      throw new IllegalArgumentException("TL payload underflow.");
    }
  }
}
