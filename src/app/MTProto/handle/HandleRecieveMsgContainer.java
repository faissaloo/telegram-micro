package mtproto.handle;

import mtproto.MTProtoConnection;
import mtproto.recieve.RecieveMsgContainer;
import mtproto.CombinatorIds;
import mtproto.Response;
import mtproto.EncryptedResponse;

public class HandleRecieveMsgContainer extends MTProtoCallback {
  public HandleRecieveMsgContainer(MTProtoConnection connection) {
    super(CombinatorIds.msg_container, connection);
  }
  
  public void execute(Response response) {
    EncryptedResponse encrypted_response = (EncryptedResponse)response;
    RecieveMsgContainer recieve_msg_container = RecieveMsgContainer.from_encrypted_message(encrypted_response);
    System.out.println("Recieved message container, triggering callbacks...");
    for (int i = 0; i < recieve_msg_container.messages.length; i += 1) {
      connection.trigger_callbacks(recieve_msg_container.messages[i]);
    }
  }
}
