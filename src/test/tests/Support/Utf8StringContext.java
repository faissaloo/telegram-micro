package test;

import mtproto.Deserialize;
import mtproto.Serializer;
import support.Utf8String;

public class Utf8StringContext {
  public static class DecodeUtf8TextTest extends Test {
    public String label() {
      return "It decodes UTF-8 text without collapsing non-ASCII characters";
    }

    public void test() throws TestFailureException {
      Utf8String value = new Utf8String(new byte[] {
        (byte)0xD0, (byte)0x9F, (byte)0xD1, (byte)0x80, (byte)0xD0, (byte)0xB8,
        (byte)0xD0, (byte)0xB2, (byte)0xD0, (byte)0xB5, (byte)0xD1, (byte)0x82,
        (byte)0x20, (byte)0xE2, (byte)0x82, (byte)0xAC
      });

      expect(value.string(), "\u041F\u0440\u0438\u0432\u0435\u0442 \u20AC");
    }
  }

  public static class EncodeUtf8TextTest extends Test {
    public String label() {
      return "It encodes Telegram strings as UTF-8 bytes";
    }

    public void test() throws TestFailureException {
      byte[] encoded = Utf8String.encode("\u041F\u0440\u0438\u0432\u0435\u0442 \u20AC");

      expect(encoded, new byte[] {
        (byte)0xD0, (byte)0x9F, (byte)0xD1, (byte)0x80, (byte)0xD0, (byte)0xB8,
        (byte)0xD0, (byte)0xB2, (byte)0xD0, (byte)0xB5, (byte)0xD1, (byte)0x82,
        (byte)0x20, (byte)0xE2, (byte)0x82, (byte)0xAC
      });
    }
  }

  public static class SerializerRoundTripsUtf8TextTest extends Test {
    public String label() {
      return "It round-trips UTF-8 strings through the Telegram serializer";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer()).append_string("\u041C\u0438\u0440 \uD83D\uDE80").end();
      Utf8String decoded = Deserialize.utf8string_deserialize(body, 0);

      expect(decoded.string(), "\u041C\u0438\u0440 \uD83D\uDE80");
    }
  }
}
