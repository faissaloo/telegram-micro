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
}
