package support;
import java.lang.StringBuffer;

public class ArrayPlus {
  public static String hex_string(byte[] array) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < array.length; i++) {
      int top_part = ((array[i]&0xF0)>>>4)&0xF;
      if (top_part > 0x9) {
        buffer.append((char) (0x41+(top_part-0x9)));
      } else {
        buffer.append((char) (0x30+top_part));
      }
      int bottom_part = (array[i]&0xF);
      if (bottom_part > 0x9) {
        buffer.append((char) (0x41+(bottom_part-0x9)));
      } else {
        buffer.append((char) (0x30+bottom_part));
      }
      buffer.append(' ');
    }
    return buffer.toString();
  }
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
  
  public static byte[] reverse(byte[] to_reverse) {
    byte[] result = new byte[to_reverse.length];
    for (int i = 0; i < result.length; i++) {
      result[result.length-i-1] = to_reverse[i];
    }
    return result;
  }
}
