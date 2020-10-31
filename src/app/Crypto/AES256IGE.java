package crypto;

import support.ByteArrayPlus;
import java.lang.IllegalArgumentException;

//https://en.wikipedia.org/wiki/Advanced_Encryption_Standard#Description_of_the_ciphers
public class AES256IGE {
  public static final byte[] substitution_box = {
    (byte)0x63, (byte)0x7c, (byte)0x77, (byte)0x7b, (byte)0xf2, (byte)0x6b, (byte)0x6f, (byte)0xc5, (byte)0x30, (byte)0x01, (byte)0x67, (byte)0x2b, (byte)0xfe, (byte)0xd7, (byte)0xab, (byte)0x76,
    (byte)0xca, (byte)0x82, (byte)0xc9, (byte)0x7d, (byte)0xfa, (byte)0x59, (byte)0x47, (byte)0xf0, (byte)0xad, (byte)0xd4, (byte)0xa2, (byte)0xaf, (byte)0x9c, (byte)0xa4, (byte)0x72, (byte)0xc0,
    (byte)0xb7, (byte)0xfd, (byte)0x93, (byte)0x26, (byte)0x36, (byte)0x3f, (byte)0xf7, (byte)0xcc, (byte)0x34, (byte)0xa5, (byte)0xe5, (byte)0xf1, (byte)0x71, (byte)0xd8, (byte)0x31, (byte)0x15,
    (byte)0x04, (byte)0xc7, (byte)0x23, (byte)0xc3, (byte)0x18, (byte)0x96, (byte)0x05, (byte)0x9a, (byte)0x07, (byte)0x12, (byte)0x80, (byte)0xe2, (byte)0xeb, (byte)0x27, (byte)0xb2, (byte)0x75,
    (byte)0x09, (byte)0x83, (byte)0x2c, (byte)0x1a, (byte)0x1b, (byte)0x6e, (byte)0x5a, (byte)0xa0, (byte)0x52, (byte)0x3b, (byte)0xd6, (byte)0xb3, (byte)0x29, (byte)0xe3, (byte)0x2f, (byte)0x84,
    (byte)0x53, (byte)0xd1, (byte)0x00, (byte)0xed, (byte)0x20, (byte)0xfc, (byte)0xb1, (byte)0x5b, (byte)0x6a, (byte)0xcb, (byte)0xbe, (byte)0x39, (byte)0x4a, (byte)0x4c, (byte)0x58, (byte)0xcf,
    (byte)0xd0, (byte)0xef, (byte)0xaa, (byte)0xfb, (byte)0x43, (byte)0x4d, (byte)0x33, (byte)0x85, (byte)0x45, (byte)0xf9, (byte)0x02, (byte)0x7f, (byte)0x50, (byte)0x3c, (byte)0x9f, (byte)0xa8,
    (byte)0x51, (byte)0xa3, (byte)0x40, (byte)0x8f, (byte)0x92, (byte)0x9d, (byte)0x38, (byte)0xf5, (byte)0xbc, (byte)0xb6, (byte)0xda, (byte)0x21, (byte)0x10, (byte)0xff, (byte)0xf3, (byte)0xd2,
    (byte)0xcd, (byte)0x0c, (byte)0x13, (byte)0xec, (byte)0x5f, (byte)0x97, (byte)0x44, (byte)0x17, (byte)0xc4, (byte)0xa7, (byte)0x7e, (byte)0x3d, (byte)0x64, (byte)0x5d, (byte)0x19, (byte)0x73,
    (byte)0x60, (byte)0x81, (byte)0x4f, (byte)0xdc, (byte)0x22, (byte)0x2a, (byte)0x90, (byte)0x88, (byte)0x46, (byte)0xee, (byte)0xb8, (byte)0x14, (byte)0xde, (byte)0x5e, (byte)0x0b, (byte)0xdb,
    (byte)0xe0, (byte)0x32, (byte)0x3a, (byte)0x0a, (byte)0x49, (byte)0x06, (byte)0x24, (byte)0x5c, (byte)0xc2, (byte)0xd3, (byte)0xac, (byte)0x62, (byte)0x91, (byte)0x95, (byte)0xe4, (byte)0x79,
    (byte)0xe7, (byte)0xc8, (byte)0x37, (byte)0x6d, (byte)0x8d, (byte)0xd5, (byte)0x4e, (byte)0xa9, (byte)0x6c, (byte)0x56, (byte)0xf4, (byte)0xea, (byte)0x65, (byte)0x7a, (byte)0xae, (byte)0x08,
    (byte)0xba, (byte)0x78, (byte)0x25, (byte)0x2e, (byte)0x1c, (byte)0xa6, (byte)0xb4, (byte)0xc6, (byte)0xe8, (byte)0xdd, (byte)0x74, (byte)0x1f, (byte)0x4b, (byte)0xbd, (byte)0x8b, (byte)0x8a,
    (byte)0x70, (byte)0x3e, (byte)0xb5, (byte)0x66, (byte)0x48, (byte)0x03, (byte)0xf6, (byte)0x0e, (byte)0x61, (byte)0x35, (byte)0x57, (byte)0xb9, (byte)0x86, (byte)0xc1, (byte)0x1d, (byte)0x9e,
    (byte)0xe1, (byte)0xf8, (byte)0x98, (byte)0x11, (byte)0x69, (byte)0xd9, (byte)0x8e, (byte)0x94, (byte)0x9b, (byte)0x1e, (byte)0x87, (byte)0xe9, (byte)0xce, (byte)0x55, (byte)0x28, (byte)0xdf,
    (byte)0x8c, (byte)0xa1, (byte)0x89, (byte)0x0d, (byte)0xbf, (byte)0xe6, (byte)0x42, (byte)0x68, (byte)0x41, (byte)0x99, (byte)0x2d, (byte)0x0f, (byte)0xb0, (byte)0x54, (byte)0xbb, (byte)0x16
  };
  public static final byte[] unsubstitution_box = {
    (byte)0x52, (byte)0x09, (byte)0x6a, (byte)0xd5, (byte)0x30, (byte)0x36, (byte)0xa5, (byte)0x38, (byte)0xbf, (byte)0x40, (byte)0xa3, (byte)0x9e, (byte)0x81, (byte)0xf3, (byte)0xd7, (byte)0xfb,
    (byte)0x7c, (byte)0xe3, (byte)0x39, (byte)0x82, (byte)0x9b, (byte)0x2f, (byte)0xff, (byte)0x87, (byte)0x34, (byte)0x8e, (byte)0x43, (byte)0x44, (byte)0xc4, (byte)0xde, (byte)0xe9, (byte)0xcb,
    (byte)0x54, (byte)0x7b, (byte)0x94, (byte)0x32, (byte)0xa6, (byte)0xc2, (byte)0x23, (byte)0x3d, (byte)0xee, (byte)0x4c, (byte)0x95, (byte)0x0b, (byte)0x42, (byte)0xfa, (byte)0xc3, (byte)0x4e,
    (byte)0x08, (byte)0x2e, (byte)0xa1, (byte)0x66, (byte)0x28, (byte)0xd9, (byte)0x24, (byte)0xb2, (byte)0x76, (byte)0x5b, (byte)0xa2, (byte)0x49, (byte)0x6d, (byte)0x8b, (byte)0xd1, (byte)0x25,
    (byte)0x72, (byte)0xf8, (byte)0xf6, (byte)0x64, (byte)0x86, (byte)0x68, (byte)0x98, (byte)0x16, (byte)0xd4, (byte)0xa4, (byte)0x5c, (byte)0xcc, (byte)0x5d, (byte)0x65, (byte)0xb6, (byte)0x92,
    (byte)0x6c, (byte)0x70, (byte)0x48, (byte)0x50, (byte)0xfd, (byte)0xed, (byte)0xb9, (byte)0xda, (byte)0x5e, (byte)0x15, (byte)0x46, (byte)0x57, (byte)0xa7, (byte)0x8d, (byte)0x9d, (byte)0x84,
    (byte)0x90, (byte)0xd8, (byte)0xab, (byte)0x00, (byte)0x8c, (byte)0xbc, (byte)0xd3, (byte)0x0a, (byte)0xf7, (byte)0xe4, (byte)0x58, (byte)0x05, (byte)0xb8, (byte)0xb3, (byte)0x45, (byte)0x06,
    (byte)0xd0, (byte)0x2c, (byte)0x1e, (byte)0x8f, (byte)0xca, (byte)0x3f, (byte)0x0f, (byte)0x02, (byte)0xc1, (byte)0xaf, (byte)0xbd, (byte)0x03, (byte)0x01, (byte)0x13, (byte)0x8a, (byte)0x6b,
    (byte)0x3a, (byte)0x91, (byte)0x11, (byte)0x41, (byte)0x4f, (byte)0x67, (byte)0xdc, (byte)0xea, (byte)0x97, (byte)0xf2, (byte)0xcf, (byte)0xce, (byte)0xf0, (byte)0xb4, (byte)0xe6, (byte)0x73,
    (byte)0x96, (byte)0xac, (byte)0x74, (byte)0x22, (byte)0xe7, (byte)0xad, (byte)0x35, (byte)0x85, (byte)0xe2, (byte)0xf9, (byte)0x37, (byte)0xe8, (byte)0x1c, (byte)0x75, (byte)0xdf, (byte)0x6e,
    (byte)0x47, (byte)0xf1, (byte)0x1a, (byte)0x71, (byte)0x1d, (byte)0x29, (byte)0xc5, (byte)0x89, (byte)0x6f, (byte)0xb7, (byte)0x62, (byte)0x0e, (byte)0xaa, (byte)0x18, (byte)0xbe, (byte)0x1b,
    (byte)0xfc, (byte)0x56, (byte)0x3e, (byte)0x4b, (byte)0xc6, (byte)0xd2, (byte)0x79, (byte)0x20, (byte)0x9a, (byte)0xdb, (byte)0xc0, (byte)0xfe, (byte)0x78, (byte)0xcd, (byte)0x5a, (byte)0xf4,
    (byte)0x1f, (byte)0xdd, (byte)0xa8, (byte)0x33, (byte)0x88, (byte)0x07, (byte)0xc7, (byte)0x31, (byte)0xb1, (byte)0x12, (byte)0x10, (byte)0x59, (byte)0x27, (byte)0x80, (byte)0xec, (byte)0x5f,
    (byte)0x60, (byte)0x51, (byte)0x7f, (byte)0xa9, (byte)0x19, (byte)0xb5, (byte)0x4a, (byte)0x0d, (byte)0x2d, (byte)0xe5, (byte)0x7a, (byte)0x9f, (byte)0x93, (byte)0xc9, (byte)0x9c, (byte)0xef,
    (byte)0xa0, (byte)0xe0, (byte)0x3b, (byte)0x4d, (byte)0xae, (byte)0x2a, (byte)0xf5, (byte)0xb0, (byte)0xc8, (byte)0xeb, (byte)0xbb, (byte)0x3c, (byte)0x83, (byte)0x53, (byte)0x99, (byte)0x61,
    (byte)0x17, (byte)0x2b, (byte)0x04, (byte)0x7e, (byte)0xba, (byte)0x77, (byte)0xd6, (byte)0x26, (byte)0xe1, (byte)0x69, (byte)0x14, (byte)0x63, (byte)0x55, (byte)0x21, (byte)0x0c, (byte)0x7d
  };

