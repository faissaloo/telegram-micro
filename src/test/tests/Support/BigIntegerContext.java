package test;

import support.BigInteger;

public class BigIntegerContext {
  public static class HexTest extends Test {
    public String label() {
      return "It represents BigIntegers correctly in hexadecimal";
    }
    public void test() throws TestFailureException {
      BigInteger subject = new BigInteger(new int[] {0x01, 0x02, 0x03, 0x04, 0x06});
      expect(subject.hex(), "600000004000000030000000200000001");
      subject = new BigInteger(new int[] {0x01, 0x02, 0x03, 0x04, 0x06}, -1);
      expect(subject.hex(), "-600000004000000030000000200000001");
      subject = new BigInteger(new int[] {});
      expect(subject.hex(), "");
    }
  }

  public static class EqualTest extends Test {
    public String label() {
      return "It can check if BigInteger is equal";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0x01, 0x02, 0x03, 0x04, 0x06});
      BigInteger b = new BigInteger(new int[] {0x01, 0x02, 0x03, 0x04, 0x06});
      BigInteger c = new BigInteger(new int[] {0x69, 0x69, 0x69, 0x69, 0x69});
      BigInteger d = new BigInteger(new int[] {0x69, 0x69, 0x69, 0x69, 0x69}, -1);
      BigInteger e = new BigInteger(new int[] {0x69, 0x69, 0x69, 0x69, 0x69}, -1);
      expect(a.equal(b), true);
      expect(b.equal(c), false);
      expect(c.equal(d), false);
      expect(d.equal(e), true);
    }
  }

  public static class MutatingAddTest extends Test {
    public String label() {
      return "It can add two BigIntegers";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0xffffffff});
      BigInteger b = new BigInteger(new int[] {0xfffffffe, 0x1});
      a.mutating_add(a);
      expect(a.equal(b), true);
    }
  }

  public static class MutatingNotTest extends Test {
    public String label() {
      return "It can add not a BigInteger";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0xffffffff, 0xffffffff});
      BigInteger b = new BigInteger(new int[] {0, 0});
      a.mutating_not();
      expect(a.equal(b), true);
    }
  }
}
