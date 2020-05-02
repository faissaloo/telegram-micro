package support;

import java.lang.ArithmeticException;
import java.lang.Math;

public class BigInteger {
  public int sign;
  //Big endian
  public int[] representation;

  public BigInteger(int[] representation) {
    this.representation = representation;
    sign = 1;
    set_zero_sign();
  }

  public BigInteger(int[] representation, int sign) {
    this.representation = representation;
    this.sign = sign;
  }

  private void set_zero_sign() {
    for (int i = 0; i < representation.length; i++) {
      if (representation[i] != 0) {
        return;
      }
    }
    sign = 0;
  }

  public String hex() {
    String new_string = "";

    if (representation.length > 0) {
      new_string += Integer.toHexString(representation[0]);
      for (int i = 1; i < representation.length; i++) {
        new_string += IntegerPlus.padded_hex(representation[i]);
      }
    }

    if (sign == -1) {
      return "-"+new_string;
    } else {
      return new_string;
    }
  }

  public boolean equal(BigInteger other) {
    if (sign == other.sign) {
      boolean still_equal = true;

      for (int i = 0; i < Math.max(representation.length, other.representation.length); i++) {
        int this_value = 0;
        int other_value = 0;
        if (i < representation.length) {
          this_value = representation[i];
        }

        if (i < other.representation.length) {
          other_value = other.representation[i];
        }
        still_equal &= this_value == other_value;
      }

      return still_equal;
    } else {
      return false;
    }
  }

  public BigInteger mutating_not() {
    for (int i=0; i < representation.length; i++) {
      representation[i] = ~representation[i];
    }
    set_zero_sign();
    return this;
  }

  //https://github.com/openjdk-mirror/jdk7u-jdk/blob/master/src/share/classes/java/math/BigInteger.java#L1133
  public BigInteger mutating_subtract(BigInteger other) {
    //this. must be larger than other.
    int bigIndex = representation.length;
    int[] big = representation;
    int[] little = other.representation;
    int[] result = new int[bigIndex];
    int littleIndex = other.representation.length;
    long difference = 0;

    while (littleIndex > 0) {
        difference = (big[--bigIndex] & 0xFFFFFFFFL) -
                     (little[--littleIndex] & 0xFFFFFFFFL) +
                     (difference >> 32);
        result[bigIndex] = (int)difference;
    }

    boolean borrow = (difference >> 32 != 0);

    while (bigIndex > 0 && borrow) {
      borrow = ((result[--bigIndex] = big[bigIndex] - 1) == -1);
    }

    while (bigIndex > 0) {
      result[--bigIndex] = big[bigIndex];
    }

    representation = result;
    return this;
  }

  public BigInteger mutating_add(BigInteger other) {
    if (sign == other.sign) {
      long last_high = 0;

      long first;
      long second;
      long partial;

      for (int i = Math.max(representation.length-1, other.representation.length-1); i > -1; i--) {
        if (i > representation.length-1) {
          grow(1);
          first = 0;
        } else {
          first = ((long) representation[i]) & 0xFFFFFFFFL;
        }

        if (i > other.representation.length-1) {
          second = 0;
        } else {
          second = ((long) other.representation[i]) & 0xFFFFFFFFL;
        }

        partial = first + second + last_high;
        representation[i] = (int) partial;
        last_high = (partial >>> 32) & 0xFFFFFFFFL;
      }

      if (last_high != 0) {
        grow(1);
        representation[0] = (int)last_high;
      }

      return this;
    } else {
      //Do some other stuff
      return this;
    }
  }

  public void grow(int by) {
    int[] resized_array = new int[representation.length+by];
    System.arraycopy(representation, 0, resized_array, by, representation.length);
    representation = resized_array;
  }
}
