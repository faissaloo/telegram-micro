package mtproto.recieve.auth;

import mtproto.CombinatorIds;
import mtproto.RPCResponse;

public class RecieveLoginResult {
  public static final int kind_unknown = 0;
  public static final int kind_sent_code = 1;
  public static final int kind_authorization = 2;
  public static final int kind_rpc_error = 3;
  public static final int kind_gzip_packed = 4;
  public static final int kind_sign_up_required = 5;
  public static final int kind_object = 6;

  public static RecieveLoginResult from_rpc_response(RPCResponse response) {
    int kind = kind_object;

    if (response.type == CombinatorIds.auth_sent_code) {
      kind = kind_sent_code;
    } else if (response.type == CombinatorIds.auth_authorization ||
      response.type == CombinatorIds.auth_sent_code_success) {
      kind = kind_authorization;
    } else if (response.type == CombinatorIds.rpc_error) {
      kind = kind_rpc_error;
    } else if (response.type == CombinatorIds.gzip_packed) {
      kind = kind_gzip_packed;
    } else if (response.type == CombinatorIds.auth_authorization_sign_up_required) {
      kind = kind_sign_up_required;
    }

    return new RecieveLoginResult(kind, response.type, response.body);
  }

  public int kind;
  public int constructor_id;
  public byte[] payload;

  public RecieveLoginResult(int kind, int constructor_id, byte[] payload) {
    this.kind = kind;
    this.constructor_id = constructor_id;
    this.payload = payload;
  }
}
