package support;

public class Debug {
  public static char[] hex_lookup_table = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

  public static String bytes_to_hex(byte[] array) {
    //We can make this way more efficient if we really want, rn this is gonna blow up the GC
    String new_string = "";
    for (int i = 0; i < array.length; i++) {
      new_string += hex_lookup_table[(array[i]&0xF0) >>> 4] + "" + hex_lookup_table[array[i]&0xF];
    }
    return new_string;
  }
}
