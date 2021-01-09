package test;

import mtproto.handle.HandleRecieveServerDHGenOk;
import support.Integer128;
import support.Integer256;

public class HandleRecieveServerDHGenOkContext {
  public static class GenerateServerSaltTest extends Test {
    public String label() {
      return "It can generate the server salt correctly";
    }
    public void test() {
      Integer256 new_nonce = new Integer256(0x88888888,0x77777777,0x66666666,0x55555555,0x44444444,0x33333333,0x22222222, 0x11111111);
      Integer128 server_nonce = new Integer128(new int[] {0x11111111, 0x22222222, 0x33333333, 0x44444444});
      expect(0x5555555599999999L, HandleRecieveServerDHGenOk.generate_server_salt(new_nonce, server_nonce));
    }
  }
}
