package mtproto.secret;

import bouncycastle.BigInteger;

import crypto.SHA1;
import support.RandomPlus;
import support.Decode;

public final class SecretChatBootstrap {
  public static final int GROUP_GENERATOR = 3;
  public static final int EXPONENT_BYTES = 256;
  private static final BigInteger GROUP_GENERATOR_BIGINT = BigInteger.valueOf(GROUP_GENERATOR);
  private static final BigInteger KNOWN_DH_PRIME = new BigInteger(
    "C71CAEB9C6B1C9048E6C522F70F13F73980D40238E3E21C14934D037563D930F" +
    "48198A0AA7C14058229493D22530F4DBFA336F6E0AC925139543AED44CCE7C37" +
    "20FD51F69458705AC68CD4FE6B6B13ABDC9746512969328454F18FAF8C595F64" +
    "2477FE96BB2A941D5BCD1D4AC8CC49880708FA9B378E3C4F3A9060BEE67CF9A4" +
    "A4A695811051907E162753B56B0F6B410DBA74D8A84B2A14B3144E0EF1284754" +
    "FD17ED950D5965B4B9DD46582DB1178D169C6BC465B0D6FF9CA3928FEF5B9AE4" +
    "E418FC15E83EBEA0F87FA9FF5EED70050DED2849F47BF959D956850CE929851F" +
    "0D8115F635B105EE2E4E15D04B2454BF6F4FADF034B10403119CD8E3B92FCC5B",
    16
  );

  private SecretChatBootstrap() {}

  public static void generateExponent(RandomPlus random, byte[] exponentBytes) {
    if (random == null) {
      throw new IllegalArgumentException("Random generator is required.");
    }
    if (exponentBytes == null || exponentBytes.length < EXPONENT_BYTES) {
      throw new IllegalArgumentException("Secret chat exponent scratch is too small.");
    }

    fillExponent(random, exponentBytes);
  }

  public static byte[] publicValueForExponent(BigInteger exponent) {
    if (exponent == null || exponent.signum() <= 0) {
      throw new IllegalArgumentException("Secret chat exponent must be positive.");
    }
    return GROUP_GENERATOR_BIGINT
      .modPow(exponent, KNOWN_DH_PRIME)
      .magnitudeToBytes();
  }

  public static byte[] sharedKeyForPeerPublic(byte[] peerPublic, BigInteger exponent) {
    if (peerPublic == null || peerPublic.length == 0) {
      throw new IllegalArgumentException("Peer public value is required.");
    }
    if (exponent == null || exponent.signum() <= 0) {
      throw new IllegalArgumentException("Secret chat exponent must be positive.");
    }

    return new BigInteger(1, peerPublic)
      .modPow(exponent, KNOWN_DH_PRIME)
      .magnitudeToBytes();
  }

  public static long fingerprintForSharedKey(byte[] sharedKey) {
    byte[] digest;

    if (sharedKey == null || sharedKey.length == 0) {
      throw new IllegalArgumentException("Secret chat shared key is required.");
    }

    digest = (new SHA1()).digest(sharedKey);
    return Decode.Little.long_decode(digest, SHA1.HASH_SIZE - 8);
  }

  public static byte[] generatePublicValue(RandomPlus random) {
    return generatePublicValue(random, new byte[EXPONENT_BYTES]);
  }

  public static byte[] generatePublicValue(RandomPlus random, byte[] exponentBytes) {
    generateExponent(random, exponentBytes);
    return publicValueForExponent(new BigInteger(1, exponentBytes));
  }

  private static void fillExponent(RandomPlus random, byte[] exponentBytes) {
    int i;

    for (i = 0; i < EXPONENT_BYTES; ++i) {
      exponentBytes[i] = random.nextByte();
    }
    exponentBytes[0] &= 0x7F;
    exponentBytes[0] |= 0x40;
    exponentBytes[exponentBytes.length - 1] |= 0x01;
  }
}
