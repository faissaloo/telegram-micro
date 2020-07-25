package mtproto.recieve;

import bouncycastle.BigInteger;

import support.Integer128;
import support.Integer256;
import support.Utf8String;
import support.ByteArrayPlus;
import support.Decode;
import support.Encode;
import support.Debug;

import crypto.SHA1;
import crypto.AES256IGE;

import mtproto.UnencryptedResponse;
import mtproto.TypeMismatchException;
import mtproto.CombinatorIds;
import mtproto.Deserialize;

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
      if (Decode.Little.int_decode(decrypted_data, decrypted_skip) == CombinatorIds.server_DH_inner_data) {
        decrypted_skip += 4;
        nonce = Decode.Little.Integer128_decode(decrypted_data, decrypted_skip);
        decrypted_skip += 16;
        server_nonce = Decode.Little.Integer128_decode(decrypted_data, decrypted_skip);
        decrypted_skip += 16;
        //https://en.wikipedia.org/wiki/Cyclic_group
        int group_generator = Decode.Little.int_decode(decrypted_data, decrypted_skip);
        decrypted_skip += 4;
        BigInteger diffie_hellman_prime = new BigInteger(1, Deserialize.bytes_deserialize(decrypted_data, decrypted_skip));
        decrypted_skip += Deserialize.bytes_length_deserialize(decrypted_data, decrypted_skip);
        BigInteger group_generator_power_a = new BigInteger(1, Deserialize.bytes_deserialize(decrypted_data, decrypted_skip));
        decrypted_skip += Deserialize.bytes_length_deserialize(decrypted_data, decrypted_skip);
        int server_time = Decode.Little.int_decode(decrypted_data, decrypted_skip);
        decrypted_skip += 4;
        return new RecieveServerDHParamsOk(nonce, server_nonce, group_generator, diffie_hellman_prime, group_generator_power_a, server_time, tmp_aes_key, tmp_aes_iv);
      } else {
        throw new TypeMismatchException("Expected a %(server_DH_inner_data)");
      }
    } else {
      throw new TypeMismatchException("Expected a %(server_DH_params_ok)");
    }
  }
  
  public Integer128 nonce;
  public Integer128 server_nonce;
  public int group_generator;
  public BigInteger diffie_hellman_prime;
  public BigInteger group_generator_power_a;
  public int server_time;
  public byte[] tmp_aes_iv;
  public byte[] tmp_aes_key;
  
  public RecieveServerDHParamsOk(Integer128 nonce, Integer128 server_nonce, int group_generator, BigInteger diffie_hellman_prime, BigInteger group_generator_power_a, int server_time, byte[] tmp_aes_key, byte[] tmp_aes_iv) {
    this.nonce = nonce;
    this.server_nonce = server_nonce;
    this.group_generator = group_generator;
    this.diffie_hellman_prime = diffie_hellman_prime;
    this.group_generator_power_a = group_generator_power_a;
    this.server_time = server_time;
    this.tmp_aes_iv = tmp_aes_iv;
    this.tmp_aes_key = tmp_aes_key;
  }
}
