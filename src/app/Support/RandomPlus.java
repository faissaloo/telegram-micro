package support;

import java.util.Random;

public class RandomPlus extends Random {
  public BigInteger nextBigInteger() {
    return new BigInteger(nextLong(), nextLong());
  }
}
