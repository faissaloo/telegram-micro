package test;

import mtproto.Serialize;

public class SerializeContext {
  public static class SerializeBytesTest extends Test {
    public String label() {
      return "Can serialize bytes";
    }

    public void test() throws TestFailureException {
      byte[] result = Serialize.serialize_bytes(new byte[] {(byte)0x25, (byte)0x6F, (byte)0x11});
      expect(result, new byte[] {(byte)0x03, (byte)0x25, (byte)0x6F, (byte)0x11, (byte)0x00});
      result = Serialize.serialize_bytes(new byte[] {(byte)0x25, (byte)0x6F, (byte)0x11, (byte)0x66});
      expect(result, new byte[] {(byte)0x04, (byte)0x25, (byte)0x6F, (byte)0x11, (byte)0x66});
      result = Serialize.serialize_bytes(new byte[] {(byte)0x25, (byte)0x6F, (byte)0x11, (byte)0x66, (byte)0xFF});
      expect(result, new byte[] {(byte)0x05, (byte)0x25, (byte)0x6F, (byte)0x11, (byte)0x66, (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x00});
    }
  }
}
