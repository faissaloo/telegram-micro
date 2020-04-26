package support;

public class Decode {
  public static class Little {
    public static int int_decode(byte[] data, int offset) {
      return (data[offset+0]&0xFF)|
      ((data[offset+1]&0xFF)<<8)|
      ((data[offset+2]&0xFF)<<16)|
      ((data[offset+3]&0xFF)<<24);
    }

    public static long long_decode(byte[] data, int offset) {
      return (data[offset+0]&0xFFL)|
      ((data[offset+1]&0xFFL)<<8)|
      ((data[offset+2]&0xFFL)<<16)|
      ((data[offset+3]&0xFFL)<<24)|
      ((data[offset+4]&0xFFL)<<32)|
      ((data[offset+5]&0xFFL)<<40)|
      ((data[offset+6]&0xFFL)<<48)|
      ((data[offset+7]&0xFFL)<<56);
    }

    public static Integer128 Integer128_decode(byte[] data, int offset) {
      return new Integer128(long_decode(data, offset+8),long_decode(data, offset));
    }
  }

  public static class Big {
    public static long long_decode(byte[] data, int offset) {
      return (data[offset+7]&0xFFL)|
      ((data[offset+6]&0xFFL)<<8)|
      ((data[offset+5]&0xFFL)<<16)|
      ((data[offset+4]&0xFFL)<<24)|
      ((data[offset+3]&0xFFL)<<32)|
      ((data[offset+2]&0xFFL)<<40)|
      ((data[offset+1]&0xFFL)<<48)|
      ((data[offset+0]&0xFFL)<<56);
    }
  }
}
