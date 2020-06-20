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
}
