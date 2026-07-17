package ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;

import config.AppCredentials;
import mtproto.MTProtoConnection;
import mtproto.recieve.auth.RecievePasswordState;
import mtproto.send.auth.SendCheckPassword;
import mtproto.send.auth.SendSendCode;
import mtproto.send.auth.SendSignIn;
import session.WarmStateSnapshot;

public class LoginScreen implements CommandListener {
  private MIDlet midlet;
  private LoginScreenHost host;
  private Display display;
  private LoginStateModel state;
  private MTProtoConnection connection;
  private RecievePasswordState passwordState;
  private AppCredentials bundledCredentials = AppCredentials.bundled();
  private Form form;

  private TextField phoneField;
  private TextField codeField;
  private TextField passwordField;
  private Thread codeExpiryWatcher;

  private final Command submitCommand = new Command("Submit", Command.OK, 1);
  private final Command backCommand = new Command("Back", Command.BACK, 2);
  private final Command exitCommand = new Command("Exit", Command.EXIT, 3);

  public LoginScreen(MIDlet midlet, Display display) {
    this.midlet = midlet;
    if (midlet instanceof LoginScreenHost) {
      this.host = (LoginScreenHost) midlet;
    }
    this.display = display;
    this.state = new LoginStateModel();
  }

  public void setConnection(MTProtoConnection connection) {
    this.connection = connection;
  }

  public void setBundledCredentials(AppCredentials bundledCredentials) {
    if (bundledCredentials != null) {
      this.bundledCredentials = bundledCredentials;
    } else {
      this.bundledCredentials = AppCredentials.bundled();
    }
  }

  public void setPhoneDefault(String phoneNumber) {
    state.setPhoneDefault(phoneNumber);
  }

  public void show() {
    rebuild();
  }

  public boolean applyWarmState(WarmStateSnapshot snapshot) {
    passwordState = null;
    if (snapshot == null) {
      return false;
    }
    return state.restorePendingCodeState(
      snapshot.loginPhoneNumber,
      snapshot.loginPhoneCodeHash,
      snapshot.loginCodeSentAtMillis,
      System.currentTimeMillis()
    );
  }

  public void captureWarmState(WarmStateSnapshot snapshot) {
    if (snapshot == null) {
      return;
    }
    if (!state.hasPendingCodeState() || state.expirePendingCodeIfNeeded(System.currentTimeMillis())) {
      return;
    }
    snapshot.loginPhoneNumber = state.phoneNumber;
    snapshot.loginPhoneCodeHash = state.phoneCodeHash;
    snapshot.loginCodeSentAtMillis = state.codeSentAtMillis;
  }

  private void notifyWarmStateChanged() {
    if (host != null) {
      host.onLoginWarmStateChanged();
    }
  }

  private synchronized void rescheduleCodeExpiryWatcher() {
    long delayMillis;
    Thread watcher;

    if (codeExpiryWatcher != null) {
      codeExpiryWatcher.interrupt();
      codeExpiryWatcher = null;
    }
    if (!state.hasPendingCodeState()) {
      return;
    }

    delayMillis = state.codeExpiryRemainingMillis(System.currentTimeMillis());
    if (delayMillis <= 0) {
      return;
    }

    watcher = new Thread(new Runnable() {
      public void run() {
        try {
          Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
          return;
        }
        synchronized (LoginScreen.this) {
          if (Thread.currentThread() != codeExpiryWatcher) {
            return;
          }
          codeExpiryWatcher = null;
        }
        if (state.expirePendingCodeIfNeeded(System.currentTimeMillis())) {
          notifyWarmStateChanged();
          rebuild();
        }
      }
    });
    codeExpiryWatcher = watcher;
    watcher.start();
  }

  public void onSessionCreated(long elapsedMs) {
    state.onSessionCreated(elapsedMs);
    rebuild();
  }

