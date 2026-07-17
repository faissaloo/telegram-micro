package mtproto;

import java.io.IOException;

import support.Decode;
import support.ArrayPlus;

public class UnencryptedResponse extends Response {
  public long auth_key_id;
  public long message_id;
  public byte[] data;

  public static UnencryptedResponse from_tcp_response(TCPResponse response) throws IOException {
    if (response == null || response.data == null || response.data.length < 4) {
      throw new IOException("Unencrypted response is truncated.");
    }
    byte[] response_data = response.data;
    if (response_data.length == 4) {
      int error_value = Decode.Little.int_decode(response_data, 0);
      throw new IOException("Recieved error "+error_value+" from server");
    } else {
      if (response_data.length < 8) {
        throw new IOException("Response auth key id is truncated.");
      }
      long auth_key_id = Decode.Little.long_decode(response_data, 0);
      if (auth_key_id == 0) {
        if (response_data.length < 24) {
          throw new IOException("Unencrypted response header is truncated.");
        }
        long message_id = Decode.Little.long_decode(response_data, 8);
        int message_data_length = Decode.Little.int_decode(response_data, 16);
        if (message_data_length < 4 || message_data_length > response_data.length - 20) {
          throw new IOException("Invalid unencrypted message length.");
        }
        int type = Decode.Little.int_decode(response_data, 20);
        byte[] message_data = ArrayPlus.subarray(response_data, 24, message_data_length - 4);
        return new UnencryptedResponse(message_id, type, message_data);
      } else {
        return null; //This is an encrypted response
      }
    }
  }

  public UnencryptedResponse(long message_id, int type, byte[] data) {
    this.message_id = message_id;
    this.type = type;
    this.data = data;
  }
}
