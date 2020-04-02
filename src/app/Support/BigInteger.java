package support;

import java.lang.ArithmeticException;

//All arithmetic is unsigned (maybe we'll change this later)
public class BigInteger {
  public static BigInteger one = new BigInteger(1L);
  public static BigInteger zero = new BigInteger(0L);

  public int representation_0 = 0;
  public int representation_1 = 0;
  public int representation_2 = 0;
  public int representation_3 = 0;

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

  public BigInteger(BigInteger to_copy) {
    representation_0 = to_copy.representation_0;
    representation_1 = to_copy.representation_1;
    representation_2 = to_copy.representation_2;
    representation_3 = to_copy.representation_3;
  }

  public BigInteger(long upper, long lower) {
    //Wonder if I could improve things by doing the shift first...
    representation_0 = (int)(lower & 0xFFFFFFFFL);
    representation_1 = (int)((lower & 0xFFFFFFFF00000000L) >>> 32);
    representation_2 = (int)(upper & 0xFFFFFFFFL);
    representation_3 = (int)((upper & 0xFFFFFFFF00000000L) >>> 32);
  }

  public BigInteger(long lower) {
    representation_0 = (int)(lower & 0xFFFFFFFFL);
    representation_1 = (int)((lower & 0xFFFFFFFF00000000L) >>> 32);
  }

  //ordered from lowest order ints to highest
  //maybe deprecated
  public BigInteger(int[] representation) {
    representation_0 = representation[0];
    representation_1 = representation[1];
    representation_2 = representation[2];
    representation_3 = representation[3];
  }

  public long to_long() {
    return ((((long)representation_1)&0xFFFFFFFFL) << 32) | (((long)representation_0)&0xFFFFFFFFL);
  }

  //for debug
  public String hex() {
    String new_string = Integer.toHexString(representation_0);
    for (int i = new_string.length(); i < 8; i ++) {
      new_string = "0"+new_string;
    }
    new_string = Integer.toHexString(representation_1)+new_string;
    for (int i = new_string.length(); i < 16; i ++) {
      new_string = "0"+new_string;
    }
    new_string = Integer.toHexString(representation_2)+new_string;
    for (int i = new_string.length(); i < 24; i ++) {
      new_string = "0"+new_string;
    }
    new_string = Integer.toHexString(representation_3)+new_string;
    for (int i = new_string.length(); i < 32; i ++) {
      new_string = "0"+new_string;
    }

    return new_string;
  }

  public boolean equals(BigInteger other) {
    return (representation_0 == other.representation_0) &&
      (representation_1 == other.representation_1) &&
      (representation_2 == other.representation_2) &&
      (representation_3 == other.representation_3);
  }