  public byte[] key_schedule;
  public byte[] state; //4x4 column major order matrix
  public int current_round;

  public AES256IGE(byte[] key) {
    if (key.length == 32) {
      generate_key_schedule(key);
    } else {
      throw new IllegalArgumentException("Key is of incorrect length, should be 32 bytes long but was "+Integer.toString(key.length));
    }
  }

  public static byte[] encrypt(byte[] key, byte[] initialization_vector, byte[] data) {
    AES256IGE encryption_primitive_engine = new AES256IGE(key);
    ByteArrayPlus encrypted_bytes = new ByteArrayPlus();

    byte[] previous_garbled_block = new byte[16];
    System.arraycopy(initialization_vector, 0, previous_garbled_block, 0, 16);
    byte[] previous_block = new byte[16];
    System.arraycopy(initialization_vector, 16, previous_block, 0, 16);

    for (int i = 0; i < data.length; i += 16) {
      byte[] encrypted_block;
      byte[] garbled_block;
      byte[] block = new byte[16];
      System.arraycopy(data, i, block, 0, 16);

      encrypted_block = encryption_primitive_engine.encrypt_block(xor_bytes(block, previous_garbled_block));
      garbled_block = xor_bytes(encrypted_block, previous_block);
      previous_block = block;
      previous_garbled_block = garbled_block;
      encrypted_bytes.append_raw_bytes(garbled_block);
    }
    return encrypted_bytes.toByteArray();
  }

