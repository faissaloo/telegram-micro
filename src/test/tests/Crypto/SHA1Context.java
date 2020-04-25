package test;

import crypto.SHA1;

public class SHA1Context {
  public static class DigestTest extends Test {
    public String label() {
      return "It generates a SHA1 digest correctly";
    }
    public void test() {
      SHA1 sha1 = new SHA1();
      expect(sha1.digest(new byte[] {
        (byte)0x69, (byte)0x69, (byte)0x69, (byte)0x69, (byte)0x69, (byte)0x69
      }), new byte[] {
        (byte)0xe6, (byte)0xee, (byte)0x3f, (byte)0xd5, (byte)0xc2, (byte)0xf5,
        (byte)0x3f, (byte)0x4f, (byte)0xaa, (byte)0xb3, (byte)0xf3, (byte)0xc3,
        (byte)0xd0, (byte)0x7f, (byte)0xaf, (byte)0x72, (byte)0xf2, (byte)0x32,
        (byte)0x89, (byte)0xed
      });
    }
  }
}