  public void onBootstrapReady(int thisDc, String endpoint, int endpointCount) {
    state.onBootstrapReady(thisDc, endpoint, endpointCount);
    notifyWarmStateChanged();
    rebuild();
  }

  public void onSentCode(String phoneCodeHash, int timeoutSeconds) {
    passwordState = null;
    state.onSentCode(phoneCodeHash, timeoutSeconds);
    notifyWarmStateChanged();
    rebuild();
  }

  public void onPasswordParametersRequested() {
    passwordState = null;
    state.onPasswordParametersRequested();
    notifyWarmStateChanged();
    rebuild();
  }

  public void onPasswordStateLoaded(RecievePasswordState passwordState) {
    this.passwordState = passwordState;
    if (passwordState == null || !passwordState.has_password) {
      state.onPasswordParametersUnavailable();
    } else {
      state.onPasswordParametersLoaded(passwordState.hint);
    }
    notifyWarmStateChanged();
    rebuild();
  }

  public void onAuthorized() {
    passwordState = null;
    state.onAuthorized();
    notifyWarmStateChanged();
    rebuild();
  }

  public void onSignUpRequired() {
    state.onSignUpRequired();
    notifyWarmStateChanged();
    rebuild();
  }

  public void onRpcError(int code, String message) {
    state.onRpcError(code, message);
    notifyWarmStateChanged();
    rebuild();
  }

  public void onConnectionFailed(String detail) {
    state.onConnectionFailed(detail);
    notifyWarmStateChanged();
    rebuild();
  }

  public void onUserVisibleError(String status, String detail) {
    state.onUserVisibleError(status, detail);
    notifyWarmStateChanged();
    rebuild();
  }

  public MTProtoConnection.PendingRetryRequest capturePendingRetryRequest() {
    if (bundledCredentials.isConfigured() &&
        state.submittedPhoneNumber != null &&
        state.submittedPhoneNumber.length() > 0 &&
        (state.phoneCodeHash == null || state.phoneCodeHash.length() == 0)) {
      return new MTProtoConnection.PendingRetryRequest(
        (new SendSendCode(state.submittedPhoneNumber, bundledCredentials.apiId, bundledCredentials.apiHash)).message_data.end(),
        true,
        true
      );
    }
    if (state.submittedPhoneNumber != null &&
        state.submittedPhoneNumber.length() > 0 &&
        state.phoneCodeHash != null &&
        state.phoneCodeHash.length() > 0 &&
        state.submittedLoginCode != null &&
        state.submittedLoginCode.length() > 0) {
      return new MTProtoConnection.PendingRetryRequest(
        (new SendSignIn(state.submittedPhoneNumber, state.phoneCodeHash, state.submittedLoginCode)).message_data.end(),
        true,
        true
      );
    }
    return null;
  }

  private static String trimOrEmpty(String value) {
    if (value == null) {
      return "";
    }
    return value.trim();
  }

