package mtproto.handle;

import mtproto.MTProtoConnection;
import mtproto.Response;
import mtproto.EncryptedResponse;
import mtproto.recieve.service.RecieveBadMsgNotification;
import mtproto.send.service.SendMsgsAck;
import mtproto.CombinatorIds;

public class HandleRecieveBadMsgNotification extends MTProtoCallback {
  public HandleRecieveBadMsgNotification(MTProtoConnection connection) {
    this(CombinatorIds.bad_msg_notification, connection);
  }

  public HandleRecieveBadMsgNotification(int combinator_id, MTProtoConnection connection) {
    super(combinator_id, connection);
  }
  
  public void execute(Response response) {
    EncryptedResponse encrypted_response = (EncryptedResponse)response;
    RecieveBadMsgNotification bad_msg_notification = RecieveBadMsgNotification.from_encrypted_message(encrypted_response);
    connection.noteRejectedMessageId(bad_msg_notification.bad_msg_id);
    if (bad_msg_notification.has_new_server_salt) {
      connection.server_salt = bad_msg_notification.new_server_salt;
    }

    if ((bad_msg_notification.has_new_server_salt ||
        bad_msg_notification.error_code == 16 ||
        bad_msg_notification.error_code == 17) &&
        connection.retryPendingRequest()) {
      System.out.println(
        "ERROR: BAD MSG; ID = "+Long.toString(bad_msg_notification.bad_msg_id)+
        "; SEQNO = "+Integer.toString(bad_msg_notification.bad_msg_seqno)+
        "; ERROR_CODE = "+Integer.toString(bad_msg_notification.error_code)+
        "; RETRY = 1"
      );
      return;
    }

    System.out.println(
      "ERROR: BAD MSG; ID = "+Long.toString(bad_msg_notification.bad_msg_id)+
      "; SEQNO = "+Integer.toString(bad_msg_notification.bad_msg_seqno)+
      "; ERROR_CODE = "+Integer.toString(bad_msg_notification.error_code)
    );
  }
}
