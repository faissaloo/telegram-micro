package test;

public abstract class Test {
  public abstract void test() throws TestFailureException;
  public String label() {
    return getClass().getName();
  }

  public void run_tests() throws TestFailureException {
    test();
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
