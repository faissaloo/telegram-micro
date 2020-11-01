package support;

public class Utf8String {
  byte[] bytes;

  public Utf8String(byte[] bytes) {
    this.bytes = bytes;
  }

  public int length() {
    int length = 0;
    for (int i = 0; i < bytes.length; i++) {
      byte character = bytes[i];
      byte character_length = (byte)((character&0x08) >>> 3 + (character&0x10) >>> 4 + (character&0x20) >>> 5 + (character&0x40) >>> 6 + (character&0x80) >>> 7);
      length += character_length;
    }
    return length;
  }

  public int byte_length() {
    return bytes.length;
  }
}
