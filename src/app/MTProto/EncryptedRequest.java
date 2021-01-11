package mtproto;

import java.io.IOException;

import crypto.SHA256;
import crypto.AES256IGE;

import support.Encode;
import support.ByteArrayPlus;
import support.ArrayPlus;
import support.RandomPlus;
import crypto.SecureRandomPlus;

public class EncryptedRequest {
  byte[] message_data;
  //https://core.telegram.org/mtproto/description#defining-aes-key-and-initialization-vector
  public EncryptedRequest(byte[] message_data) {
    this.message_data = message_data;
  }

  private long message_id() {
    //https://core.telegram.org/mtproto/description#message-identifier-msg-id
    return (System.currentTimeMillis()/1000L)<<32;
  }
  
  public void send(MTProtoConnection sender) {
    //https://core.telegram.org/mtproto/description#encrypted-message-encrypted-data
    //https://core.telegram.org/mtproto/description#protocol-description
    byte[] unencrypted_data = (new ByteArrayPlus())
      .append_long(sender.server_salt)
      .append_long(sender.session_id)
      .append_long(message_id())
      .append_int(sender.seq_no)
      .append_int(message_data.length)
      .append_raw_bytes(
        (new ByteArrayPlus())
          .append_raw_bytes(message_data)
          .pad_random_align_range(16, 12, 1024, sender.random_number_generator)
          .toByteArray()
      )
      .toByteArray();
    
    //msg_key_large = SHA256 (substr (auth_key, 88+x, 32) + plaintext + random_padding);
    byte[] msg_key_large = (new SHA256()).digest(
      (new ByteArrayPlus())
        .append_raw_bytes_from_up_to(sender.auth_key, 88, 32)
        .append_raw_bytes(unencrypted_data)
        .toByteArray()
    );
    
    byte[] msg_key = ArrayPlus.subarray(msg_key_large, 8, 16);

    byte[] sha256_a = (new SHA256()).digest(
      (new ByteArrayPlus())
        .append_raw_bytes(msg_key)
        .append_raw_bytes_up_to(sender.auth_key, 36)
        .toByteArray()
    );
    byte[] sha256_b = (new SHA256()).digest(
      (new ByteArrayPlus())
        .append_raw_bytes_from_up_to(sender.auth_key, 40, 36)
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
    
    byte[] encrypted_data = AES256IGE.encrypt(aes_key, aes_iv, unencrypted_data);
    
    //https://core.telegram.org/mtproto/description#encrypted-message
    byte[] encrypted_message = (new ByteArrayPlus())
      .append_long(sender.auth_key_id)
      .append_raw_bytes(msg_key)
      .append_raw_bytes(encrypted_data)
      .toByteArray();
    
    (new TCPRequest(encrypted_message)).send(sender);
  }
}
