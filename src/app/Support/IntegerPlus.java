package support;

public class IntegerPlus {
  public static int rotateRight(int integer, int shift) {
     return (integer >>> shift) | (integer << (32 - shift));
  }
}
