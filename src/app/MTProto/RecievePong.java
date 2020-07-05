package mtproto;

import support.Decode;

public class RecievePong {
  public long message_id;
  public long ping_id;
  
  public static RecievePong from_unencrypted_message(UnencryptedResponse message) throws TypeMismatchException {
    int skip = 0;
    byte[] data = message.data();
    int message_type = message.type();
    
    if (message_type == CombinatorIds.pong) {
      long message_id = Decode.Little.long_decode(Deserialize.bytes_deserialize(data, skip), 0);
      skip += 8;
      long ping_id = Decode.Little.long_decode(Deserialize.bytes_deserialize(data, skip), 0);
      return new RecievePong(message_id, ping_id);
    } else {
      throw new TypeMismatchException("Expected a %(pong)");
    }
  }
  
  public RecievePong(long message_id, long ping_id) {
    this.message_id = message_id;
    this.ping_id = ping_id;
  }
}
