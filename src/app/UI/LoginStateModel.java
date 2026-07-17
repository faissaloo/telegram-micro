package ui;

public class LoginStateModel {
  public static final long CODE_VALIDITY_WINDOW_MILLIS = 5L * 60L * 1000L;
  public static final int STAGE_BOOTSTRAP = 0;
  public static final int STAGE_PHONE = 1;
  public static final int STAGE_CODE = 2;
  public static final int STAGE_PASSWORD = 3;
  public static final int STAGE_AUTHORIZED = 4;
  public static final int STAGE_ERROR = 5;

  public int stage = STAGE_BOOTSTRAP;
  public boolean bootstrapReady = false;
  public String phoneNumber = "";
  public String phoneCodeHash = "";
  public String loginCode = "";
  public String password = "";
  public String submittedPhoneNumber = "";
  public String submittedLoginCode = "";
  public long codeSentAtMillis = 0;
  public boolean passwordParametersLoaded = false;
  public String passwordHint = "";
  public String statusLine = "Connecting";
  public String detailLine = "Waiting for Telegram bootstrap.";
  private int bootstrapDc = 0;
  private String bootstrapEndpoint = "";
  private int bootstrapEndpointCount = 0;

  private static boolean starts_with(String value, String prefix) {
    return value != null && prefix != null && value.startsWith(prefix);
  }

  private static String phone_code_status(String message) {
    if ("PHONE_CODE_EXPIRED".equals(message)) {
      return "Code expired";
    }
    return "Code rejected";
  }

  private static String phone_code_detail(String message) {
    if ("PHONE_CODE_EXPIRED".equals(message)) {
      return "This login code expired. Use Back to request a new code.";
    }
    if ("PHONE_CODE_INVALID".equals(message)) {
      return "Telegram rejected the login code.";
    }
    return message;
  }

  private static String phone_number_detail(String message) {
    if ("PHONE_NUMBER_INVALID".equals(message)) {
      return "Telegram rejected the phone number format.";
    }
    return message;
  }

  public void setPhoneDefault(String phoneNumber) {
    if (phoneNumber != null) {
      this.phoneNumber = phoneNumber;
    }
  }

  private void clearPasswordState() {
    password = "";
    passwordParametersLoaded = false;
    passwordHint = "";
  }

  private void clearPendingCodeState() {
    phoneCodeHash = "";
    loginCode = "";
    submittedLoginCode = "";
    codeSentAtMillis = 0;
    clearPasswordState();
  }

  private void enterPhoneOrBootstrapStage(String status, String detail) {
    stage = bootstrapReady ? STAGE_PHONE : STAGE_BOOTSTRAP;
    statusLine = status;
    detailLine = detail;
  }

  private String bootstrapDetail() {
    return "DC " + Integer.toString(bootstrapDc) +
      ", " + Integer.toString(bootstrapEndpointCount) + " endpoints" +
      (bootstrapEndpoint != null && bootstrapEndpoint.length() > 0 ? ", primary " + bootstrapEndpoint : "");
  }

  private void enterBootstrapReadyPhoneStage() {
    clearPendingCodeState();
    stage = STAGE_PHONE;
    statusLine = "Bootstrap ready";
    detailLine = bootstrapDetail();
  }

  private void enterCodeStage(boolean restoring) {
    stage = STAGE_CODE;
    statusLine = restoring && !bootstrapReady ? "Restoring code" : "Code sent";
    if (bootstrapReady) {
      detailLine = "Enter the login code Telegram sent. This code expires 5 minutes after it was sent.";
    } else {
      detailLine = "Waiting for Telegram bootstrap before the saved login code can continue.";
    }
  }

  public boolean hasPendingCodeState() {
    return phoneNumber != null &&
      phoneNumber.length() > 0 &&
      phoneCodeHash != null &&
      phoneCodeHash.length() > 0 &&
      codeSentAtMillis > 0;
  }

  public long codeExpiryRemainingMillis(long now) {
    long deadline;
    long remaining;

    if (!hasPendingCodeState()) {
      return 0;
    }

    deadline = codeSentAtMillis + CODE_VALIDITY_WINDOW_MILLIS;
    remaining = deadline - now;
    return remaining > 0 ? remaining : 0;
  }

  public boolean expirePendingCodeIfNeeded(long now) {
    if (!hasPendingCodeState() || codeExpiryRemainingMillis(now) > 0) {
      return false;
    }

    clearPendingCodeState();
    enterPhoneOrBootstrapStage(
      "Code expired",
      "The previous login code expired after 5 minutes. Request a new code."
    );
    return true;
  }

  public boolean restorePendingCodeState(String phoneNumber, String phoneCodeHash, long sentAtMillis, long now) {
    if (phoneNumber == null || phoneCodeHash == null ||
        phoneNumber.length() == 0 || phoneCodeHash.length() == 0 ||
        sentAtMillis <= 0) {
      return false;
    }

    this.phoneNumber = phoneNumber;
    this.phoneCodeHash = phoneCodeHash;
    this.codeSentAtMillis = sentAtMillis;
    this.loginCode = "";
    this.submittedLoginCode = "";
    clearPasswordState();
    if (expirePendingCodeIfNeeded(now)) {
      return false;
    }
    enterCodeStage(true);
    return true;
  }

