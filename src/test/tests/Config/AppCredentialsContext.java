package test;

import config.AppCredentials;

public class AppCredentialsContext {
  public static class ConfiguredCredentialsAreUsableTest extends Test {
    public String label() {
      return "Bundled app credentials are usable when generated";
    }

    public void test() throws TestFailureException {
      AppCredentials credentials = new AppCredentials(true, 123456, "abcdef0123456789");

      expect(credentials.isConfigured(), true);
      expect(credentials.headerApiId(), 123456);
    }
  }

  public static class MissingGeneratedCredentialsFallBackForHeaderTest extends Test {
    public String label() {
      return "Missing bundled credentials keep the legacy header fallback";
    }

    public void test() throws TestFailureException {
      AppCredentials credentials = new AppCredentials(false, 0, "");

      expect(credentials.isConfigured(), false);
      expect(credentials.headerApiId(), AppCredentials.LEGACY_HEADER_API_ID);
      expect(credentials.missingDetail().indexOf("TELEGRAM_MICRO_API_ID") >= 0, true);
    }
  }
}
