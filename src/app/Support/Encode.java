package support;

public class Encode {
  //Encodes things into little endian byte arrays
  public static byte[] long_encode(long to_encode) {
    return new byte[] {
      (byte) (to_encode&0xFFL),
      (byte) ((to_encode&0xFF00L)>>8),
      (byte) ((to_encode&0xFF0000L)>>16),
      (byte) ((to_encode&0xFF000000L)>>24),
      (byte) ((to_encode&0xFF00000000L)>>32),
      (byte) ((to_encode&0xFF0000000000L)>>40),
      (byte) ((to_encode&0xFF000000000000L)>>48),
      (byte) ((to_encode&0xFF00000000000000L)>>56)
    };
  }

  public static byte[] int_encode(int to_encode) {
    return new byte[] {
      (byte) (to_encode&0xFFL),
      (byte) ((to_encode&0xFF00L)>>8),
      (byte) ((to_encode&0xFF0000L)>>16),
      (byte) ((to_encode&0xFF000000L)>>24),
    };
  }

  public static byte[] biginteger_encode(BigInteger to_encode) {
    int[] representation = to_encode.representation();

    return new byte[] {
      (byte) (representation[0]&0xFF),
      (byte) ((representation[0]&0xFF00) >> 8),
      (byte) ((representation[0]&0xFF0000) >> 16),
      (byte) ((representation[0]&0xFF000000) >> 24),
      (byte) (representation[1]&0xFF),
      (byte) ((representation[1]&0xFF00) >> 8),
      (byte) ((representation[1]&0xFF0000) >> 16),
      (byte) ((representation[1]&0xFF000000) >> 24),
      (byte) (representation[2]&0xFF),
      (byte) ((representation[2]&0xFF00) >> 8),
      (byte) ((representation[2]&0xFF0000) >> 16),
      (byte) ((representation[2]&0xFF000000) >> 24),
      (byte) (representation[3]&0xFF),
      (byte) ((representation[3]&0xFF00) >> 8),
      (byte) ((representation[3]&0xFF0000) >> 16),
      (byte) ((representation[3]&0xFF000000) >> 24),
    };
  }
}
