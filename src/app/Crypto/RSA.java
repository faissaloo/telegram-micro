package crypto;
import bouncycastle.BigInteger;
import support.Debug;

public class RSA {
  public static byte[] encrypt(RSAPublicKey key, byte[] plaintext) {
    BigInteger result = (new BigInteger(plaintext)).modPow(key.exponent, key.modulus);
    System.out.println("ENCRYPTED DATA BYTES: "+result.toString(16));
    return result.magnitudeToBytes();
  }
}
