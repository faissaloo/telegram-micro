package ui;

public interface LoginScreenHost {
  void onLoginWarmStateChanged();
  void onLoginReconnectRequested();
  void onLoginRequestTimedOut();
}
