package support;

import java.lang.ArithmeticException;

//All arithmetic is unsigned (maybe we'll change this later)
public class BigInteger {
  int[] representation = new int[4];

  public static class QuotientRemainder {
    BigInteger quotient;
    BigInteger remainder;

    public QuotientRemainder(BigInteger quotient, BigInteger remainder) {
      this.quotient = quotient;
      this.remainder = remainder;
    }

    public BigInteger quotient() {
      return quotient;
    }

    public BigInteger remainder() {
      return remainder;
    }
  }

  public BigInteger(long upper, long lower) {
    representation[0] = (int)(lower & 0xFFFFFFFFL);
    representation[1] = (int)((lower & 0xFFFFFFFF00000000L) >>> 32);
    representation[2] = (int)(upper & 0xFFFFFFFFL);
    representation[3] = (int)((upper & 0xFFFFFFFF00000000L) >>> 32);
  }

  public BigInteger(long lower) {
    representation[0] = (int)(lower & 0xFFFFFFFFL);
    representation[1] = (int)((lower & 0xFFFFFFFF00000000L) >>> 32);
  }

  //ordered from lowest order ints to highest
  public BigInteger(int[] representation) {
    this.representation = representation;
  }

  public int[] representation() {
    return representation;
  }

  public long to_long() {
    return composeLowHigh(((long)representation[0])&0xFFFFFFFFL, ((long)representation[1])&0xFFFFFFFFL);
  }

  //for debug
  public String hex() {
    String new_string = Integer.toHexString(representation()[0]);
    for (int i = new_string.length(); i < 8; i ++) {
      new_string = "0"+new_string;
    }
    new_string = Integer.toHexString(representation()[1])+new_string;
    for (int i = new_string.length(); i < 16; i ++) {
      new_string = "0"+new_string;
    }
    new_string = Integer.toHexString(representation()[2])+new_string;
    for (int i = new_string.length(); i < 24; i ++) {
      new_string = "0"+new_string;
    }
    new_string = Integer.toHexString(representation()[3])+new_string;
    for (int i = new_string.length(); i < 32; i ++) {
      new_string = "0"+new_string;
    }

    return new_string;
  }

  public boolean equals(BigInteger other) {
    return (representation()[0] == other.representation()[0]) &&
      (representation()[1] == other.representation()[1]) &&
      (representation()[2] == other.representation()[2]) &&
      (representation()[3] == other.representation()[3]);
  }

  public boolean unsigned_greater_than(BigInteger other) {
    int[] other_representation = other.representation();

    if (representation[3] != other_representation[3]) {
      return ((long)representation[3]&0xFFFFFFFFL) > ((long)other_representation[3]&0xFFFFFFFFL);
    } else if (representation[2] != other_representation[2]) {
      return ((long)representation[2]&0xFFFFFFFFL) > ((long)other_representation[2]&0xFFFFFFFFL);
    } else if (representation[1] != other_representation[1]) {
      return ((long)representation[1]&0xFFFFFFFFL) > ((long)other_representation[1]&0xFFFFFFFFL);
    } else {
      return ((long)representation[0]&0xFFFFFFFFL) > ((long)other_representation[0]&0xFFFFFFFFL);
    }
  }

  public boolean unsigned_less_than(BigInteger other) {
    return !(equals(other) || unsigned_greater_than(other));
  }

  public boolean unsigned_greater_than_equal(BigInteger other) {
    return equals(other) || unsigned_greater_than(other);
  }

  public boolean unsigned_less_than_equal(BigInteger other) {
    return equals(other) || unsigned_less_than(other);
  }

  public BigInteger and(BigInteger other) {
    int[] other_representation = other.representation();

    return new BigInteger(new int[] {
      representation[0] & other_representation[0],
      representation[1] & other_representation[1],
      representation[2] & other_representation[2],
      representation[3] & other_representation[3]
    });
  }

  public BigInteger or(BigInteger other) {
    int[] other_representation = other.representation();

    return new BigInteger(new int[] {
      representation[0] | other_representation[0],
      representation[1] | other_representation[1],
      representation[2] | other_representation[2],
      representation[3] | other_representation[3]
    });
  }

  public BigInteger negate() {
    return not().add(one());
  }

  public BigInteger not() {
    return new BigInteger(new int[] {
      ~representation[0],
      ~representation[1],
      ~representation[2],
      ~representation[3]
    });
  }

  public BigInteger subtract(BigInteger other) {
    return add(other.negate());
  }

  public BigInteger add(BigInteger other) {
    int[] other_representation = other.representation();

    long partial_00 = (((long) representation[0]) & 0xFFFFFFFFL) +
      (((long) other_representation[0]) & 0xFFFFFFFFL);
    int partial_00_low = (int) unsignedLow(partial_00);
    int partial_00_high = (int) unsignedHigh(partial_00);

    long partial_11 = (((long) representation[1]) & 0xFFFFFFFFL) +
      (((long) other_representation[1]) & 0xFFFFFFFFL) +
      (((long) partial_00_high) & 0xFFFFFFFFL);
    int partial_11_low = (int) unsignedLow(partial_11);
    int partial_11_high = (int) unsignedHigh(partial_11);

    long partial_22 = (((long) representation[2]) & 0xFFFFFFFFL) +
      (((long) other_representation[2]) & 0xFFFFFFFFL) +
      (((long) partial_11_high) & 0xFFFFFFFFL);
    int partial_22_low = (int) unsignedLow(partial_22);
    int partial_22_high = (int) unsignedHigh(partial_22);

    long partial_33 = (((long) representation[3]) & 0xFFFFFFFFL) +
      (((long) other_representation[3]) & 0xFFFFFFFFL) +
      (((long) partial_22_high) & 0xFFFFFFFFL);
    int partial_33_low = (int) unsignedLow(partial_33);
    //Discard the high because it's overflow

    return new BigInteger(new int[] {
      partial_00_low,
      partial_11_low,
      partial_22_low,
      partial_33_low
    });
  }

