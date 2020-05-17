package test;

import crypto.RSAPublicKey;
import crypto.RSA;

public class RSAContext {
  public static class RSAEncryptionPrimitiveTest extends Test {
    public String label() {
      return "Can perform RSA encryption";
    }

    public void test() throws TestFailureException {
      RSAPublicKey public_key = new RSAPublicKey(0x10001, new byte[] {(byte)0x66, (byte)0x77, (byte)0x88});
      byte[] result = RSA.encrypt(public_key, new byte[] {(byte)0x14, (byte)0xFF, (byte)0x21});
      expect(result, new byte[] {(byte)0x39,(byte)0x84,0x11});
    }
  }
}
