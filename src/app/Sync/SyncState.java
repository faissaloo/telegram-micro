package sync;

import mtproto.recieve.service.RecieveUpdateShortSentMessage;
import mtproto.recieve.updates.RecieveUpdatesDifference;
import mtproto.recieve.updates.RecieveUpdatesState;

public final class SyncState {
  public static final int DEFAULT_PTS_LIMIT_HINT = 128;
  public static final int DEFAULT_PTS_TOTAL_LIMIT_HINT = 256;
  public static final int DEFAULT_QTS_LIMIT_HINT = 64;

  public static final class DifferenceArgs {
    public int pts;
    public int date;
    public int qts;
    public int ptsLimit;
    public int ptsTotalLimit;
    public int qtsLimit;
    public boolean hasPtsLimit;
    public boolean hasPtsTotalLimit;
    public boolean hasQtsLimit;
  }

  public int pts;
  public int qts;
  public int date;
  public int seq;
  public int unreadCount;
  public int ptsLimitHint;
  public int ptsTotalLimitHint;
  public int qtsLimitHint;
  public boolean hasState;
  public boolean needsDifference;
  public boolean differenceTooLong;

  public SyncState() {
    clear();
  }

  public void clear() {
    pts = 0;
    qts = 0;
    date = 0;
    seq = 0;
    unreadCount = 0;
    ptsLimitHint = 0;
    ptsTotalLimitHint = 0;
    qtsLimitHint = 0;
    hasState = false;
    needsDifference = false;
    differenceTooLong = false;
  }

  public void applyUpdatesState(RecieveUpdatesState state) {
    if (state == null) {
      return;
    }

    pts = state.pts;
    qts = state.qts;
    date = state.date;
    seq = state.seq;
    unreadCount = state.unreadCount;
    hasState = true;
    needsDifference = false;
    differenceTooLong = false;
    ensureDefaultHints();
  }

  public void noteDifferenceEmpty(int newDate, int newSeq) {
    date = newDate;
    seq = newSeq;
    hasState = true;
    needsDifference = false;
    differenceTooLong = false;
    ensureDefaultHints();
  }

  public void noteDifferenceTooLong(int newPts) {
    pts = newPts;
    hasState = true;
    needsDifference = true;
    differenceTooLong = true;
    ensureDefaultHints();
  }

  public void markNeedsDifference() {
    needsDifference = true;
  }

  public void noteDifferenceHint(int ptsLimit, int ptsTotalLimit, int qtsLimit) {
    if (ptsLimit > 0) {
      ptsLimitHint = ptsLimit;
    }
    if (ptsTotalLimit > 0) {
      ptsTotalLimitHint = ptsTotalLimit;
    }
    if (qtsLimit > 0) {
      qtsLimitHint = qtsLimit;
    }
  }

  public void noteDifferenceResult(RecieveUpdatesDifference difference) {
    if (difference == null) {
      return;
    }

    if (difference.type == RecieveUpdatesDifference.TYPE_EMPTY) {
      noteDifferenceEmpty(difference.date, difference.seq);
      return;
    }
    if (difference.type == RecieveUpdatesDifference.TYPE_TOO_LONG) {
      noteDifferenceTooLong(difference.pts);
      return;
    }
    if (difference.hasState()) {
      pts = difference.pts;
      qts = difference.qts;
      date = difference.date;
      seq = difference.seq;
      unreadCount = difference.unreadCount;
      hasState = true;
      needsDifference = difference.type == RecieveUpdatesDifference.TYPE_SLICE;
      differenceTooLong = false;
      ensureDefaultHints();
    }
  }

  public void noteShortSent(RecieveUpdateShortSentMessage sent) {
    if (sent == null) {
      return;
    }

    notePtsAcknowledgement(sent.pts, sent.ptsCount, sent.date);
  }

  public void noteShortUpdate(int newPts, int newDate) {
    pts = newPts;
    date = newDate;
    hasState = true;
    needsDifference = false;
    differenceTooLong = false;
    ensureDefaultHints();
  }

  /**
   * Applies a PTS-bearing acknowledgement only when it is contiguous with the
   * last known state. Older duplicate acknowledgements are harmless; a gap is
   * left unapplied and schedules updates.getDifference instead.
   */
  public boolean notePtsAcknowledgement(int newPts, int ptsCount, int newDate) {
    if (ptsCount < 0 || newPts <= ptsCount) {
      needsDifference = true;
      return false;
    }
    if (needsDifference) {
      return false;
    }
    if (hasState) {
      if (newPts <= pts) {
        return true;
      }
      if (newPts - ptsCount != pts) {
        needsDifference = true;
        return false;
      }
    }

    pts = newPts;
    if (newDate > 0) {
      date = newDate;
    }
    hasState = true;
    needsDifference = false;
    differenceTooLong = false;
    ensureDefaultHints();
    return true;
  }

  public boolean canBuildDifferenceQuery() {
    return hasState;
  }

  public DifferenceArgs buildDifferenceArgs() {
    DifferenceArgs out = new DifferenceArgs();

    out.pts = pts;
    out.date = date;
    out.qts = qts;
    if (ptsLimitHint > 0) {
      out.hasPtsLimit = true;
      out.ptsLimit = ptsLimitHint;
    }
    if (ptsTotalLimitHint > 0) {
      out.hasPtsTotalLimit = true;
      out.ptsTotalLimit = ptsTotalLimitHint;
    }
    if (qtsLimitHint > 0) {
      out.hasQtsLimit = true;
      out.qtsLimit = qtsLimitHint;
    }
    return out;
  }

  private void ensureDefaultHints() {
    if (ptsLimitHint <= 0) {
      ptsLimitHint = DEFAULT_PTS_LIMIT_HINT;
    }
    if (ptsTotalLimitHint <= 0) {
      ptsTotalLimitHint = DEFAULT_PTS_TOTAL_LIMIT_HINT;
    }
    if (qtsLimitHint <= 0) {
      qtsLimitHint = DEFAULT_QTS_LIMIT_HINT;
    }
  }
}
