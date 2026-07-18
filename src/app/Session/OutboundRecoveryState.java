package session;

public final class OutboundRecoveryState {
  public static final int KIND_NONE = 0;
  public static final int KIND_TEXT = 1;
  public static final int KIND_FORWARD = 2;

  public static final int PHASE_IDLE = 0;
  public static final int PHASE_SELECTED = 1;
  public static final int PHASE_IN_FLIGHT = 2;
  public static final int PHASE_SENT = 3;
  public static final int PHASE_FAILED = 4;

  public static final int LABEL_MAX = 48;

  public int sequence = 0;
  public int kind = KIND_NONE;
  public int phase = PHASE_IDLE;
  public int peerKind = 0;
  public long peerId = 0;
  public int code = 0;
  public int detail = 0;
  public String label = "";

  public void clear() {
    sequence = 0;
    kind = KIND_NONE;
    phase = PHASE_IDLE;
    peerKind = 0;
    peerId = 0;
    code = 0;
    detail = 0;
    label = "";
  }

  public void copyFrom(OutboundRecoveryState other) {
    if (other == null) {
      clear();
      return;
    }

    sequence = other.sequence;
    kind = other.kind;
    phase = other.phase;
    peerKind = other.peerKind;
    peerId = other.peerId;
    code = other.code;
    detail = other.detail;
    label = other.label != null ? other.label : "";
  }

  public void note(int kind, int phase, int peerKind, long peerId, int code, int detail, String label) {
    sequence += 1;
    this.kind = kind;
    this.phase = phase;
    this.peerKind = peerKind;
    this.peerId = peerId;
    this.code = code;
    this.detail = detail;
    this.label = limit(label, LABEL_MAX);
  }

  public boolean hasUsefulState() {
    return kind != KIND_NONE || phase != PHASE_IDLE || label.length() > 0;
  }

  public static boolean resumePrerequisitesReady(
    boolean syncReady,
    boolean dialogsLoading,
    boolean historyLoading,
    boolean historyRequestInFlight,
    boolean syncStateRequestInFlight,
    boolean differenceRequestInFlight,
    boolean channelDifferenceRequestInFlight,
    boolean needsDifference
  ) {
    return syncReady &&
      !dialogsLoading &&
      !historyLoading &&
      !historyRequestInFlight &&
      !syncStateRequestInFlight &&
      !differenceRequestInFlight &&
      !channelDifferenceRequestInFlight &&
      !needsDifference;
  }

  /**
   * A live reconnect can reconcile updateMessageID only if it asks for the
   * difference from the last locally applied state. Fetching the newest state
   * first would skip the ambiguous acknowledgement. A cold start without a
   * checkpoint must fall back to updates.getState and rely on random_id
   * idempotence instead.
   */
  public static boolean shouldRequestDifferenceBeforeResume(
    boolean resumePending,
    boolean hasSyncCheckpoint
  ) {
    return resumePending && hasSyncCheckpoint;
  }

  public String summary() {
    return kindName(kind) + " " + phaseName(phase);
  }

  public String detail() {
    StringBuffer buffer = new StringBuffer();

    if (label != null && label.length() > 0) {
      buffer.append(label);
    } else {
      buffer.append("No outbound recovery state.");
    }
    if (peerId != 0) {
      buffer.append(" @");
      buffer.append(Long.toString(peerId));
    }
    if (code != 0) {
      buffer.append(" code ");
      buffer.append(Integer.toString(code));
    }
    return buffer.toString();
  }

  public static String kindName(int kind) {
    switch (kind) {
      case KIND_TEXT:
        return "TEXT";
      case KIND_FORWARD:
        return "FORWARD";
      default:
        return "NONE";
    }
  }

  public static String phaseName(int phase) {
    switch (phase) {
      case PHASE_SELECTED:
        return "SELECT";
      case PHASE_IN_FLIGHT:
        return "SEND";
      case PHASE_SENT:
        return "SENT";
      case PHASE_FAILED:
        return "FAIL";
      default:
        return "IDLE";
    }
  }

  private static String limit(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    if (value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }
}
