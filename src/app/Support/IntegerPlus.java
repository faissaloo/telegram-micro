package support;

public class IntegerPlus {
  public static int power(int base, int exponent) {
    int accumulator = 1;
    for (int i = 0; i < exponent; i++) {
      accumulator *= base;
    }
    return accumulator;
  }
}
