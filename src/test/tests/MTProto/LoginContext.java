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
      byte[] body = (new Serializer())
        .append_int(RecievePasswordState.FLAG_HAS_PASSWORD | RecievePasswordState.FLAG_HAS_HINT)
        .append_int(CombinatorIds.password_kdf_algo_modpow)
        .append_byte_string(new byte[] {1, 2, 3})
        .append_byte_string(new byte[] {4, 5, 6})
        .append_int(3)
        .append_byte_string(new byte[] {7, 8, 9})
        .append_byte_string(new byte[] {10, 11, 12, 13})
        .append_long(0x1122334455667788L)
        .append_string("hint")
        .end();

      RecievePasswordState state = RecievePasswordState.from_rpc_response(new RPCResponse(CombinatorIds.account_password, body));

      expect(state.has_password, true);
      expect(state.g, 3);
      expect(state.srp_id, 0x1122334455667788L);
      expect(state.salt1, new byte[] {1, 2, 3});
      expect(state.salt2, new byte[] {4, 5, 6});
      expect(state.p, new byte[] {7, 8, 9});
      expect(state.srp_B, new byte[] {10, 11, 12, 13});
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

  public static class ComputePasswordCheckVectorTest extends SkippedTest {
    public String label() {
      return "Can compute deterministic Telegram SRP password proofs";
    }

    public void test() throws TestFailureException {
      BigInteger p_integer = BigInteger.ONE.shiftLeft(2048).subtract(BigInteger.valueOf(159));
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
        "e9d8eb6ba6165a0dfd0b89baf7b6d3b86742030fa02334532c6e02f3d3e8d266" +
        "0f43b4941e5a576c7f1959385105dca0e8c2e8663215b7a7df39e66e61bfc611" +
        "208d6ddc7e3c7fdd39c9cf21c9a72b31856c3109edae4cc0db1cac752639679c" +
        "766881bc8a08ac487b846f933c4308a2cf2317803a866c9853c7904d5d0b7b7b" +
        "da7a3578db6eccad26082d85abebd657ac937b1f2f617fd1d4199a860f7cefab" +
        "1db580bce3f6b1490d4d4f848293931b4c0e10a0098e77020dc203c7c8d9d729" +
        "913763d1488745259c3d5f49896be1d4ac32aa1644cce99df71dac73dc56e535" +
        "9da4a452a1abb41e20a2ab934a70bd48556b8bb4a449d28cb17eb21686558674"
      );
      expect(
        Debug.bytes_to_hex(check.M1),
        "54d45af3e7a38709d9db518a0d3ac8fed0a2568641b298bdafbd3fbdd37d9479"
      );
    }
  }
}
