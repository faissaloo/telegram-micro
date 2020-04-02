package test;

import support.LongPlus;

public class LongPlusContext {
  public static class PowerTest extends Test {
    public String label() {
      return "It can calculate the power of a long";
    }
    public void test() throws TestFailureException {
      expect(LongPlus.power(2,2), 4);
    }
  }
}
