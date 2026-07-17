package test;

import bouncycastle.BigInteger;
import mtproto.CombinatorIds;
import mtproto.RPCResponse;
import mtproto.Serializer;
import mtproto.recieve.messages.ReaderResultParser;
import mtproto.recieve.messages.SecretChatResultParser;
import mtproto.secret.SecretChatBootstrap;

public class SecretChatContext {
  public static class BootstrapPublicValueUsesKnownGeneratorTest extends Test {
    public String label() {
      return "Secret chat bootstrap uses the known Telegram generator";
    }

    public void test() throws TestFailureException {
      byte[] value = SecretChatBootstrap.publicValueForExponent(BigInteger.ONE);

      expect(value.length, 1);
      expect((int)(value[0] & 0xff), 3);
    }
  }

  public static class BootstrapPublicValueCanReuseCallerScratchTest extends Test {
    public String label() {
      return "Secret chat bootstrap can reuse caller-provided exponent scratch";
    }

    public void test() throws TestFailureException {
      byte[] scratch = new byte[SecretChatBootstrap.EXPONENT_BYTES];
      byte[] value;

      scratch[scratch.length - 1] = 1;
      value = SecretChatBootstrap.publicValueForExponent(new BigInteger(1, scratch));

      expect(value != null, true);
      expect(value.length, 1);
      expect((int)(value[0] & 0xff), 3);
      expect((int)(scratch[scratch.length - 1] & 0xff), 1);
    }
  }

  public static class ParseEncryptedChatWaitingPayloadTest extends Test {
    public String label() {
      return "Can parse encryptedChatWaiting responses";
    }

    public void test() throws TestFailureException {
      SecretChatResultParser parser = new SecretChatResultParser();
      SecretChatResultParser.Result result = new SecretChatResultParser.Result();
      RPCResponse response = RPCResponse.from_bytes(
        (new Serializer())
          .append_int(CombinatorIds.encrypted_chat_waiting)
          .append_int(77)
          .append_long(0x1122334455667788L)
          .append_int(1700000000)
          .append_long(1001L)
          .append_long(1002L)
          .end()
      );

      expect(parser.parse(response, result), true);
      expect(result.state, SecretChatResultParser.STATE_WAITING);
      expect(result.chatId, 77);
      expect(result.accessHash, 0x1122334455667788L);
      expect(result.adminId, 1001L);
      expect(result.participantId, 1002L);
    }
  }

  public static class ParseEncryptedChatRequestedPayloadTest extends Test {
    public String label() {
      return "Can parse encryptedChatRequested responses";
    }

    public void test() throws TestFailureException {
      SecretChatResultParser parser = new SecretChatResultParser();
      SecretChatResultParser.Result result = new SecretChatResultParser.Result();
      RPCResponse response = RPCResponse.from_bytes(
        (new Serializer())
          .append_int(CombinatorIds.encrypted_chat_requested)
          .append_int(0)
          .append_int(91)
          .append_long(0x8877665544332211L)
          .append_int(1700001234)
          .append_long(2001L)
          .append_long(2002L)
          .append_byte_string(new byte[] {1, 2, 3, 4})
          .end()
      );

      expect(parser.parse(response, result), true);
      expect(result.state, SecretChatResultParser.STATE_REQUESTED);
      expect(result.chatId, 91);
      expect(result.accessHash, 0x8877665544332211L);
      expect(result.adminId, 2001L);
      expect(result.participantId, 2002L);
      expect(result.exchangeBytes[0], (byte)1);
      expect(result.exchangeLength, 4);
    }
  }

  public static class FingerprintUsesLowerSha1BitsTest extends Test {
    public String label() {
      return "Secret chat shared-key fingerprint uses lower SHA-1 bits";
    }

    public void test() throws TestFailureException {
      long fingerprint = SecretChatBootstrap.fingerprintForSharedKey(new byte[] {1, 2, 3, 4});

      expect(fingerprint, 8013942524700532807L);
    }
  }

  public static class ParseWrappedShortSecretUpdateTest extends Test {
    public String label() {
      return "Can parse wrapped short secret chat updates";
    }

    public void test() throws TestFailureException {
      ReaderResultParser parser = new ReaderResultParser(4, 4, 4);
      SecretChatResultParser.Result result = new SecretChatResultParser.Result();
      byte[] body = (new Serializer())
        .append_int(CombinatorIds.update_encryption)
        .append_int(CombinatorIds.encrypted_chat_requested)
        .append_int(0)
        .append_int(91)
        .append_long(0x8877665544332211L)
        .append_int(1700001234)
        .append_long(2001L)
        .append_long(2002L)
        .append_byte_string(new byte[] {9, 8, 7})
        .append_int(1700001235)
        .append_int(1700001236)
        .end();

      expect(parser.parseShortSecretUpdate(CombinatorIds.update_short, body, result), true);
      expect(result.state, SecretChatResultParser.STATE_REQUESTED);
      expect(result.chatId, 91);
      expect(result.date, 1700001234);
      expect(result.exchangeLength, 3);
      expect(result.exchangeBytes[0], (byte)9);
    }
  }
}
