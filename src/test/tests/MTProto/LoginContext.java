package test;

import bouncycastle.BigInteger;

import mtproto.auth.PasswordCheck;
import mtproto.CombinatorIds;
import mtproto.Deserialize;
import mtproto.RPCResponse;
import mtproto.Serializer;
import mtproto.recieve.auth.RecievePasswordState;
import mtproto.recieve.auth.RecieveLoginResult;
import mtproto.recieve.auth.RecieveSentCode;
import mtproto.send.auth.SendCheckPassword;
import mtproto.send.auth.SendGetPassword;
import mtproto.send.auth.SendSendCode;
import mtproto.send.auth.SendSignIn;

import support.Decode;
import support.Debug;

public class LoginContext {
  private static String read_ascii_string(byte[] data, int offset) {
    return Deserialize.utf8string_deserialize(data, offset).ascii_string();
  }

  public static class BuildSendCodeRequestTest extends Test {
    public String label() {
      return "Can build auth.sendCode requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendSendCode("+441234567890", 777000, "deadbeef")).message_data.end();
      int offset = 4;

      expect(Decode.Little.int_decode(body, 0), CombinatorIds.send_code);
      expect(read_ascii_string(body, offset), "+441234567890");
      offset += Deserialize.utf8string_length_deserialize(body, offset);

      expect(Decode.Little.int_decode(body, offset), 777000);
      offset += 4;

      expect(read_ascii_string(body, offset), "deadbeef");
      offset += Deserialize.utf8string_length_deserialize(body, offset);

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.code_settings);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 0);
      offset += 4;

