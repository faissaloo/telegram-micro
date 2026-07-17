package mtproto;

import support.Utf8String;
import support.ByteArrayPlus;
import support.Decode;

public class Deserialize {
  //https://core.telegram.org/type/bytes
  //https://core.telegram.org/type/string
  public static byte[] bytes_deserialize(byte[] data, int offset) {
    return bytes_deserialize(data, offset, ProtocolLimits.MAX_TL_BYTES);
  }

  public static byte[] bytes_deserialize(byte[] data, int offset, int maximum) {
    int headerLength = bytes_header_length(data, offset);
    int bytesLength = bytes_payload_length(data, offset, headerLength);
    ProtocolLimits.requireLength(bytesLength, maximum, "TL bytes");
    require(data, offset, padded_length(headerLength, bytesLength));
    byte[] bytes = new byte[bytesLength];
    System.arraycopy(data, offset + headerLength, bytes, 0, bytesLength);
    return bytes;
  }

  public static int bytes_length_deserialize(byte[] data, int offset) {
    int headerLength = bytes_header_length(data, offset);
    int bytesLength = bytes_payload_length(data, offset, headerLength);
    ProtocolLimits.requireLength(bytesLength, ProtocolLimits.MAX_TL_BYTES, "TL bytes");
    int paddedLength = padded_length(headerLength, bytesLength);
    require(data, offset, paddedLength);
    return paddedLength;
  }

  //https://core.telegram.org/type/string
  public static Utf8String utf8string_deserialize(byte[] data, int offset) {
    return utf8string_deserialize(
      data, offset, ProtocolLimits.MAX_DISPLAY_STRING_BYTES
    );
  }

  public static Utf8String utf8string_deserialize(
    byte[] data,
    int offset,
    int maximum
  ) {
    return new Utf8String(bytes_deserialize(data, offset, maximum));
  }

  public static int utf8string_length_deserialize(byte[] data, int offset) {
    return bytes_length_deserialize(data, offset);
  }

  //https://core.telegram.org/mtproto/serialize#built-in-composite-types-vectors-and-associative-arrays
  public static long[] vector_long_deserialize(byte[] data, int offset) throws TypeMismatchException {
    require(data, offset, 8);
    if (Decode.Little.int_decode(data, offset) == CombinatorIds.vector) {
      offset += 4;
      int length = Decode.Little.int_decode(data, offset);
      offset += 4;
      ProtocolLimits.requireLength(length, ProtocolLimits.MAX_VECTOR_ITEMS, "Long vector");
      if (length > (data.length - offset) / 8) {
        throw new IllegalArgumentException("Long vector exceeds its payload.");
      }
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

  private static int bytes_header_length(byte[] data, int offset) {
    require(data, offset, 1);
    return (data[offset] & 0xff) < 254 ? 1 : 4;
  }

  private static int bytes_payload_length(byte[] data, int offset, int headerLength) {
    if (headerLength == 1) {
      return data[offset] & 0xff;
    }
    require(data, offset, 4);
    return Decode.Little.int24_decode(data, offset + 1);
  }

  private static int padded_length(int headerLength, int payloadLength) {
    int total = headerLength + payloadLength;
    if (total < 0 || total > ProtocolLimits.MAX_TL_BYTES + 4) {
      throw new IllegalArgumentException("TL bytes length overflow.");
    }
    return (total + 3) & ~3;
  }

  private static void require(byte[] data, int offset, int amount) {
    if (data == null || offset < 0 || amount < 0 || offset > data.length - amount) {
      throw new IllegalArgumentException("TL payload underflow.");
    }
  }
}
