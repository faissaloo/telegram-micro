package session;

public class SessionSnapshot {
  public static final int LEGACY_VERSION = 1;
  public static final int VERSION = 2;
  public static final int AUTH_KEY_BYTES = 256;

  public int version;
  public boolean hasAuthState;
  public boolean authorized;
  public int dcId;
  public long authKeyId;
  public long serverSalt;
  public long sessionId;
  public int seqNo;
  public int timeOffsetSeconds;
  public long lastMessageId;
  public final byte[] authKey;

  public SessionSnapshot() {
    authKey = new byte[AUTH_KEY_BYTES];
    clear();
  }

  public void clear() {
    version = VERSION;
    hasAuthState = false;
    authorized = false;
    dcId = 0;
    authKeyId = 0;
    serverSalt = 0;
    sessionId = 0;
    seqNo = 0;
    timeOffsetSeconds = 0;
    lastMessageId = 0;
    clearAuthKey();
  }

  public void copyFrom(SessionSnapshot other) {
    if (other == null) {
      clear();
      return;
    }

    version = other.version;
    hasAuthState = other.hasAuthState;
    authorized = other.authorized;
    dcId = other.dcId;
    authKeyId = other.authKeyId;
    serverSalt = other.serverSalt;
    sessionId = other.sessionId;
    seqNo = other.seqNo;
    timeOffsetSeconds = other.timeOffsetSeconds;
    lastMessageId = other.lastMessageId;
    System.arraycopy(other.authKey, 0, authKey, 0, AUTH_KEY_BYTES);
  }

  public void setAuthKey(byte[] value) {
    int copyLength;
    int copyOffset;

    clearAuthKey();
    if (value == null || value.length <= 0 || value.length > AUTH_KEY_BYTES) {
      hasAuthState = false;
      return;
    }

    copyLength = value.length;
    copyOffset = AUTH_KEY_BYTES - copyLength;
    System.arraycopy(value, 0, authKey, copyOffset, copyLength);
    hasAuthState = true;
  }

  public boolean hasUsableAuth() {
    return hasAuthState && authKeyId != 0;
  }

  public void clearTransportReplayState() {
    sessionId = 0;
    seqNo = 0;
    lastMessageId = 0;
  }

  public boolean hasTransportReplayState() {
    return sessionId != 0 || seqNo != 0 || lastMessageId != 0;
  }

  public boolean shouldResumeSession() {
    return hasUsableAuth() && authorized;
  }

  private void clearAuthKey() {
    for (int i = 0; i < AUTH_KEY_BYTES; ++i) {
      authKey[i] = 0;
    }
  }
}
