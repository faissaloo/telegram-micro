package test;

import ui.LoginStateModel;

public class LoginStateModelContext {
  public static class BootstrapTransitionsToPhoneStageTest extends Test {
    public String label() {
      return "Login state enters phone stage after bootstrap";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      state.onBootstrapReady(3, "149.154.175.100:443", 19);

      expect(state.bootstrapReady, true);
      expect(state.stage, LoginStateModel.STAGE_PHONE);
      expect(state.statusLine, "Bootstrap ready");
      expect(state.detailLine.indexOf("149.154.175.100:443") >= 0, true);
    }
  }

  public static class SentCodeTransitionsToCodeStageTest extends Test {
    public String label() {
      return "Login state enters code stage after auth.sentCode";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      state.onSentCode("hash123", 30);

      expect(state.stage, LoginStateModel.STAGE_CODE);
      expect(state.phoneCodeHash, "hash123");
      expect(state.codeSentAtMillis > 0, true);
      expect(state.statusLine, "Code sent");
      expect(state.detailLine.indexOf("5 minutes") >= 0, true);
    }
  }

  public static class PasswordRpcTransitionsToPasswordStageTest extends Test {
    public String label() {
      return "Login state recognizes SESSION_PASSWORD_NEEDED";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      state.onRpcError(401, "SESSION_PASSWORD_NEEDED");

      expect(state.stage, LoginStateModel.STAGE_PASSWORD);
      expect(state.statusLine, "Password required");
      expect(state.passwordParametersLoaded, false);
    }
  }

  public static class SendCodeSubmitTracksRetryPhoneTest extends Test {
    public String label() {
      return "Login state keeps the submitted phone for login retry";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      state.phoneNumber = "+441234567890";
      state.onSendCodeSubmitted();

      expect(state.submittedPhoneNumber, "+441234567890");
      expect(state.statusLine, "Submitting phone");
    }
  }

  public static class PasswordParametersLoadedStayOnPasswordStageTest extends Test {
    public String label() {
      return "Login state keeps password stage when SRP parameters load";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      state.onPasswordParametersRequested();
      state.onPasswordParametersLoaded("pet");

      expect(state.stage, LoginStateModel.STAGE_PASSWORD);
      expect(state.passwordParametersLoaded, true);
      expect(state.detailLine.indexOf("pet") >= 0, true);
    }
  }

  public static class PasswordInvalidRpcStaysOnPasswordStageTest extends Test {
    public String label() {
      return "Login state keeps password stage on PASSWORD_HASH_INVALID";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      state.onPasswordParametersRequested();
      state.onPasswordParametersLoaded("pet");
      state.onRpcError(400, "PASSWORD_HASH_INVALID");

      expect(state.stage, LoginStateModel.STAGE_PASSWORD);
      expect(state.statusLine, "Password rejected");
    }
  }

  public static class PhoneNumberInvalidRpcStaysOnPhoneStageTest extends Test {
    public String label() {
      return "Login state keeps phone stage on PHONE_NUMBER_INVALID";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      state.onRpcError(400, "PHONE_NUMBER_INVALID");

      expect(state.stage, LoginStateModel.STAGE_PHONE);
      expect(state.statusLine, "Phone rejected");
      expect(state.detailLine, "Telegram rejected the phone number format.");
    }
  }

  public static class PhoneCodeInvalidRpcStaysOnCodeStageTest extends Test {
    public String label() {
      return "Login state keeps code stage on PHONE_CODE_INVALID";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      state.onSentCode("hash123", 30);
      state.loginCode = "12345";
      state.onSignInSubmitted();
      state.onRpcError(400, "PHONE_CODE_INVALID");

      expect(state.stage, LoginStateModel.STAGE_CODE);
      expect(state.statusLine, "Code rejected");
      expect(state.detailLine, "Telegram rejected the login code.");
      expect(state.submittedLoginCode, "");
    }
  }

  public static class PhoneCodeExpiredRpcStaysOnCodeStageTest extends Test {
    public String label() {
      return "Login state returns to phone stage on PHONE_CODE_EXPIRED";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      state.onSentCode("hash123", 30);
      state.loginCode = "12345";
      state.onSignInSubmitted();
      state.onRpcError(400, "PHONE_CODE_EXPIRED");

      expect(state.stage, LoginStateModel.STAGE_PHONE);
      expect(state.statusLine, "Code expired");
      expect(state.phoneCodeHash, "");
    }
  }

  public static class SignInSubmitTracksRetryCodeTest extends Test {
    public String label() {
      return "Login state keeps the submitted code for migration retry";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      state.onSentCode("hash123", 30);
      state.loginCode = "12345";
      state.onSignInSubmitted();

      expect(state.submittedPhoneNumber, "");
      expect(state.submittedLoginCode, "12345");
      expect(state.loginCode, "");
      expect(state.statusLine, "Signing in");
    }
  }

  public static class AuthorizationTransitionsToAuthorizedStageTest extends Test {
    public String label() {
      return "Login state enters authorized stage";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      state.onAuthorized();

      expect(state.stage, LoginStateModel.STAGE_AUTHORIZED);
      expect(state.statusLine, "Authorized");
    }
  }

