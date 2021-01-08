package mtproto.send;

import support.Encode;
import support.Integer128;
import support.Integer256;
import support.ArrayPlus;
import support.ByteArrayPlus;
import support.RandomPlus;

import crypto.RSAPublicKey;
import crypto.SHA1;
import crypto.RSA;
import crypto.SecureRandomPlus;

import support.Debug;
import mtproto.CombinatorIds;
import mtproto.Serializer;
import mtproto.UnencryptedRequest;

public class SendReqDhParams extends SendUnencrypted {
  public SendReqDhParams(SecureRandomPlus random_number_generator, Integer128 nonce, Integer128 server_nonce, long pq, long p, long q, RSAPublicKey public_key, Integer256 new_nonce) {
    byte[] encrypted_data_bytes = RSA.encrypt(public_key, data_with_hash(random_number_generator, p_q_inner_data(nonce, server_nonce, pq, p, q, new_nonce)));
    
    message_data 
      .append_int(CombinatorIds.req_DH_params)
      .append_Integer128(nonce)
      .append_Integer128(server_nonce)
      .append_long_as_byte_string(p)
      .append_long_as_byte_string(q)
      .append_long(public_key.fingerprint)
      .append_byte_string(encrypted_data_bytes);
  }
  
  public static byte[] p_q_inner_data(Integer128 nonce, Integer128 server_nonce, long pq, long p, long q, Integer256 new_nonce) {
    return (new Serializer())
      .append_int(CombinatorIds.p_q_inner_data)
      .append_long_as_byte_string(pq)
      .append_long_as_byte_string(p)
      .append_long_as_byte_string(q)
      .append_Integer128(nonce)
      .append_Integer128(server_nonce)
      .append_Integer256(new_nonce)
      .end();
  }
  
  public static byte[] data_with_hash(SecureRandomPlus random_number_generator, byte[] p_q_inner_data) {
    return (new Serializer())
      .append_raw_bytes((new SHA1()).digest(p_q_inner_data))
      .append_raw_bytes(p_q_inner_data)
      .pad_to_length(255, random_number_generator)
      .end();
  }
}
