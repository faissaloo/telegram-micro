package mtproto.handle;

import mtproto.MTProtoConnection;
import mtproto.Response;
import mtproto.EncryptedResponse;
import mtproto.RPCResponse;
import mtproto.recieve.help.RecieveGzipPacked;
import mtproto.send.service.SendMsgsAck;
import mtproto.CombinatorIds;
import mtproto.TypeMismatchException;
import mtproto.Deserialize;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import compression.Gzip;
import java.io.IOException;
import mtproto.TCPResponse;
import support.ArrayPlus;

public class HandleRecieveGzipPacked extends MTProtoCallback {
  public HandleRecieveGzipPacked(MTProtoConnection connection) {
    super(CombinatorIds.gzip_packed, connection);
  }
  
  public void execute(Response response) throws TypeMismatchException {
    RPCResponse message = (RPCResponse)response;
    //RecieveGzipPacked gzip_packed = RecieveGzipPacked.from_encrypted_message(rpc_response);
    
    int skip = 0;
    byte[] data = message.body;
    int message_type = message.type;
    
    if (message_type == CombinatorIds.gzip_packed) {
      byte[] packed_data = Deserialize.bytes_deserialize(message.body, 0);
      ByteArrayInputStream input_stream = new ByteArrayInputStream(packed_data);
      ByteArrayOutputStream output_stream = new ByteArrayOutputStream();
      try {
        Gzip.gunzip(input_stream, output_stream);
        byte[] a = output_stream.toByteArray();
        //this comes back with just the object inside, so we'll need to abstract
        //that away somehow to deal with such responses so recieve_ can just
        //take the raw objects maybe we should have some kind MTProtoObject
        //class to hold CombinatorIds + data instead of relying on the
        // unencrypted/encrypted containers for messages???
        //at the moment we're keeping the message and object data in the
        //unencrypted/encrypted containers, it would be better to have
        //the MTProtoObject for the inner message be created by it instead
        //since that's all any of the recieve_encrypted static methods
        //are actually interested in anyway
        //https://core.telegram.org/api/datacenter
        System.out.println(ArrayPlus.hex_string(a));
      } catch (IOException e) {
        System.out.println(e);
      }
    } else {
      throw new TypeMismatchException("Expected a %(gzip_packed)");
    }
  }
}
