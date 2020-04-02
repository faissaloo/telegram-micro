package test;

import java.lang.RuntimeException;
import java.lang.Throwable;

public class TestFailureException extends RuntimeException {
  public TestFailureException(String message) {
    super(message);
  }
}
