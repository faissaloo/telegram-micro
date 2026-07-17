package media;

import reader.DialogListModel;

public final class MediaPlanQueue {
  public static final int DEFAULT_CAPACITY = 6;
  public static final int STATUS_NONE = 0;
  public static final int STATUS_QUEUED = 1;
  public static final int STATUS_LOADING = 2;
  public static final int STATUS_READY = 3;
  public static final int STATUS_FAILED = 4;
  public static final int DATA_BYTES_MAX = 48 * 1024;
  public static final int TITLE_MAX = DialogListModel.TITLE_MAX;
  public static final int LABEL_MAX = 12;
  public static final int DETAIL_MAX = 64;

  public static final class Entry {
    public int peerKind = DialogListModel.PEER_UNKNOWN;
    public long peerId = 0;
    public int messageId = 0;
    public String peerTitle = "";
    public String label = "";
    public String detail = "";
    public final MediaReference mediaRef = new MediaReference();
    public int status = STATUS_NONE;
    public int loadedBytes = 0;
    public int expectedBytes = 0;
    public byte[] data = null;

    public void clear() {
      peerKind = DialogListModel.PEER_UNKNOWN;
      peerId = 0;
      messageId = 0;
      peerTitle = "";
      label = "";
      detail = "";
      mediaRef.clear();
      status = STATUS_NONE;
      loadedBytes = 0;
      expectedBytes = 0;
      data = null;
    }

    public void set(
      int peerKind,
      long peerId,
      int messageId,
      String peerTitle,
      String label,
      String detail,
      MediaReference mediaRef
    ) {
      this.peerKind = peerKind;
      this.peerId = peerId;
      this.messageId = messageId;
      this.peerTitle = DialogListModel.limit(peerTitle, TITLE_MAX);
      this.label = DialogListModel.limit(label, LABEL_MAX);
      this.detail = DialogListModel.limit(detail, DETAIL_MAX);
      this.mediaRef.copyFrom(mediaRef);
    }

    public boolean matches(int peerKind, long peerId, int messageId) {
      return this.peerKind == peerKind && this.peerId == peerId && this.messageId == messageId;
    }
  }

  public final Entry[] items;
  public int count = 0;

  public MediaPlanQueue() {
    this(DEFAULT_CAPACITY);
  }

  public MediaPlanQueue(int capacity) {
    int i;

    if (capacity <= 0) {
      throw new IllegalArgumentException("Media plan queue capacity must be positive.");
    }

    items = new Entry[capacity];
    for (i = 0; i < items.length; ++i) {
      items[i] = new Entry();
    }
  }

  public void clear() {
    int i;

    count = 0;
    for (i = 0; i < items.length; ++i) {
      items[i].clear();
    }
  }

  public boolean contains(int peerKind, long peerId, int messageId) {
    return indexOf(peerKind, peerId, messageId) >= 0;
  }

  public boolean enqueue(
    int peerKind,
    long peerId,
    int messageId,
    String peerTitle,
    String label,
    String detail,
    MediaReference mediaRef
  ) {
    int index = indexOf(peerKind, peerId, messageId);
    int i;

    if (peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0 || messageId == 0) {
      return false;
    }
    if (index >= 0) {
      items[index].set(peerKind, peerId, messageId, peerTitle, label, detail, mediaRef);
      if (items[index].status == STATUS_NONE) {
        items[index].status = STATUS_QUEUED;
      }
      return true;
    }

    if (count < items.length) {
      items[count].set(peerKind, peerId, messageId, peerTitle, label, detail, mediaRef);
      items[count].status = STATUS_QUEUED;
      count++;
      return true;
    }

    for (i = 1; i < items.length; ++i) {
      items[i - 1].set(
        items[i].peerKind,
        items[i].peerId,
        items[i].messageId,
        items[i].peerTitle,
        items[i].label,
        items[i].detail,
        items[i].mediaRef
      );
      items[i - 1].status = items[i].status;
      items[i - 1].loadedBytes = items[i].loadedBytes;
      items[i - 1].expectedBytes = items[i].expectedBytes;
      items[i - 1].data = items[i].data;
    }
    items[items.length - 1].set(peerKind, peerId, messageId, peerTitle, label, detail, mediaRef);
    items[items.length - 1].status = STATUS_QUEUED;
    items[items.length - 1].loadedBytes = 0;
    items[items.length - 1].expectedBytes = 0;
    items[items.length - 1].data = null;
    return true;
  }

  public Entry entryFor(int peerKind, long peerId, int messageId) {
    int index = indexOf(peerKind, peerId, messageId);

    if (index < 0) {
      return null;
    }
    return items[index];
  }

  public Entry nextQueued() {
    int i;

    for (i = 0; i < count; ++i) {
      if (items[i].status == STATUS_QUEUED) {
        return items[i];
      }
    }
    return null;
  }

  public boolean markLoading(int peerKind, long peerId, int messageId, int expectedBytes) {
    Entry entry = entryFor(peerKind, peerId, messageId);

    if (entry == null) {
      return false;
    }
    entry.status = STATUS_LOADING;
    entry.loadedBytes = 0;
    entry.expectedBytes = expectedBytes > 0 ? expectedBytes : 0;
    entry.data = null;
    return true;
  }

  public boolean markProgress(int peerKind, long peerId, int messageId, int loadedBytes, int expectedBytes) {
    Entry entry = entryFor(peerKind, peerId, messageId);

    if (entry == null) {
      return false;
    }
    entry.loadedBytes = loadedBytes > 0 ? loadedBytes : 0;
    entry.expectedBytes = expectedBytes > 0 ? expectedBytes : 0;
    if (entry.status == STATUS_QUEUED) {
      entry.status = STATUS_LOADING;
    }
    return true;
  }

  public boolean markReady(int peerKind, long peerId, int messageId, byte[] data, int expectedBytes) {
    Entry entry = entryFor(peerKind, peerId, messageId);

    if (entry == null || data == null || data.length <= 0 || data.length > DATA_BYTES_MAX) {
      return false;
    }
    clearStoredDataExcept(entry);
    entry.data = new byte[data.length];
    System.arraycopy(data, 0, entry.data, 0, data.length);
    entry.loadedBytes = data.length;
    entry.expectedBytes = expectedBytes > 0 ? expectedBytes : data.length;
    entry.status = STATUS_READY;
    return true;
  }

  public boolean markFailed(int peerKind, long peerId, int messageId, String detail) {
    Entry entry = entryFor(peerKind, peerId, messageId);

    if (entry == null) {
      return false;
    }
    entry.status = STATUS_FAILED;
    entry.data = null;
    if (detail != null && detail.length() > 0) {
      entry.detail = DialogListModel.limit(detail, DETAIL_MAX);
    }
    return true;
  }

  public int totalStoredBytes() {
    int total = 0;
    int i;

    for (i = 0; i < count; ++i) {
      if (items[i].data != null) {
        total += items[i].data.length;
      }
    }
    return total;
  }

  public Entry latest() {
    if (count <= 0) {
      return null;
    }
    return items[count - 1];
  }

  private int indexOf(int peerKind, long peerId, int messageId) {
    int i;

    for (i = 0; i < count; ++i) {
      if (items[i].matches(peerKind, peerId, messageId)) {
        return i;
      }
    }
    return -1;
  }

  private void clearStoredDataExcept(Entry keep) {
    int i;

    for (i = 0; i < count; ++i) {
      if (items[i] != keep) {
        items[i].data = null;
      }
    }
  }
}
