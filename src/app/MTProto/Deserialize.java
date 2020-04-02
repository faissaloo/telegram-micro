package mtproto;

import support.Utf8String;
import support.ByteArrayPlus;
import support.Decode;

public class Deserialize {
  //https://core.telegram.org/type/bytes
  public static byte[] bytes_deserialize(byte[] data, int offset) {
    int bytes_length = data[offset];
    int bytes_offset = 1;
    if (bytes_length >= 254) {
      bytes_length = Decode.Little.int_decode(data, offset);
      bytes_offset = 4;
    }
    ByteArrayPlus bytes = new ByteArrayPlus();
    for (int i = 0; i < bytes_length;i++) {
      bytes.append_byte(data[offset+bytes_offset+i]);
    }
    return bytes.toByteArray();
  }

  public static int bytes_length_deserialize(byte[] data, int offset) {
    int bytes_length = data[offset];
    int bytes_offset = 1;
    if (bytes_length >= 254) {
      bytes_offset = 4;
    }
    int bytes_padding = (4-(bytes_length+bytes_offset)%4)%4;

    return bytes_offset+bytes_length+bytes_padding;
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
    if (Decode.Little.int_decode(data, offset) == 0x1cb5c415) {
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
