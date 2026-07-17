package config;

public final class AppCredentials {
  public static final int LEGACY_HEADER_API_ID = 487068;

  public final boolean configured;
  public final int apiId;
  public final String apiHash;

  public AppCredentials(boolean configured, int apiId, String apiHash) {
    this.configured = configured;
    this.apiId = apiId;
    this.apiHash = apiHash != null ? apiHash : "";
  }

  public static AppCredentials bundled() {
    return new AppCredentials(
      GeneratedApiConfig.CONFIGURED,
      GeneratedApiConfig.API_ID,
      GeneratedApiConfig.API_HASH
    );
  }

  public boolean isConfigured() {
    return configured && apiId > 0 && apiHash.length() > 0;
  }

  public int headerApiId() {
    return isConfigured() ? apiId : LEGACY_HEADER_API_ID;
  }

  public String missingDetail() {
    return "Rebuild with TELEGRAM_MICRO_API_ID and TELEGRAM_MICRO_API_HASH.";
  }
}