  public static byte[] decrypt(byte[] key, byte[] initialization_vector, byte[] data) {
    AES256IGE encryption_primitive_engine = new AES256IGE(key);
    ByteArrayPlus decrypted_bytes = new ByteArrayPlus();

    byte[] previous_degarbled_block = new byte[16];
    System.arraycopy(initialization_vector, 16, previous_degarbled_block, 0, 16);
    byte[] previous_block = new byte[16];
    System.arraycopy(initialization_vector, 0, previous_block, 0, 16);

    for (int i = 0; i < data.length; i += 16) {
      byte[] decrypted_block;
      byte[] degarbled_block;
      byte[] block = new byte[16];
      System.arraycopy(data, i, block, 0, 16);

      decrypted_block = encryption_primitive_engine.decrypt_block(xor_bytes(block, previous_degarbled_block));
      degarbled_block = xor_bytes(decrypted_block, previous_block);
      previous_block = block;
      previous_degarbled_block = degarbled_block;
      decrypted_bytes.append_raw_bytes(degarbled_block);
    }
    return decrypted_bytes.toByteArray();
  }

  public static byte[] xor_bytes(byte[] a, byte[] b) {
    byte[] result = new byte[16];

    for (int i = 0; i < 16; i++) {
      result[i] = (byte)((a[i]&0xFF) ^ (b[i]&0xFF));
    }

    return result;
  }

