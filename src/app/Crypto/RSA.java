package crypto;
import bouncycastle.BigInteger;

public class RSA {
  public static byte[] encrypt(RSAPublicKey key, byte[] plaintext) {
    return (new BigInteger(plaintext))
      .modPow(key.exponent, key.modulus)
      .toByteArray();
  }
}
