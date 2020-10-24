package support;

public class ArrayPlus {
  public static byte[] remove_leading_zeroes(byte[] array) {
    if (array.length == 0) {
      return new byte[] {};
    }
    
    int first_nonzero_index = 0;
    while (array[first_nonzero_index] == 0) {
      first_nonzero_index++;
    }
    byte[] new_array = new byte[array.length-first_nonzero_index];
    System.arraycopy(array, first_nonzero_index, new_array, 0, new_array.length);
    
    return new_array;
  }
  
  public static byte[] xor(byte[] xorand, byte[] xorier) {
    byte[] result = new byte[Math.max(xorand.length, xorier.length)];
    for (int i = 0; i < xorand.length; i++) {
      result[i] = (byte)(xorand[i]^xorier[i]);
    }
    return result;
  }
  
  public static byte[] subarray(byte[] array, int from, int length) {
    byte[] new_array = new byte[length];
    System.arraycopy(array, from, new_array, 0, length);
    return new_array;
  }
  
  public static byte[] subarray(byte[] array, int length) {
    return subarray(array, 0, length);
  }
}
