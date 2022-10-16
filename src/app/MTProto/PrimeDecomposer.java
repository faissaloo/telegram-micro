package mtproto;

import java.lang.Math;

import support.Integer128;

import support.ServerConnection;

public class PrimeDecomposer {
  public static class Coprimes {
    public long lesser_prime;
    public long greater_prime;

    public Coprimes(long first_prime, long second_prime) {
      if (first_prime < second_prime) {
        this.lesser_prime = first_prime;
        this.greater_prime = second_prime;
      } else {
        this.lesser_prime = second_prime;
        this.greater_prime = first_prime;
      }
    }
  }

  public static long finite_ring(long current_value, Integer128 big_limit, long limit, Integer128 multiplication_scratch) {
    return multiplication_scratch
      .set_unsigned_long_multiply(current_value, current_value)
      .mutating_add()
      .fast_unsigned_modulo_long(limit);
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

  private static long getPrimeFromServer(long product) {
    ServerConnection serverConnection = new ServerConnection("http://localhost:5221");
    return serverConnection.getPrime(product);
  }

  public static Coprimes decompose(long to_factorise) {
    if (to_factorise % 2 == 0) {
      return new Coprimes(2L, to_factorise/2);
    } else {
    long prime = getPrimeFromServer(to_factorise);
        if (prime != -1)
            return new Coprimes(prime, to_factorise/prime);
        System.out.println("Failed to calculate prime factorization on server. Will do it on device (takes a while)");
        prime = calc_prime(to_factorise);
        return new Coprimes(prime, to_factorise/prime);
    }
  }
  
  private static long calc_prime(long to_factorise) {
    Integer128 big_to_factorise = new Integer128(to_factorise);
    Integer128 multiplication_scratch = new Integer128(); // preallocated object so we don't keep instantiating

    long slow_pointer = 1L;
    long fast_pointer = 1L;
    long nontrivial_denominator = 1L;
    long search_range = 1L;
    long initial_guess = 1L;
    long already_searched = 0L;
    while (nontrivial_denominator == 1L) {
      slow_pointer = fast_pointer;
      for (long i = 0L; i < search_range; i++) {
        fast_pointer = finite_ring(fast_pointer, big_to_factorise, to_factorise, multiplication_scratch);
      }
      already_searched = 0L;
      while (already_searched < search_range && nontrivial_denominator == 1L) {
        for (long i = 0L; i < Math.min(1L, search_range - already_searched); i++) {
          fast_pointer = finite_ring(fast_pointer, big_to_factorise, to_factorise, multiplication_scratch);
          initial_guess = multiplication_scratch
            .set_unsigned_long_multiply(initial_guess, Math.abs(slow_pointer-fast_pointer))
            .fast_unsigned_modulo_long(to_factorise);
        }
        nontrivial_denominator = euclidian_greatest_common_denominator(initial_guess, to_factorise);
        already_searched ++;
      }
      search_range <<=1;
    }

    if (nontrivial_denominator == to_factorise) {
      while (true) {
        fast_pointer = finite_ring(fast_pointer, big_to_factorise, to_factorise, multiplication_scratch);
        nontrivial_denominator = euclidian_greatest_common_denominator(Math.abs(slow_pointer - fast_pointer), to_factorise);
        if (nontrivial_denominator > 1L) {
          break;
        }
      }
    }
    return nontrivial_denominator;
  }
}

