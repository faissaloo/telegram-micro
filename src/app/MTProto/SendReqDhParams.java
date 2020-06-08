package mtproto;
import support.Encode;
import support.Integer128;
import support.Integer256;
import support.ByteArrayPlus;
import crypto.RSAPublicKey;
import crypto.SHA1;
import crypto.RSA;

import support.Debug;

public class SendReqDhParams {
  ByteArrayPlus message_data;

  public SendReqDhParams(Integer128 nonce, Integer128 server_nonce, long pq, long p, long q, RSAPublicKey public_key, Integer256 new_nonce) {
    ByteArrayPlus p_q_inner_data = new ByteArrayPlus();
    {
      p_q_inner_data.append_int(0x83c95aec); //p_q_inner_data#83c95aec combinator_id
      //we probably have to strip leading zeroes from these
      p_q_inner_data.append_raw_bytes(Serialize.serialize_bytes(Encode.Big.long_encode(pq)));
      p_q_inner_data.append_raw_bytes(Serialize.serialize_bytes(Encode.Big.long_encode(p)));
      p_q_inner_data.append_raw_bytes(Serialize.serialize_bytes(Encode.Big.long_encode(q)));
      p_q_inner_data.append_Integer128(nonce);
      p_q_inner_data.append_Integer128(server_nonce);
      p_q_inner_data.append_Integer256(new_nonce);
    }

    ByteArrayPlus data_with_hash = new ByteArrayPlus();
    {
      data_with_hash.append_raw_bytes((new SHA1()).digest(p_q_inner_data.toByteArray()));
      data_with_hash.append_raw_bytes(p_q_inner_data.toByteArray());
      int padding_needed = 255 - data_with_hash.size();
      for (int i = 0; i < padding_needed; i++) {
        data_with_hash.append_byte((byte)0);
      }
    }
    byte[] encrypted_data_bytes = RSA.encrypt(public_key, data_with_hash.toByteArray());
    //System.out.println("ENCRYPTED DATA LENGTH: " + Integer.toString(encrypted_data_bytes.length));
    System.out.println("MODULUS: "+Debug.bytes_to_hex(public_key.modulus.magnitudeToBytes()));
    System.out.println("UNENCRYPTED DATA BYTES: "+Debug.bytes_to_hex(data_with_hash.toByteArray()));
    //The encrypted data seems to be utterly wrong... Is it an endianness issue?
    System.out.println("ENCRYPTED DATA BYTES: "+Debug.bytes_to_hex(encrypted_data_bytes));
    System.out.println("ENCRYPTED DATA BYTES LENGTH: "+encrypted_data_bytes.length);

    message_data = new ByteArrayPlus();
    message_data.append_int(0xd712e4be); //combinator_id
    message_data.append_Integer128(nonce);
    message_data.append_Integer128(server_nonce);
    //we probably have to strip leading zeroes from these
    message_data.append_raw_bytes(Serialize.serialize_bytes(Encode.Big.long_encode(p)));
    message_data.append_raw_bytes(Serialize.serialize_bytes(Encode.Big.long_encode(q)));
    message_data.append_long(public_key.fingerprint);
    message_data.append_raw_bytes(Serialize.serialize_bytes(encrypted_data_bytes));
  }

  public void send() {
    (new UnencryptedRequest(message_data.toByteArray())).send();
  }
}
