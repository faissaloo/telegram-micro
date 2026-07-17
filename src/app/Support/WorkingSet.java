package support;

public class WorkingSet {
  public static final int WORK_AUTH = 0;
  public static final int WORK_SESSION = 1;
  public static final int WORK_UI = 2;
  public static final int WORK_READER_DIALOGS = 3;
  public static final int WORK_READER_HISTORY = 4;
  public static final int WORK_MEDIA_QUEUE = 5;
  public static final int WORK_DEVICE_STATE = 6;

  public static final int MAX_ITEMS = 8;

  public static class Item {
    public int kind = WORK_AUTH;
    public int bytes = 0;
    public boolean required = false;

    public void set(int kind, int bytes, boolean required) {
      this.kind = kind;
      this.bytes = bytes;
      this.required = required;
    }
  }

  public static class Plan {
    public int requiredBytes = 0;
    public int optionalBytes = 0;
    public int totalBytes = 0;
    public int availableBytes = 0;
    public int optionalFitBytes = 0;
    public int headroomBytes = 0;
    public int idleBudgetBytes = 0;
    public boolean fitsInRam = false;
    public boolean optionalFits = false;
    public boolean allowIdlePrefetch = false;
  }

  public final Item[] items;
  public int count = 0;
  public int ramBudgetBytes = 0;
  public int ramReservedBytes = 0;

  public WorkingSet() {
    items = new Item[MAX_ITEMS];
    for (int i = 0; i < items.length; ++i) {
      items[i] = new Item();
    }
  }

  public void init(int ramBudgetBytes, int ramReservedBytes) {
    this.ramBudgetBytes = ramBudgetBytes;
    this.ramReservedBytes = ramReservedBytes;
    count = 0;
  }

  public boolean add(int kind, int bytes, boolean required) {
    if (count >= items.length) {
      return false;
    }
    items[count].set(kind, bytes, required);
    count += 1;
    return true;
  }

  public Plan plan() {
    Plan plan = new Plan();
    plan.availableBytes = ramReservedBytes >= ramBudgetBytes ? 0 : (ramBudgetBytes - ramReservedBytes);

    for (int i = 0; i < count; ++i) {
      plan.totalBytes += items[i].bytes;
      if (items[i].required) {
        plan.requiredBytes += items[i].bytes;
      } else {
        plan.optionalBytes += items[i].bytes;
      }
    }

    plan.fitsInRam = plan.requiredBytes <= plan.availableBytes;
    plan.optionalFitBytes = plan.availableBytes > plan.requiredBytes ? (plan.availableBytes - plan.requiredBytes) : 0;
    plan.optionalFits = plan.totalBytes <= plan.availableBytes;
    plan.headroomBytes = plan.fitsInRam ? plan.availableBytes - plan.requiredBytes : 0;
    plan.idleBudgetBytes = plan.optionalFitBytes / 2;
    if (plan.idleBudgetBytes > 32768) {
      plan.idleBudgetBytes = 32768;
    }
    plan.allowIdlePrefetch = plan.fitsInRam && plan.headroomBytes >= 8192;
    return plan;
  }

  public static String name(int kind) {
    switch (kind) {
      case WORK_AUTH:
        return "AUTH";
      case WORK_SESSION:
        return "SESSION";
      case WORK_UI:
        return "UI";
      case WORK_READER_DIALOGS:
        return "DIALOGS";
      case WORK_READER_HISTORY:
        return "HISTORY";
      case WORK_MEDIA_QUEUE:
        return "MEDIAQ";
      case WORK_DEVICE_STATE:
        return "DEVICE";
      default:
        return "UNKNOWN";
    }
  }
}