  private void rebuild() {
    if (state.expirePendingCodeIfNeeded(System.currentTimeMillis())) {
      passwordState = null;
      notifyWarmStateChanged();
    }

    Form next = new Form("Telegram Micro");
    next.append(state.statusLine);
    if (state.detailLine != null && state.detailLine.length() > 0) {
      next.append("\n" + state.detailLine);
    }

    phoneField = null;
    codeField = null;
    passwordField = null;

    if (state.stage == LoginStateModel.STAGE_PHONE) {
      next.append("\n\nEnter the phone number, then submit sendCode.");
      if (bundledCredentials.isConfigured()) {
        next.append("\nApp API credentials are bundled into this build.");
      } else {
        next.append("\nBundled API credentials are missing.");
        next.append("\n" + bundledCredentials.missingDetail());
      }
      phoneField = new TextField("Phone", state.phoneNumber, 32, TextField.PHONENUMBER);
      next.append(phoneField);
      if (bundledCredentials.isConfigured()) {
        next.addCommand(submitCommand);
      }
    } else if (state.stage == LoginStateModel.STAGE_CODE) {
      next.append("\n\nEnter the login code Telegram sent.");
      codeField = new TextField("Code", state.loginCode, 16, TextField.NUMERIC);
      next.append(codeField);
      if (state.bootstrapReady) {
        next.addCommand(submitCommand);
      }
      next.addCommand(backCommand);
    } else if (state.stage == LoginStateModel.STAGE_PASSWORD) {
      next.append("\n\nEnter the Telegram two-factor password.");
      passwordField = new TextField("Password", state.password, 64, TextField.PASSWORD);
      next.append(passwordField);
      if (state.passwordParametersLoaded) {
        next.addCommand(submitCommand);
      }
      next.addCommand(backCommand);
    } else if (state.stage == LoginStateModel.STAGE_ERROR) {
      if (state.bootstrapReady) {
        next.append("\n\nUse Back to return to the login form.");
        next.addCommand(backCommand);
      }
    }

    next.addCommand(exitCommand);
    next.setCommandListener(this);
    form = next;
    rescheduleCodeExpiryWatcher();
    display.setCurrent(form);
  }

  public void commandAction(Command command, Displayable displayable) {
    if (command == exitCommand) {
      midlet.notifyDestroyed();
      return;
    }

    if (command == backCommand) {
      state.goBack();
      passwordState = null;
      notifyWarmStateChanged();
      rebuild();
      return;
    }

    if (command != submitCommand) {
      return;
    }

    if (connection == null) {
      state.onUserVisibleError("Connection unavailable", "MTProto connection is not ready yet.");
      notifyWarmStateChanged();
      rebuild();
      return;
    }

    if (state.stage == LoginStateModel.STAGE_PHONE) {
      state.phoneNumber = trimOrEmpty(phoneField.getString());

      if (!bundledCredentials.isConfigured()) {
        state.onUserVisibleError("Build missing API creds", bundledCredentials.missingDetail());
        notifyWarmStateChanged();
        rebuild();
        return;
      }
      if (state.phoneNumber.length() == 0) {
        state.onUserVisibleError("Missing fields", "Phone number is required.");
        notifyWarmStateChanged();
        rebuild();
        return;
      }

      (new SendSendCode(state.phoneNumber, bundledCredentials.apiId, bundledCredentials.apiHash)).send(connection);
      state.onSendCodeSubmitted();
      notifyWarmStateChanged();
      rebuild();
      return;
    }

    if (state.stage == LoginStateModel.STAGE_CODE) {
      state.loginCode = trimOrEmpty(codeField.getString());
      if (state.loginCode.length() == 0 || state.phoneCodeHash.length() == 0) {
        state.onUserVisibleError("Missing code", "A login code and phone_code_hash are required.");
        notifyWarmStateChanged();
        rebuild();
        return;
      }

      (new SendSignIn(state.phoneNumber, state.phoneCodeHash, state.loginCode)).send(connection);
      state.onSignInSubmitted();
      notifyWarmStateChanged();
      rebuild();
      return;
    }

    if (state.stage == LoginStateModel.STAGE_PASSWORD) {
      state.password = trimOrEmpty(passwordField.getString());
      if (state.password.length() == 0) {
        state.onUserVisibleError("Missing password", "The Telegram two-factor password is required.");
        notifyWarmStateChanged();
        rebuild();
        return;
      }
      if (passwordState == null || !state.passwordParametersLoaded) {
        state.onUserVisibleError("Password unavailable", "Password parameters are not loaded yet.");
        notifyWarmStateChanged();
        rebuild();
        return;
      }

      try {
        (new SendCheckPassword(passwordState, state.password, connection.random_number_generator.nextBytes(256))).send(connection);
        state.onPasswordSubmitted();
      } catch (IllegalArgumentException e) {
        state.onUserVisibleError("Password check failed", e.getMessage());
      }
      notifyWarmStateChanged();
      rebuild();
    }
  }
}
