package support;

public class IntegerPlus {
  public static int rotateRight(int integer, int shift) {
     return (integer >>> shift) | (integer << (32 - shift));
  }

  public static String padded_hex(int integer) {
    String new_string = Integer.toHexString(integer);
    for (int i = new_string.length(); i < 8; i ++) {
      new_string = "0"+new_string;
    }
    return new_string;
  }
}
