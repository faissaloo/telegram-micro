package reader;

public class PeerDirectory {
  public static final int DEFAULT_CAPACITY = 48;
  public static final int TITLE_MAX = 48;
  public static final int USERNAME_MAX = 24;

  public static class Item {
    public int peerKind = DialogListModel.PEER_UNKNOWN;
    public long peerId = 0;
    public long accessHash = 0;
    public boolean hasAccessHash = false;
    public boolean minAccessHash = false;
    public String title = "";
    public String username = "";

    public void clear() {
      peerKind = DialogListModel.PEER_UNKNOWN;
      peerId = 0;
      accessHash = 0;
      hasAccessHash = false;
      minAccessHash = false;
      title = "";
      username = "";
    }

    public void set(
      int peerKind,
      long peerId,
      long accessHash,
      boolean hasAccessHash,
      boolean minAccessHash,
      String title,
      String username
    ) {
      this.peerKind = peerKind;
      this.peerId = peerId;
      this.accessHash = accessHash;
      this.hasAccessHash = hasAccessHash;
      this.minAccessHash = minAccessHash;
      this.title = DialogListModel.limit(title, TITLE_MAX);
      this.username = DialogListModel.limit(username, USERNAME_MAX);
    }
  }

  public final Item[] items;
  public int count = 0;

  public PeerDirectory() {
    this(DEFAULT_CAPACITY);
  }

  public PeerDirectory(int capacity) {
    int i;

    if (capacity <= 0) {
      throw new IllegalArgumentException("Peer directory capacity must be positive.");
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

  public Item find(int peerKind, long peerId) {
    int i;

    for (i = 0; i < count; ++i) {
      if (items[i].peerKind == peerKind && items[i].peerId == peerId) {
        return items[i];
      }
    }
    return null;
  }

  public boolean upsertIdentity(
    int peerKind,
    long peerId,
    long accessHash,
    boolean hasAccessHash,
    boolean minAccessHash,
    String title,
    String username
  ) {
    Item item = find(peerKind, peerId);

    if (item != null) {
      if (hasAccessHash && (!item.hasAccessHash || !item.minAccessHash || !minAccessHash)) {
        item.accessHash = accessHash;
        item.hasAccessHash = true;
        item.minAccessHash = minAccessHash;
      }
      if (title != null && title.length() > 0) {
        item.title = DialogListModel.limit(title, TITLE_MAX);
      }
      if (username != null) {
        item.username = DialogListModel.limit(username, USERNAME_MAX);
      }
      return true;
    }

    if (count >= items.length) {
      return false;
    }

    items[count].set(peerKind, peerId, accessHash, hasAccessHash, minAccessHash, title, username);
    count += 1;
    return true;
  }

  public boolean copyFrom(PeerDirectory other) {
    int i;

    if (other == null || other.count > items.length) {
      return false;
    }

    clear();
    for (i = 0; i < other.count; ++i) {
      Item source = other.items[i];
      if (!upsertIdentity(
          source.peerKind,
          source.peerId,
          source.accessHash,
          source.hasAccessHash,
          source.minAccessHash,
          source.title,
          source.username)) {
        return false;
      }
    }
    return true;
  }

  public String titleOrFallback(int peerKind, long peerId) {
    Item item = find(peerKind, peerId);
    if (item != null && item.title.length() > 0) {
      return item.title;
    }

    switch (peerKind) {
      case DialogListModel.PEER_CHAT:
        return "CHAT " + Long.toString(peerId);
      case DialogListModel.PEER_CHANNEL:
        return "CHANNEL " + Long.toString(peerId);
      case DialogListModel.PEER_USER:
        return "USER " + Long.toString(peerId);
      default:
        return "PEER " + Long.toString(peerId);
    }
  }
}
