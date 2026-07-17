package reader;

public class DialogListModel {
  public static final int PEER_UNKNOWN = 0;
  public static final int PEER_USER = 1;
  public static final int PEER_CHAT = 2;
  public static final int PEER_CHANNEL = 3;

  public static final int DEFAULT_CAPACITY = 12;
  public static final int TITLE_MAX = 32;
  public static final int PREVIEW_MAX = 72;

  public static class Item {
    public int peerKind = PEER_UNKNOWN;
    public long peerId = 0;
    public long accessHash = 0;
    public boolean hasAccessHash = false;
    public int topMessageId = 0;
    public String title = "";
    public String preview = "";
    public int unreadCount = 0;
    public boolean hasMedia = false;

    public void set(
      int peerKind,
      long peerId,
      long accessHash,
      boolean hasAccessHash,
      int topMessageId,
      String title,
      String preview,
      int unreadCount,
      boolean hasMedia
    ) {
      this.peerKind = peerKind;
      this.peerId = peerId;
      this.accessHash = accessHash;
      this.hasAccessHash = hasAccessHash;
      this.topMessageId = topMessageId;
      this.title = DialogListModel.limit(title, TITLE_MAX);
      this.preview = DialogListModel.limit(preview, PREVIEW_MAX);
      this.unreadCount = unreadCount;
      this.hasMedia = hasMedia;
    }

    public void clear() {
      peerKind = PEER_UNKNOWN;
      peerId = 0;
      accessHash = 0;
      hasAccessHash = false;
      topMessageId = 0;
      title = "";
      preview = "";
      unreadCount = 0;
      hasMedia = false;
    }
  }

  public final Item[] items;
  public int count = 0;
  public int selectedIndex = 0;

  public DialogListModel() {
    this(DEFAULT_CAPACITY);
  }

  public DialogListModel(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("Dialog capacity must be positive.");
    }

    items = new Item[capacity];
    for (int i = 0; i < items.length; ++i) {
      items[i] = new Item();
    }
  }

  public void clear() {
    for (int i = 0; i < count; ++i) {
      items[i].clear();
    }
    count = 0;
    selectedIndex = 0;
  }

  public boolean append(int peerKind, long peerId, String title, String preview, int unreadCount, boolean hasMedia) {
    return append(peerKind, peerId, 0, false, 0, title, preview, unreadCount, hasMedia);
  }

  public boolean append(
    int peerKind,
    long peerId,
    long accessHash,
    boolean hasAccessHash,
    int topMessageId,
    String title,
    String preview,
    int unreadCount,
    boolean hasMedia
  ) {
    if (count >= items.length) {
      return false;
    }

    items[count].set(peerKind, peerId, accessHash, hasAccessHash, topMessageId, title, preview, unreadCount, hasMedia);
    count += 1;
    if (selectedIndex >= count) {
      selectedIndex = count - 1;
    }
    return true;
  }

  public int indexOfPeer(int peerKind, long peerId) {
    int i;

    for (i = 0; i < count; ++i) {
      if (items[i].peerKind == peerKind && items[i].peerId == peerId) {
        return i;
      }
    }
    return -1;
  }

  public boolean prepend(
    int peerKind,
    long peerId,
    long accessHash,
    boolean hasAccessHash,
    int topMessageId,
    String title,
    String preview,
    int unreadCount,
    boolean hasMedia
  ) {
    Item slot;
    int i;

    if (count >= items.length) {
      return false;
    }

    slot = items[count];
    for (i = count; i > 0; --i) {
      items[i] = items[i - 1];
    }
    items[0] = slot;
    slot.set(peerKind, peerId, accessHash, hasAccessHash, topMessageId, title, preview, unreadCount, hasMedia);
    count += 1;
    if (count == 1) {
      selectedIndex = 0;
    } else {
      selectedIndex += 1;
    }
    return true;
  }

  public boolean upsertActivity(
    int peerKind,
    long peerId,
    long accessHash,
    boolean hasAccessHash,
    int topMessageId,
    String title,
    String preview,
    int unreadCount,
    boolean hasMedia
  ) {
    int index = indexOfPeer(peerKind, peerId);
    Item item;

    if (index < 0) {
      return prepend(
        peerKind,
        peerId,
        accessHash,
        hasAccessHash,
        topMessageId,
        title,
        preview,
        unreadCount,
        hasMedia
      );
    }

    item = items[index];
    if (hasAccessHash || !item.hasAccessHash) {
      item.accessHash = accessHash;
      item.hasAccessHash = hasAccessHash;
    }
    if (topMessageId != 0) {
      item.topMessageId = topMessageId;
    }
    if (title != null && title.length() > 0) {
      item.title = limit(title, TITLE_MAX);
    }
    item.preview = limit(preview, PREVIEW_MAX);
    item.unreadCount = unreadCount > 0 ? unreadCount : 0;
    item.hasMedia = hasMedia;
    if (index > 0) {
      moveToFront(index);
    }
    return true;
  }

  public Item selected() {
    if (count == 0 || selectedIndex < 0 || selectedIndex >= count) {
      return null;
    }
    return items[selectedIndex];
  }

  public boolean selectIndex(int index) {
    if (index < 0 || index >= count) {
      return false;
    }
    selectedIndex = index;
    return true;
  }

  public boolean selectNext() {
    return selectIndex(selectedIndex + 1);
  }

  public boolean selectPrevious() {
    return selectIndex(selectedIndex - 1);
  }

  public int capacity() {
    return items.length;
  }

  private void moveToFront(int index) {
    Item moving;
    int i;

    if (index <= 0 || index >= count) {
      return;
    }

    moving = items[index];
    for (i = index; i > 0; --i) {
      items[i] = items[i - 1];
    }
    items[0] = moving;

    if (selectedIndex == index) {
      selectedIndex = 0;
    } else if (selectedIndex < index) {
      selectedIndex += 1;
    }
  }

  public static String limit(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    if (value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }
}
