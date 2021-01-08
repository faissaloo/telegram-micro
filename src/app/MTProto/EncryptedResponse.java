package mtproto;

import java.io.IOException;

import support.Encode;
import support.ByteArrayPlus;
import support.ArrayPlus;
import support.Decode;
import support.Integer128;
import crypto.AES256IGE;
import crypto.SHA256;

public class EncryptedResponse {
  public long salt;
  public long session_id;
  public long message_id;
  public int seq_no;
  public int type;
  public byte[] data;

  public static EncryptedResponse from_tcp_response(TCPResponse response, MTProtoConnection reciever) throws IOException {
    byte[] response_data = response.data;
    if (response_data.length == 4) {
      int error_value = Decode.Little.int_decode(response_data, 0);
      throw new IOException("Recieved error "+error_value+" from server");
    } else {
      int skip = 0;
      long auth_key_id = Decode.Little.long_decode(response_data, skip); //this is fine
      
      skip += 8;
      
      if (auth_key_id == 0) {
        return null; //this is an unencrypted response
      } else {
        byte[] msg_key = ArrayPlus.subarray(response_data, skip, 16);
        skip += 16;
        
        int encrypted_data_length = response_data.length-skip;
        byte[] encrypted_data = ArrayPlus.subarray(response_data, skip, encrypted_data_length);
        
        //basically copied from EncryptedRequest, can probably be broken out into something else
        //so we deduplicate this chunk of code
        byte[] sha256_a = (new SHA256()).digest(
          (new ByteArrayPlus())
            .append_raw_bytes(msg_key)
            .append_raw_bytes_from_up_to(reciever.auth_key, 8, 36)
            .toByteArray()
        );
        byte[] sha256_b = (new SHA256()).digest(
          (new ByteArrayPlus())
            .append_raw_bytes_from_up_to(reciever.auth_key, 40+8, 36)
            .append_raw_bytes(msg_key)
            .toByteArray()
        );
        
        //aes_key = substr (sha256_a, 0, 8) + substr (sha256_b, 8, 16) + substr (sha256_a, 24, 8);
        byte[] aes_key = (new ByteArrayPlus())
          .append_raw_bytes_up_to(sha256_a, 8)
          .append_raw_bytes_from_up_to(sha256_b, 8, 16)
          .append_raw_bytes_from_up_to(sha256_a, 24, 8)
          .toByteArray();
        
        //aes_iv = substr (sha256_b, 0, 8) + substr (sha256_a, 8, 16) + substr (sha256_b, 24, 8);
        byte[] aes_iv = (new ByteArrayPlus())
          .append_raw_bytes_up_to(sha256_b, 8)
          .append_raw_bytes_from_up_to(sha256_a, 8, 16)
          .append_raw_bytes_from_up_to(sha256_b, 24, 8)
          .toByteArray();
          
        byte[] decrypted_data = AES256IGE.decrypt(aes_key, aes_iv, encrypted_data);
        
        int decrypted_skip = 0;
        long salt = Decode.Little.long_decode(decrypted_data, decrypted_skip);
        
        decrypted_skip += 8;
        long session_id = Decode.Little.long_decode(decrypted_data, decrypted_skip);
        decrypted_skip += 8;
        long message_id = Decode.Little.long_decode(decrypted_data, decrypted_skip);
        decrypted_skip += 8;
        int seq_no = Decode.Little.int_decode(decrypted_data, decrypted_skip);
        decrypted_skip += 4;
        int message_data_length = Decode.Little.int_decode(decrypted_data, decrypted_skip);
        decrypted_skip += 4;
        int type = Decode.Little.int_decode(decrypted_data, decrypted_skip);
        decrypted_skip += 4;
        byte[] message_data = ArrayPlus.subarray(decrypted_data, decrypted_skip, message_data_length-4);

        return new EncryptedResponse(salt, session_id, message_id, seq_no, type, message_data);
      }
    }
  }

  public EncryptedResponse(long salt, long session_id, long message_id, int seq_no, int type, byte[] data) {
    this.salt = salt;
    this.session_id = session_id;
    this.message_id = message_id;
    this.seq_no = seq_no;
    this.type = type;
    this.data = data;
  }
}
