package test;

public abstract class Test {
  public boolean focused() {
    return false;
  }

  public boolean skipped() {
    return false;
  }

  public abstract void test() throws TestFailureException;
  public String label() {
    return getClass().getName();
  }

  public void run_tests() throws TestFailureException {
    test();
  }

  public void expect(byte[] got, byte[] expected) throws TestFailureException {
    if (got.length == expected.length) {
      for (int i = 0; i < expected.length; i++) {
        if (got[i] != expected[i]) {
          throw new TestFailureException(
            "Expected byte at index "+Integer.toString(i)+
            " to equal 0x"+Integer.toHexString(((int)expected[i])&0xFF)+
            " but got 0x"+Integer.toHexString(((int)got[i])&0xFF)
          );
        }
      }
    } else {
      throw new TestFailureException("Expected an array of length "+ Integer.toString(expected.length) + " but got " + Integer.toString(got.length));
    }
  }

  public void expect(boolean got, boolean expected) throws TestFailureException {
    if (got != expected) {
      if (expected) {
        throw new TestFailureException("Expected boolean true, got false");
      } else {
        throw new TestFailureException("Expected boolean false, got true");
      }
    }
  }

  public void expect(String got, String expected) {
    if (!got.equals(expected)) {
      throw new TestFailureException("Expected String \""+expected+"\", got \""+got+"\"");
    }
  }

  public void expect(int got, int expected) {
    if (got != expected) {
      throw new TestFailureException("Expected int \""+Integer.toString(expected)+"\", got \""+Integer.toString(got)+"\"");
    }
  }

  public void expect(long got, long expected) {
    if (got != expected) {
      throw new TestFailureException("Expected long \""+Long.toString(expected)+"\", got \""+Long.toString(got)+"\"");
    }
  }
}
