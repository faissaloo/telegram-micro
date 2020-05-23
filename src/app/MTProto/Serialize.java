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
    to_return.append_bytes(data);
    for (int i = 0; i < (4-(data.length%4))%4; i++) {
      to_return.append_byte((byte)0x00);
    }
    return to_return.toByteArray();
  }
}