  public void substitute_bytes() {
    for (int i = 0; i < 16; i++) {
      state[i] = substitution_box[((int)state[i])&0xFF];
    }
  }

  public void unsubstitute_bytes() {
    for (int i = 0; i < 16; i++) {
      state[i] = unsubstitution_box[((int)state[i])&0xFF];
    }
  }

  public byte galois_field_multiply(byte multiplicand, byte multiplier) {
    byte result = 0;
    for (int i = 0; i < 8; i++) {
      if ((multiplier & 1) != 0) {
        result ^= multiplicand;
      }
      boolean hi_bit_set = (multiplicand & 0x80) != 0;
      multiplicand <<= 1;
      if (hi_bit_set) {
        multiplicand ^= 0x1B;
      }
      multiplier = (byte)((((int)multiplier)&0xFF) >>> 1);
    }
    return result;
  }

  public void mix_columns() {
    byte[] new_state = new byte[16];

    for (int i = 0; i < 4; i++) {
      new_state[0|(i<<2)] = (byte)(
        galois_field_multiply((byte)0x02, state[0|(i<<2)]) ^
        galois_field_multiply((byte)0x03, state[1|(i<<2)]) ^
        state[2|(i<<2)] ^
        state[3|(i<<2)]
      );
      new_state[1|(i<<2)] = (byte)(
        state[0|(i<<2)] ^
        galois_field_multiply((byte)0x02, state[1|(i<<2)]) ^
        galois_field_multiply((byte)0x03, state[2|(i<<2)]) ^
        state[3|(i<<2)]
      );
      new_state[2|(i<<2)] = (byte)(
        state[0|(i<<2)] ^
        state[1|(i<<2)] ^
        galois_field_multiply((byte)0x02, state[2|(i<<2)]) ^
        galois_field_multiply((byte)0x03, state[3|(i<<2)])
      );
      new_state[3|(i<<2)] = (byte)(
        galois_field_multiply((byte)0x03, state[0|(i<<2)]) ^
        state[1|(i<<2)] ^
        state[2|(i<<2)] ^
        galois_field_multiply((byte)0x02, state[3|(i<<2)])
      );
    }

    state = new_state;
  }

  public void unmix_columns() {
    byte[] new_state = new byte[16];

    for (int i = 0; i < 4; i++) {
      new_state[0|(i<<2)] = (byte)(
        galois_field_multiply((byte)0x0E, state[0|(i<<2)]) ^
        galois_field_multiply((byte)0x0B, state[1|(i<<2)]) ^
        galois_field_multiply((byte)0x0D, state[2|(i<<2)]) ^
        galois_field_multiply((byte)0x09, state[3|(i<<2)])
      );
      new_state[1|(i<<2)] = (byte)(
        galois_field_multiply((byte)0x09, state[0|(i<<2)]) ^
        galois_field_multiply((byte)0x0E, state[1|(i<<2)]) ^
        galois_field_multiply((byte)0x0B, state[2|(i<<2)]) ^
        galois_field_multiply((byte)0x0D, state[3|(i<<2)])
      );
      new_state[2|(i<<2)] = (byte)(
        galois_field_multiply((byte)0x0D, state[0|(i<<2)]) ^
        galois_field_multiply((byte)0x09, state[1|(i<<2)]) ^
        galois_field_multiply((byte)0x0E, state[2|(i<<2)]) ^
        galois_field_multiply((byte)0x0B, state[3|(i<<2)])
      );
      new_state[3|(i<<2)] = (byte)(
        galois_field_multiply((byte)0x0B, state[0|(i<<2)]) ^
        galois_field_multiply((byte)0x0D, state[1|(i<<2)]) ^
        galois_field_multiply((byte)0x09, state[2|(i<<2)]) ^
        galois_field_multiply((byte)0x0E, state[3|(i<<2)])
      );
    }

    state = new_state;
  }

