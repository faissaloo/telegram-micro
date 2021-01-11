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
  
  public ByteArrayPlus pad_random_to_length(int min_length, int up_to, RandomPlus random_number_generator) {
    int max_length = up_to-size();
    int length = min_length+random_number_generator.nextInt(max_length-min_length);
    append_raw_bytes(random_number_generator.nextBytes(length));
    return this;
  }
  
  public ByteArrayPlus pad_to_length(int up_to, RandomPlus random_number_generator) {
    int to_pad = up_to-size();
    append_raw_bytes(random_number_generator.nextBytes(to_pad));
    return this;
  }
  
  public ByteArrayPlus pad_to_alignment(int alignment) {
    int to_pad = (alignment-(size()%alignment))%alignment;
    for (int i = 0; i < to_pad; i++) {
      append_byte((byte)0);
    }
    return this;
  }
  
  //bro my brain just like vomitted this up so if you don't get it you're on your own
  public static int align_range_length(int size, int alignment, int minimum, int maximum, RandomPlus random_number_generator) {
    int aligned_by = ((alignment-(size%alignment))%alignment);
    int needed_to_meet_minimum = minimum-aligned_by;
    int aligned_needed_to_meet_minimum = needed_to_meet_minimum + ((alignment-(needed_to_meet_minimum%alignment))%alignment);
    int aligned_bytes_needed = aligned_by + aligned_needed_to_meet_minimum;
    
    int times_alignment_fits_into_remaining_bytes = (maximum-aligned_bytes_needed)/alignment;
    int bytes_to_add = alignment*random_number_generator.nextInt(times_alignment_fits_into_remaining_bytes);
    return aligned_bytes_needed+bytes_to_add;
  }
  
  public ByteArrayPlus pad_random_align_range(int alignment, int minimum, int maximum, RandomPlus random_number_generator) {
    int to_pad = align_range_length(size(), alignment, minimum, maximum, random_number_generator);
    append_raw_bytes(random_number_generator.nextBytes(to_pad));
    return this;
  }
  
  public ByteArrayPlus pad_to_alignment(int alignment, RandomPlus random_number_generator) {
    int to_pad = (alignment-(size()%alignment))%alignment;
    append_raw_bytes(random_number_generator.nextBytes(to_pad));
    return this;
  }
}
