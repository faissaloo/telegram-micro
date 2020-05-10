package crypto;
import bouncycastle.BigInteger;
import support.ByteArrayPlus;

//7.2.2 of SP 800-56B
public class RSAPKCS1OAEP {
  //public static void encrypt(RSAPublicKey key, byte[] keying_material, byte[] additional_input) {
    //(n,e) key
    //K = keying_material
    //A = additional_input (probably blank?)
    //hashLen = byte length of hash function output

    //nlen = n.length
    //KLen = K.length
    //if KLen > nlen - 2hashLen -2 {
    //  raise new Exception("Keying material too long")
    //}
    //em = oeap()
    //return rsa_encryption_primitive(key, em)
  //}

  public static byte[] mask_generation_function(byte[] mask_generation_functionSeed, int masklen) {
    //Don't need to check if masklen is >2^32 hashlen because masklen is a 32-bit quantity that can't store that value anyway
    //hashlen is 32
    ByteArrayPlus to_return = new ByteArrayPlus();
    for (int i = 0; i<masklen/SHA256.HASH_SIZE+1; i++) {
      SHA256 sha256_engine = new SHA256();
      ByteArrayPlus to_hash = new ByteArrayPlus();
      to_hash.append_bytes(mask_generation_functionSeed);
      to_hash.append_int(i);
      int bytes_to_append = (((SHA256.HASH_SIZE-((SHA256.HASH_SIZE*(i+1))%masklen))+31)%SHA256.HASH_SIZE)+1;
      System.out.println(bytes_to_append);
      to_return.append_bytes_up_to(sha256_engine.digest(to_hash.toByteArray()), bytes_to_append); //We need some way to make sure we don't add more than masklen
    }
    return to_return.toByteArray();
  }

  //public static byte[] oeap(RSAPublicKey key, byte[] keying_material, byte[] additional_input) {
  //  hashA = some kind of hash of the additional input?
  //  PS = new byte[nLen1-KLen-2*hashLen-2]; //These will all be zero bytes
  //  DB = hashA + PS + new byte[1] {1} + K
  //  mask_generation_functionSeed = random byte string of hashLen length
  //  dbMask = mask_generation_function(mask_generation_functionSeed, nlen, - hashLen - 1)
  //  masked_db = db^mask_generation_functionSeedMask
  //  em = new byte[1] {0} + maskedMGFSeed + masked_db
  //  return em;
  //}

  public static byte[] rsa_encryption_primitive(RSAPublicKey key, byte[] plaintext) {
    return (new BigInteger(plaintext))
      .modPow(key.exponent, key.modulus)
      .toByteArray();
  }
}
