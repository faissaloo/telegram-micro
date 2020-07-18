package test;

import mtproto.SendReqDhParams;
import support.Integer128;
import support.Integer256;
import crypto.SHA1;

public class SendReqDhParamsContext {
  public static class PQInnerDataTest extends Test {
    public String label() {
      return "It produces the correct p_q_inner_data";
    }
    public void test() throws TestFailureException {
      //https://core.telegram.org/mtproto/samples-auth_key#4-encrypted-data-generation
      long pq = 0x17ED48941A08F981L;
      long p = 0x494C553B;
      long q = 0x53911073;
      Integer128 nonce = new Integer128(new int[] {0x8249053E, 0xE927CA8C, 0xA401B366, 0xFCE2EC8F});
      Integer128 server_nonce = new Integer128(new int[] {0x334DCFA5, 0xA81EA1F4, 0xA54ABA77, 0x30739073});
      Integer256 new_nonce = new Integer256(0xDB851C31, 0x64A24A23, 0x764AFC0A, 0x5BCF35A7, 0x8BD60F1F, 0x81A17FD1, 0xD89A22E1, 0x4D02CC67);

      byte[] expected_sha1 = new byte[] {
        (byte)0xDB, (byte)0x76, (byte)0x1C, (byte)0x27,
        (byte)0x71, (byte)0x8A, (byte)0x23, (byte)0x05,
        (byte)0x04, (byte)0x4F, (byte)0x71, (byte)0xF2,
        (byte)0xAD, (byte)0x95, (byte)0x16, (byte)0x29,
        (byte)0xD7, (byte)0x8B, (byte)0x24, (byte)0x49
      };
      
      byte[] result = SendReqDhParams.p_q_inner_data(nonce, server_nonce, pq, p, q, new_nonce);
      byte[] result_sha1 = (new SHA1()).digest(result);
      expect(result_sha1, expected_sha1);
    }
  }
}
