package crypto;
import bouncycastle.BigInteger;

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

  //public static mask_generation_function(byte[] mask_generation_functionSeed, int masklen) {
  //Don't need to check if masklen is >2^32 hashlen because masklen is a 32-bit quantity that can't store that value anyway
  //  byte[] T = new byte[0];
  //  for (int i = 0; i<masklen/hashLen; i++) {
  //    D = integer_to_byte_array(i);
  //    T = T+hash(mask_generation_functionSeed + D);
  //  }
  //  return T[0...masklen];
  //}

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
