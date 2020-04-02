package support;

public class LongPlus {
  public static long power(long base, long exponent) {
    long accumulator = 1;
    for (long i = 0; i < exponent; i++) {
      accumulator *= base;
    }
    return accumulator;
  }
}