      expect(offset, body.length);
    }
  }

  public static class BuildSignInRequestTest extends Test {
    public String label() {
      return "Can build auth.signIn requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendSignIn("+441234567890", "hash123", "123456")).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.sign_in);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), 1);
      offset += 4;

      expect(read_ascii_string(body, offset), "+441234567890");
      offset += Deserialize.utf8string_length_deserialize(body, offset);

      expect(read_ascii_string(body, offset), "hash123");
      offset += Deserialize.utf8string_length_deserialize(body, offset);

      expect(read_ascii_string(body, offset), "123456");
      offset += Deserialize.utf8string_length_deserialize(body, offset);

      expect(offset, body.length);
    }
  }

  public static class ParseSentCodePayloadTest extends Test {
    public String label() {
      return "Can parse auth.sentCode payloads";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_int(CombinatorIds.auth_sent_code)
        .append_int((1 << 1) | (1 << 2))
        .append_int(RecieveSentCode.sent_code_type_sms)
        .append_int(6)
        .append_string("hash123")
        .append_int(RecieveSentCode.code_type_sms)
        .append_int(30)
        .end();

      RecieveSentCode sent_code = RecieveSentCode.from_rpc_response(RPCResponse.from_bytes(body));

      expect(sent_code.phone_code_hash, "hash123");
      expect(sent_code.sent_code_type_id, RecieveSentCode.sent_code_type_sms);
      expect(sent_code.has_next_type, true);
      expect(sent_code.next_type_id, RecieveSentCode.code_type_sms);
      expect(sent_code.has_timeout, true);
      expect(sent_code.timeout_seconds, 30);
    }
  }

  public static class ClassifySignUpRequiredPayloadTest extends Test {
    public String label() {
      return "Can classify auth.authorizationSignUpRequired payloads";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_int(CombinatorIds.auth_authorization_sign_up_required)
        .append_int(0)
        .end();

      RecieveLoginResult result = RecieveLoginResult.from_rpc_response(RPCResponse.from_bytes(body));
      expect(result.kind, RecieveLoginResult.kind_sign_up_required);
      expect(result.constructor_id, CombinatorIds.auth_authorization_sign_up_required);
    }
  }

  public static class ClassifySentCodeSuccessAsAuthorizationTest extends Test {
    public String label() {
      return "Can classify auth.sentCodeSuccess payloads as authorization";
    }

    public void test() throws TestFailureException {
      byte[] body = (new Serializer())
        .append_int(CombinatorIds.auth_sent_code_success)
        .append_int(CombinatorIds.auth_authorization)
        .append_int(0)
        .end();

      RecieveLoginResult result = RecieveLoginResult.from_rpc_response(RPCResponse.from_bytes(body));
      expect(result.kind, RecieveLoginResult.kind_authorization);
      expect(result.constructor_id, CombinatorIds.auth_sent_code_success);
    }
  }

  public static class BuildAccountGetPasswordRequestTest extends Test {
    public String label() {
      return "Can build account.getPassword requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendGetPassword()).message_data.end();
      expect(body.length, 4);
      expect(Decode.Little.int_decode(body, 0), CombinatorIds.account_get_password);
    }
  }

  public static class BuildAuthCheckPasswordRequestTest extends Test {
    public String label() {
      return "Can build auth.checkPassword requests";
    }

    public void test() throws TestFailureException {
      byte[] body = (new SendCheckPassword(0x1122334455667788L, new byte[] {1, 2, 3}, new byte[] {4, 5, 6, 7})).message_data.end();
      int offset = 0;

      expect(Decode.Little.int_decode(body, offset), CombinatorIds.auth_check_password);
      offset += 4;
      expect(Decode.Little.int_decode(body, offset), CombinatorIds.input_check_password_srp);
      offset += 4;
      expect(Decode.Little.long_decode(body, offset), 0x1122334455667788L);
      offset += 8;
      expect(Deserialize.bytes_deserialize(body, offset), new byte[] {1, 2, 3});
      offset += Deserialize.bytes_length_deserialize(body, offset);
      expect(Deserialize.bytes_deserialize(body, offset), new byte[] {4, 5, 6, 7});
      offset += Deserialize.bytes_length_deserialize(body, offset);

      expect(offset, body.length);
    }
  }

  public static class ParseAccountPasswordPayloadTest extends Test {
    public String label() {
      return "Can parse account.Password payloads";
    }

    public void test() throws TestFailureException {
      byte[] p = new byte[256];
      byte[] srpB = new byte[248];
      for (int i = 0; i < p.length; ++i) {
        p[i] = (byte)(i + 1);
      }
      for (int i = 0; i < srpB.length; ++i) {
        srpB[i] = (byte)(255 - i);
      }
      byte[] body = (new Serializer())
        .append_int(RecievePasswordState.FLAG_HAS_PASSWORD | RecievePasswordState.FLAG_HAS_HINT)
        .append_int(CombinatorIds.password_kdf_algo_modpow)
        .append_byte_string(new byte[] {1, 2, 3})
        .append_byte_string(new byte[] {4, 5, 6})
        .append_int(3)
        .append_byte_string(p)
        .append_byte_string(srpB)
        .append_long(0x1122334455667788L)
        .append_string("hint")
        .end();

      RecievePasswordState state = RecievePasswordState.from_rpc_response(new RPCResponse(CombinatorIds.account_password, body));

      expect(state.has_password, true);
      expect(state.g, 3);
      expect(state.srp_id, 0x1122334455667788L);
      expect(state.salt1, new byte[] {1, 2, 3});
      expect(state.salt2, new byte[] {4, 5, 6});
      expect(state.p, p);
      expect(state.srp_B, srpB);
      expect(state.hint, "hint");
    }
  }

  public static class ParseAccountPasswordWithoutCurrentPasswordTest extends Test {
    public String label() {
      return "Can parse account.Password payloads without current SRP data";
    }

    public void test() throws TestFailureException {
      RecievePasswordState state = RecievePasswordState.from_rpc_response(
        new RPCResponse(CombinatorIds.account_password, (new Serializer()).append_int(0).end())
      );

      expect(state.has_password, false);
      expect(state.hint, "");
      expect(state.salt1.length, 0);
      expect(state.salt2.length, 0);
    }
  }

  public static class HmacSha512VectorTest extends Test {
    public String label() {
      return "Can compute HMAC-SHA512 vectors for password proofs";
    }

    public void test() throws TestFailureException {
      expect(
        Debug.bytes_to_hex(
          PasswordCheck.hmac_sha512(
            "key".getBytes(),
            "The quick brown fox jumps over the lazy dog".getBytes()
          )
        ),
        "b42af09057bac1e2d41708e48a902e09b5ff7f12ab428a4fe86653c73dd248fb" +
        "82f948a549f7b791a5b41915ee4d1ec3935357e4e2317250d0372afa2ebeeb3a"
      );
    }
  }

  public static class Pbkdf2Sha512VectorTest extends Test {
    public String label() {
      return "Can compute PBKDF2-HMAC-SHA512 vectors for password proofs";
    }

    public void test() throws TestFailureException {
      expect(
        Debug.bytes_to_hex(
          PasswordCheck.pbkdf2_hmac_sha512(
            "password".getBytes(),
            "salt".getBytes(),
            2,
            64
          )
        ),
        "e1d9c16aa681708a45f5c7c4e215ceb66e011a2e9f0040713f18aefdb866d53c" +
        "f76cab2868a39b9f7840edce4fef5a82be67335c77a6068e04112754f27ccf4e"
      );
    }
  }

  public static class ComputePasswordCheckVectorTest extends SlowTest {
    public String label() {
      return "Can compute deterministic Telegram SRP password proofs";
    }

    public void test() throws TestFailureException {
      BigInteger p_integer = new BigInteger(
        "C71CAEB9C6B1C9048E6C522F70F13F73980D40238E3E21C14934D037563D930F" +
        "48198A0AA7C14058229493D22530F4DBFA336F6E0AC925139543AED44CCE7C37" +
        "20FD51F69458705AC68CD4FE6B6B13ABDC9746512969328454F18FAF8C595F64" +
        "2477FE96BB2A941D5BCD1D4AC8CC49880708FA9B378E3C4F3A9060BEE67CF9A4" +
        "A4A695811051907E162753B56B0F6B410DBA74D8A84B2A14B3144E0EF1284754" +
        "FD17ED950D5965B4B9DD46582DB1178D169C6BC465B0D6FF9CA3928FEF5B9AE4" +
        "E418FC15E83EBEA0F87FA9FF5EED70050DED2849F47BF959D956850CE929851F" +
        "0D8115F635B105EE2E4E15D04B2454BF6F4FADF034B10403119CD8E3B92FCC5B",
        16
      );
      BigInteger B_integer = p_integer.subtract(new BigInteger("123456789abcdef", 16));
      byte[] p = PasswordCheck.left_pad(p_integer.magnitudeToBytes(), 256);
      byte[] B = PasswordCheck.left_pad(B_integer.magnitudeToBytes(), 256);
      byte[] a_bytes = new byte[256];
      for (int i = 0; i < a_bytes.length; ++i) {
        a_bytes[i] = (byte)(i + 1);
      }

      RecievePasswordState state = new RecievePasswordState(
        true,
        3,
        0x0102030405060708L,
        new byte[] {1, 2, 3, 4, 5, 6},
        new byte[] {7, 8, 9, 10, 11, 12},
        p,
        B,
        ""
      );

      PasswordCheck.Check check = PasswordCheck.compute_check(state, "hunter2", a_bytes);

      expect(
        Debug.bytes_to_hex(check.A),
        "882aac74b56e2a5884d858316667b33fa09f10e867919ca457221229d82c41c3" +
        "426bdd405f9a1f19ec4172c273771ef1e713cbcb8a6d40247c56bc6ff788f7ce" +
        "41ce69f8c524bfc54e458e7168ee2c36152f9d65ac78b7da27cc02dd31ec5f2" +
        "eb0a61f1c02685cb3572d9946e15d55a8dd668f85c342fe70a213b86f5c1801" +
        "4b6a666fc85675e2b3e8da17f415cf6002ad69f4ed6354b438d19e320249e4d" +
        "3b5fe22ac67a15a1b8dd6f0120779f392f75ebbd2c49c313ce6a9dc9310283c" +
        "e3b29dd21ba6575feab1454f088c182641c682a1e2cfa8b399e43b0c1eea8dd" +
        "24b7aa3ac1d0a367c459078a3f02c162e22236e14def5f85d2ce36db3c6ed2d" +
        "f22fad"
      );
      expect(
        Debug.bytes_to_hex(check.M1),
        "30f80ea7dbfc2b4a89100cdc6ebbc5e4bfefb642960e8f57a57086ffa8c0e528"
      );
    }
  }
}
