package crypto;
import bouncycastle.BigInteger;
import support.ByteArrayPlus;
import support.Decode;
import mtproto.Serialize;

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
    ByteArrayPlus to_hash = new ByteArrayPlus();
    to_hash.append_raw_bytes(Serialize.serialize_bytes(modulus.toByteArray()));
    to_hash.append_raw_bytes(Serialize.serialize_bytes(exponent.toByteArray()));
    byte[] to_hash_bytes = to_hash.toByteArray();
    byte[] full_hash = (new SHA1()).process_input_bytes(to_hash_bytes).digest();
    fingerprint = Decode.Little.long_decode(full_hash, SHA1.HASH_SIZE-8);
  }
}
