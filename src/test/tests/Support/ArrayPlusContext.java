package test;

import support.ArrayPlus;

public class ArrayPlusContext {
  public static class RemoveLeadingZeroesTest extends Test {
    public String label() {
      return "It removes leading zeroes";
    }
    public void test() throws TestFailureException {
      byte[] result = ArrayPlus.remove_leading_zeroes(new byte[] {(byte)0, (byte)0, (byte)0, (byte)6, (byte)7, (byte)9});
      expect(result, new byte[] {(byte)6, (byte)7, (byte)9});
    }
  }
}
