package sync;

public final class ChannelSyncState {
  public static final int DEFAULT_LIMIT_HINT = 24;

  public static final class DifferenceArgs {
    public int pts;
    public int limit;
    public boolean force;
  }

  public static final class Entry {
    public long peerId = 0;
    public int pts = 0;
    public int timeout = 0;
    public boolean hasState = false;
    public boolean needsDifference = false;

    public void clear() {
      peerId = 0;
      pts = 0;
      timeout = 0;
      hasState = false;
      needsDifference = false;
    }
  }

  public final Entry[] entries;
  public int limitHint = DEFAULT_LIMIT_HINT;

  public ChannelSyncState(int capacity) {
    int i;

    if (capacity <= 0) {
      throw new IllegalArgumentException("Channel sync capacity must be positive.");
    }

    entries = new Entry[capacity];
    for (i = 0; i < entries.length; ++i) {
      entries[i] = new Entry();
    }
  }

  public void clear() {
    int i;

    for (i = 0; i < entries.length; ++i) {
      entries[i].clear();
    }
    limitHint = DEFAULT_LIMIT_HINT;
  }

  public Entry find(long peerId) {
    int i;

    if (peerId == 0) {
      return null;
    }

    for (i = 0; i < entries.length; ++i) {
      if (entries[i].peerId == peerId) {
        return entries[i];
      }
    }
    return null;
  }

  public Entry upsert(long peerId) {
    Entry entry;
    int i;

    if (peerId == 0) {
      return null;
    }

    entry = find(peerId);
    if (entry != null) {
      return entry;
    }

    for (i = 0; i < entries.length; ++i) {
      if (entries[i].peerId == 0) {
        entries[i].peerId = peerId;
        return entries[i];
      }
    }
    return null;
  }

  public boolean seedFromDialog(long peerId, int pts) {
    Entry entry;

    if (peerId == 0 || pts <= 0) {
      return false;
    }

    entry = upsert(peerId);
    if (entry == null) {
      return false;
    }

    if (!entry.hasState || pts > entry.pts) {
      entry.pts = pts;
    }
    entry.hasState = true;
    entry.needsDifference = false;
    return true;
  }

  public boolean noteDifference(long peerId, int pts, int timeout, boolean isFinal) {
    Entry entry;

    if (peerId == 0) {
      return false;
    }

    entry = upsert(peerId);
    if (entry == null) {
      return false;
    }

    if (pts > 0) {
      if (!entry.hasState || pts > entry.pts) {
        entry.pts = pts;
      }
      entry.hasState = true;
    }
    entry.timeout = timeout;
    entry.needsDifference = !isFinal;
    return true;
  }

  public boolean noteLiveUpdate(long peerId, int pts) {
    Entry entry;

    if (peerId == 0 || pts <= 0) {
      return false;
    }

    entry = upsert(peerId);
    if (entry == null) {
      return false;
    }

    if (!entry.hasState || pts > entry.pts) {
      entry.pts = pts;
    }
    entry.hasState = true;
    entry.needsDifference = false;
    return true;
  }

  public boolean noteAcknowledgement(long peerId, int newPts, int ptsCount) {
    Entry entry = upsert(peerId);

    if (entry == null) {
      return false;
    }
    if (ptsCount < 0 || newPts <= ptsCount) {
      entry.needsDifference = true;
      return false;
    }
    if (entry.needsDifference) {
      return false;
    }
    if (entry.hasState) {
      if (newPts <= entry.pts) {
        return true;
      }
      if (newPts - ptsCount != entry.pts) {
        entry.needsDifference = true;
        return false;
      }
    }

    entry.pts = newPts;
    entry.hasState = true;
    entry.needsDifference = false;
    return true;
  }

  public boolean markDirty(long peerId) {
    Entry entry = upsert(peerId);

    if (entry == null) {
      return false;
    }
    entry.needsDifference = true;
    return true;
  }

  public DifferenceArgs buildDifferenceArgs(long peerId, int limit) {
    Entry entry = find(peerId);
    DifferenceArgs out = new DifferenceArgs();

    if (limit <= 0) {
      limit = limitHint > 0 ? limitHint : DEFAULT_LIMIT_HINT;
    }

    out.limit = limit;
    if (entry == null || !entry.hasState || entry.pts <= 0) {
      out.force = true;
      out.pts = 1;
    } else {
      out.force = false;
      out.pts = entry.pts;
    }
    return out;
  }
}
