package crypto;
import bouncycastle.BigInteger;

public class RSAPublicKey {
  public BigInteger exponent;
  public BigInteger modulus;

  public RSAPublicKey(int exponent, byte[] modulus) {
    this.exponent = BigInteger.valueOf(exponent);
    this.modulus = new BigInteger(modulus);
  }
}
