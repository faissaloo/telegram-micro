package mtproto;

import java.io.IOException;

import support.Encode;
import support.ByteArrayPlus;
import support.Decode;

public class UnencryptedResponse {
  long auth_key_id;
  long message_id;
  int type;
  byte[] data;

  public static UnencryptedResponse from_tcp_response(TCPResponse response) throws IOException {
    byte[] response_data = response.data();
    long auth_key_id = Decode.Little.long_decode(response_data, 0);
    if (auth_key_id == 0) {
      long message_id = Decode.Little.long_decode(response_data, 8);
      int message_data_length = Decode.Little.int_decode(response_data, 16);
      int type = Decode.Little.int_decode(response_data, 20);

      //Then get the data
      ByteArrayPlus message_data = new ByteArrayPlus();
      for (int i = 4; i < message_data_length; i++) {
        message_data.append_byte(response_data[20+i]);
      }

      return new UnencryptedResponse(message_id, type, message_data.toByteArray());
    } else {
      return null; //This is an encrypted response
    }
  }

  public UnencryptedResponse(long message_id, int type, byte[] data) {
    this.message_id = message_id;
    this.type = type;
    this.data = data;
  }

  public long auth_key_id() {
    return auth_key_id;
  }
  public long message_id() {
    return message_id;
  }
  public int type() {
    return type;
  }
  public byte[] data() {
    return data;
  }
}
