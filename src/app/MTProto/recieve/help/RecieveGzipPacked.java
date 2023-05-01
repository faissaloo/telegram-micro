package mtproto.recieve.help;

import support.Decode;

import mtproto.CombinatorIds;
import mtproto.EncryptedResponse;
import mtproto.TypeMismatchException;
import mtproto.RPCResponse;
import mtproto.Deserialize;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import compression.Gzip;
import java.io.IOException;

public class RecieveGzipPacked {
  public static RecieveGzipPacked from_encrypted_message(RPCResponse message) throws TypeMismatchException {
    int skip = 0;
    byte[] data = message.body;
    int message_type = message.type;
    
    if (message_type == CombinatorIds.gzip_packed) {
      byte[] packed_data = Deserialize.bytes_deserialize(message.body, 0);
      ByteArrayInputStream input_stream = new ByteArrayInputStream(packed_data);
      ByteArrayOutputStream output_stream = new ByteArrayOutputStream();
      try {
        Gzip.gunzip(input_stream, output_stream);
      } catch (IOException e) {
        System.out.println(e);
      }
      
      return new RecieveGzipPacked(output_stream.toByteArray());
    } else {
      throw new TypeMismatchException("Expected a %(gzip_packed)");
    }
  }
  
  byte[] inflated_data;
  public RecieveGzipPacked(byte[] inflated_data) {
    this.inflated_data = inflated_data;
  }
}
