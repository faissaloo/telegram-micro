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

import mtproto.EncryptedResponse;
import mtproto.TypeMismatchException;
import mtproto.CombinatorIds;
import mtproto.Deserialize;

//rpc_result#f35c6d01 req_msg_id:long result:Object = RpcResult;
public class RecieveRpcResult {
  public static RecieveRpcResult from_encrypted_message(EncryptedResponse message) throws TypeMismatchException {
    int skip = 0;
    byte[] data = message.data;
    int message_type = message.type;

    if (message_type == CombinatorIds.rpc_result) {
      // req_msg_id:long result:Object = RpcResult;
      long req_msg_id = Decode.Little.long_decode(data, skip);
      skip += 8;
      byte[] result = ArrayPlus.subarray(data, skip, data.length-skip);
      System.out.println("OBJECT TYPE");
      System.out.println(Integer.toHexString(Decode.Little.int_decode(data, skip))); //rpc_error#2144ca19
      return new RecieveRpcResult(req_msg_id, result);
    } else {
      throw new TypeMismatchException("Expected a %(rpc_result)");
    }
  }
  
  public long req_msg_id;
  public byte[] result;
  
  public RecieveRpcResult(long req_msg_id, byte[] result) {
    this.req_msg_id = req_msg_id;
    this.result = result;
  }
}
