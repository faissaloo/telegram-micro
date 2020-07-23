package mtproto;
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

public class SendReqDhParams {
  ByteArrayPlus message_data;

  public SendReqDhParams(Integer128 nonce, Integer128 server_nonce, long pq, long p, long q, RSAPublicKey public_key, Integer256 new_nonce) {
    byte[] encrypted_data_bytes = RSA.encrypt(public_key, data_with_hash(p_q_inner_data(nonce, server_nonce, pq, p, q, new_nonce)));
    
    message_data = new ByteArrayPlus();
    message_data.append_int(CombinatorIds.req_DH_params);
    message_data.append_Integer128(nonce);
    message_data.append_Integer128(server_nonce);
    message_data.append_raw_bytes(Serialize.serialize_bytes(ArrayPlus.remove_leading_zeroes(Encode.Big.long_encode(p))));
    message_data.append_raw_bytes(Serialize.serialize_bytes(ArrayPlus.remove_leading_zeroes(Encode.Big.long_encode(q))));
    message_data.append_long(public_key.fingerprint);
    message_data.append_raw_bytes(Serialize.serialize_bytes(encrypted_data_bytes));
  }
  
  public static byte[] p_q_inner_data(Integer128 nonce, Integer128 server_nonce, long pq, long p, long q, Integer256 new_nonce) {
    ByteArrayPlus p_q_inner_data = new ByteArrayPlus();
    {
      p_q_inner_data.append_int(CombinatorIds.p_q_inner_data);
      p_q_inner_data.append_raw_bytes(Serialize.serialize_bytes(ArrayPlus.remove_leading_zeroes(Encode.Big.long_encode(pq))));
      p_q_inner_data.append_raw_bytes(Serialize.serialize_bytes(ArrayPlus.remove_leading_zeroes(Encode.Big.long_encode(p))));
      p_q_inner_data.append_raw_bytes(Serialize.serialize_bytes(ArrayPlus.remove_leading_zeroes(Encode.Big.long_encode(q))));
      p_q_inner_data.append_Integer128(nonce);
      p_q_inner_data.append_Integer128(server_nonce);
      p_q_inner_data.append_Integer256(new_nonce);
    }
    return p_q_inner_data.toByteArray();
  }
  
  public static byte[] data_with_hash(byte[] p_q_inner_data) {
    return (new ByteArrayPlus())
      .append_raw_bytes((new SHA1()).digest(p_q_inner_data));
      .append_raw_bytes(p_q_inner_data);
      .pad_to_length(255, new SecureRandomPlus());
      .toByteArray();
  }

  public void send() {
    (new UnencryptedRequest(message_data.toByteArray())).send();
  }
}
