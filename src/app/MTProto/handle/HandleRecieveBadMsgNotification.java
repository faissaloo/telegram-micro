package mtproto.handle;

import mtproto.MTProtoConnection;
import mtproto.Response;
import mtproto.EncryptedResponse;
import mtproto.recieve.RecieveBadMsgNotification;
import mtproto.send.SendMsgsAck;
import mtproto.CombinatorIds;

public class HandleRecieveBadMsgNotification extends MTProtoCallback {
  public HandleRecieveBadMsgNotification(MTProtoConnection connection) {
    super(CombinatorIds.bad_msg_notification, connection);
  }
  
  public void execute(Response response) {
    EncryptedResponse encrypted_response = (EncryptedResponse)response;
    RecieveBadMsgNotification bad_msg_notification = RecieveBadMsgNotification.from_encrypted_message(encrypted_response);
    System.out.println("ERROR: BAD MSG; ID = "+Long.toString(bad_msg_notification.bad_msg_id)+"; SEQNO = "+Integer.toString(bad_msg_notification.bad_msg_seqno)+"; ERROR_CODE = "+Integer.toString(bad_msg_notification.error_code));
  }
}
