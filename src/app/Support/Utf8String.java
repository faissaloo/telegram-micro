package support;

public class Utf8String {
  private final byte[] bytes;

  public Utf8String(byte[] bytes) {
    this.bytes = bytes != null ? bytes : new byte[0];
  }

  public int length() {
    return string().length();
  }

  public String ascii_string() {
    return string();
  }

  public String string() {
    StringBuffer out = new StringBuffer(bytes.length);
    int i = 0;

    while (i < bytes.length) {
      int a = bytes[i] & 0xFF;

      if ((a & 0x80) == 0) {
        appendCodePoint(out, sanitizeCodePoint(a));
        i += 1;
        continue;
      }

      if ((a & 0xE0) == 0xC0 && i + 1 < bytes.length) {
        int b = bytes[i + 1] & 0x3F;
        int codePoint = ((a & 0x1F) << 6) | b;

        if ((bytes[i + 1] & 0xC0) == 0x80 && codePoint >= 0x80) {
          appendCodePoint(out, sanitizeCodePoint(codePoint));
          i += 2;
          continue;
        }
      }

      if ((a & 0xF0) == 0xE0 && i + 2 < bytes.length) {
        int b0 = bytes[i + 1] & 0xFF;
        int c0 = bytes[i + 2] & 0xFF;
        int codePoint = ((a & 0x0F) << 12) | ((b0 & 0x3F) << 6) | (c0 & 0x3F);

        if ((b0 & 0xC0) == 0x80 &&
            (c0 & 0xC0) == 0x80 &&
            codePoint >= 0x800 &&
            !(codePoint >= 0xD800 && codePoint <= 0xDFFF)) {
          appendCodePoint(out, sanitizeCodePoint(codePoint));
          i += 3;
          continue;
        }
      }

      if ((a & 0xF8) == 0xF0 && i + 3 < bytes.length) {
        int b1 = bytes[i + 1] & 0xFF;
        int c1 = bytes[i + 2] & 0xFF;
        int d1 = bytes[i + 3] & 0xFF;
        int codePoint = ((a & 0x07) << 18) | ((b1 & 0x3F) << 12) |
          ((c1 & 0x3F) << 6) | (d1 & 0x3F);

        if ((b1 & 0xC0) == 0x80 &&
            (c1 & 0xC0) == 0x80 &&
            (d1 & 0xC0) == 0x80 &&
            codePoint >= 0x10000 &&
            codePoint <= 0x10FFFF) {
          appendCodePoint(out, sanitizeCodePoint(codePoint));
          i += 4;
          continue;
        }
      }

      out.append('?');
      i += 1;
    }

    return out.toString();
  }

  public int byte_length() {
    return bytes.length;
  }

  public byte[] bytes() {
    byte[] copy = new byte[bytes.length];
    System.arraycopy(bytes, 0, copy, 0, bytes.length);
    return copy;
  }

  public static byte[] encode(String value) {
    int i;
    int length = 0;
    byte[] out;
    int offset = 0;

    if (value == null || value.length() == 0) {
      return new byte[0];
    }

    for (i = 0; i < value.length(); ++i) {
      int codePoint = value.charAt(i);

      if (isHighSurrogate(codePoint) && i + 1 < value.length() &&
          isLowSurrogate(value.charAt(i + 1))) {
        codePoint = toCodePoint((char)codePoint, value.charAt(i + 1));
        i += 1;
      } else if (isLowSurrogate(codePoint)) {
        codePoint = '?';
      }

      length += encodedLength(codePoint);
    }

    out = new byte[length];

    for (i = 0; i < value.length(); ++i) {
      int codePoint = value.charAt(i);

      if (isHighSurrogate(codePoint) && i + 1 < value.length() &&
          isLowSurrogate(value.charAt(i + 1))) {
        codePoint = toCodePoint((char)codePoint, value.charAt(i + 1));
        i += 1;
      } else if (isLowSurrogate(codePoint)) {
        codePoint = '?';
      }

      offset = encodeCodePoint(codePoint, out, offset);
    }

    return out;
  }

  private static int sanitizeCodePoint(int codePoint) {
    if (codePoint == '\r') {
      return '\n';
    }
    if (codePoint == '\t') {
      return ' ';
    }
    if (codePoint < 0x20 && codePoint != '\n') {
      return '?';
    }
    return codePoint;
  }

  private static void appendCodePoint(StringBuffer out, int codePoint) {
    if (codePoint <= 0xFFFF) {
      out.append((char)codePoint);
      return;
    }

    codePoint -= 0x10000;
    out.append((char)(0xD800 | ((codePoint >> 10) & 0x3FF)));
    out.append((char)(0xDC00 | (codePoint & 0x3FF)));
  }

  private static boolean isHighSurrogate(int value) {
    return value >= 0xD800 && value <= 0xDBFF;
  }

  private static boolean isLowSurrogate(int value) {
    return value >= 0xDC00 && value <= 0xDFFF;
  }

  private static int toCodePoint(char high, char low) {
    return 0x10000 + (((high - 0xD800) & 0x3FF) << 10) + ((low - 0xDC00) & 0x3FF);
  }

  private static int encodedLength(int codePoint) {
    if (codePoint < 0x80) {
      return 1;
    }
    if (codePoint < 0x800) {
      return 2;
    }
    if (codePoint < 0x10000) {
      return 3;
    }
    return 4;
  }

  private static int encodeCodePoint(int codePoint, byte[] out, int offset) {
    if (codePoint < 0x80) {
      out[offset] = (byte)codePoint;
      return offset + 1;
    }
    if (codePoint < 0x800) {
      out[offset] = (byte)(0xC0 | (codePoint >> 6));
      out[offset + 1] = (byte)(0x80 | (codePoint & 0x3F));
      return offset + 2;
    }
    if (codePoint < 0x10000) {
      out[offset] = (byte)(0xE0 | (codePoint >> 12));
      out[offset + 1] = (byte)(0x80 | ((codePoint >> 6) & 0x3F));
      out[offset + 2] = (byte)(0x80 | (codePoint & 0x3F));
      return offset + 3;
    }

    out[offset] = (byte)(0xF0 | (codePoint >> 18));
    out[offset + 1] = (byte)(0x80 | ((codePoint >> 12) & 0x3F));
    out[offset + 2] = (byte)(0x80 | ((codePoint >> 6) & 0x3F));
    out[offset + 3] = (byte)(0x80 | (codePoint & 0x3F));
    return offset + 4;
  }
}
