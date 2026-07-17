package test;

import media.MediaReference;
import mtproto.CombinatorIds;
import mtproto.RPCResponse;
import mtproto.Serializer;
import mtproto.recieve.messages.RecieveAffectedMessages;
import mtproto.recieve.service.RecieveUpdateShortSentMessage;
import mtproto.recieve.upload.RecieveUploadFile;
import mtproto.send.contacts.SendSearch;
import mtproto.send.channels.SendJoinChannel;
import mtproto.send.channels.SendLeaveChannel;
import mtproto.send.messages.SendDeleteChannelMessages;
import mtproto.send.messages.SendDeleteMessages;
import mtproto.send.messages.SendDiscardEncryption;
import mtproto.send.messages.SendGetDialogs;
import mtproto.send.messages.SendForwardMessages;
import mtproto.send.messages.SendRequestEncryption;
import mtproto.send.messages.SendSendEncryptedService;
import mtproto.send.messages.SendSendMessage;
import mtproto.send.upload.SendUploadGetFile;
import mtproto.send.users.SendGetFullUser;
import support.Decode;

public class OutboundContext {
  public static class BuildSendMessageRequestTest extends Test {
    public String label() {
      return "Can build messages.sendMessage requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendSendMessage(
        SendGetDialogs.DialogInputPeer.user(77L, 0x1122334455667788L),
        "hello",
        0x0102030405060708L
      )).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.messages_send_message);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 0);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_peer_user);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 77L);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x1122334455667788L);
      offset += 8;
      expect((int)(body[offset] & 0xff), 5);
      expect((char)body[offset + 1], 'h');
      expect((char)body[offset + 2], 'e');
      expect((char)body[offset + 3], 'l');
      expect((char)body[offset + 4], 'l');
      expect((char)body[offset + 5], 'o');
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x0102030405060708L);
      offset += 8;
      expect(offset, body.length);
    }
  }

  public static class BuildReplySendMessageRequestTest extends Test {
    public String label() {
      return "Can build reply-aware messages.sendMessage requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendSendMessage(
        SendGetDialogs.DialogInputPeer.user(77L, 0x1122334455667788L),
        "hello",
        0x0102030405060708L,
        91
      )).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.messages_send_message);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 1);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_peer_user);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 77L);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x1122334455667788L);
      offset += 8;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_reply_to_message);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 0);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 91);
      offset += 4;
      expect((int)(body[offset] & 0xff), 5);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x0102030405060708L);
      offset += 8;
      expect(offset, body.length);
    }
  }

  public static class BuildForwardMessagesRequestTest extends Test {
    public String label() {
      return "Can build messages.forwardMessages requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendForwardMessages(
        SendGetDialogs.DialogInputPeer.user(77L, 0x1122334455667788L),
        91,
        0x0102030405060708L,
        SendGetDialogs.DialogInputPeer.channel(88L, 0x8877665544332211L)
      )).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.messages_forward_messages);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 0);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_peer_user);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 77L);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x1122334455667788L);
      offset += 8;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.vector);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 1);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 91);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.vector);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 1);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 0x0102030405060708L);
      offset += 8;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_peer_channel);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 88L);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x8877665544332211L);
      offset += 8;
      expect(offset, body.length);
    }
  }

  public static class ParseUpdateShortSentMessageTest extends Test {
    public String label() {
      return "Can parse updateShortSentMessage send results";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_int(CombinatorIds.update_short_sent_message)
        .append_int(1 << 1)
        .append_int(77)
        .append_int(88)
        .append_int(1)
        .append_int(1700000000)
        .end();
      RecieveUpdateShortSentMessage sent =
        RecieveUpdateShortSentMessage.from_rpc_response(RPCResponse.from_bytes(body));

      expect(sent.outgoing, true);
      expect(sent.messageId, 77);
      expect(sent.pts, 88);
      expect(sent.ptsCount, 1);
      expect(sent.date, 1700000000);
    }
  }

  public static class BuildUploadGetFileRequestTest extends Test {
    public String label() {
      return "Can build upload.getFile requests";
    }

    public void test() throws TestFailureException {
      MediaReference ref = new MediaReference();
      byte[] body;
      int offset = 0;

      ref.setDocument(77L, 0x1122334455667788L, 2, new byte[] {1, 2, 3, 4}, 4, 4096, "image/jpeg");
      body = (new SendUploadGetFile(ref, 4096L, 8192)).message_data.end();

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.upload_get_file);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 0);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_document_file_location);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 77L);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x1122334455667788L);
      offset += 8;
      expect(body[offset] & 0xff, 4);
      offset += 8;
      expect(body[offset] & 0xff, 0);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 4096L);
      offset += 8;
      expect(Decode.Little.int_decode(body, offset), 8192);
      offset += 4;
      expect(offset, body.length);
    }
  }

  public static class ParseUploadFileResponseTest extends Test {
    public String label() {
      return "Can parse upload.File responses";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_int(CombinatorIds.upload_file)
        .append_int(0xaa963b05)
        .append_int(1700000000)
        .append_byte_string(new byte[] {9, 8, 7, 6})
        .end();
      RecieveUploadFile file = RecieveUploadFile.from_rpc_response(RPCResponse.from_bytes(body));

      expect(file.kind, RecieveUploadFile.KIND_FILE);
      expect(file.mtime, 1700000000);
      expect(file.bytes.length, 4);
      expect(file.bytes[0] & 0xff, 9);
      expect(file.bytes[3] & 0xff, 6);
    }
  }

  public static class BuildJoinChannelRequestTest extends Test {
    public String label() {
      return "Can build channels.joinChannel requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendJoinChannel(123L, 0x1122334455667788L)).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.channels_join_channel);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_channel);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 123L);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x1122334455667788L);
      offset += 8;
      expect(offset, body.length);
    }
  }

  public static class BuildLeaveChannelRequestTest extends Test {
    public String label() {
      return "Can build channels.leaveChannel requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendLeaveChannel(123L, 0x1122334455667788L)).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.channels_leave_channel);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_channel);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 123L);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x1122334455667788L);
      offset += 8;
      expect(offset, body.length);
    }
  }

  public static class BuildRequestEncryptionRequestTest extends Test {
    public String label() {
      return "Can build messages.requestEncryption requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendRequestEncryption(77L, 0x1122334455667788L, 55, new byte[] {1, 2, 3, 4})).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.messages_request_encryption);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_user);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 77L);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x1122334455667788L);
      offset += 8;
      expect(Decode.Little.int_decode(body, offset), 55);
      offset += 4;
      expect((int)(body[offset] & 0xff), 4);
      offset += 8;
      expect(offset, body.length);
    }
  }

  public static class BuildDiscardEncryptionRequestTest extends Test {
    public String label() {
      return "Can build messages.discardEncryption requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendDiscardEncryption(77, false)).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.messages_discard_encryption);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 0);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 77);
      offset += 4;
      expect(offset, body.length);
    }
  }

  public static class BuildDeleteMessagesRequestTest extends Test {
    public String label() {
      return "Can build messages.deleteMessages requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendDeleteMessages(77, true)).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.messages_delete_messages);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 1);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.vector);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 1);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 77);
      offset += 4;
      expect(offset, body.length);
    }
  }

  public static class BuildDeleteChannelMessagesRequestTest extends Test {
    public String label() {
      return "Can build channels.deleteMessages requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendDeleteChannelMessages(88L, 0x1122334455667788L, 77)).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.channels_delete_messages);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_channel);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 88L);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x1122334455667788L);
      offset += 8;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.vector);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 1);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 77);
      offset += 4;
      expect(offset, body.length);
    }
  }

  public static class ParseAffectedMessagesResponseTest extends Test {
    public String label() {
      return "Can parse messages.AffectedMessages responses";
    }

    public void test() throws TestFailureException {
      RecieveAffectedMessages affected = RecieveAffectedMessages.from_rpc_response(
        RPCResponse.from_bytes(
          (new Serializer())
            .append_int(CombinatorIds.messages_affected_messages)
            .append_int(901)
            .append_int(1)
            .end()
        )
      );

      expect(affected.pts, 901);
      expect(affected.ptsCount, 1);
    }
  }

  public static class BuildSendEncryptedServiceRequestTest extends Test {
    public String label() {
      return "Can build messages.sendEncryptedService requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendSendEncryptedService(
        77,
        0x1122334455667788L,
        0x0102030405060708L,
        new byte[] {9, 8, 7}
      )).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.messages_send_encrypted_service);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_encrypted_chat);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 77);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 0x1122334455667788L);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x0102030405060708L);
      offset += 8;
      expect((int)(body[offset] & 0xff), 3);
      expect((int)(body[offset + 1] & 0xff), 9);
      expect((int)(body[offset + 2] & 0xff), 8);
      expect((int)(body[offset + 3] & 0xff), 7);
      offset += 4;
      expect(offset, body.length);
    }
  }

  public static class BuildContactsSearchRequestTest extends Test {
    public String label() {
      return "Can build contacts.search requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendSearch("alice", 12)).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.contacts_search);
      offset += 4;
      expect((int)(body[offset] & 0xff), 5);
      expect((char)body[offset + 1], 'a');
      expect((char)body[offset + 2], 'l');
      expect((char)body[offset + 3], 'i');
      expect((char)body[offset + 4], 'c');
      expect((char)body[offset + 5], 'e');
      offset += 8;
      expect(Decode.Little.int_decode(body, offset), 12);
      offset += 4;
      expect(offset, body.length);
    }
  }

  public static class BuildGetFullUserRequestTest extends Test {
    public String label() {
      return "Can build users.getFullUser requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendGetFullUser(77L, 0x1122334455667788L)).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.users_get_full_user);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_user);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 77L);
      offset += 8;
      expect(Decode.Little.long_decode(body, offset), 0x1122334455667788L);
      offset += 8;
      expect(offset, body.length);
    }
  }
}
