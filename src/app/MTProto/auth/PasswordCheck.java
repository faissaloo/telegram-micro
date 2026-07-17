package mtproto.auth;

import bouncycastle.BigInteger;

import crypto.SHA256;
import crypto.SHA512;

import mtproto.recieve.auth.RecievePasswordState;
import support.Utf8String;

public class PasswordCheck {
  public static final int PBKDF2_ROUNDS = 100000;
  public static final int SRP_PAD_LENGTH = 256;

  public static class Check {
    public byte[] A;
    public byte[] M1;

    public Check(byte[] A, byte[] M1) {
      this.A = A;
      this.M1 = M1;
    }
  }

  public static Check compute_check(RecievePasswordState state, String password, byte[] a_bytes) {
    if (state == null || !state.has_password) {
      throw new IllegalArgumentException("Password state is unavailable.");
    }
    if (password == null || password.length() == 0) {
      throw new IllegalArgumentException("Password is required.");
    }
    if (a_bytes == null || a_bytes.length == 0) {
      throw new IllegalArgumentException("SRP random input is required.");
    }
    if (state.srp_B == null || state.srp_B.length < 248 || state.srp_B.length > SRP_PAD_LENGTH) {
      throw new IllegalArgumentException("Telegram returned an invalid SRP B value.");
    }

    byte[] password_bytes = Utf8String.encode(password);
    byte[] hash1 = sha256_concat3(state.salt1, password_bytes, state.salt1);
    byte[] hash2 = sha256_concat3(state.salt2, hash1, state.salt2);
    byte[] pbkdf = pbkdf2_hmac_sha512(hash2, state.salt1, PBKDF2_ROUNDS, 64);
    byte[] x_bytes = sha256_concat3(state.salt2, pbkdf, state.salt2);

    BigInteger p = new BigInteger(1, state.p);
    BigInteger g = BigInteger.valueOf((long)state.g);
    BigInteger B = new BigInteger(1, state.srp_B);
    BigInteger zero = BigInteger.ZERO;

    if (B.compareTo(zero) <= 0 || B.compareTo(p) >= 0) {
      throw new IllegalArgumentException("Telegram returned an out-of-range SRP B value.");
    }

    BigInteger x = new BigInteger(1, x_bytes);
    BigInteger a = new BigInteger(1, a_bytes);
    BigInteger A = g.modPow(a, p);
    byte[] A_pad = fixed_bigint_bytes(A, SRP_PAD_LENGTH);
    byte[] B_pad = left_pad(state.srp_B, SRP_PAD_LENGTH);
    byte[] g_pad = fixed_bigint_bytes(g, SRP_PAD_LENGTH);

    BigInteger u = new BigInteger(1, sha256_concat2(A_pad, B_pad));
    BigInteger k = new BigInteger(1, sha256_concat2(state.p, g_pad));
    BigInteger v = g.modPow(x, p);
    BigInteger kv = k.multiply(v).mod(p);
    BigInteger t = B.subtract(kv).mod(p);
    BigInteger exponent = u.multiply(x).add(a);
    BigInteger S = t.modPow(exponent, p);

    byte[] K = sha256(fixed_bigint_bytes(S, SRP_PAD_LENGTH));
    byte[] p_hash = sha256(state.p);
    byte[] g_hash = sha256(g_pad);
    xor_in_place(p_hash, g_hash);
    byte[] salt1_hash = sha256(state.salt1);
    byte[] salt2_hash = sha256(state.salt2);
    byte[] M1 = sha256(concat6(p_hash, salt1_hash, salt2_hash, A_pad, B_pad, K));

    return new Check(A_pad, M1);
  }

  public static byte[] pbkdf2_hmac_sha512(byte[] password, byte[] salt, int rounds, int out_length) {
    if (password == null || salt == null || rounds <= 0 || out_length <= 0) {
      throw new IllegalArgumentException("Invalid PBKDF2 input.");
    }

    byte[] result = new byte[out_length];
    int offset = 0;
    int block_index = 1;

    while (offset < out_length) {
      byte[] salt_block = new byte[salt.length + 4];
      System.arraycopy(salt, 0, salt_block, 0, salt.length);
      salt_block[salt.length + 0] = (byte)(block_index >>> 24);
      salt_block[salt.length + 1] = (byte)(block_index >>> 16);
      salt_block[salt.length + 2] = (byte)(block_index >>> 8);
      salt_block[salt.length + 3] = (byte)block_index;

      byte[] u = hmac_sha512(password, salt_block);
      byte[] block = copy_bytes(u);

      for (int round = 1; round < rounds; ++round) {
        u = hmac_sha512(password, u);
        xor_in_place(block, u);
      }

      int remaining = out_length - offset;
      int amount = remaining < block.length ? remaining : block.length;
      System.arraycopy(block, 0, result, offset, amount);
      offset += amount;
      block_index += 1;
    }

    return result;
  }

