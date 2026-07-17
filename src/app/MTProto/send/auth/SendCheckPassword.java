package mtproto.send.auth;

import mtproto.auth.PasswordCheck;
import mtproto.recieve.auth.RecievePasswordState;
import mtproto.send.SendEncrypted;

import mtproto.CombinatorIds;
import mtproto.MTProtoConnection;

public class SendCheckPassword extends SendEncrypted {
  public SendCheckPassword(long srp_id, byte[] A, byte[] M1) {
    if (A == null || A.length == 0 || M1 == null || M1.length == 0) {
      throw new IllegalArgumentException("SRP proof data is missing.");
    }

    message_data
      .append_int(CombinatorIds.auth_check_password)
      .append_int(CombinatorIds.input_check_password_srp)
      .append_long(srp_id)
      .append_byte_string(A)
      .append_byte_string(M1);
  }

  public SendCheckPassword(RecievePasswordState password_state, String password, byte[] a_bytes) {
    this(
      checked_srp_id(password_state),
      PasswordCheck.compute_check(password_state, password, a_bytes)
    );
  }

  private SendCheckPassword(long srp_id, PasswordCheck.Check check) {
    this(srp_id, check.A, check.M1);
  }

  public void send(MTProtoConnection sender) {
    super.send(sender, true, true);
  }

  private static long checked_srp_id(RecievePasswordState password_state) {
    if (password_state == null) {
      throw new IllegalArgumentException("Password state is unavailable.");
    }
    return password_state.srp_id;
  }
}
