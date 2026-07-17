package mtproto.recieve.auth;

import mtproto.CombinatorIds;
import mtproto.Deserialize;
import mtproto.RPCResponse;
import mtproto.TypeMismatchException;

import support.Decode;
import support.Utf8String;

public class RecievePasswordState {
  public static final int FLAG_HAS_PASSWORD = 1 << 2;
  public static final int FLAG_HAS_HINT = 1 << 3;

  public static RecievePasswordState from_rpc_response(RPCResponse response) {
    if (response.type != CombinatorIds.account_password) {
      throw new TypeMismatchException("Expected an %(account.Password)");
    }

    int offset = 0;
    byte[] data = response.body;
    int flags = Decode.Little.int_decode(data, offset);
    offset += 4;

    if ((flags & FLAG_HAS_PASSWORD) == 0) {
      return new RecievePasswordState(false, 0, 0, new byte[0], new byte[0], new byte[0], new byte[0], "");
    }

    int algo_id = Decode.Little.int_decode(data, offset);
    offset += 4;
    if (algo_id != CombinatorIds.password_kdf_algo_modpow) {
      throw new TypeMismatchException("Expected a supported %(PasswordKdfAlgo)");
    }

    byte[] salt1 = Deserialize.bytes_deserialize(data, offset);
    offset += Deserialize.bytes_length_deserialize(data, offset);
    byte[] salt2 = Deserialize.bytes_deserialize(data, offset);
    offset += Deserialize.bytes_length_deserialize(data, offset);
    int g = Decode.Little.int_decode(data, offset);
    offset += 4;
    byte[] p = Deserialize.bytes_deserialize(data, offset);
    offset += Deserialize.bytes_length_deserialize(data, offset);
    byte[] srp_B = Deserialize.bytes_deserialize(data, offset);
    offset += Deserialize.bytes_length_deserialize(data, offset);
    long srp_id = Decode.Little.long_decode(data, offset);
    offset += 8;

    String hint = "";
    if ((flags & FLAG_HAS_HINT) != 0) {
      Utf8String hint_data = Deserialize.utf8string_deserialize(data, offset);
      hint = hint_data.ascii_string();
    }

    return new RecievePasswordState(true, g, srp_id, salt1, salt2, p, srp_B, hint);
  }

  public boolean has_password;
  public int g;
  public long srp_id;
  public byte[] salt1;
  public byte[] salt2;
  public byte[] p;
  public byte[] srp_B;
  public String hint;

  public RecievePasswordState(
    boolean has_password,
    int g,
    long srp_id,
    byte[] salt1,
    byte[] salt2,
    byte[] p,
    byte[] srp_B,
    String hint
  ) {
    this.has_password = has_password;
    this.g = g;
    this.srp_id = srp_id;
    this.salt1 = salt1;
    this.salt2 = salt2;
    this.p = p;
    this.srp_B = srp_B;
    this.hint = hint;
  }
}
