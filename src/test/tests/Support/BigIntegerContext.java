package test;

import support.BigInteger;

public class BigIntegerContext {
  public static class HexTest extends Test {
    public String label() {
      return "It represents BigIntegers correctly in hexadecimal";
    }
    public void test() throws TestFailureException {
      BigInteger subject = new BigInteger(new int[] {0x01, 0x02, 0x03, 0x04});
      expect(subject.hex(), "00000004000000030000000200000001");
      subject = new BigInteger(new int[] {0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF});
      expect(subject.hex(), "ffffffffffffffffffffffffffffffff");
    }
  }

  public static class EqualTest extends Test {
    public String label() {
      return "It can test two BigIntegers for equality";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0x6c965892, 0x7a17d760, 0x30f53561, 0xe5351a86});
      BigInteger b = new BigInteger(new int[] {0x6c965892, 0x7a17d760, 0x30f53561, 0xe5351a86});
      BigInteger c = new BigInteger(new int[] {0x1fb64b73, 0x839774b9, 0x3a585498, 0x7487bc04});

      expect(a.equals(b), true);
      expect(a.equals(c), false);
    }
  }

  public static class NotTest extends Test {
    public String label() {
      return "It can correctly not a BigInteger";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0xfdc71e60, 0x87960ac4, 0xac3a47f5, 0x2d518e0c});
      BigInteger b = new BigInteger(new int[] {0x0238e19f, 0x7869f53b, 0x53c5b80a, 0xd2ae71f3});

      expect(a.not().equals(b), true);
    }
  }

  public static class AddTest extends Test {
    public String label() {
      return "It can correctly add two BigIntegers";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0xfdc71e60, 0x87960ac4, 0xac3a47f5, 0x2d518e0c});
      BigInteger b = new BigInteger(new int[] {1, 0, 0, 0});
      BigInteger c = new BigInteger(new int[] {0xfdc71e61, 0x87960ac4, 0xac3a47f5, 0x2d518e0c});

      expect(a.add(b).equals(c), true);
    }
  }

  public static class NegateTest extends Test {
    public String label() {
      return "It can correctly negate a BigInteger";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0x5f6e7db2, 0x1db5d1ed, 0x60e87101, 0xc6931d4});
      BigInteger b = new BigInteger(new int[] {0xa091824e, 0xe24a2e12, 0x9f178efe, 0xf396ce2b});

      expect(a.negate().equals(b), true);
    }
  }

  public static class SubtractTest extends Test {
    public String label() {
      return "It can correctly subtract a BigInteger";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0x5f6e7db2, 0x1db5d1ed, 0x60e87101, 0xc6931d4});
      BigInteger b = new BigInteger(new int[] {0, 0, 0, 0});

      BigInteger c = new BigInteger(new int[] {0xf1a1c4d1, 0xd70f4971, 0x3746b3d6, 0xc82a7421});
      BigInteger d = new BigInteger(new int[] {0x75a63f8d, 0x0369138a, 0x4aac4ea1, 0x613ec389});
      BigInteger e = new BigInteger(new int[] {0x7bfb8544, 0xd3a635e7, 0xec9a6535, 0x66ebb097});

      expect(a.subtract(a).equals(b), true);
      expect(c.subtract(d).equals(e), true);
    }
  }

  public static class UnsignedLongMultiplyTest extends Test {
    public String label() {
      return "It can correctly perform an unsigned multiplication of two longs";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0x06c572ec, 0xf0eb08b1, 0x04540433, 0xa930c6});
      expect(BigInteger.unsigned_long_multiply(0x223ee25f9573322L,0x4f0c478bf49f9716L).equals(a), true);
    }
  }

  public static class UnsignedDivideModuloTest extends Test {
    public String label() {
      return "It can perform an unsigned divide and modulo of a BigInteger";
    }
    public void test() throws TestFailureException {
      BigInteger e = new BigInteger(new int[] { 0xed48fc90, 0xf98ad4a4, 0x4fddfd81, 0x55286f18});
      BigInteger f = new BigInteger(0x211cafa9555101f5L);
      BigInteger g = new BigInteger(0xd5428d6f285c04cL);
      expect(e.unsigned_modulo(f).equals(g), true);

      BigInteger a = new BigInteger(new int[] {0x3b025880, 0x3eb2ac58, 0xc91477f8, 0x4bbdddc1});
      BigInteger b = new BigInteger(new int[] {0x8c345e10, 0x57916d26, 0, 0});
      BigInteger c = new BigInteger(new int[] {0xc2c7c688, 0xdd6d1c17, 0, 0});
      BigInteger d = new BigInteger(new int[] {0, 0, 0, 0});
      BigInteger.QuotientRemainder quotient_remainder = a.unsigned_divide_modulo(b);

      expect(quotient_remainder.quotient().equals(c), true);
      expect(quotient_remainder.remainder().equals(d), true);
      expect(a.unsigned_divide(b).equals(c), true);
      expect(a.unsigned_modulo(b).equals(d), true);
    }
  }

  public static class UnsignedRightShiftTest extends Test {
    public String label() {
      return "It can perform an unsigned right shift on a BigInteger";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0x3b025880, 0x3eb2ac58, 0xc91477f8, 0x4bbdddc1});
      BigInteger b = new BigInteger(new int[] {0xac583b02, 0x77f83eb2, 0xddc1c914, 0x4bbd});

      BigInteger c = new BigInteger(new int[] {0x77f83eb2, 0xddc1c914, 0x4bbd, 0});

      expect(a.unsigned_right_shift(0x10).equals(b), true);
      expect(a.unsigned_right_shift(0x30).equals(c), true);
    }
  }

  public static class UnsignedLeftShiftTest extends Test {
    public String label() {
      return "It can perform an unsigned left shift on a BigInteger";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0xfb3cfbbb, 0x62d96381, 0x1374706a, 0x792dc404});
      BigInteger b = new BigInteger(new int[] {0x00000000, 0x00000000, 0x00000000, 0xfb3cfbbb});

      expect(a.unsigned_left_shift(0x60).equals(b), true);
      expect(a.mutating_unsigned_left_shift(0x60).equals(b), true);

      BigInteger d = new BigInteger(0x55286f184fddfcL);
      BigInteger e = new BigInteger(0xaa50de309fbbf8L);
      expect(d.mutating_unsigned_left_shift().equals(e), true);
    }
  }

  public static class UnsignedGreaterThanTest extends Test {
    public String label() {
      return "It can check if a BigInteger is greater than another unsigned";
    }
    public void test() throws TestFailureException {

      BigInteger a = new BigInteger(new int[] {0xb3a7b3b0, 0x7597b168, 0xf6d11e33, 0xae5d22cf});
      BigInteger b = new BigInteger(new int[] {0x8ef2b7ac, 0x7896e5ab, 0x95d49fdd, 0x2ce812b8});

      expect(a.unsigned_greater_than(b), true);
      expect(b.unsigned_greater_than(a), false);
    }
  }

  public static class UnsignedLessThanTest extends Test {
    public String label() {
      return "It can check if a BigInteger is less than another unsigned";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0xb3a7b3b0, 0x7597b168, 0xf6d11e33, 0xae5d22cf});
      BigInteger b = new BigInteger(new int[] {0x8ef2b7ac, 0x7896e5ab, 0x95d49fdd, 0x2ce812b8});

      expect(b.unsigned_less_than(a), true);
      expect(a.unsigned_less_than(b), false);
    }
  }

  public static class UnsignedGreaterThanEqualTest extends Test {
    public String label() {
      return "It can check if a BigInteger is greater than or equal to another unsigned";
    }
    public void test() throws TestFailureException {

      BigInteger a = new BigInteger(new int[] {0xb3a7b3b0, 0x7597b168, 0xf6d11e33, 0xae5d22cf});
      BigInteger b = new BigInteger(new int[] {0x8ef2b7ac, 0x7896e5ab, 0x95d49fdd, 0x2ce812b8});

      expect(a.unsigned_greater_than_equal(b), true);
      expect(a.unsigned_greater_than_equal(a), true);
      expect(b.unsigned_greater_than_equal(a), false);
    }
  }

  public static class UnsignedLessThanEqualTest extends Test {
    public String label() {
      return "It can check if a BigInteger is less than or equal to another unsigned";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0xb3a7b3b0, 0x7597b168, 0xf6d11e33, 0xae5d22cf});
      BigInteger b = new BigInteger(new int[] {0x8ef2b7ac, 0x7896e5ab, 0x95d49fdd, 0x2ce812b8});

      expect(b.unsigned_less_than_equal(a), true);
      expect(b.unsigned_less_than_equal(b), true);
      expect(a.unsigned_less_than_equal(b), false);
    }
  }

  public static class AndTest extends Test {
    public String label() {
      return "It can check if two BigIntegers can be anded together";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0x21a68039, 0xd3567c5f, 0x0a1a8fc8, 0x7fa482a5});
      BigInteger b = new BigInteger(new int[] {0x07732be5, 0x2df6796c, 0x8ac1ee13, 0xba44a4a2});
      BigInteger c = new BigInteger(new int[] {0x01220021, 0x0156784c, 0x0a008e00, 0x3a0480a0});

      expect(a.and(b).equals(c), true);
    }
  }

  public static class OrTest extends Test {
    public String label() {
      return "It can check if two BigIntegers can be ored together";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0x21a68039, 0xd3567c5f, 0x0a1a8fc8, 0x7fa482a5});
      BigInteger b = new BigInteger(new int[] {0x07732be5, 0x2df6796c, 0x8ac1ee13, 0xba44a4a2});
      BigInteger c = new BigInteger(new int[] {0x27f7abfd, 0xfff67d7f, 0x8adbefdb, 0xffe4a6a7});

      expect(a.or(b).equals(c), true);
      expect(a.mutating_or(b).equals(c), true);
    }
  }

  public static class ToLongTest extends Test {
    public String label() {
      return "It can convert a BigInteger into a long";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0x27f7abfd, 0xd3567c5f, 0, 0});

      expect(a.to_long(), 0xd3567c5f27f7abfdL);
    }
  }
}
