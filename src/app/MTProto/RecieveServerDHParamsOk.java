package mtproto;

import support.Integer128;
import support.Integer256;
import support.Utf8String;
import support.ByteArrayPlus;
import support.Decode;
import support.Encode;
import crypto.SHA1;
import crypto.AES256IGE;
import support.Debug;

public class RecieveServerDHParamsOk {
  public static RecieveServerDHParamsOk from_unencrypted_message(UnencryptedResponse message, Integer256 new_nonce) throws TypeMismatchException {
    int skip = 0;
    byte[] data = message.data();
    int message_type = message.type();

    if (message_type == CombinatorIds.server_DH_params_ok) {
      Integer128 nonce = Decode.Little.Integer128_decode(data, skip);
      skip += 16;
      Integer128 server_nonce = Decode.Little.Integer128_decode(data, skip);
      skip += 16;
      byte[] encrypted_data = Deserialize.bytes_deserialize(data, skip);
      
      byte[] tmp_aes_key;
      byte[] tmp_aes_iv;
      {
        byte[] encoded_new_nonce = Encode.Integer256_encode(new_nonce);
        byte[] encoded_server_nonce = Encode.Integer128_encode(server_nonce);
        
        byte[] new_nonce_hash = (new SHA1())
          .digest(Encode.Integer256_encode(new_nonce), 16);
        
        byte[] new_nonce_server_nonce_hash = (new SHA1())
          .digest(
            (new ByteArrayPlus())
              .append_raw_bytes(encoded_new_nonce)
              .append_raw_bytes(encoded_server_nonce)
              .toByteArray()
          );
        
        byte[] server_nonce_new_nonce_hash = (new SHA1())
          .digest(
            (new ByteArrayPlus())
              .append_raw_bytes(encoded_server_nonce)
              .append_raw_bytes(encoded_new_nonce)
              .toByteArray()
          );
          
        byte[] new_nonce_new_nonce_hash = (new SHA1())
          .digest(
            (new ByteArrayPlus())
              .append_raw_bytes(encoded_new_nonce)
              .append_raw_bytes(encoded_new_nonce)
              .toByteArray()
          );
        
        //Guessing here that the SHAs might be wrong
        tmp_aes_key = (new ByteArrayPlus())
          .append_raw_bytes(new_nonce_server_nonce_hash)
          .append_raw_bytes_up_to(server_nonce_new_nonce_hash, 12)
          .toByteArray();
        
        tmp_aes_iv = (new ByteArrayPlus())
          .append_raw_bytes_from_up_to(server_nonce_new_nonce_hash, 12, 8)
          .append_raw_bytes(new_nonce_new_nonce_hash)
          .append_raw_bytes(encoded_new_nonce)
          .toByteArray();
      }
      
      byte[] decrypted_data = AES256IGE.decrypt(tmp_aes_key, tmp_aes_iv, encrypted_data);
      byte[] answer_hash = new byte[SHA1.HASH_SIZE];
      int decrypted_skip = 0;
      System.arraycopy(decrypted_data, 0, answer_hash, 0, SHA1.HASH_SIZE);
      decrypted_skip += SHA1.HASH_SIZE;
      System.out.println(Debug.bytes_to_hex(decrypted_data));
      if (Decode.Little.int_decode(decrypted_data, decrypted_skip) == CombinatorIds.server_DH_inner_data) {
        System.out.println("DECRYPTED PROPERLY");
      } else {
        System.out.println("NOT DECRYPTED PROPERLY");
      }
      //server_DH_inner_data#b5890dba nonce:int128 server_nonce:int128 g:int dh_prime:string g_a:string server_time:int = Server_DH_inner_data;
      
      return null;//new RecieveServerDHParamsOk(nonce, server_nonce, pq, server_public_key_fingerprints);
    } else {
      throw new TypeMismatchException("Expected a %(server_DH_params_ok)");
    }
  }

/*
  public RecieveServerDHParamsOk(Integer128 nonce, Integer128 server_nonce, long pq, long[] server_public_key_fingerprints) {
    this.nonce = nonce;
    this.server_nonce = server_nonce;
    this.pq = pq;
    this.server_public_key_fingerprints = server_public_key_fingerprints;
  }*/
}
