package mtproto.recieve;

import bouncycastle.BigInteger;

import support.Integer128;
import support.Integer256;
import support.Utf8String;
import support.ByteArrayPlus;
import support.ArrayPlus;
import support.Decode;
import support.Encode;
import support.Debug;

import crypto.SHA1;
import crypto.AES256IGE;

import mtproto.RPCResponse;
import mtproto.TypeMismatchException;
import mtproto.CombinatorIds;
import mtproto.Deserialize;

//rpc_result#f35c6d01 req_msg_id:long result:Object = RpcResult;
public class RecieveRPCError {
  public static RecieveRPCError from_rpc_response(RPCResponse message) throws TypeMismatchException {
    int skip = 0;
    byte[] data = message.body;
    int message_type = message.type;

    if (message_type == CombinatorIds.rpc_error) {
      int error_code = Decode.Little.int_decode(data, skip);
      skip += 4;
      Utf8String error_message = Deserialize.utf8string_deserialize(data, skip); //maybe String will be fine for this, not sure if this supports UTF-8
      return new RecieveRPCError(error_code, error_message);
    } else {
      throw new TypeMismatchException("Expected an %(rpc_error)");
    }
  }
  
  public int error_code;
  public Utf8String error_message;
  
  public RecieveRPCError(int error_code, Utf8String error_message) {
    this.error_code = error_code;
    this.error_message = error_message;
  }
}
