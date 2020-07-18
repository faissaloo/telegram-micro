package support;

import java.io.ByteArrayOutputStream;

public class ByteArrayPlus extends ByteArrayOutputStream {
  public ByteArrayPlus append_byte(byte to_write) {
    write(to_write);
    return this;
  }

  public ByteArrayPlus append_long(long to_write) {
    append_raw_bytes(Encode.long_encode(to_write));
    return this;
  }

  public ByteArrayPlus append_int(int to_write) {
    append_raw_bytes(Encode.int_encode(to_write));
    return this;
  }

  public ByteArrayPlus append_int24(int to_write) {
    append_raw_bytes(Encode.int24_encode(to_write));
    return this;
  }

  public ByteArrayPlus append_Integer128(Integer128 to_write) {
    append_raw_bytes(Encode.Integer128_encode(to_write));
    return this;
  }

  public ByteArrayPlus append_Integer256(Integer256 to_write) {
    append_raw_bytes(Encode.Integer256_encode(to_write));
    return this;
  }

  public ByteArrayPlus append_raw_bytes(byte[] to_write) {
    write(to_write, 0, to_write.length);
    return this;
  }

  public ByteArrayPlus append_raw_bytes_up_to(byte[] to_write, int length) {
    write(to_write, 0, length);
    return this;
  }
  
  public ByteArrayPlus append_raw_bytes_from_up_to(byte[] to_write, int from, int length) {
    write(to_write, from, length);
    return this;
  }
}
