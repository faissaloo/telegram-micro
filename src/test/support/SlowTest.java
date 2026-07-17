package test;

/**
 * A deterministic test which is intentionally omitted from the quick lane.
 * Slow tests must run in the full lane; they are not unsupported skips.
 */
public abstract class SlowTest extends Test {
  public boolean slow() {
    return true;
  }
}
