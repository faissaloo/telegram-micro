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
  
  public static class XorTest extends Test {
    public String label() {
      return "It can XOR two arrays";
    }
    public void test() throws TestFailureException {
      //someone needed to invent these terms
      byte[] xorand = new byte[] {(byte) 0x30, (byte) 0x14, (byte) 0x22};
      byte[] xorier = new byte[] {(byte) 0x93, (byte) 0xAA, (byte) 0x21};
      byte[] result = ArrayPlus.xor(xorand, xorier);
      expect(result, new byte[] {(byte)0xA3, (byte) 0xBE, (byte)0x03});
    }
  }
  
  public static class SubarrayTest extends Test {
    public String label() {
      return "It can extract a subarray from an array";
    }
    public void test() throws TestFailureException {
      byte[] array = new byte[] {(byte)0x11, (byte)0x22, (byte)0x33, (byte)0x44, (byte)0x55, (byte)0x66, (byte)0x77, (byte)0x88, (byte)0x99, (byte)0xAA};
      byte[] result = ArrayPlus.subarray(array, 2, 7);
      expect(result, new byte[] {(byte)0x33, (byte)0x44, (byte)0x55, (byte)0x66, (byte)0x77, (byte)0x88, (byte)0x99});
    }
  }
}
