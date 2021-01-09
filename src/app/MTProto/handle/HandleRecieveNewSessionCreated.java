package mtproto.handle;

import mtproto.MTProtoConnection;
import mtproto.Response;
import mtproto.EncryptedResponse;
import mtproto.recieve.RecieveNewSessionCreated;
import mtproto.send.SendMsgsAck;
import mtproto.CombinatorIds;

public class HandleRecieveNewSessionCreated extends MTProtoCallback {
  public HandleRecieveNewSessionCreated(MTProtoConnection connection) {
    super(CombinatorIds.new_session_created, connection);
  }
  
  public void execute(Response response) {
    System.out.println("Session created in "+Long.toString(System.currentTimeMillis()-connection.handshake_start)+"ms");
    EncryptedResponse encrypted_response = (EncryptedResponse)response;
    RecieveNewSessionCreated recieve_new_session_created = RecieveNewSessionCreated.from_encrypted_message(encrypted_response);
    System.out.println("NEW SESSION CREATED, sending ack...");
    (new SendMsgsAck(new long[] {encrypted_response.message_id})).send(connection);
  }
}