  public static class BackReturnsToPhoneStageTest extends Test {
    public String label() {
      return "Login state back navigation returns to phone stage";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      state.onSentCode("hash123", 30);
      state.goBack();

      expect(state.stage, LoginStateModel.STAGE_PHONE);
      expect(state.phoneCodeHash, "");
      expect(state.codeSentAtMillis, 0L);
    }
  }

  public static class BootstrapPreservesRestoredCodeStageTest extends Test {
    public String label() {
      return "Login state restores the sent-code stage across bootstrap";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      long now = System.currentTimeMillis();

      expect(state.restorePendingCodeState("+15555550123", "hash123", now, now), true);
      expect(state.stage, LoginStateModel.STAGE_CODE);
      expect(state.statusLine, "Restoring code");

      state.onBootstrapReady(4, "149.154.167.91:443", 17);

      expect(state.stage, LoginStateModel.STAGE_CODE);
      expect(state.statusLine, "Code sent");
      expect(state.detailLine.indexOf("5 minutes") >= 0, true);
    }
  }

  public static class RestoredCodeExpiresToPhoneStageTest extends Test {
    public String label() {
      return "Login state expires restored codes using the system clock";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      long sentAt = 1000L;
      long expiredAt = sentAt + LoginStateModel.CODE_VALIDITY_WINDOW_MILLIS + 1L;

      expect(state.restorePendingCodeState("+15555550123", "hash123", sentAt, sentAt), true);
      expect(state.expirePendingCodeIfNeeded(expiredAt), true);
      expect(state.stage, LoginStateModel.STAGE_BOOTSTRAP);
      expect(state.phoneCodeHash, "");
      expect(state.statusLine, "Code expired");
    }
  }

  public static class UserVisiblePhoneErrorKeepsPhoneStageTest extends Test {
    public String label() {
      return "Inline login validation errors keep the current entry stage";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      state.onUserVisibleError("Missing fields", "Phone number is required.");

      expect(state.stage, LoginStateModel.STAGE_PHONE);
      expect(state.statusLine, "Missing fields");
    }
  }

  public static class SendCodeWaitExpiresBoundedlyTest extends Test {
    public String label() {
      return "Login send-code wait expires without enabling an implicit retry";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();
      long submittedAt = 1000L;

      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      state.phoneNumber = "+15555550123";
      state.onSendCodeSubmitted(submittedAt);

      expect(state.hasPendingRequest(), true);
      expect(state.expirePendingRequestIfNeeded(
        submittedAt + LoginStateModel.REQUEST_TIMEOUT_MILLIS - 1L), false);
      expect(state.expirePendingRequestIfNeeded(
        submittedAt + LoginStateModel.REQUEST_TIMEOUT_MILLIS), true);
      expect(state.hasPendingRequest(), false);
      expect(state.bootstrapReady, false);
      expect(state.stage, LoginStateModel.STAGE_ERROR);
      expect(state.statusLine, "Send-code result unknown");
      expect(state.detailLine.indexOf("do not request another") >= 0, true);
    }
  }

  public static class SendCodeDisconnectRequiresExplicitRetryTest extends Test {
    public String label() {
      return "Login reconnect preserves phone but never replays send-code";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();

      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      state.phoneNumber = "+15555550123";
      state.onSendCodeSubmitted(1000L);
      state.onConnectionFailed("Connection terminated by server");

      expect(state.phoneNumber, "+15555550123");
      expect(state.hasPendingRequest(), false);
      expect(state.statusLine, "Send-code result unknown");

      state.onReconnectStarted();
      expect(state.stage, LoginStateModel.STAGE_BOOTSTRAP);
      expect(state.hasPendingRequest(), false);

      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      expect(state.stage, LoginStateModel.STAGE_PHONE);
      expect(state.phoneNumber, "+15555550123");
      expect(state.statusLine, "Ready to retry login");
      expect(state.detailLine.indexOf("submit") >= 0, true);
      expect(state.hasPendingRequest(), false);
    }
  }

  public static class SignInDisconnectClearsSecretAndPreservesCodeContextTest extends Test {
    public String label() {
      return "Login reconnect clears ambiguous code input but keeps its code hash";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();

      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      state.phoneNumber = "+15555550123";
      state.onSentCode("hash123", 30);
      state.loginCode = "12345";
      state.onSignInSubmitted(1000L);
      state.onConnectionFailed("Connection terminated by server");

      expect(state.loginCode, "");
      expect(state.submittedLoginCode, "");
      expect(state.phoneCodeHash, "hash123");
      expect(state.statusLine, "Sign-in result unknown");

      state.onReconnectStarted();
      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      expect(state.stage, LoginStateModel.STAGE_CODE);
      expect(state.statusLine, "Ready to retry sign-in");
      expect(state.hasPendingRequest(), false);
    }
  }

  public static class IdleDisconnectInvalidatesBootstrapTest extends Test {
    public String label() {
      return "Idle login disconnect invalidates the stale bootstrap socket";
    }

    public void test() throws TestFailureException {
      LoginStateModel state = new LoginStateModel();

      state.onBootstrapReady(3, "149.154.175.100:443", 19);
      state.onConnectionFailed("Connection terminated by server");

      expect(state.bootstrapReady, false);
      expect(state.stage, LoginStateModel.STAGE_ERROR);
      expect(state.statusLine, "Connection failed");
      expect(state.detailLine.indexOf("Back to reconnect") >= 0, true);
    }
  }
}
