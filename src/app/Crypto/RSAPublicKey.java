package crypto;
import bouncycastle.BigInteger;
import support.ByteArrayPlus;
import support.Decode;
import mtproto.Serialize;

public class RSAPublicKey {
  public BigInteger exponent;
  public BigInteger modulus;
  public long signature;

  public RSAPublicKey(int exponent, byte[] modulus) {
    this.exponent = BigInteger.valueOf(exponent);
    this.modulus = new BigInteger(modulus);

    update_signature();
  }

  //https://github.com/tdlib/td/issues/250
  public void update_signature() {
    ByteArrayPlus to_hash = new ByteArrayPlus();
    to_hash.append_bytes(Serialize.serialize_bytes(exponent.toByteArray()));
    to_hash.append_bytes(Serialize.serialize_bytes(modulus.toByteArray()));
    SHA1 hash_engine = new SHA1();
    signature = Decode.Little.long_decode(hash_engine.digest(to_hash.toByteArray()), SHA1.HASH_SIZE-8);
  }
}