  //https://en.wikipedia.org/wiki/Division_algorithm#Integer_division_(unsigned)_with_remainder
  public QuotientRemainder unsigned_divide_modulo(BigInteger other) throws ArithmeticException {
    if (other.equals(zero())) {
      throw new ArithmeticException("Division by zero");
    }
    BigInteger quotient = zero();
    BigInteger remainder = zero();

    for (int i = 128; i > 0; i--) {
      remainder = remainder.unsigned_left_shift(1);
      remainder = remainder.or(get_bit(i-1));

      if (remainder.unsigned_greater_than_equal(other)) {
        remainder = remainder.subtract(other);
        quotient = quotient.set_bit(i-1);
      }
    }
    return new QuotientRemainder(quotient, remainder);
  }

  public BigInteger get_bit(int index) {
    return unsigned_right_shift(index).and(one());
  }

  public BigInteger set_bit(int index) {
    return or(one().unsigned_left_shift(index));
  }

  public BigInteger unsigned_divide(BigInteger other) throws ArithmeticException {
    return unsigned_divide_modulo(other).quotient();
  }

  public BigInteger unsigned_modulo(BigInteger other) throws ArithmeticException {
    return unsigned_divide_modulo(other).remainder();
  }

  public static BigInteger zero() {
    return new BigInteger(new int[] {0,0,0,0});
  }

  public static BigInteger one() {
    return new BigInteger(new int[] {1,0,0,0});
  }

  public BigInteger unsigned_right_shift(int shift) {
    long low = 0L;
    long high = 0L;
    if (shift < 64) {
      low = composeLowHigh(((long)representation[0])&0xFFFFFFFFL, ((long)representation[1])&0xFFFFFFFFL);
      high = composeLowHigh(((long)representation[2])&0xFFFFFFFFL, ((long)representation[3])&0xFFFFFFFFL);
      long shift_mask = (LongPlus.power(2,shift)-1L);
      low = (low >>> shift) | ((high & shift_mask) << (64-shift));
      high = high >>> shift;
    } else if (shift < 128) {
      low = composeLowHigh(((long)representation[2])&0xFFFFFFFFL, ((long)representation[3])&0xFFFFFFFFL);
      low = (low >>> (shift-64));
    }
    return new BigInteger(high,low);
  }

  public BigInteger unsigned_left_shift(int shift) {
    long low = 0L;
    long high = 0L;
    if (shift < 64) {
      low = composeLowHigh(((long)representation[0])&0xFFFFFFFFL, ((long)representation[1])&0xFFFFFFFFL);
      high = composeLowHigh(((long)representation[2])&0xFFFFFFFFL, ((long)representation[3])&0xFFFFFFFFL);
      long shift_mask = ~(LongPlus.power(2,64-shift)-1L);
      int shift_into = 64-shift;
      high = high << shift | ((low & shift_mask) >> (64-shift));
      low = low << shift;
    } else if (shift < 128) {
      high = composeLowHigh(((long)representation[0])&0xFFFFFFFFL, ((long)representation[1])&0xFFFFFFFFL);
      high = (high << (shift-64));
    }
    return new BigInteger(high,low);
  }

  //https://codereview.stackexchange.com/questions/72286/multiplication-128-x-64-bits
  public static BigInteger unsigned_long_multiply(long x, long y) {
    long high = 0;
    long low = y;
    long y0  = unsignedLow(low);
    long y1  = unsignedHigh(low);
    long x0  = unsignedLow(x);
    long x1  = unsignedHigh(x);
    long y12 = composeLowHigh(y1, high);

    // The low 64-bits of the output can be computed easily.
    low = x * low;

    // The upper 64 bits of the output is a combination of several
    // factors.  These are the first two:
    high = (high * x0) + (y12 * x1);

    // Now handle the factors coming from the rest:
    long p01 = x0 * y1;
    long p10 = x1 * y0;
    long p00 = x0 * y0;

    // Add the high parts directly in.
    high += unsignedHigh(p01);
    high += unsignedHigh(p10);

    // Account for the possible carry from the low parts.
    long p2 = unsignedHigh(p00) + unsignedLow(p01) + unsignedLow(p10);
    high += unsignedHigh(p2);
    return new BigInteger(high, low);
  }

  /** Extract the 32 least significant bits treated as unsigned number. */
  private static long unsignedLow(long x) {
    return x & 0xFFFFFFFFL;
  }

  /** Extract the 32 most significant bits treated as unsigned number. */
  private static long unsignedHigh(long x) {
    return x >>> 32;
  }

  /** Compose a long from two unsigned parts. The upper 32 bits of low must be zero. */
  private static long composeLowHigh(long low, long high) {
    return (high << 32) + low;
  }
}
