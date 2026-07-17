package test;

import config.RuntimeDeviceProfile;
import mtproto.CombinatorIds;
import mtproto.EncryptedResponse;
import mtproto.MTProtoConnection;
import mtproto.RPCResponse;
import mtproto.Deserialize;
import mtproto.recieve.RecieveMsgContainer;
import mtproto.recieve.service.RecieveBadMsgNotification;
import mtproto.recieve.service.RecieveFutureSalts;
import mtproto.recieve.service.RecievePong;
import mtproto.send.service.SendMsgsAck;

import support.Decode;

public class MTProtoConnectionContext {
  public static class ClientMessageIdUsesSavedOffsetTest extends Test {
    public String label() {
      return "Client message ids apply the saved transport offset";
    }

    public void test() throws TestFailureException {
      long messageId = MTProtoConnection.clientMessageId(2500L, 10, 0L);

      expect(messageId, (12L << 32) | 0x80000000L);
    }
  }

  public static class ClientMessageIdStaysMonotonicTest extends Test {
    public String label() {
      return "Client message ids stay monotonic when local time goes backward";
    }

    public void test() throws TestFailureException {
      long first = MTProtoConnection.clientMessageId(1500L, 0, 0L);
      long second = MTProtoConnection.clientMessageId(1000L, 0, first);

      expect(second, first + 4L);
    }
  }

  public static class WireSeqNoMatchesContentRulesTest extends Test {
    public String label() {
      return "Wire seqnos follow MTProto content-related rules";
    }

    public void test() throws TestFailureException {
      expect(MTProtoConnection.wireSeqNo(0, false), 0);
      expect(MTProtoConnection.wireSeqNo(0, true), 1);
      expect(MTProtoConnection.wireSeqNo(3, false), 6);
      expect(MTProtoConnection.wireSeqNo(3, true), 7);
    }
  }

  public static class ParseBadServerSaltNotificationTest extends Test {
    public String label() {
      return "Can parse bad_server_salt notifications";
    }

    public void test() throws TestFailureException {
      EncryptedResponse response = new EncryptedResponse(
        0L,
        1L,
        2L,
        3,
        CombinatorIds.bad_server_salt,
        (new mtproto.Serializer())
          .append_long(0x0102030405060708L)
          .append_int(9)
          .append_int(48)
          .append_long(0x1122334455667788L)
          .end()
      );
      RecieveBadMsgNotification parsed = RecieveBadMsgNotification.from_encrypted_message(response);

      expect(parsed.bad_msg_id, 0x0102030405060708L);
      expect(parsed.bad_msg_seqno, 9);
      expect(parsed.error_code, 48);
      expect(parsed.has_new_server_salt, true);
      expect(parsed.new_server_salt, 0x1122334455667788L);
    }
  }

  public static class ParseFutureSaltsRpcTest extends Test {
    public String label() {
      return "Can parse future_salts RPC payloads";
    }

    public void test() throws TestFailureException {
      RPCResponse response = new RPCResponse(
        CombinatorIds.future_salts,
        (new mtproto.Serializer())
          .append_long(0x0102030405060708L)
          .append_int(100)
          .append_int(CombinatorIds.vector)
          .append_int(2)
          .append_int(CombinatorIds.future_salt)
          .append_int(90)
          .append_int(120)
          .append_long(0x1111111111111111L)
          .append_int(CombinatorIds.future_salt)
          .append_int(121)
          .append_int(180)
          .append_long(0x2222222222222222L)
          .end()
      );
      RecieveFutureSalts parsed = RecieveFutureSalts.from_rpc_response(response);

      expect(parsed.req_msg_id, 0x0102030405060708L);
      expect(parsed.now, 100);
      expect(parsed.salts.length, 2);
      expect(parsed.salts[0].valid_since, 90);
      expect(parsed.salts[0].valid_until, 120);
      expect(parsed.salts[0].salt, 0x1111111111111111L);
      expect(parsed.salts[1].valid_since, 121);
      expect(parsed.salts[1].valid_until, 180);
      expect(parsed.salts[1].salt, 0x2222222222222222L);
    }
  }

