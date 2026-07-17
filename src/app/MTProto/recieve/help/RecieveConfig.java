package mtproto.recieve.help;

import mtproto.CombinatorIds;
import mtproto.Deserialize;
import mtproto.RPCResponse;
import mtproto.TypeMismatchException;

import support.Decode;

public class RecieveConfig {
  public static final int bool_false = 0xbc799737;
  public static final int bool_true = 0x997275b5;
  public static final int dc_option = 0x18b7a10d;

  public static class DCOption {
    public int flags;
    public int id;
    public String ip_address;
    public int port;
    public byte[] secret;

    public DCOption(int flags, int id, String ip_address, int port, byte[] secret) {
      this.flags = flags;
      this.id = id;
      this.ip_address = ip_address;
      this.port = port;
      this.secret = secret;
    }

    public boolean has_secret() {
      return secret != null && secret.length > 0;
    }
  }

  public static boolean is_config_type(int type) {
    return type == CombinatorIds.config || type == CombinatorIds.config_modern;
  }

  public static RecieveConfig from_rpc_response(RPCResponse response) throws TypeMismatchException {
    int skip = 0;
    byte[] data = response.body;
    int message_type = response.type;

    if (!is_config_type(message_type)) {
      throw new TypeMismatchException("Expected a %(Config)");
    }

    int flags = Decode.Little.int_decode(data, skip);
    skip += 4;
    int date = Decode.Little.int_decode(data, skip);
    skip += 4;
    int expires = Decode.Little.int_decode(data, skip);
    skip += 4;

    int bool_id = Decode.Little.int_decode(data, skip);
    skip += 4;
    boolean test_mode;
    if (bool_id == bool_true) {
      test_mode = true;
    } else if (bool_id == bool_false) {
      test_mode = false;
    } else {
      throw new TypeMismatchException("Expected a %(Bool)");
    }

    int this_dc = Decode.Little.int_decode(data, skip);
    skip += 4;

    int vector_type = Decode.Little.int_decode(data, skip);
    skip += 4;
    if (vector_type != CombinatorIds.vector) {
      throw new TypeMismatchException("Expected a %(Vector DcOption)");
    }

    int dc_option_count = Decode.Little.int_decode(data, skip);
    skip += 4;
    DCOption[] dc_options = new DCOption[dc_option_count];
    for (int i = 0; i < dc_option_count; i += 1) {
      int option_type = Decode.Little.int_decode(data, skip);
      skip += 4;
      if (option_type != dc_option) {
        throw new TypeMismatchException("Expected a %(dcOption)");
      }

      int option_flags = Decode.Little.int_decode(data, skip);
      skip += 4;
      int id = Decode.Little.int_decode(data, skip);
      skip += 4;

      String ip_address = Deserialize.utf8string_deserialize(data, skip).ascii_string();
      skip += Deserialize.utf8string_length_deserialize(data, skip);

      int port = Decode.Little.int_decode(data, skip);
      skip += 4;

      byte[] secret = new byte[0];
      if ((option_flags & (1 << 10)) != 0) {
        secret = Deserialize.bytes_deserialize(data, skip);
        skip += Deserialize.bytes_length_deserialize(data, skip);
      }

      dc_options[i] = new DCOption(option_flags, id, ip_address, port, secret);
    }

    return new RecieveConfig(flags, date, expires, test_mode, this_dc, dc_options);
  }

  public int flags;
  public int date;
  public int expires;
  public boolean test_mode;
  public int this_dc;
  public DCOption[] dc_options;

  public RecieveConfig(
    int flags,
    int date,
    int expires,
    boolean test_mode,
    int this_dc,
    DCOption[] dc_options
  ) {
    this.flags = flags;
    this.date = date;
    this.expires = expires;
    this.test_mode = test_mode;
    this.this_dc = this_dc;
    this.dc_options = dc_options;
  }

  public DCOption find_this_dc_option() {
    return find_preferred_dc_option(this_dc);
  }

  public DCOption find_dc_option(int dc_id) {
    int i;

    for (i = 0; i < dc_options.length; i += 1) {
      if (dc_options[i].id == dc_id) {
        return dc_options[i];
      }
    }
    return null;
  }

  public DCOption find_preferred_dc_option(int dc_id) {
    DCOption fallback = null;

    for (int i = 0; i < dc_options.length; i += 1) {
      if (dc_options[i].id != dc_id) {
        continue;
      }
      if (fallback == null) {
        fallback = dc_options[i];
      }
      if (!dc_options[i].has_secret() && dc_options[i].ip_address.indexOf(':') < 0) {
        return dc_options[i];
      }
    }
    return fallback;
  }
}
