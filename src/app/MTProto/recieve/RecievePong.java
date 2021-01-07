package mtproto.recieve;

import support.Decode;

import mtproto.CombinatorIds;
import mtproto.EncryptedResponse;
import mtproto.TypeMismatchException;
import mtproto.Deserialize;

public class RecievePong {
  public static RecievePong from_encrypted_message(EncryptedResponse message) throws TypeMismatchException {
    int skip = 0;
    byte[] data = message.data;
    int message_type = message.type;
    
    if (message_type == CombinatorIds.pong) {
      long message_id = Decode.Little.long_decode(data, skip);
      skip += 8;
      long ping_id = Decode.Little.long_decode(data, skip);
      return new RecievePong(message_id, ping_id);
    } else {
      throw new TypeMismatchException("Expected a %(pong)");
    }
  }
  
  public long message_id;
  public long ping_id;
  
  public RecievePong(long message_id, long ping_id) {
    this.message_id = message_id;
    this.ping_id = ping_id;
  }
}
