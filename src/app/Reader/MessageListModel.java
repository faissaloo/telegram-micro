package reader;

import media.MediaReference;

public class MessageListModel {
  public static final int SENDER_MAX = 24;
  public static final int TEXT_MAX = 96;
  public static final int MEDIA_LABEL_MAX = 12;
  public static final int MEDIA_INFO_MAX = 48;
  public static final int DEFAULT_CAPACITY = 24;

  public static class Item {
    public int messageId = 0;
    public String sender = "";
    public String text = "";
    public String mediaLabel = "";
    public String mediaInfo = "";
    public final MediaReference mediaRef = new MediaReference();
    public boolean outgoing = false;
    public boolean pending = false;
    public boolean hasMedia = false;

    public void set(int messageId, String sender, String text, boolean outgoing, boolean pending, boolean hasMedia) {
      set(messageId, sender, text, outgoing, pending, hasMedia, "", "", null);
    }

    public void set(
      int messageId,
      String sender,
      String text,
      boolean outgoing,
      boolean pending,
      boolean hasMedia,
      String mediaLabel,
      String mediaInfo
    ) {
      set(messageId, sender, text, outgoing, pending, hasMedia, mediaLabel, mediaInfo, null);
    }

    public void set(
      int messageId,
      String sender,
      String text,
      boolean outgoing,
      boolean pending,
      boolean hasMedia,
      String mediaLabel,
      String mediaInfo,
      MediaReference mediaRef
    ) {
      this.messageId = messageId;
      this.sender = DialogListModel.limit(sender, SENDER_MAX);
      this.text = DialogListModel.limit(text, TEXT_MAX);
      this.mediaLabel = DialogListModel.limit(mediaLabel, MEDIA_LABEL_MAX);
      this.mediaInfo = DialogListModel.limit(mediaInfo, MEDIA_INFO_MAX);
      this.mediaRef.copyFrom(mediaRef);
      this.outgoing = outgoing;
      this.pending = pending;
      this.hasMedia = hasMedia;
    }

    public void copyFrom(Item other) {
      if (other == null) {
        clear();
        return;
      }
      set(
        other.messageId,
        other.sender,
        other.text,
        other.outgoing,
        other.pending,
        other.hasMedia,
        other.mediaLabel,
        other.mediaInfo,
        other.mediaRef
      );
    }

    public void clear() {
      messageId = 0;
      sender = "";
      text = "";
      mediaLabel = "";
      mediaInfo = "";
      mediaRef.clear();
      outgoing = false;
      pending = false;
      hasMedia = false;
    }

    public String mediaSummary() {
      if (!hasMedia) {
        return "";
      }
      if (mediaLabel != null && mediaLabel.length() > 0) {
        if (mediaInfo != null && mediaInfo.length() > 0) {
          return mediaLabel + " " + mediaInfo;
        }
        return mediaLabel;
      }
      if (mediaInfo != null && mediaInfo.length() > 0) {
        return mediaInfo;
      }
      return "MEDIA";
    }

    public String previewText() {
      if (text != null && text.length() > 0) {
        return text;
      }
      return mediaSummary();
    }
  }

  public final Item[] items;
  public int count = 0;
  public int selectedIndex = 0;

  public MessageListModel() {
    this(DEFAULT_CAPACITY);
  }

  public MessageListModel(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("History capacity must be positive.");
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

  public boolean append(String sender, String text, boolean outgoing, boolean pending) {
    return append(0, sender, text, outgoing, pending, false);
  }

  public boolean append(int messageId, String sender, String text, boolean outgoing, boolean pending, boolean hasMedia) {
    return append(messageId, sender, text, outgoing, pending, hasMedia, "", "");
  }

  public boolean append(
    int messageId,
    String sender,
    String text,
      boolean outgoing,
      boolean pending,
      boolean hasMedia,
      String mediaLabel,
      String mediaInfo
    ) {
      return append(messageId, sender, text, outgoing, pending, hasMedia, mediaLabel, mediaInfo, null);
    }

    public boolean append(
      int messageId,
      String sender,
      String text,
      boolean outgoing,
      boolean pending,
      boolean hasMedia,
      String mediaLabel,
      String mediaInfo,
      MediaReference mediaRef
    ) {
      if (count >= items.length) {
        return false;
      }

    items[count].set(messageId, sender, text, outgoing, pending, hasMedia, mediaLabel, mediaInfo, mediaRef);
    count += 1;
    if (selectedIndex >= count) {
      selectedIndex = count - 1;
    }
    return true;
  }

  public boolean pushBounded(String sender, String text, boolean outgoing, boolean pending) {
    return pushBounded(0, sender, text, outgoing, pending, false);
  }

  public boolean pushBounded(int messageId, String sender, String text, boolean outgoing, boolean pending, boolean hasMedia) {
    return pushBounded(messageId, sender, text, outgoing, pending, hasMedia, "", "");
  }

  public boolean pushBounded(
    int messageId,
    String sender,
    String text,
      boolean outgoing,
      boolean pending,
      boolean hasMedia,
      String mediaLabel,
      String mediaInfo
    ) {
      return pushBounded(messageId, sender, text, outgoing, pending, hasMedia, mediaLabel, mediaInfo, null);
    }

    public boolean pushBounded(
      int messageId,
      String sender,
      String text,
      boolean outgoing,
      boolean pending,
      boolean hasMedia,
      String mediaLabel,
      String mediaInfo,
      MediaReference mediaRef
    ) {
      int i;

      if (count < items.length) {
      return append(messageId, sender, text, outgoing, pending, hasMedia, mediaLabel, mediaInfo, mediaRef);
      }
    if (items.length <= 0) {
      return false;
    }

    for (i = 1; i < items.length; ++i) {
      items[i - 1].copyFrom(items[i]);
    }
    items[items.length - 1].set(messageId, sender, text, outgoing, pending, hasMedia, mediaLabel, mediaInfo, mediaRef);
    if (selectedIndex > 0) {
      selectedIndex -= 1;
    }
    return true;
  }

  public boolean removeAt(int index) {
    int i;

    if (index < 0 || index >= count) {
      return false;
    }

    for (i = index + 1; i < count; ++i) {
      items[i - 1].copyFrom(items[i]);
    }
    items[count - 1].clear();
    count -= 1;

    if (count <= 0) {
      selectedIndex = 0;
    } else if (selectedIndex >= count) {
      selectedIndex = count - 1;
    }
    return true;
  }

  public Item selected() {
    if (count == 0 || selectedIndex < 0 || selectedIndex >= count) {
      return null;
    }
    return items[selectedIndex];
  }

  public boolean selectNext() {
    if (selectedIndex + 1 >= count) {
      return false;
    }
    selectedIndex += 1;
    return true;
  }

  public boolean selectPrevious() {
    if (selectedIndex <= 0 || count == 0) {
      return false;
    }
    selectedIndex -= 1;
    return true;
  }

  public boolean selectLast() {
    if (count <= 0) {
      selectedIndex = 0;
      return false;
    }
    selectedIndex = count - 1;
    return true;
  }

  public int capacity() {
    return items.length;
  }
}
