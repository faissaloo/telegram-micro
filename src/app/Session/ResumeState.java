package session;

public class ResumeState {
  public static final int PHASE_IDLE = 0;
  public static final int PHASE_READY = 1;
  public static final int PHASE_RECONNECT = 2;
  public static final int PHASE_FAILED = 3;

  public int phase;
  public boolean hasAuth;
  public boolean hasTransportState;
  public boolean authorized;
  public int dcId;
  public long authKeyId;
  public long sessionId;
  public int timeOffsetSeconds;
  public int reconnectAttempts;
  public int lastErrorCode;

  public ResumeState() {
    clear();
  }

  public void clear() {
    phase = PHASE_IDLE;
    hasAuth = false;
    hasTransportState = false;
    authorized = false;
    dcId = 0;
    authKeyId = 0;
    sessionId = 0;
    timeOffsetSeconds = 0;
    reconnectAttempts = 0;
    lastErrorCode = 0;
  }

  public boolean load(SessionSnapshot snapshot) {
    clear();
    if (snapshot == null || !snapshot.hasUsableAuth()) {
      return false;
    }

    hasAuth = true;
    hasTransportState = snapshot.sessionId != 0;
    authorized = snapshot.authorized;
    dcId = snapshot.dcId;
    authKeyId = snapshot.authKeyId;
    sessionId = snapshot.sessionId;
    timeOffsetSeconds = snapshot.timeOffsetSeconds;
    phase = hasTransportState ? PHASE_READY : PHASE_RECONNECT;
    return true;
  }

  public void noteDisconnect(int errorCode) {
    phase = PHASE_RECONNECT;
    lastErrorCode = errorCode;
    reconnectAttempts += 1;
  }

  public void noteSuccess() {
    phase = PHASE_READY;
    lastErrorCode = 0;
    reconnectAttempts = 0;
  }

  public void noteFailure(int errorCode) {
    phase = PHASE_FAILED;
    lastErrorCode = errorCode;
    reconnectAttempts += 1;
  }

  public String phaseName() {
    switch (phase) {
      case PHASE_IDLE:
        return "IDLE";
      case PHASE_READY:
        return "READY";
      case PHASE_RECONNECT:
        return "RECONNECT";
      case PHASE_FAILED:
        return "FAILED";
      default:
        return "UNKNOWN";
    }
  }
}
