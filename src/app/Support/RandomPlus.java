package support;

import java.util.Random;

public class RandomPlus extends Random {
  public byte nextByte() {
    return (byte)next(8);
  }
  
  public byte[] nextBytes(int length) {
    byte[] new_array = new byte[length];
    for (int i = 0; i < new_array.length; i++) {
      new_array[i] = nextByte();
    }
    return new_array;
  }
  
  public Integer128 nextInteger128() {
    return new Integer128(nextLong(), nextLong());
  }

  public Integer256 nextInteger256() {
    return new Integer256(nextInt(), nextInt(), nextInt(), nextInt(), nextInt(), nextInt(), nextInt(), nextInt());
  }
}
