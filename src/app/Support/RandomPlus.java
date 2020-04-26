package support;

import java.util.Random;

public class RandomPlus extends Random {
  public Integer128 nextInteger128() {
    return new Integer128(nextLong(), nextLong());
  }
}