  public void onSessionCreated(long elapsedMs) {
    statusLine = "Session created";
    detailLine = "Created in " + Long.toString(elapsedMs) + "ms.";
  }

  public void onBootstrapReady(int thisDc, String endpoint, int endpointCount) {
    bootstrapReady = true;
    bootstrapDc = thisDc;
    bootstrapEndpoint = endpoint != null ? endpoint : "";
    bootstrapEndpointCount = endpointCount;
    submittedLoginCode = "";
    if (expirePendingCodeIfNeeded(System.currentTimeMillis())) {
      return;
    }
    if (hasPendingCodeState()) {
      enterCodeStage(false);
      return;
    }
    enterBootstrapReadyPhoneStage();
  }

  public void onSendCodeSubmitted() {
    submittedPhoneNumber = phoneNumber;
    statusLine = "Submitting phone";
    detailLine = "Waiting for auth.sentCode.";
  }

  public void onSentCode(String phoneCodeHash, int timeoutSeconds) {
    this.phoneCodeHash = phoneCodeHash;
    this.codeSentAtMillis = System.currentTimeMillis();
    this.loginCode = "";
    this.submittedLoginCode = "";
    clearPasswordState();
    enterCodeStage(false);
  }

  public void onSignInSubmitted() {
    submittedLoginCode = loginCode;
    loginCode = "";
    statusLine = "Signing in";
    detailLine = "Waiting for auth.Authorization.";
  }

  public void onPasswordNeeded() {
    onPasswordParametersRequested();
  }

  public void onPasswordParametersRequested() {
    clearPendingCodeState();
    stage = STAGE_PASSWORD;
    clearPasswordState();
    statusLine = "Password required";
    detailLine = "Loading Telegram password settings.";
  }

  public void onPasswordParametersLoaded(String hint) {
    stage = STAGE_PASSWORD;
    passwordParametersLoaded = true;
    passwordHint = hint != null ? hint : "";
    statusLine = "Password required";
    detailLine =
      "Enter the Telegram two-factor password." +
      (passwordHint.length() > 0 ? " Hint: " + passwordHint : "");
  }

  public void onPasswordParametersUnavailable() {
    stage = STAGE_ERROR;
    passwordParametersLoaded = false;
    statusLine = "Password query failed";
    detailLine = "Telegram did not return usable password parameters.";
  }

  public void onPasswordSubmitted() {
    stage = STAGE_PASSWORD;
    password = "";
    statusLine = "Checking password";
    detailLine = "Waiting for auth.Authorization.";
  }

  public void onAuthorized() {
    stage = STAGE_AUTHORIZED;
    clearPendingCodeState();
    statusLine = "Authorized";
    detailLine = "Telegram accepted the login.";
  }

  public void onSignUpRequired() {
    clearPendingCodeState();
    stage = STAGE_ERROR;
    statusLine = "Sign-up required";
    detailLine = "This phone number is not registered yet.";
  }

  public void onRpcError(int code, String message) {
    if ("SESSION_PASSWORD_NEEDED".equals(message)) {
      onPasswordNeeded();
      return;
    }
    if (stage == STAGE_PHONE && starts_with(message, "PHONE_NUMBER_")) {
      stage = STAGE_PHONE;
      statusLine = "Phone rejected";
      detailLine = phone_number_detail(message);
      return;
    }
    if (stage == STAGE_CODE && starts_with(message, "PHONE_CODE_")) {
      if ("PHONE_CODE_EXPIRED".equals(message)) {
        clearPendingCodeState();
        enterPhoneOrBootstrapStage("Code expired", "The previous login code expired. Request a new code.");
      } else {
        stage = STAGE_CODE;
        loginCode = "";
        submittedLoginCode = "";
        statusLine = phone_code_status(message);
        detailLine = phone_code_detail(message);
      }
      return;
    }
    if (stage == STAGE_PASSWORD && "PASSWORD_HASH_INVALID".equals(message)) {
      stage = STAGE_PASSWORD;
      statusLine = "Password rejected";
      detailLine = "Telegram rejected the two-factor password.";
      return;
    }

    stage = STAGE_ERROR;
    statusLine = "RPC " + Integer.toString(code);
    detailLine = message;
  }

  public void onConnectionFailed(String detail) {
    stage = STAGE_ERROR;
    submittedLoginCode = "";
    statusLine = "Connection failed";
    detailLine = detail;
  }

  public void onUserVisibleError(String status, String detail) {
    if (stage != STAGE_PHONE && stage != STAGE_CODE && stage != STAGE_PASSWORD) {
      stage = STAGE_ERROR;
    }
    statusLine = status;
    detailLine = detail;
  }

  public void goBack() {
    if (stage == STAGE_CODE || stage == STAGE_PASSWORD || stage == STAGE_ERROR) {
      clearPendingCodeState();
      enterPhoneOrBootstrapStage(
        bootstrapReady ? "Bootstrap ready" : "Connecting",
        bootstrapReady ? bootstrapDetail() : "Waiting for Telegram bootstrap."
      );
    }
  }
}
