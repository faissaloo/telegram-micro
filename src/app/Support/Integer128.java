package support;

import java.lang.ArithmeticException;

//All arithmetic is unsigned (maybe we'll change this later)
public class Integer128 {
  public static Integer128 one = new Integer128(1L);
  public static Integer128 zero = new Integer128(0L);

  public int representation_0 = 0;
  public int representation_1 = 0;
  public int representation_2 = 0;
  public int representation_3 = 0;

  public static class QuotientRemainder {
    Integer128 quotient;
    Integer128 remainder;

    public QuotientRemainder(Integer128 quotient, Integer128 remainder) {
      this.quotient = quotient;
      this.remainder = remainder;
    }

    public Integer128 quotient() {
      return quotient;
    }

    public Integer128 remainder() {
      return remainder;
    }
  }

  public Integer128(Integer128 to_copy) {
    set(to_copy);
  }
  
  public Integer128 set(Integer128 other) {
    representation_0 = other.representation_0;
    representation_1 = other.representation_1;
    representation_2 = other.representation_2;
    representation_3 = other.representation_3;
    
    return this;
  }

  public Integer128(long upper, long lower) {
    //Wonder if I could improve things by doing the shift first...
    representation_0 = (int)(lower & 0xFFFFFFFFL);
    representation_1 = (int)((lower & 0xFFFFFFFF00000000L) >>> 32);
    representation_2 = (int)(upper & 0xFFFFFFFFL);
    representation_3 = (int)((upper & 0xFFFFFFFF00000000L) >>> 32);
  }

  public Integer128(long lower) {
    representation_0 = (int)(lower & 0xFFFFFFFFL);
    representation_1 = (int)((lower & 0xFFFFFFFF00000000L) >>> 32);
  }

  //ordered from lowest order ints to highest
  //maybe deprecated
  public Integer128(int[] representation) {
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
    return IntegerPlus.padded_hex(representation_3)+
      IntegerPlus.padded_hex(representation_2)+
      IntegerPlus.padded_hex(representation_1)+
      IntegerPlus.padded_hex(representation_0);
  }

  public boolean equals(Integer128 other) {
    return (representation_0 == other.representation_0) &&
      (representation_1 == other.representation_1) &&
      (representation_2 == other.representation_2) &&
      (representation_3 == other.representation_3);
  }

  public boolean unsigned_greater_than(Integer128 other) {
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

  public boolean unsigned_less_than(Integer128 other) {
    return !(equals(other) || unsigned_greater_than(other));
  }

  public boolean unsigned_greater_than_equal(Integer128 other) {
    return equals(other) || unsigned_greater_than(other);
  }

  public boolean unsigned_less_than_equal(Integer128 other) {
    return equals(other) || unsigned_less_than(other);
  }

  public Integer128 and(Integer128 other) {
    return new Integer128(new int[] {
      representation_0 & other.representation_0,
      representation_1 & other.representation_1,
      representation_2 & other.representation_2,
      representation_3 & other.representation_3
    });
  }

  public Integer128 mutating_or(Integer128 other) {
    representation_0 |= other.representation_0;
    representation_1 |= other.representation_1;
    representation_2 |= other.representation_2;
    representation_3 |= other.representation_3;

    return this;
  }

  public Integer128 or(Integer128 other) {
    return (new Integer128(this)).mutating_or(other);
  }

  public Integer128 mutating_negate() {
    return mutating_not().mutating_add();
  }

  public Integer128 negate() {
    return (new Integer128(this)).mutating_negate();
  }

  public Integer128 mutating_not() {
    representation_0 = ~representation_0;
    representation_1 = ~representation_1;
    representation_2 = ~representation_2;
    representation_3 = ~representation_3;

    return this;
  }

  public Integer128 not() {
    return (new Integer128(this)).mutating_not();
  }

  public Integer128 mutating_subtract(Integer128 other) {
    mutating_add(other.negate());

    return this;
  }

  public Integer128 subtract(Integer128 other) {
    return (new Integer128(this)).mutating_subtract(other);
  }

  public Integer128 mutating_add() {
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

  public Integer128 mutating_add(Integer128 other) {
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

  public Integer128 add(Integer128 other) {
    return (new Integer128(this)).mutating_add(other);
  }

  public Integer128 get_bit(int index) {
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

  public Integer128 mutating_set_bit(int index) {
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

  public Integer128 set_bit(int index) {
    return (new Integer128(this)).mutating_set_bit(index);
  }

  //https://en.wikipedia.org/wiki/Division_algorithm#Integer_division_(unsigned)_with_remainder
  public QuotientRemainder unsigned_divide_modulo(Integer128 other) throws ArithmeticException {
    if (other.equals(zero)) {
      throw new ArithmeticException("Division by zero");
    }

    Integer128 quotient = new Integer128(zero);
    Integer128 remainder = new Integer128(zero);
    Integer128 negated_other = other.negate();

    for (int i = 128; i > 0; i--) {
      remainder.mutating_unsigned_left_shift().mutating_or(get_bit(i-1));

      if (remainder.unsigned_greater_than_equal(other)) {
        remainder.mutating_add(negated_other);
        quotient.mutating_set_bit(i-1);
      }
    }
    return new QuotientRemainder(quotient, remainder);
  }

  public Integer128 unsigned_divide(Integer128 other) throws ArithmeticException {
    if (other.equals(zero)) {
      throw new ArithmeticException("Division by zero");
    }

    Integer128 quotient = new Integer128(zero);
    Integer128 remainder = new Integer128(zero);
    Integer128 negated_other = other.negate();

    for (int i = 128; i > 0; i--) {
      remainder.mutating_unsigned_left_shift().mutating_or(get_bit(i-1));

      if (remainder.unsigned_greater_than_equal(other)) {
        remainder.mutating_add(negated_other);
        quotient.mutating_set_bit(i-1);
      }
    }
    return quotient;
  }

  public Integer128 unsigned_modulo(Integer128 other) throws ArithmeticException {
    return fast_unsigned_modulo(other, other.negate());
  }
  
  public Integer128 fast_unsigned_modulo(Integer128 other, Integer128 negated_other) {
    if (other.equals(zero)) {
      throw new ArithmeticException("Division by zero");
    }

    //https://stackoverflow.com/a/33333636/5269447
    if (unsigned_greater_than_equal(other)) {
      Integer128 remainder = new Integer128(zero);

      for (int i = 128; i > 0; i--) {
        remainder.mutating_unsigned_left_shift().mutating_or(get_bit(i-1));

        if (remainder.unsigned_greater_than_equal(other)) {
          remainder.mutating_add(negated_other);
        }
      }
      
      set(remainder);
    }
    return this;
  }

  public Integer128 unsigned_right_shift(int shift) {
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
    return new Integer128(high,low);
  }

  //This shortcut makes division way faster
  public Integer128 mutating_unsigned_left_shift() {
    representation_3 = (representation_3 << 1) | (representation_2 >>> 31);
    representation_2 = (representation_2 << 1) | (representation_1 >>> 31);
    representation_1 = (representation_1 << 1) | (representation_0 >>> 31);
    representation_0 = (representation_0 << 1);

    return this;
  }

  public Integer128 mutating_unsigned_left_shift(int shift) {
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

  public Integer128 unsigned_left_shift(int shift) {
    return (new Integer128(this)).mutating_unsigned_left_shift(shift);
  }

  //https://codereview.stackexchange.com/questions/72286/multiplication-128-x-64-bits
  public static Integer128 unsigned_long_multiply(long x, long y) {
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
    return new Integer128(high, low);
  }
}
