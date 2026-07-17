package session;

public final class FailureLog {
  public static final int MAX_ENTRIES = 8;

  public static final int CATEGORY_NONE = 0;
  public static final int CATEGORY_NETWORK = 1;
  public static final int CATEGORY_BOOTSTRAP = 2;
  public static final int CATEGORY_STORAGE = 3;
  public static final int CATEGORY_READER = 4;
  public static final int CATEGORY_DEVICE = 5;

  public static final int SEVERITY_INFO = 0;
  public static final int SEVERITY_WARN = 1;
  public static final int SEVERITY_ERROR = 2;

  public static final class Entry {
    public int sequence;
    public int category;
    public int severity;
    public int code;
    public int detail;

    public void clear() {
      sequence = 0;
      category = CATEGORY_NONE;
      severity = SEVERITY_INFO;
      code = 0;
      detail = 0;
    }
  }

  public final Entry[] entries;
  public int nextSequence;
  public int count;
  public int head;

  public FailureLog() {
    int i;

    entries = new Entry[MAX_ENTRIES];
    for (i = 0; i < entries.length; ++i) {
      entries[i] = new Entry();
    }
    clear();
  }

  public void clear() {
    int i;

    nextSequence = 0;
    count = 0;
    head = 0;
    for (i = 0; i < entries.length; ++i) {
      entries[i].clear();
    }
  }

  public void push(int category, int severity, int code, int detail) {
    Entry entry = entries[head];

    entry.sequence = nextSequence++;
    entry.category = category;
    entry.severity = severity;
    entry.code = code;
    entry.detail = detail;

    head += 1;
    if (head >= MAX_ENTRIES) {
      head = 0;
    }
    if (count < MAX_ENTRIES) {
      count += 1;
    }
  }

  public Entry latest() {
    if (count <= 0) {
      return null;
    }

    return newestAt(0);
  }

  public Entry newestAt(int offset) {
    int index;

    if (offset < 0 || offset >= count) {
      return null;
    }

    index = head - 1 - offset;
    while (index < 0) {
      index += MAX_ENTRIES;
    }
    return entries[index];
  }

  public static String categoryName(int category) {
    switch (category) {
      case CATEGORY_NETWORK:
        return "NETWORK";
      case CATEGORY_BOOTSTRAP:
        return "BOOT";
      case CATEGORY_STORAGE:
        return "STORAGE";
      case CATEGORY_READER:
        return "READER";
      case CATEGORY_DEVICE:
        return "DEVICE";
      case CATEGORY_NONE:
      default:
        return "NONE";
    }
  }

  public static String severityName(int severity) {
    switch (severity) {
      case SEVERITY_INFO:
        return "INFO";
      case SEVERITY_WARN:
        return "WARN";
      case SEVERITY_ERROR:
        return "ERROR";
      default:
        return "UNKNOWN";
    }
  }
}
