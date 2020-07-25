package crypto;
import bouncycastle.BigInteger;
import support.Debug;

public class RSA {
  public static byte[] encrypt(RSAPublicKey key, byte[] plaindata) {
    BigInteger numeric_plaindata = new BigInteger(1, plaindata);
    BigInteger result = numeric_plaindata.modPow(key.exponent, key.modulus);

    return result.toByteArray();
  }
}
