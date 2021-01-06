package mtproto.recieve;

import bouncycastle.BigInteger;

import support.Integer128;
import support.Integer256;
import support.Utf8String;
import support.ByteArrayPlus;
import support.Decode;
import support.Encode;
import support.Debug;
import support.ArrayPlus;

import crypto.SHA1;
import crypto.AES256IGE;

import mtproto.EncryptedResponse;
import mtproto.TypeMismatchException;
import mtproto.CombinatorIds;
import mtproto.Deserialize;

public class RecieveMsgContainer {
  public static RecieveMsgContainer from_encrypted_message(EncryptedResponse message) throws TypeMismatchException {
    int skip = 0;
    byte[] data = message.data;
    int message_type = message.type;
    /*
msg_container#73f1f8dc messages:vector<%Message> = MessageContainer;
message msg_id:long seqno:int bytes:int body:Object = Message;
msg_copy#e06046b2 orig_message:Message = MessageCopy;
    */
    if (message_type == CombinatorIds.msg_container) {
      int length = Decode.Little.int_decode(data, skip);

      skip += 4;
      EncryptedResponse messages[] = new EncryptedResponse[length];
      for (int i = 0; i < length; i++) {
        long message_id = Decode.Little.long_decode(data, skip);
        skip += 8;
        int sequence_number = Decode.Little.int_decode(data, skip);
        skip += 4;
        int message_length = Decode.Little.int_decode(data, skip);
        skip += 4;
        int type = Decode.Little.int_decode(data, skip);
        skip += 4;
        byte[] message_content = ArrayPlus.subarray(data, skip, message_length-4);
        skip += message_length-4;
        messages[i] = new EncryptedResponse(message.salt, message.session_id, message_id, sequence_number, type, data);
      }
      return new RecieveMsgContainer(messages);
    } else {
      throw new TypeMismatchException("Expected a %(msg_container)");
    }
  }
  
  public EncryptedResponse messages[];
  
  public RecieveMsgContainer(EncryptedResponse messages[]) {
    this.messages = messages;
  }
}
