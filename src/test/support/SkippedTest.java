package test;

public abstract class SkippedTest extends Test {
  public boolean skipped() {
    return true;
  }
}
