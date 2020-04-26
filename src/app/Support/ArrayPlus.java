package support;

public class ArrayPlus {
  public static boolean items_equal(int[] array_a, int[] array_b) {
    if (array_a.length != array_b.length) {
      return false;
    } else {
      boolean still_equal = true;

      for (int i = 0; i < array_a.length; i++) {
        still_equal &= array_a[i] == array_b[i];
      }

      return still_equal;
    }
  }
}
