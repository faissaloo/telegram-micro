package test;

import crypto.AES256IGE;

public class AES256IGEContext {
  public static class SubBytesTest extends FocusedTest {
    public String label() {
      return "It can perform the SubBytes step";
    }
    public void test() throws TestFailureException {
      AES256IGE subject = new AES256IGE();
      subject.state = new byte[] {
        (byte)0x10, (byte)0x34, (byte)0x69, (byte)0x4A,
        (byte)0xFF, (byte)0xEE, (byte)0xDD, (byte)0xCC,
        (byte)0x10, (byte)0x34, (byte)0x69, (byte)0x4A,
        (byte)0xFF, (byte)0xEE, (byte)0xDD, (byte)0xCC,
      };
      subject.substitute_bytes();
      expect(subject.state, new byte[] {
        (byte)0xCA, (byte)0x18, (byte)0xF9, (byte)0xD6,
        (byte)0x16, (byte)0x28, (byte)0xC1, (byte)0x4B,
        (byte)0xCA, (byte)0x18, (byte)0xF9, (byte)0xD6,
        (byte)0x16, (byte)0x28, (byte)0xC1, (byte)0x4B
      });
    }
  }

  public static class MixColumnsTest extends FocusedTest {
    public String label() {
      return "It can perform the MixColumns step";
    }
    public void test() throws TestFailureException {
      AES256IGE subject = new AES256IGE();
      subject.state = new byte[] {
        (byte)0x10, (byte)0x34, (byte)0x69, (byte)0x4A,
        (byte)0xFF, (byte)0xEE, (byte)0xDD, (byte)0xCC,
        (byte)0x10, (byte)0x34, (byte)0x69, (byte)0x4A,
        (byte)0xFF, (byte)0xEE, (byte)0xDD, (byte)0xCC,
      };
      subject.mix_columns();
      expect(subject.state, new byte[] {
        (byte)0x5F, (byte)0x89, (byte)0x28, (byte)0xF9,
        (byte)0xDD, (byte)0x88, (byte)0xFF, (byte)0xAA,
        (byte)0x5F, (byte)0x89, (byte)0x28, (byte)0xF9,
        (byte)0xDD, (byte)0x88, (byte)0xFF, (byte)0xAA
      });
    }
  }

  public static class ShiftRowsTest extends FocusedTest {
    public String label() {
      return "It can perform the ShiftRows step";
    }
    public void test() throws TestFailureException {
      AES256IGE subject = new AES256IGE();
      subject.state = new byte[] {
        (byte)0x10, (byte)0x34, (byte)0x69, (byte)0x4A,
        (byte)0xFF, (byte)0xEE, (byte)0xDD, (byte)0xCC,
        (byte)0x10, (byte)0x34, (byte)0x69, (byte)0x4A,
        (byte)0xFF, (byte)0xEE, (byte)0xDD, (byte)0xCC,
      };
      subject.shift_rows();
      expect(subject.state, new byte[] {
        (byte)0x10, (byte)0xEE, (byte)0x69, (byte)0xCC,
        (byte)0xFF, (byte)0x34, (byte)0xDD, (byte)0x4A,
        (byte)0x10, (byte)0xEE, (byte)0x69, (byte)0xCC,
        (byte)0xFF, (byte)0x34, (byte)0xDD, (byte)0x4A,
      });
    }
  }
}
