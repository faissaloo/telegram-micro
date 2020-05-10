package test;

import crypto.RSAPublicKey;
import crypto.RSAPKCS1OAEP;

public class RSAPKCS1OAEPContext {
  public static class RSAEncryptionPrimitiveTest extends Test {
    public String label() {
      return "Can perform RSA encryption";
    }

    public void test() throws TestFailureException {
      RSAPublicKey public_key = new RSAPublicKey(0x10001, new byte[] {(byte)0x66, (byte)0x77, (byte)0x88});
      System.out.println("PERFORMING RSA ENCRYPTION");
      byte[] result = RSAPKCS1OAEP.rsa_encryption_primitive(public_key, new byte[] {(byte)0x14, (byte)0xFF, (byte)0x21});
      expect(result, new byte[] {(byte)0x39,(byte)0x84,0x11});
      System.out.println("DONE");
    }
  }

  public static class MaskGenerationTest extends Test {
    public String label() {
      return "Can generate a mask";
    }

    public void test() throws TestFailureException {
      byte[] result = RSAPKCS1OAEP.mask_generation_function(new byte[] {(byte)0xFF, (byte)0x66, (byte)0x22},36);
      expect(result, new byte[] {
        (byte)0xF4, (byte)0x1F, (byte)0x3E, (byte)0xD9,
        (byte)0x42, (byte)0x0B, (byte)0x6A, (byte)0xB7,
        (byte)0x42, (byte)0x39, (byte)0x4D, (byte)0xB6,
        (byte)0x50, (byte)0x07, (byte)0x4B, (byte)0xA0,
        (byte)0x14, (byte)0x3C, (byte)0x03, (byte)0x80,
        (byte)0xA6, (byte)0x54, (byte)0xFB, (byte)0x85,
        (byte)0xF9, (byte)0x65, (byte)0xB3, (byte)0xE1,
        (byte)0xE6, (byte)0x93, (byte)0x30, (byte)0x7C,
        (byte)0xCF, (byte)0xEE, (byte)0x15, (byte)0x34
      });
    }
  }
}
