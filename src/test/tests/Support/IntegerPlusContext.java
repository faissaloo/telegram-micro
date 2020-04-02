package test;

import support.IntegerPlus;

public class IntegerPlusContext {
  public static class PowerTest extends Test {
    public String label() {
      return "It can calculate the power of an integer";
    }
    public void test() throws TestFailureException {
      expect(IntegerPlus.power(2,2), 4);
      expect(IntegerPlus.power(3,2), 9);
      expect(IntegerPlus.power(10,5), 100000);
      expect(IntegerPlus.power(69,3), 328509);
      expect(IntegerPlus.power(6,0), 1);
      expect(IntegerPlus.power(6,1), 6);
    }
  }
}
