package test;

import crypto.SHA256;

public class SHA256Context {
  public static class DigestTest extends Test {
    public String label() {
      return "It generates a SHA256 digest correctly";
    }
    public void test() {
      SHA256 sha256 = new SHA256();
      expect(sha256.digest(new byte[] {
        (byte)0x69, (byte)0x69, (byte)0x69, (byte)0x69, (byte)0x69, (byte)0x69
      }), new byte[] {
        (byte)0xde, (byte)0xad, (byte)0xa9, (byte)0xb6, (byte)0x01, (byte)0xa1, (byte)0xd5, (byte)0x99,
        (byte)0x69, (byte)0x55, (byte)0x1d, (byte)0x78, (byte)0x81, (byte)0x35, (byte)0xe2, (byte)0xfa,
        (byte)0x7d, (byte)0x2c, (byte)0x83, (byte)0xc2, (byte)0xef, (byte)0x81, (byte)0x8c, (byte)0x16,
        (byte)0xda, (byte)0xbe, (byte)0x4f, (byte)0xde, (byte)0x18, (byte)0xc4, (byte)0x35, (byte)0xa1
      });
    }
  }
}
