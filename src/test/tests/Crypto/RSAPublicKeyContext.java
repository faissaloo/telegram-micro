package test;

import crypto.RSAPublicKey;
import crypto.RSA;

public class RSAPublicKeyContext {
  public static class RSAPublicKeySignatureTest extends Test {
    public String label() {
      return "Can get a signature for a public key";
    }

    public void test() throws TestFailureException {
      RSAPublicKey public_key = new RSAPublicKey(0x10001, new byte[] {(byte)0x66, (byte)0x77, (byte)0x88});
      expect(public_key.signature, 0xAEBD06335441BD73L);
    }
  }
}
