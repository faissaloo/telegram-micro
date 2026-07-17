package test;

import java.util.Hashtable;

import config.RuntimeDeviceProfile;

public class RuntimeDeviceProfileContext {
  private static final class HashtablePropertySource implements RuntimeDeviceProfile.PropertySource {
    private final Hashtable values = new Hashtable();

    public HashtablePropertySource with(String key, String value) {
      values.put(key, value);
      return this;
    }

    public String get(String key) {
      return (String) values.get(key);
    }
  }

  public static class PlatformPropertyBuildsRealDeviceModelTest extends Test {
    public String label() {
      return "Runtime device profile uses microedition.platform instead of a fake target model";
    }

    public void test() throws TestFailureException {
      RuntimeDeviceProfile profile = RuntimeDeviceProfile.detect(
        new HashtablePropertySource()
          .with("microedition.platform", "NokiaC2-01/06.97")
          .with("microedition.profiles", "MIDP-2.1")
          .with("microedition.configuration", "CLDC-1.1")
          .with("microedition.locale", "en_GB")
      );

      expect(profile.deviceModel, "Nokia C2-01");
      expect(profile.systemVersion, "06.97 / MIDP-2.1 / CLDC-1.1");
      expect(profile.systemLangCode, "en");
      expect(profile.langCode, "en");
    }
  }

  public static class ManufacturerAndModelPropertiesCombineTest extends Test {
    public String label() {
      return "Runtime device profile combines manufacturer and model properties";
    }

    public void test() throws TestFailureException {
      RuntimeDeviceProfile profile = RuntimeDeviceProfile.detect(
        new HashtablePropertySource()
          .with("device.manufacturer", "Sony Ericsson")
          .with("device.model", "K800i")
          .with("microedition.profiles", "MIDP-2.0")
          .with("microedition.configuration", "CLDC-1.1")
          .with("microedition.locale", "sv-SE")
      );

      expect(profile.deviceModel, "Sony Ericsson K800i");
      expect(profile.systemVersion, "MIDP-2.0 / CLDC-1.1");
      expect(profile.systemLangCode, "sv");
    }
  }

  public static class SquirrelJmeFallbackUsesRuntimeIdentityTest extends Test {
    public String label() {
      return "Runtime device profile falls back to the current runtime identity";
    }

    public void test() throws TestFailureException {
      RuntimeDeviceProfile profile = RuntimeDeviceProfile.detect(
        new HashtablePropertySource()
          .with("java.vm.name", "SquirrelJME Hosted")
      );

      expect(profile.deviceModel, "SquirrelJME Hosted");
      expect(profile.systemVersion, "SquirrelJME Hosted");
      expect(profile.systemLangCode, RuntimeDeviceProfile.DEFAULT_LANG_CODE);
    }
  }
}
