package mtproto.handle;

import mtproto.MTProtoConnection;
import mtproto.Response;
import mtproto.EncryptedResponse;
import mtproto.RPCResponse;
import mtproto.CombinatorIds;
import mtproto.TypeMismatchException;
import mtproto.Deserialize;
import java.io.ByteArrayInputStream;
import compression.Gzip;
import java.io.IOException;
import mtproto.ProtocolLimits;
import support.BoundedByteArrayOutputStream;

public class HandleRecieveGzipPacked extends MTProtoCallback {
  private boolean inflating;

  public HandleRecieveGzipPacked(MTProtoConnection connection) {
    super(CombinatorIds.gzip_packed, connection);
  }
  
  public void execute(Response response) throws TypeMismatchException {
    RPCResponse message = (RPCResponse)response;
    byte[] data = message.body;
    int message_type = message.type;
    
    if (message_type == CombinatorIds.gzip_packed) {
      if (inflating) {
        System.out.println("Nested gzip payload rejected.");
        return;
      }
      byte[] packed_data = Deserialize.bytes_deserialize(message.body, 0);
      ByteArrayInputStream input_stream = new ByteArrayInputStream(packed_data);
      BoundedByteArrayOutputStream output_stream = new BoundedByteArrayOutputStream(
        packed_data.length < 1024 ? 1024 : packed_data.length,
        ProtocolLimits.MAX_INFLATED_BYTES
      );
      try {
        inflating = true;
        Gzip.gunzip(input_stream, output_stream);
        byte[] unpacked = output_stream.toByteArray();
        connection.trigger_callbacks(RPCResponse.from_bytes(unpacked));
      } catch (IOException e) {
        System.out.println(e);
      } finally {
        inflating = false;
      }
    } else {
      throw new TypeMismatchException("Expected a %(gzip_packed)");
    }
  }
}
