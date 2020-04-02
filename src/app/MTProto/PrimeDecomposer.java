package mtproto;

import java.lang.Math;

import support.BigInteger;

public class PrimeDecomposer {
  public static class Coprimes {
    long lesser_prime;
    long greater_prime;

    public Coprimes(long first_prime, long second_prime) {
      if (first_prime < second_prime) {
        this.lesser_prime = first_prime;
        this.greater_prime = second_prime;
      } else {
        this.lesser_prime = second_prime;
        this.greater_prime = first_prime;
      }
    }

    public long lesser_prime() {
      return lesser_prime;
    }

    public long greater_prime() {
      return greater_prime;
    }
  }

  public static long limited_polynomial(long current_value, long limit) {
    BigInteger big_limit = new BigInteger(limit);
    BigInteger current_value_squared = BigInteger.unsigned_long_multiply(current_value, current_value);
    BigInteger result = current_value_squared.unsigned_modulo(big_limit).add(BigInteger.one()).unsigned_modulo(big_limit);
    return result.to_long();
  }

  public static long euclidian_greatest_common_denominator(long a, long b) {
    a = Math.abs(a);
    b = Math.abs(b);
    while (a != 0) {
      long tmp = a;
      a = b % a;
      b = tmp;
    }
    return b;
  }

  public static Coprimes decompose(long to_factorise) {
    if (to_factorise % 2 == 0) {
      return new Coprimes(2L, to_factorise/2);
    } else {
      long slow_pointer = 1L;
      long fast_pointer = 1L;
      long nontrivial_denominator = 1L;
      long search_range = 1L;
      long initial_guess = 1L;
      long already_searched = 0L;
      long iteration = 0;
      while (nontrivial_denominator == 1L) {
        slow_pointer = fast_pointer;
        for (long i = 0L; i < search_range; i++) {
          fast_pointer = limited_polynomial(fast_pointer, to_factorise);
        }
        already_searched = 0L;
        while (already_searched < search_range && nontrivial_denominator == 1L) {
          for (long i = 0L; i < Math.min(1L, search_range - already_searched); i++) {
            fast_pointer = limited_polynomial(fast_pointer, to_factorise);
            //This one might be excessive, but better safe than sorry until we get it working
            initial_guess = BigInteger.unsigned_long_multiply(initial_guess, Math.abs(slow_pointer-fast_pointer)).unsigned_modulo(new BigInteger(to_factorise)).to_long();
          }
          nontrivial_denominator = euclidian_greatest_common_denominator(initial_guess, to_factorise);
          already_searched ++;
        }
        search_range *=2;
      }

      if (nontrivial_denominator == to_factorise) {
        while (true) {
          fast_pointer = limited_polynomial(fast_pointer, to_factorise);
          nontrivial_denominator = euclidian_greatest_common_denominator(Math.abs(slow_pointer - fast_pointer), to_factorise);
          if (nontrivial_denominator > 1L) {
            break;
          }
        }
      }
      return new Coprimes(nontrivial_denominator, to_factorise/nontrivial_denominator);
    }
  }
}