  public static class ParseMsgContainerNestedBodiesTest extends Test {
    public String label() {
      return "Message containers preserve nested message bodies";
    }

    public void test() throws TestFailureException {
      byte[] pongBody = (new mtproto.Serializer())
        .append_long(0x0102030405060708L)
        .append_long(0x1122334455667788L)
        .end();
      byte[] badSaltBody = (new mtproto.Serializer())
        .append_long(0x1112131415161718L)
        .append_int(7)
        .append_int(48)
        .append_long(0x2122232425262728L)
        .end();
      byte[] containerBody = (new mtproto.Serializer())
        .append_int(2)
        .append_long(0x100L)
        .append_int(0)
        .append_int(pongBody.length + 4)
        .append_int(CombinatorIds.pong)
        .append_raw_bytes(pongBody)
        .append_long(0x104L)
        .append_int(1)
        .append_int(badSaltBody.length + 4)
        .append_int(CombinatorIds.bad_server_salt)
        .append_raw_bytes(badSaltBody)
        .end();
      RecieveMsgContainer parsed = RecieveMsgContainer.from_encrypted_message(
        new EncryptedResponse(9L, 10L, 11L, 12, CombinatorIds.msg_container, containerBody)
      );
      RecievePong pong = RecievePong.from_encrypted_message(parsed.messages[0]);
      RecieveBadMsgNotification badSalt = RecieveBadMsgNotification.from_encrypted_message(parsed.messages[1]);

      expect(parsed.messages.length, 2);
      expect(parsed.messages[0].type, CombinatorIds.pong);
      expect(parsed.messages[1].type, CombinatorIds.bad_server_salt);
      expect(pong.message_id, 0x0102030405060708L);
      expect(pong.ping_id, 0x1122334455667788L);
      expect(badSalt.bad_msg_id, 0x1112131415161718L);
      expect(badSalt.bad_msg_seqno, 7);
      expect(badSalt.error_code, 48);
      expect(badSalt.has_new_server_salt, true);
      expect(badSalt.new_server_salt, 0x2122232425262728L);
    }
  }

  public static class DefaultHeaderPinsCurrentApiLayerTest extends Test {
    public String label() {
      return "Default initConnection header pins the current API layer";
    }

    public void test() throws TestFailureException {
      byte[] header;
      int offset = 0;
      RuntimeDeviceProfile profile = RuntimeDeviceProfile.detect();

      header = MTProtoConnection.buildDefaultMTProtoHeader(null);

      expect(Decode.Little.int_decode(header, offset), CombinatorIds.invoke_with_layer);
      offset += 4;
      expect(Decode.Little.int_decode(header, offset), MTProtoConnection.DEFAULT_API_LAYER);
      offset += 4;
      expect(Decode.Little.int_decode(header, offset), CombinatorIds.init_connection);
      offset += 4;
      expect(Decode.Little.int_decode(header, offset), 0);
      offset += 4;
      expect(Decode.Little.int_decode(header, offset), 487068);
      offset += 4;

      try {
        expect(Deserialize.utf8string_deserialize(header, offset).ascii_string(), profile.deviceModel);
      } catch (Exception e) {
        throw new TestFailureException("Could not decode device model from initConnection header");
      }
      offset += Deserialize.utf8string_length_deserialize(header, offset);

      try {
        expect(Deserialize.utf8string_deserialize(header, offset).ascii_string(), profile.systemVersion);
      } catch (Exception e) {
        throw new TestFailureException("Could not decode system version from initConnection header");
      }
    }
  }

  public static class MsgsAckSkipsApiHeaderTest extends Test {
    public String label() {
      return "msgs_ack stays outside the initConnection API header";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendMsgsAck(new long[] {0x0102030405060708L})).message_data.end();
      int offset = 0;

      expect(SendMsgsAck.INCLUDE_API_HEADER, false);
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.msgs_ack);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.vector);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 1);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 0x0102030405060708L);
      offset += 8;
      expect(offset, body.length);
    }
  }
}
