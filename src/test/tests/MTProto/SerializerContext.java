package test;

import mtproto.Serializer;

public class SerializerContext {
  public static class AppendByteStringTest extends Test {
    public String label() {
      return "Can append serialized bytes";
    }

    public void test() throws TestFailureException {
      byte[] result = (new Serializer()).append_byte_string(new byte[] {(byte)0x25, (byte)0x6F, (byte)0x11}).end();
      expect(result, new byte[] {(byte)0x03, (byte)0x25, (byte)0x6F, (byte)0x11});
      result = (new Serializer()).append_byte_string(new byte[] {(byte)0x25, (byte)0x6F, (byte)0x11, (byte)0x66}).end();
      expect(result, new byte[] {(byte)0x04, (byte)0x25, (byte)0x6F, (byte)0x11, (byte)0x66, (byte)0x00, (byte)0x00, (byte)0x00});
      result = (new Serializer()).append_byte_string(new byte[] {(byte)0x25, (byte)0x6F, (byte)0x11, (byte)0x66, (byte)0xFF}).end();
      expect(result, new byte[] {(byte)0x05, (byte)0x25, (byte)0x6F, (byte)0x11, (byte)0x66, (byte)0xFF, (byte)0x00, (byte)0x00});
    }
  }
}
