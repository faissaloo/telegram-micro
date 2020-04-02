package test;

public class TestFailure {
  Exception exception;
  Test klass;

  TestFailure(Test klass, Exception exception) {
    this.klass = klass;
    this.exception = exception;
  }

  public void print() {
    System.out.println(klass.label()+" failed with:");
    exception.printStackTrace();
  }
}
