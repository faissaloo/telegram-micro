package crypto;
import bouncycastle.BigInteger;
import support.ByteArrayPlus;
import support.Decode;
import mtproto.Serializer;

public class RSAPublicKey {
  public BigInteger exponent;
  public BigInteger modulus;
  public long fingerprint;

  public RSAPublicKey(int exponent, byte[] modulus) {
    this.exponent = BigInteger.valueOf(exponent);
    this.modulus = new BigInteger(1, modulus);

    update_fingerprint();
  }

  public RSAPublicKey(long fingerprint, int exponent, byte[] modulus) {
    this.fingerprint = fingerprint;
    this.exponent = BigInteger.valueOf(exponent);
    this.modulus = new BigInteger(1, modulus);
  }

  //https://github.com/tdlib/td/issues/250
  //https://github.com/tdlib/td/blob/4eed84132e1389f2c80c77f0b8d6ca0d81a278d5/td/mtproto/RSA.cpp#L77
  public void update_fingerprint() {
    byte[] to_hash_bytes = (new Serializer())
      .append_BigInteger(modulus)
      .append_BigInteger(exponent)
      .end();
    byte[] full_hash = (new SHA1()).digest(to_hash_bytes);
    fingerprint = Decode.Little.long_decode(full_hash, SHA1.HASH_SIZE-8);
  }
}
