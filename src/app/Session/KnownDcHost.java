package session;

public final class KnownDcHost {
  private KnownDcHost() {
  }

  public static String production(int dcId) {
    switch (dcId) {
      case 1:
        return "149.154.175.53";
      case 2:
        return "149.154.167.50";
      case 3:
        return "149.154.175.100";
      case 4:
        return "149.154.167.91";
      case 5:
        return "91.108.56.130";
      default:
        return null;
    }
  }
}
