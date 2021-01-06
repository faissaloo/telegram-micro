package mtproto;

import support.Utf8String;
import support.ByteArrayPlus;
import support.Decode;

public class Deserialize {
  //https://core.telegram.org/type/bytes
  //https://core.telegram.org/type/string
  public static byte[] bytes_deserialize(byte[] data, int offset) {
    int bytes_length = data[offset]&0xFF;
    offset += 1;
    if (bytes_length >= 254) {
      bytes_length = Decode.Little.int24_decode(data, offset);
      offset += 3;
    }
    ByteArrayPlus bytes = new ByteArrayPlus();
    for (int i = 0; i < bytes_length;i++) {
      bytes.append_byte(data[offset+i]);
    }
    return bytes.toByteArray();
  }

  public static int bytes_length_deserialize(byte[] data, int offset) {
    int bytes_length = data[offset]&0xFF;
    if (bytes_length >= 254) {
      bytes_length = Decode.Little.int24_decode(data, offset+1);
    }
    return ((bytes_length/4)+1)*4;
  }

  //https://core.telegram.org/type/string
  public static Utf8String utf8string_deserialize(byte[] data, int offset) {
    return new Utf8String(bytes_deserialize(data, offset));
  }

  public static int utf8string_length_deserialize(byte[] data, int offset) {
    return bytes_length_deserialize(data, offset);
  }

  //https://core.telegram.org/mtproto/serialize#built-in-composite-types-vectors-and-associative-arrays
  public static long[] vector_long_deserialize(byte[] data, int offset) throws TypeMismatchException {
    if (Decode.Little.int_decode(data, offset) == CombinatorIds.vector) {
      offset += 4;
      int length = Decode.Little.int_decode(data, offset);
      offset += 4;
      long[] array = new long[length];
      for (int i = 0; i < length; i++) {
        array[i] = Decode.Little.long_decode(data, offset);
        offset += 8;
      }
      return array;
    } else {
      throw new TypeMismatchException("Expected a %(Vector long)");
    }
  }
}