  public static byte[] hmac_sha512(byte[] key, byte[] data) {
    byte[] actual_key = key;
    if (actual_key.length > 128) {
      actual_key = sha512(actual_key);
    }

    byte[] key_block = new byte[128];
    System.arraycopy(actual_key, 0, key_block, 0, actual_key.length);

    byte[] inner_key = new byte[128];
    byte[] outer_key = new byte[128];
    for (int i = 0; i < key_block.length; ++i) {
      inner_key[i] = (byte)(key_block[i] ^ 0x36);
      outer_key[i] = (byte)(key_block[i] ^ 0x5c);
    }

    byte[] inner = new byte[inner_key.length + data.length];
    System.arraycopy(inner_key, 0, inner, 0, inner_key.length);
    System.arraycopy(data, 0, inner, inner_key.length, data.length);

    byte[] inner_hash = sha512(inner);
    byte[] outer = new byte[outer_key.length + inner_hash.length];
    System.arraycopy(outer_key, 0, outer, 0, outer_key.length);
    System.arraycopy(inner_hash, 0, outer, outer_key.length, inner_hash.length);
    return sha512(outer);
  }

  public static byte[] sha256(byte[] data) {
    return (new SHA256()).digest(data);
  }

  public static byte[] sha512(byte[] data) {
    return (new SHA512()).digest(data);
  }

  public static byte[] sha256_concat2(byte[] a, byte[] b) {
    return sha256(concat2(a, b));
  }

  public static byte[] sha256_concat3(byte[] a, byte[] b, byte[] c) {
    return sha256(concat3(a, b, c));
  }

  public static byte[] concat2(byte[] a, byte[] b) {
    byte[] result = new byte[a.length + b.length];
    System.arraycopy(a, 0, result, 0, a.length);
    System.arraycopy(b, 0, result, a.length, b.length);
    return result;
  }

  public static byte[] concat3(byte[] a, byte[] b, byte[] c) {
    byte[] result = new byte[a.length + b.length + c.length];
    System.arraycopy(a, 0, result, 0, a.length);
    System.arraycopy(b, 0, result, a.length, b.length);
    System.arraycopy(c, 0, result, a.length + b.length, c.length);
    return result;
  }

  public static byte[] concat6(byte[] a, byte[] b, byte[] c, byte[] d, byte[] e, byte[] f) {
    byte[] result = new byte[a.length + b.length + c.length + d.length + e.length + f.length];
    int offset = 0;
    System.arraycopy(a, 0, result, offset, a.length);
    offset += a.length;
    System.arraycopy(b, 0, result, offset, b.length);
    offset += b.length;
    System.arraycopy(c, 0, result, offset, c.length);
    offset += c.length;
    System.arraycopy(d, 0, result, offset, d.length);
    offset += d.length;
    System.arraycopy(e, 0, result, offset, e.length);
    offset += e.length;
    System.arraycopy(f, 0, result, offset, f.length);
    return result;
  }

  public static void xor_in_place(byte[] target, byte[] xorier) {
    int length = target.length < xorier.length ? target.length : xorier.length;
    for (int i = 0; i < length; ++i) {
      target[i] = (byte)(target[i] ^ xorier[i]);
    }
  }

  public static byte[] fixed_bigint_bytes(BigInteger value, int length) {
    byte[] magnitude = value.magnitudeToBytes();
    if (magnitude.length > length) {
      throw new IllegalArgumentException("BigInteger does not fit the requested SRP width.");
    }
    return left_pad(magnitude, length);
  }

  public static byte[] left_pad(byte[] value, int length) {
    if (value.length > length) {
      throw new IllegalArgumentException("Byte array does not fit the requested SRP width.");
    }
    byte[] padded = new byte[length];
    System.arraycopy(value, 0, padded, length - value.length, value.length);
    return padded;
  }

  public static byte[] copy_bytes(byte[] input) {
    byte[] copy = new byte[input.length];
    System.arraycopy(input, 0, copy, 0, input.length);
    return copy;
  }
}
