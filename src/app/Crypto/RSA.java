package crypto;
import bouncycastle.BigInteger;
import support.Debug;

public class RSA {
  public static byte[] encrypt(RSAPublicKey key, byte[] plaindata) {
    BigInteger numeric_plaindata = new BigInteger(1, plaindata);
    BigInteger result = numeric_plaindata.modPow(key.exponent, key.modulus);

    System.out.println("EXPONENT: " + key.exponent.toString(16));
    System.out.println("MODULUS: " + key.modulus.toString(16));
    System.out.println("PLAINDATA: " + numeric_plaindata.toString(16));
    System.out.println("ENCRYPTED DATA BYTES: "+result.toString(16));
    return result.magnitudeToBytes();
  }
}
