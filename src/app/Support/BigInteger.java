package support;

import java.lang.ArithmeticException;

public class BigInteger {
  public int sign;
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
      for (int i = 0; i < representation.length-1; i++) {
        new_string = IntegerPlus.padded_hex(representation[i])+new_string;
      }
      new_string = Integer.toHexString(representation[representation.length-1]) + new_string;
    }

    if (sign == -1) {
      return "-"+new_string;
    } else {
      return new_string;
    }
  }

  public boolean equal(BigInteger other) {
    if (sign == other.sign) {
      return ArrayPlus.items_equal(representation, other.representation);
    } else {
      return false;
    }
  }

  public BigInteger mutating_subtract(BigInteger other) {
    //this. must be larger than other.
    int bigIndex = representation.length;
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

      for (int i = 0; i < Math.max(representation.length, other.representation.length); i++) {
        if (representation.length-1 > i) {
          grow(1);
          first = 0;
        } else {
          first = ((long) representation[i]) & 0xFFFFFFFFL;
        }

        if (other.representation.length-1 > i) {
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
        representation[representation.length-1] = (int)last_high;
      }

      return this;
    } else {
      //Do some other stuff
      return this;
    }
  }

  public void grow(int by) {
    int[] resized_array = new int[representation.length+by];
    System.arraycopy(representation, 0, resized_array, 0, representation.length);
    representation = resized_array;
  }
}
