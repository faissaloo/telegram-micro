package test;

import support.BigInteger;

public class BigIntegerContext {
  public static class HexTest extends Test {
    public String label() {
      return "It represents BigIntegers correctly in hexadecimal";
    }
    public void test() throws TestFailureException {
      BigInteger subject = new BigInteger(new int[] {0x01, 0x02, 0x03, 0x04, 0x06});
      expect(subject.hex(), "100000002000000030000000400000006");
      subject = new BigInteger(new int[] {0x01, 0x02, 0x03, 0x04, 0x06}, -1);
      expect(subject.hex(), "-100000002000000030000000400000006");
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
      BigInteger b = new BigInteger(new int[] {0x1, 0xfffffffe});
      a.mutating_add(a);
      expect(a.equal(b), true);
    }
  }

  public static class MutatingSubTest extends Test {
    public String label() {
      return "It can subtract two BigIntegers";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0x6a751ac4, 0x083a2bac});
      BigInteger b = new BigInteger(new int[] {0x294273b7, 0x09068806});
      BigInteger c = new BigInteger(new int[] {0x4132a70c, 0xff33a3a6});
      a.mutating_subtract(b);
      expect(a.equal(c), true);
    }
  }

  public static class MutatingNotTest extends Test {
    public String label() {
      return "It can not a BigInteger";
    }
    public void test() throws TestFailureException {
      BigInteger a = new BigInteger(new int[] {0xffffffff, 0xffffffff});
      BigInteger b = new BigInteger(new int[] {0});
      a.mutating_not();
      expect(a.equal(b), true);
    }
  }
}
