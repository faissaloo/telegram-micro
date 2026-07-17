package mtproto;

import support.ArrayPlus;
import support.Decode;

public class RPCResponse extends Response {
  public static RPCResponse from_bytes(byte[] response) {
    if (response == null || response.length < 4 ||
        response.length > ProtocolLimits.MAX_INFLATED_BYTES) {
      throw new IllegalArgumentException("Invalid RPC response length.");
    }
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
