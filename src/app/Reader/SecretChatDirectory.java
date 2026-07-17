package reader;

public final class SecretChatDirectory {
  public static final int DEFAULT_CAPACITY = 12;
  public static final int PENDING_KEY_BYTES_MAX = 256;

  public static final int STATE_UNKNOWN = 0;
  public static final int STATE_WAITING = 1;
  public static final int STATE_REQUESTED = 2;
  public static final int STATE_READY = 3;
  public static final int STATE_CLOSED = 4;

  public static final class Item {
    public int chatId = 0;
    public int peerKind = DialogListModel.PEER_UNKNOWN;
    public long peerId = 0;
    public long accessHash = 0;
    public int state = STATE_UNKNOWN;
    public boolean outbound = false;
    public int date = 0;
    public long keyFingerprint = 0;
    public byte[] pendingKeyData = null;

    public void clear() {
      chatId = 0;
      peerKind = DialogListModel.PEER_UNKNOWN;
      peerId = 0;
      accessHash = 0;
      state = STATE_UNKNOWN;
      outbound = false;
      date = 0;
      keyFingerprint = 0;
      pendingKeyData = null;
    }

    public void set(
      int chatId,
      int peerKind,
      long peerId,
      long accessHash,
      int state,
      boolean outbound,
      int date,
      long keyFingerprint,
      byte[] pendingKeyData,
      int pendingKeyLength
    ) {
      this.chatId = chatId;
      this.peerKind = peerKind;
      this.peerId = peerId;
      this.accessHash = accessHash;
      this.state = state;
      this.outbound = outbound;
      this.date = date;
      this.keyFingerprint = keyFingerprint;
      if (pendingKeyData != null && pendingKeyLength > 0) {
        if (this.pendingKeyData == null || this.pendingKeyData.length != pendingKeyLength) {
          this.pendingKeyData = new byte[pendingKeyLength];
        }
        System.arraycopy(pendingKeyData, 0, this.pendingKeyData, 0, pendingKeyLength);
      } else {
        this.pendingKeyData = null;
      }
    }
  }

  public final Item[] items;
  public int count = 0;

  public SecretChatDirectory() {
    this(DEFAULT_CAPACITY);
  }

  public SecretChatDirectory(int capacity) {
    int i;

    if (capacity <= 0) {
      throw new IllegalArgumentException("Secret chat capacity must be positive.");
    }

    items = new Item[capacity];
    for (i = 0; i < items.length; ++i) {
      items[i] = new Item();
    }
  }

  public void clear() {
    int i;

    for (i = 0; i < count; ++i) {
      items[i].clear();
    }
    count = 0;
  }

  public Item findByChatId(int chatId) {
    int i;

    if (chatId == 0) {
      return null;
    }

    for (i = 0; i < count; ++i) {
      if (items[i].chatId == chatId) {
        return items[i];
      }
    }
    return null;
  }

  public Item findByPeer(int peerKind, long peerId) {
    int i;

    if (peerKind == DialogListModel.PEER_UNKNOWN || peerId == 0) {
      return null;
    }

    for (i = 0; i < count; ++i) {
      if (items[i].peerKind == peerKind && items[i].peerId == peerId) {
        return items[i];
      }
    }
    return null;
  }

  public boolean upsert(int chatId, int peerKind, long peerId, long accessHash, int state, boolean outbound, int date) {
    return upsert(chatId, peerKind, peerId, accessHash, state, outbound, date, 0, null, 0);
  }

  public boolean upsert(
    int chatId,
    int peerKind,
    long peerId,
    long accessHash,
    int state,
    boolean outbound,
    int date,
    long keyFingerprint,
    byte[] pendingKeyData,
    int pendingKeyLength
  ) {
    if (pendingKeyLength < 0 || pendingKeyLength > PENDING_KEY_BYTES_MAX ||
        (pendingKeyLength > 0 &&
          (pendingKeyData == null || pendingKeyLength > pendingKeyData.length))) {
      return false;
    }
    Item item = findByChatId(chatId);

    if (item == null) {
      item = findByPeer(peerKind, peerId);
    }

    if (item == null) {
      if (count < items.length) {
        item = items[count];
        count += 1;
      } else {
        item = oldestEntry();
      }
    }

    if (item == null) {
      return false;
    }

    item.set(chatId, peerKind, peerId, accessHash, state, outbound, date, keyFingerprint, pendingKeyData, pendingKeyLength);
    return true;
  }

  public boolean copyFrom(SecretChatDirectory other) {
    int i;

    if (other == null || other.count > items.length) {
      return false;
    }

    clear();
    for (i = 0; i < other.count; ++i) {
      Item source = other.items[i];
      if (!upsert(
          source.chatId,
          source.peerKind,
          source.peerId,
          source.accessHash,
          source.state,
          source.outbound,
          source.date,
          source.keyFingerprint,
          source.pendingKeyData,
          source.pendingKeyData != null ? source.pendingKeyData.length : 0)) {
        return false;
      }
    }
    return true;
  }

  private Item oldestEntry() {
    Item oldest = null;
    int i;

    for (i = 0; i < count; ++i) {
      if (oldest == null || items[i].date < oldest.date) {
        oldest = items[i];
      }
    }
    return oldest;
  }
}
