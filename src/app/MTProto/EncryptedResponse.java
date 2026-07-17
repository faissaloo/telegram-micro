package mtproto;

import java.io.IOException;

import support.Encode;
import support.ByteArrayPlus;
import support.ArrayPlus;
import support.Decode;
import support.Integer128;
import crypto.AES256IGE;
import crypto.SHA256;

public class EncryptedResponse extends Response {
  public long salt;
  public long session_id;
  public long message_id;
  public int seq_no;
  public byte[] data;

  public static EncryptedResponse from_tcp_response(
    TCPResponse response,
    MTProtoConnection reciever
  ) throws IOException {
    if (response == null || response.data == null || response.data.length < 4) {
      throw new IOException("Encrypted response is truncated.");
    }
    byte[] response_data = response.data;
    if (response_data.length == 4) {
      int error_value = Decode.Little.int_decode(response_data, 0);
      throw new IOException("Recieved error "+error_value+" from server");
    } else {
      int skip = 0;
      if (response_data.length < 8) {
        throw new IOException("Response auth key id is truncated.");
      }
      long auth_key_id = Decode.Little.long_decode(response_data, skip); //this is fine
      
      skip += 8;
      
      if (auth_key_id == 0) {
        return null; //this is an unencrypted response
      } else {
        if (reciever == null || reciever.auth_key == null || reciever.auth_key.length < 128) {
          throw new IOException("Encrypted response received without an auth key.");
        }
        if (reciever.auth_key_id != 0 && auth_key_id != reciever.auth_key_id) {
          throw new IOException("Encrypted response uses an unexpected auth key.");
        }
        if (response_data.length < 40) {
          throw new IOException("Encrypted response body is truncated.");
        }
        byte[] msg_key = ArrayPlus.subarray(response_data, skip, 16);
        skip += 16;
        
        int encrypted_data_length = response_data.length-skip;
        if (encrypted_data_length < 16 || (encrypted_data_length & 15) != 0) {
          throw new IOException("Encrypted response is not block aligned.");
        }
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
        if (decrypted_data == null || decrypted_data.length < 48) {
          throw new IOException("Decrypted response is too short.");
        }

        byte[] msg_key_large = (new SHA256()).digest(
          (new ByteArrayPlus())
            .append_raw_bytes_from_up_to(reciever.auth_key, 96, 32)
            .append_raw_bytes(decrypted_data)
            .toByteArray()
        );
        int msg_key_difference = 0;
        for (int i = 0; i < msg_key.length; ++i) {
          msg_key_difference |= msg_key[i] ^ msg_key_large[8 + i];
        }
        if (msg_key_difference != 0) {
          throw new IOException("Encrypted response message key verification failed.");
        }
        
        int decrypted_skip = 0;
        long salt = Decode.Little.long_decode(decrypted_data, decrypted_skip);
        
        decrypted_skip += 8;
        long session_id = Decode.Little.long_decode(decrypted_data, decrypted_skip);
        decrypted_skip += 8;
        if (reciever.session_id != 0 && session_id != reciever.session_id) {
          throw new IOException("Encrypted response uses an unexpected session.");
        }
        long message_id = Decode.Little.long_decode(decrypted_data, decrypted_skip);
        decrypted_skip += 8;
        int seq_no = Decode.Little.int_decode(decrypted_data, decrypted_skip);
        decrypted_skip += 4;
        int message_data_length = Decode.Little.int_decode(decrypted_data, decrypted_skip);
        decrypted_skip += 4;
        if (message_data_length < 4 ||
            message_data_length > ProtocolLimits.MAX_TL_BYTES ||
            message_data_length > decrypted_data.length - decrypted_skip) {
          throw new IOException("Invalid encrypted message length.");
        }
        int padding_length = decrypted_data.length - decrypted_skip - message_data_length;
        if (padding_length < 12 || padding_length > 1024) {
          throw new IOException("Invalid encrypted message padding length.");
        }
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
