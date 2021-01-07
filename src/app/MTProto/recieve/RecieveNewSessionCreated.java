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

import mtproto.EncryptedResponse;
import mtproto.TypeMismatchException;
import mtproto.CombinatorIds;
import mtproto.Deserialize;

public class RecieveNewSessionCreated {
  public static RecieveNewSessionCreated from_encrypted_message(EncryptedResponse message) throws TypeMismatchException {
    int skip = 0;
    byte[] data = message.data;
    int message_type = message.type;

    if (message_type == CombinatorIds.new_session_created) {
      long first_msg_id = Decode.Little.long_decode(data, skip);
      skip += 8;
      long unique_id = Decode.Little.long_decode(data, skip);
      skip += 8;
      long server_salt = Decode.Little.long_decode(data, skip);
      skip += 8;
      return new RecieveNewSessionCreated(first_msg_id, unique_id, server_salt);
    } else {
      throw new TypeMismatchException("Expected a %(new_session_created)");
    }
  }
  
  public long first_msg_id;
  public long unique_id;
  public long server_salt;
  
  //new_session_created#9ec20908 first_msg_id:long unique_id:long server_salt:long = NewSession;
  public RecieveNewSessionCreated(long first_msg_id, long unique_id, long server_salt) {
    this.first_msg_id = first_msg_id;
    this.unique_id = unique_id;
    this.server_salt = server_salt;
  }
}
