package support;

import java.io.ByteArrayOutputStream;

public class ByteArrayPlus extends ByteArrayOutputStream {
  public void append_byte(byte to_write) {
    write(to_write);
  }

  public void append_long(long to_write) {
    append_raw_bytes(Encode.long_encode(to_write));
  }

  public void append_int(int to_write) {
    append_raw_bytes(Encode.int_encode(to_write));
  }

  public void append_int24(int to_write) {
    append_raw_bytes(Encode.int24_encode(to_write));
  }

  public void append_Integer128(Integer128 to_write) {
    append_raw_bytes(Encode.Integer128_encode(to_write));
  }

  public void append_Integer256(Integer256 to_write) {
    append_raw_bytes(Encode.Integer256_encode(to_write));
  }

  public void append_raw_bytes(byte[] to_write) {
    write(to_write, 0, to_write.length);
  }

  public void append_raw_bytes_up_to(byte[] to_write, int length) {
    write(to_write, 0, length);
  }
}
