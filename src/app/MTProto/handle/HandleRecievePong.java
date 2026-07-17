package mtproto.handle;

import mtproto.MTProtoConnection;
import mtproto.Response;
import mtproto.EncryptedResponse;
import mtproto.recieve.service.RecievePong;
import mtproto.CombinatorIds;

public class HandleRecievePong extends MTProtoCallback {
  public HandleRecievePong(MTProtoConnection connection) {
    super(CombinatorIds.pong, connection);
  }

  public void execute(Response response) {
    EncryptedResponse encrypted_response = (EncryptedResponse)response;
    RecievePong recieve_pong = RecievePong.from_encrypted_message(encrypted_response);
    connection.last_pong = recieve_pong;
    System.out.println(
      "PONG: message_id=" + Long.toString(recieve_pong.message_id) +
      " ping_id=" + Long.toString(recieve_pong.ping_id)
    );
  }
}
