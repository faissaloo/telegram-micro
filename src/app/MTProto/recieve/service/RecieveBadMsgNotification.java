package mtproto.recieve.service;

import bouncycastle.BigInteger;

import support.Integer128;
import support.Integer256;
import support.Utf8String;
import support.ByteArrayPlus;
import support.Decode;
import support.Encode;
import support.Debug;

import crypto.SHA1;
import crypto.AES256IGE;

import mtproto.EncryptedResponse;
import mtproto.TypeMismatchException;
import mtproto.CombinatorIds;
import mtproto.Deserialize;

//bad_msg_notification#a7eff811 bad_msg_id:long bad_msg_seqno:int error_code:int = BadMsgNotification;
public class RecieveBadMsgNotification {
  /*
    16: msg_id too low (most likely, client time is wrong; it would be worthwhile to synchronize it using msg_id notifications and re-send the original message with the “correct” msg_id or wrap it in a container with a new msg_id if the original message had waited too long on the client to be transmitted)
    17: msg_id too high (similar to the previous case, the client time has to be synchronized, and the message re-sent with the correct msg_id)
    18: incorrect two lower order msg_id bits (the server expects client message msg_id to be divisible by 4)
    19: container msg_id is the same as msg_id of a previously received message (this must never happen)
    20: message too old, and it cannot be verified whether the server has received a message with this msg_id or not
    32: msg_seqno too low (the server has already received a message with a lower msg_id but with either a higher or an equal and odd seqno)
    33: msg_seqno too high (similarly, there is a message with a higher msg_id but with either a lower or an equal and odd seqno)
    34: an even msg_seqno expected (irrelevant message), but odd received
    35: odd msg_seqno expected (relevant message), but even received
    48: incorrect server salt (in this case, the bad_server_salt response is received with the correct salt, and the message is to be re-sent with it)
    64: invalid container.
*/
  public static RecieveBadMsgNotification from_encrypted_message(EncryptedResponse message) throws TypeMismatchException {
    int skip = 0;
    byte[] data = message.data;
    int message_type = message.type;

    if (message_type == CombinatorIds.bad_msg_notification) {
      // bad_msg_id:long bad_msg_seqno:int error_code:int
      long bad_msg_id = Decode.Little.long_decode(data, skip);
      skip += 8;
      int bad_msg_seqno = Decode.Little.int_decode(data, skip);
      skip += 4;
      int error_code = Decode.Little.int_decode(data, skip);
      skip += 4;
      return new RecieveBadMsgNotification(bad_msg_id, bad_msg_seqno, error_code);
    } else {
      throw new TypeMismatchException("Expected a %(bad_msg_notification)");
    }
  }
  
  public long bad_msg_id;
  public int bad_msg_seqno;
  public int error_code;
  
  //new_session_created#9ec20908 first_msg_id:long unique_id:long server_salt:long = NewSession;
  public RecieveBadMsgNotification(long bad_msg_id, int bad_msg_seqno, int error_code) {
    this.bad_msg_id = bad_msg_id;
    this.bad_msg_seqno = bad_msg_seqno;
    this.error_code = error_code;
  }
}
