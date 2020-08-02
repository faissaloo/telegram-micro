package mtproto;

import java.io.IOException;

import support.Encode;
import support.ByteArrayPlus;
import crypto.SHA256;

public class EncryptedRequest {
  ByteArrayPlus message_data;

  public EncryptedRequest(long auth_key_id, byte[] auth_key, byte[] data) {
    //msg_key_large = SHA256 (substr (auth_key, 88+x, 32) + plaintext + random_padding);
    byte[] msg_key_large = (new SHA256()).digest(
      (new ByteArrayPlus())
        .append_raw_bytes_from_up_to(auth_key, 88, 32)
        .append_raw_bytes(data)
        .pad_to_alignment(16)
        .toByteArray()
    );
    byte[] msg_key = (new ByteArrayPlus()) //Optimise me, we should just have a static method for ArrayPlus that can extract a chunk from a byte array
      .append_raw_bytes_from_up_to(msg_key_large, 8, 16)
      .toByteArray();
      
    byte[] sha256_a = (new SHA256()).digest(
      (new ByteArrayPlus())
        .append_raw_bytes(msg_key)
        .append_raw_bytes_up_to(auth_key, 36)
        .toByteArray()
    );
    byte[] sha256_b = (new SHA256()).digest(
      (new ByteArrayPlus())
        .append_raw_bytes_from_up_to(auth_key, 40, 36)
        .append_raw_bytes(msg_key)
        .toByteArray()
    );
    
    byte[] aes_key = (new ByteArrayPlus())
      .append_raw_bytes_up_to(sha256_a, 8)
      .append_raw_bytes_from_up_to(sha256_b, 8, 16)
      .append_raw_bytes_from_up_to(sha256_a, 24, 8)
      .toByteArray();
    
    byte[] aes_iv = (new ByteArrayPlus())
      .append_raw_bytes_up_to(sha256_b, 8)
      .append_raw_bytes_from_up_to(sha256_a, 8, 16)
      .append_raw_bytes_from_up_to(sha256_b, 24, 8)
      .toByteArray();
    
    message_data = new ByteArrayPlus();
    message_data.append_long(auth_key_id);
    message_data.append_long(message_id());
    message_data.append_int(data.length);
    message_data.append_raw_bytes(data);
  }

  private long message_id() {
    //https://core.telegram.org/mtproto/description#message-identifier-msg-id
    return (System.currentTimeMillis()/1000L)*4294967296L;
  }

  public void send(MTProtoConnection sender) {
    (new TCPRequest(message_data.toByteArray())).send(sender);
  }
}
