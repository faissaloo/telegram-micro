package mtproto;
import support.ByteArrayPlus;

public class Serialize {
  public static byte[] serialize_bytes(byte[] data) {
    ByteArrayPlus to_return = new ByteArrayPlus();
    if (data.length <= 253) {
      to_return.append_byte((byte)data.length);
    } else {
      to_return.append_byte((byte)254);
      to_return.append_int24(data.length);
    }
    
    return to_return
      .append_raw_bytes(data)
      .pad_to_alignment(4)
      .toByteArray();
  }
}
