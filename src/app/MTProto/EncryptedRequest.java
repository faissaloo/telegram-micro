package mtproto;

import java.io.IOException;

import crypto.SHA256;
import crypto.AES256IGE;

import support.Encode;
import support.ByteArrayPlus;
import support.ArrayPlus;

public class EncryptedRequest {
  byte[] unencrypted_data;
  //https://core.telegram.org/mtproto/description#defining-aes-key-and-initialization-vector
  public EncryptedRequest(byte[] unencrypted_data) {
    this.unencrypted_data = unencrypted_data;
  }

  //real talk i'm not entirely sure if this works in the slightest since 
  //unencrypted communication doesn't seem to care about what you slap in here
  private long message_id() {
    //https://core.telegram.org/mtproto/description#message-identifier-msg-id
    return (System.currentTimeMillis()/1000L)*4294967296L;
  }

  public void send(MTProtoConnection sender) {
    byte[] padded_unencrypted_data = (new ByteArrayPlus())
      .append_raw_bytes(unencrypted_data)
      .pad_to_length(12) //this padding has to be between 12-1024, so let's just pad 12 for now, we should later randomize this in both length and content
      .pad_to_alignment(16) //this should also have its content randomized
      .toByteArray();
     
    //we should get the auth_key_id and auth_key from the MTProtoConnection
    //msg_key_large = SHA256 (substr (auth_key, 88+x, 32) + plaintext + random_padding);
    byte[] msg_key_large = (new SHA256()).digest(
      (new ByteArrayPlus())
        .append_raw_bytes_from_up_to(sender.auth_key, 88, 32)
        .append_raw_bytes(padded_unencrypted_data)
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
    
    byte[] message_data = AES256IGE.encrypt(aes_key, aes_iv, padded_unencrypted_data);
    //https://core.telegram.org/mtproto/description#encrypted-message-encrypted-data
    byte[] encrypted_data = (new ByteArrayPlus())
      .append_raw_bytes(sender.server_salt)
      .append_long(sender.session_id)
      .append_long(message_id())
      .append_int(sender.seq_no)
      .append_int(message_data.length)
      .append_raw_bytes(message_data)
      .pad_to_length(12) //this padding has to be between 12-1024, so let's just pad 12 for now, we should later randomize this in both length and content
      .pad_to_alignment(16) //this should also have its content randomized, also I'm not actually sure if this has to be padded to alignment
      .toByteArray();
    
    //https://core.telegram.org/mtproto/description#encrypted-message
    byte[] encrypted_message = (new ByteArrayPlus())
      .append_long(sender.auth_key_id)
      .append_raw_bytes(msg_key)
      .append_raw_bytes(encrypted_data)
      .toByteArray();
    
    sender.seq_no += 1; //this might be a race condition idk lol
    
    (new TCPRequest(encrypted_message)).send(sender);
  }
}
