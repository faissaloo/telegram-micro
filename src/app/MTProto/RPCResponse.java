package mtproto;

import support.ArrayPlus;
import support.Decode;

public class RPCResponse extends Response {
  public static RPCResponse from_bytes(byte[] response) {
    int skip = 0;
    int type = Decode.Little.int_decode(response, skip);
    skip += 4;
    byte[] body = ArrayPlus.subarray(response, skip, response.length-skip);
    return new RPCResponse(type, body);
  }
  
  public byte[] body;
  
  public RPCResponse(int type, byte[] body) {
    this.type = type;
    this.body = body;
  }
}