  public boolean unsigned_greater_than(BigInteger other) {
    if (representation_3 != other.representation_3) {
      return ((long)representation_3&0xFFFFFFFFL) > ((long)other.representation_3&0xFFFFFFFFL);
    } else if (representation_2 != other.representation_2) {
      return ((long)representation_2&0xFFFFFFFFL) > ((long)other.representation_2&0xFFFFFFFFL);
    } else if (representation_1 != other.representation_1) {
      return ((long)representation_1&0xFFFFFFFFL) > ((long)other.representation_1&0xFFFFFFFFL);
    } else {
      return ((long)representation_0&0xFFFFFFFFL) > ((long)other.representation_0&0xFFFFFFFFL);
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
    return new BigInteger(new int[] {
      representation_0 & other.representation_0,
      representation_1 & other.representation_1,
      representation_2 & other.representation_2,
      representation_3 & other.representation_3
    });
  }

  public BigInteger mutating_or(BigInteger other) {
    representation_0 |= other.representation_0;
    representation_1 |= other.representation_1;
    representation_2 |= other.representation_2;
    representation_3 |= other.representation_3;

    return this;
  }

  public BigInteger or(BigInteger other) {
    return (new BigInteger(this)).mutating_or(other);
  }

  public BigInteger mutating_negate() {
    return mutating_not().mutating_add();
  }

  public BigInteger negate() {
    return (new BigInteger(this)).mutating_negate();
  }

  public BigInteger mutating_not() {
    representation_0 = ~representation_0;
    representation_1 = ~representation_1;
    representation_2 = ~representation_2;
    representation_3 = ~representation_3;

    return this;
  }

  public BigInteger not() {
    return (new BigInteger(this)).mutating_not();
  }

  public BigInteger mutating_subtract(BigInteger other) {
    mutating_add(other.negate());

    return this;
  }

  public BigInteger subtract(BigInteger other) {
    return (new BigInteger(this)).mutating_subtract(other);
  }

  public BigInteger mutating_add() {
    long partial_00 = (((long) representation_0) & 0xFFFFFFFFL) + 1L;
    representation_0 = (int) partial_00;
    long partial_00_high = (partial_00 >>> 32);

    long partial_11 = (((long) representation_1) & 0xFFFFFFFFL) + partial_00_high;
    representation_1 = (int) partial_11;
    long partial_11_high = (partial_11 >>> 32);

    long partial_22 = (((long) representation_2) & 0xFFFFFFFFL) + partial_11_high;
    representation_2 = (int) partial_22;
    long partial_22_high = (partial_22 >>> 32);

    long partial_33 = (((long) representation_3) & 0xFFFFFFFFL) + partial_22_high;
    representation_3 = (int) partial_33;
    //Discard the high because it's overflow

    return this;
  }

  public BigInteger mutating_add(BigInteger other) {
    long partial_00 = (((long) representation_0) & 0xFFFFFFFFL) +
      (((long) other.representation_0) & 0xFFFFFFFFL);
    representation_0 = (int) partial_00;
    int partial_00_high = (int) (partial_00 >>> 32);

    long partial_11 = (((long) representation_1) & 0xFFFFFFFFL) +
      (((long) other.representation_1) & 0xFFFFFFFFL) +
      (((long) partial_00_high) & 0xFFFFFFFFL);
    representation_1 = (int) partial_11;
    int partial_11_high = (int) (partial_11 >>> 32);

    long partial_22 = (((long) representation_2) & 0xFFFFFFFFL) +
      (((long) other.representation_2) & 0xFFFFFFFFL) +
      (((long) partial_11_high) & 0xFFFFFFFFL);
    representation_2 = (int) partial_22;
    int partial_22_high = (int) (partial_22 >>> 32);

    long partial_33 = (((long) representation_3) & 0xFFFFFFFFL) +
      (((long) other.representation_3) & 0xFFFFFFFFL) +
      (((long) partial_22_high) & 0xFFFFFFFFL);
    representation_3 = (int) partial_33;
    //Discard the high because it's overflow

    return this;
  }

  public BigInteger add(BigInteger other) {
    return (new BigInteger(this)).mutating_add(other);
  }

  public BigInteger get_bit(int index) {
    if (index >= 0) {
      if (index >= 32) {
        if (index >= 64) {
          if (index >= 96) {
            if (((representation_3 >>> (index - 96))&1) == 1) {
              return one;
            } else {
              return zero;
            }
          } else {
            if (((representation_2 >>> (index - 64))&1) == 1) {
              return one;
            } else {
              return zero;
            }
          }
        } else {
          if (((representation_1 >>> (index - 32))&1) == 1) {
            return one;
          } else {
            return zero;
          }
        }
      } else {
        if (((representation_0 >>> index)&1) == 1) {
          return one;
        } else {
          return zero;
        }
      }
    } else {
      return zero;
    }
  }

  public BigInteger mutating_set_bit(int index) {
    if (index >= 0) {
      if (index >= 32) {
        if (index >= 64) {
          if (index >= 96) {
            representation_3 |= (1<<(index - 96));
          } else {
            representation_2 |= (1<<(index - 64));
          }
        } else {
          representation_1 |= (1<<(index - 32));
        }
      } else {
        representation_0 |= (1<<index);
      }
    }
    return this;
  }

  public BigInteger set_bit(int index) {
    return (new BigInteger(this)).mutating_set_bit(index);
  }

  //https://en.wikipedia.org/wiki/Division_algorithm#Integer_division_(unsigned)_with_remainder
  public QuotientRemainder unsigned_divide_modulo(BigInteger other) throws ArithmeticException {
    if (other.equals(zero)) {
      throw new ArithmeticException("Division by zero");
    }

    BigInteger quotient = new BigInteger(zero);
    BigInteger remainder = new BigInteger(zero);
    BigInteger negated_other = other.negate();

    for (int i = 128; i > 0; i--) {
      remainder.mutating_unsigned_left_shift().mutating_or(get_bit(i-1));

      if (remainder.unsigned_greater_than_equal(other)) {
        remainder.mutating_add(negated_other);
        quotient.mutating_set_bit(i-1);
      }
    }
    return new QuotientRemainder(quotient, remainder);
  }

  public BigInteger unsigned_divide(BigInteger other) throws ArithmeticException {
    if (other.equals(zero)) {
      throw new ArithmeticException("Division by zero");
    }

    BigInteger quotient = new BigInteger(zero);
    BigInteger remainder = new BigInteger(zero);
    BigInteger negated_other = other.negate();

    for (int i = 128; i > 0; i--) {
      remainder.mutating_unsigned_left_shift().mutating_or(get_bit(i-1));

      if (remainder.unsigned_greater_than_equal(other)) {
        remainder.mutating_add(negated_other);
        quotient.mutating_set_bit(i-1);
      }
    }
    return quotient;
  }

  public BigInteger unsigned_modulo(BigInteger other) throws ArithmeticException {
    if (other.equals(zero)) {
      throw new ArithmeticException("Division by zero");
    }

    //https://stackoverflow.com/a/33333636/5269447
    if (unsigned_greater_than_equal(other)) {
      BigInteger remainder = new BigInteger(zero);
      BigInteger negated_other = other.negate();

      for (int i = 128; i > 0; i--) {
        remainder.mutating_unsigned_left_shift().mutating_or(get_bit(i-1));

        if (remainder.unsigned_greater_than_equal(other)) {
          remainder.mutating_add(negated_other);
        }
      }
      return remainder;
    } else {
      return new BigInteger(this);
    }
  }

  public BigInteger unsigned_right_shift(int shift) {
    long low = 0L;
    long high = 0L;
    if (shift < 64) {
      low = ((((long)representation_1)&0xFFFFFFFFL) << 32) | (((long)representation_0)&0xFFFFFFFFL);
      high = ((((long)representation_3)&0xFFFFFFFFL) << 32) | (((long)representation_2)&0xFFFFFFFFL);
      long shift_mask = ((2L << shift)-1L);
      low = (low >>> shift) | ((high & shift_mask) << (64-shift));
      high = high >>> shift;
    } else if (shift < 128) {
      low = ((((long)representation_3)&0xFFFFFFFFL) << 32) | (((long)representation_2)&0xFFFFFFFFL);
      low = (low >>> (shift-64));
    }
    return new BigInteger(high,low);
  }

  //This shortcut makes division way faster
  public BigInteger mutating_unsigned_left_shift() {
    representation_3 = (representation_3 << 1) | (representation_2 >>> 31);
    representation_2 = (representation_2 << 1) | (representation_1 >>> 31);
    representation_1 = (representation_1 << 1) | (representation_0 >>> 31);
    representation_0 = (representation_0 << 1);

    return this;
  }

  public BigInteger mutating_unsigned_left_shift(int shift) {
    long low = 0L;
    long high = 0L;
    if (shift < 64) {
      low = ((((long)representation_1)&0xFFFFFFFFL) << 32) | (((long)representation_0)&0xFFFFFFFFL);
      high = ((((long)representation_3)&0xFFFFFFFFL) << 32) | (((long)representation_2)&0xFFFFFFFFL);
      long shift_mask = ~((2L << (64-shift))-1L);
      high = high << shift | ((low & shift_mask) >>> (64-shift));
      low = low << shift;
    } else if (shift < 128) {
      high = ((((long)representation_1)&0xFFFFFFFFL) << 32) | (((long)representation_0)&0xFFFFFFFFL);
      high = (high << (shift-64));
    }

    representation_0 = (int)(low & 0xFFFFFFFFL);
    representation_1 = (int)((low & 0xFFFFFFFF00000000L) >>> 32);
    representation_2 = (int)(high & 0xFFFFFFFFL);
    representation_3 = (int)((high & 0xFFFFFFFF00000000L) >>> 32);

    return this;
  }

  public BigInteger unsigned_left_shift(int shift) {
    return (new BigInteger(this)).mutating_unsigned_left_shift(shift);
  }

  //https://codereview.stackexchange.com/questions/72286/multiplication-128-x-64-bits
  public static BigInteger unsigned_long_multiply(long x, long y) {
    long high = 0;
    long low = y;
    long y0  = (low & 0xFFFFFFFFL);
    long y1  = (low >>> 32);
    long x0  = (x & 0xFFFFFFFFL);
    long x1  = (x >>> 32);
    long y12 = (high << 32) | y1;

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
    high += (p01 >>> 32);
    high += (p10 >>> 32);

    // Account for the possible carry from the low parts.
    long p2 = (p00 >>> 32) + (p01 & 0xFFFFFFFFL) + (p10 & 0xFFFFFFFFL);
    high += (p2 >>> 32);
    return new BigInteger(high, low);
  }
}
