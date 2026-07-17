package mtproto.recieve.auth;

import mtproto.CombinatorIds;
import mtproto.RPCResponse;
import mtproto.TypeMismatchException;

import support.Utf8String;
import support.Decode;

public class RecieveSentCode {
  public static final int sent_code_type_app = 0x3dbb5986;
  public static final int sent_code_type_sms = 0xc000bba2;
  public static final int sent_code_type_call = 0x5353e5a7;
  public static final int sent_code_type_flash_call = 0xab03c6d9;
  public static final int sent_code_type_missed_call = 0x82006484;
  public static final int sent_code_type_email_code = 0xf450f59b;
  public static final int sent_code_type_set_up_email_required = 0xa5491dea;
  public static final int sent_code_type_fragment_sms = 0xd9565c39;
  public static final int sent_code_type_firebase_sms = 0x009fd736;
  public static final int sent_code_type_sms_word = 0xa416ac81;
  public static final int sent_code_type_sms_phrase = 0xb37794af;

  public static final int code_type_sms = 1923290508;
  public static final int code_type_call = 1948046307;
  public static final int code_type_flash_call = 577556219;
  public static final int code_type_missed_call = -702884114;
  public static final int code_type_fragment = 116234636;

  private static class Reader {
    public byte[] data;
    public int offset;
    public boolean error;

    public Reader(byte[] data) {
      this.data = data;
      this.offset = 0;
      this.error = false;
    }

    public int remaining() {
      return data.length - offset;
    }
  }

  private static int read_u32(Reader reader) {
    if (reader.remaining() < 4) {
      reader.error = true;
      return 0;
    }
    int value = Decode.Little.int_decode(reader.data, reader.offset);
    reader.offset += 4;
    return value;
  }

  private static byte[] read_tl_bytes(Reader reader) {
    int start = reader.offset;
    int size;

    if (reader.remaining() < 1) {
      reader.error = true;
      return null;
    }

    size = reader.data[reader.offset] & 0xFF;
    reader.offset += 1;
    if (size >= 254) {
      if (reader.remaining() < 3) {
        reader.error = true;
        return null;
      }
      size =
        (reader.data[reader.offset] & 0xFF) |
        ((reader.data[reader.offset + 1] & 0xFF) << 8) |
        ((reader.data[reader.offset + 2] & 0xFF) << 16);
      reader.offset += 3;
    }

    if (reader.remaining() < size) {
      reader.error = true;
      return null;
    }

    byte[] value = new byte[size];
    System.arraycopy(reader.data, reader.offset, value, 0, size);
    reader.offset += size;

    while (((reader.offset - start) & 3) != 0) {
      if (reader.remaining() < 1) {
        reader.error = true;
        return null;
      }
      reader.offset += 1;
    }

    return value;
  }

  private static boolean skip_auth_code_type(Reader reader, int constructor_id) {
    switch (constructor_id) {
      case code_type_sms:
      case code_type_call:
      case code_type_flash_call:
      case code_type_missed_call:
      case code_type_fragment:
        return !reader.error;
      default:
        reader.error = true;
        return false;
    }
  }

  private static boolean skip_auth_sent_code_type(Reader reader, int constructor_id) {
    int flags;

    switch (constructor_id) {
      case sent_code_type_app:
      case sent_code_type_sms:
      case sent_code_type_call:
        read_u32(reader);
        return !reader.error;
      case sent_code_type_flash_call:
        read_tl_bytes(reader);
        return !reader.error;
      case sent_code_type_missed_call:
        read_tl_bytes(reader);
        read_u32(reader);
        return !reader.error;
      case sent_code_type_email_code:
        flags = read_u32(reader);
        read_tl_bytes(reader);
        read_u32(reader);
        if ((flags & (1 << 3)) != 0) {
          read_u32(reader);
        }
        if ((flags & (1 << 4)) != 0) {
          read_u32(reader);
        }
        return !reader.error;
      case sent_code_type_set_up_email_required:
        read_u32(reader);
        return !reader.error;
      case sent_code_type_fragment_sms:
        read_tl_bytes(reader);
        read_u32(reader);
        return !reader.error;
      case sent_code_type_firebase_sms:
        flags = read_u32(reader);
        if ((flags & (1 << 0)) != 0) {
          read_tl_bytes(reader);
        }
        if ((flags & (1 << 2)) != 0) {
          read_u32(reader);
          read_u32(reader);
          read_tl_bytes(reader);
        }
        if ((flags & (1 << 1)) != 0) {
          read_tl_bytes(reader);
          read_u32(reader);
        }
        read_u32(reader);
        return !reader.error;
      case sent_code_type_sms_word:
      case sent_code_type_sms_phrase:
        flags = read_u32(reader);
        if ((flags & (1 << 0)) != 0) {
          read_tl_bytes(reader);
        }
        return !reader.error;
      default:
        reader.error = true;
        return false;
    }
  }

  public static RecieveSentCode from_rpc_response(RPCResponse response) {
    if (response.type != CombinatorIds.auth_sent_code) {
      throw new TypeMismatchException("Expected an %(auth.sentCode)");
    }

    Reader reader = new Reader(response.body);
    int flags = read_u32(reader);
    int sent_code_type_id = read_u32(reader);
    if (!skip_auth_sent_code_type(reader, sent_code_type_id)) {
      throw new TypeMismatchException("Expected a supported %(auth.SentCodeType)");
    }

    byte[] phone_code_hash_data = read_tl_bytes(reader);
    if (reader.error || phone_code_hash_data == null || phone_code_hash_data.length == 0) {
      throw new TypeMismatchException("Expected a %(phone_code_hash)");
    }

    String phone_code_hash = (new Utf8String(phone_code_hash_data)).ascii_string();

    boolean has_next_type = false;
    int next_type_id = 0;
    if ((flags & (1 << 1)) != 0) {
      has_next_type = true;
      next_type_id = read_u32(reader);
      if (!skip_auth_code_type(reader, next_type_id)) {
        throw new TypeMismatchException("Expected a supported %(auth.CodeType)");
      }
    }

    boolean has_timeout = false;
    int timeout_seconds = 0;
    if ((flags & (1 << 2)) != 0) {
      has_timeout = true;
      timeout_seconds = read_u32(reader);
      if (reader.error) {
        throw new TypeMismatchException("Expected a %(timeout)");
      }
    }

    return new RecieveSentCode(
      flags,
      sent_code_type_id,
      phone_code_hash,
      has_next_type,
      next_type_id,
      has_timeout,
      timeout_seconds
    );
  }

  public int flags;
  public int sent_code_type_id;
  public String phone_code_hash;
  public boolean has_next_type;
  public int next_type_id;
  public boolean has_timeout;
  public int timeout_seconds;

  public RecieveSentCode(
    int flags,
    int sent_code_type_id,
    String phone_code_hash,
    boolean has_next_type,
    int next_type_id,
    boolean has_timeout,
    int timeout_seconds
  ) {
    this.flags = flags;
    this.sent_code_type_id = sent_code_type_id;
    this.phone_code_hash = phone_code_hash;
    this.has_next_type = has_next_type;
    this.next_type_id = next_type_id;
    this.has_timeout = has_timeout;
    this.timeout_seconds = timeout_seconds;
  }
}
