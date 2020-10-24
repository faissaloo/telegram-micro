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

public class RecieveServerDHGenOk {
  public static RecieveServerDHGenOk from_unencrypted_message(UnencryptedResponse message) throws TypeMismatchException {
    int skip = 0;
    byte[] data = message.data();
    int message_type = message.type();

    if (message_type == CombinatorIds.dh_gen_ok) {
      Integer128 nonce = Decode.Little.Integer128_decode(data, skip);
      skip += 16;
      Integer128 server_nonce = Decode.Little.Integer128_decode(data, skip);
      skip += 16;
      Integer128 new_nonce_hash1 = Decode.Little.Integer128_decode(data, skip);
      return new RecieveServerDHGenOk(nonce, server_nonce, new_nonce_hash1);
    } else {
      throw new TypeMismatchException("Expected a %(DH_gen_ok)");
    }
  }
  
  public Integer128 nonce;
  public Integer128 server_nonce;
  public Integer128 new_nonce_hash1;
  
  public RecieveServerDHGenOk(Integer128 nonce, Integer128 server_nonce, Integer128 new_nonce_hash1) {
    this.nonce = nonce;
    this.server_nonce = server_nonce;
    this.new_nonce_hash1 = server_nonce;
  }
}