  //also sometimes called permutation
  public void shift_rows() {
    state = new byte[] {
      state[0], state[5], state[10], state[15],
      state[4], state[9], state[14], state[3],
      state[8], state[13], state[2], state[7],
      state[12], state[1], state[6], state[11]
    };
  }

  public void unshift_rows() {
    state = new byte[] {
      state[0], state[13], state[10], state[7],
      state[4], state[1], state[14], state[11],
      state[8], state[5], state[2], state[15],
      state[12], state[9], state[6], state[3]
    };
  }

  public void add_round_key() {
    for (int i = 0; i < state.length; i++) {
      state[i] ^= key_schedule[current_round*16+i];
    }
   }

  public void generate_key_schedule(byte[] key) {
    key_schedule = new byte[240];
    System.arraycopy(key, 0, key_schedule, 0, key.length);
    byte[] t = new byte[4];
    int c = 32;
    int exponent = 1;

    while (c < 240) {
      /* Copy the temporary variable over */
      for (int i = 0; i < 4; i++) {
        t[i] = key_schedule[i + c - 4];
      }
      /* Every eight sets, do a complex calculation */
      if (c % 32 == 0) {
        t = schedule_core(t, exponent);
        exponent++;
      }
      /* For 256-bit keys, we add an extra sbox to the
      * calculation */
      if (c % 32 == 16) {
        for (int i = 0; i < 4; i++) {
          t[i] = substitution_box[((int)t[i])&0xFF];
        }
      }
      for (int i = 0; i < 4; i++) {
        key_schedule[c] = (byte)(key_schedule[c - 32] ^ t[i]);
        c++;
      }
    }
  }

  public byte[] schedule_core(byte[] in, int exponent) {
    byte[] new_array = rotate_left(in);
    /* Apply Rijndael's s-box on all 4 bytes */
    for(int i = 0; i < 4; i++) {
      new_array[i] = substitution_box[((int)new_array[i])&0xFF];
    }
    /* On just the first byte, add 2^i to the byte */
    new_array[0] ^= galois_field_2_power(exponent);
    return new_array;
  }

  //rcon
  public byte galois_field_2_power(int exponent) {
    byte power = 1;
    if (exponent == 0) {
      return 0;
    }
    while (exponent != 1) {
      power = galois_field_multiply(power, (byte)2);
      exponent--;
    }
    return power;
  }

  public byte[] rotate_left(byte[] in) {
    byte[] new_array = new byte[in.length];
    for (int i = 0; i < in.length-1; i++) {
      new_array[i] = in[i + 1];
    }
    new_array[new_array.length-1] = in[0];
    return new_array;
  }

  public byte[] encrypt_block(byte[] block) {
    if (block.length == 16) {
      state = new byte[16];
      current_round = 0;
      System.arraycopy(block, 0, state, 0, state.length);
      add_round_key();
      current_round++;
      for (int i = 0; i < 13; i++) {
        substitute_bytes();
        shift_rows();
        mix_columns();
        add_round_key();
        current_round++;
      }
      substitute_bytes();
      shift_rows();
      add_round_key();

      return state;
    } else {
      throw new IllegalArgumentException("Block is of incorrect length, should be 16 bytes long but was "+Integer.toString(block.length));
    }
  }

  public byte[] decrypt_block(byte[] block) {
    if (block.length == 16) {
      state = new byte[16];
      System.arraycopy(block, 0, state, 0, state.length);
      current_round = 14;
      add_round_key();
      unshift_rows();
      unsubstitute_bytes();
      for (int i = 0; i < 13; i++) {
        current_round--;
        add_round_key();
        unmix_columns();
        unshift_rows();
        unsubstitute_bytes();
      }
      current_round--;
      add_round_key();

      return state;
    } else {
      throw new IllegalArgumentException("Block is of incorrect length, should be 16 bytes long but was "+Integer.toString(block.length));
    }
  }
}
