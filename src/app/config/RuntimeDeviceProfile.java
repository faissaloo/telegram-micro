package config;

public final class RuntimeDeviceProfile {
  public static final String DEFAULT_DEVICE_MODEL = "Unknown MIDP Device";
  public static final String DEFAULT_SYSTEM_VERSION = "MIDP / CLDC";
  public static final String DEFAULT_LANG_CODE = "en";

  public interface PropertySource {
    String get(String key);
  }

  private static final String[] MAKE_KEYS = new String[] {
    "device.manufacturer",
    "device.vendor",
    "com.nokia.mid.manufacturer"
  };

  private static final String[] MODEL_KEYS = new String[] {
    "device.model",
    "device.name",
    "phone.model"
  };

  private static final String[] LOCALE_KEYS = new String[] {
    "microedition.locale",
    "user.language"
  };

  public final String deviceModel;
  public final String systemVersion;
  public final String systemLangCode;
  public final String langCode;

  public RuntimeDeviceProfile(
    String deviceModel,
    String systemVersion,
    String systemLangCode,
    String langCode
  ) {
    this.deviceModel = fallback(deviceModel, DEFAULT_DEVICE_MODEL);
    this.systemVersion = fallback(systemVersion, DEFAULT_SYSTEM_VERSION);
    this.systemLangCode = fallback(systemLangCode, DEFAULT_LANG_CODE);
    this.langCode = fallback(langCode, this.systemLangCode);
  }

  public static RuntimeDeviceProfile detect() {
    return detect(new SystemPropertySource());
  }

  public static RuntimeDeviceProfile detect(PropertySource properties) {
    String rawPlatform = clean(read(properties, "microedition.platform"));
    String make = normalizeDeviceName(firstNonEmpty(properties, MAKE_KEYS), false);
    String model = normalizeDeviceName(firstNonEmpty(properties, MODEL_KEYS), false);
    String platformModel = normalizeDeviceName(rawPlatform, true);
    String deviceModel = detectDeviceModel(properties, rawPlatform, make, model, platformModel);
    String systemVersion = detectSystemVersion(properties, rawPlatform);
    String lang = detectLanguage(properties);

    return new RuntimeDeviceProfile(deviceModel, systemVersion, lang, lang);
  }

  private static String detectDeviceModel(
    PropertySource properties,
    String rawPlatform,
    String make,
    String model,
    String platformModel
  ) {
    String vmName = clean(read(properties, "java.vm.name"));
    String javaVendor = clean(read(properties, "java.vendor"));

    if (make.length() > 0 && model.length() > 0) {
      if (containsIgnoreCase(model, make)) {
        return model;
      }
      return make + " " + model;
    }
    if (model.length() > 0) {
      return model;
    }
    if (platformModel.length() > 0) {
      return platformModel;
    }
    if (make.length() > 0) {
      return make;
    }
    if (containsIgnoreCase(vmName, "squirreljme") ||
        containsIgnoreCase(javaVendor, "squirreljme")) {
      return "SquirrelJME Hosted";
    }
    if (rawPlatform.length() > 0) {
      return rawPlatform;
    }
    return DEFAULT_DEVICE_MODEL;
  }

  private static String detectSystemVersion(PropertySource properties, String rawPlatform) {
    String versionPart = clean(platformVersion(rawPlatform));
    String profiles = clean(read(properties, "microedition.profiles"));
    String configuration = clean(read(properties, "microedition.configuration"));
    String vmName = clean(read(properties, "java.vm.name"));
    String javaVendor = clean(read(properties, "java.vendor"));
    StringBuffer out = new StringBuffer();

    appendSegment(out, versionPart);
    appendSegment(out, profiles);
    appendSegment(out, configuration);

    if (out.length() == 0 &&
        (containsIgnoreCase(vmName, "squirreljme") ||
         containsIgnoreCase(javaVendor, "squirreljme"))) {
      appendSegment(out, "SquirrelJME Hosted");
    }

    if (out.length() == 0) {
      return DEFAULT_SYSTEM_VERSION;
    }
    return out.toString();
  }

  private static String detectLanguage(PropertySource properties) {
    String raw = clean(firstNonEmpty(properties, LOCALE_KEYS));
    StringBuffer out = new StringBuffer();
    int length = raw.length();

    for (int i = 0; i < length; ++i) {
      char ch = raw.charAt(i);

      if (out.length() > 0 && !isAsciiLetter(ch)) {
        break;
      }
      if (isAsciiLetter(ch)) {
        out.append(Character.toLowerCase(ch));
      }
    }

    if (out.length() == 0) {
      return DEFAULT_LANG_CODE;
    }
    return out.toString();
  }

  private static void appendSegment(StringBuffer out, String value) {
    String cleaned = clean(value);

    if (cleaned.length() == 0) {
      return;
    }
    if (out.length() > 0) {
      out.append(" / ");
    }
    out.append(cleaned);
  }

  private static String firstNonEmpty(PropertySource properties, String[] keys) {
    int i;

    for (i = 0; i < keys.length; ++i) {
      String value = clean(read(properties, keys[i]));

      if (value.length() > 0) {
        return value;
      }
    }
    return "";
  }

  private static String read(PropertySource properties, String key) {
    if (properties == null || key == null) {
      return null;
    }

    try {
      return properties.get(key);
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static String fallback(String value, String fallback) {
    String cleaned = clean(value);

    if (cleaned.length() == 0) {
      return fallback;
    }
    return cleaned;
  }

  private static String clean(String value) {
    StringBuffer out;
    boolean lastWasSpace = false;
    int length;
    int i;

    if (value == null) {
      return "";
    }

    value = value.trim();
    if (value.length() == 0) {
      return "";
    }

    out = new StringBuffer();
    length = value.length();
    for (i = 0; i < length; ++i) {
      char ch = value.charAt(i);

      if (ch <= ' ') {
        if (!lastWasSpace && out.length() > 0) {
          out.append(' ');
          lastWasSpace = true;
        }
        continue;
      }

      out.append(ch);
      lastWasSpace = false;
    }

    if (out.length() > 0 && out.charAt(out.length() - 1) == ' ') {
      out.setLength(out.length() - 1);
    }
    return out.toString();
  }

  private static String normalizeDeviceName(String value, boolean stripPlatformVersion) {
    String cleaned = clean(value);
    StringBuffer out;
    int length;
    int i;
    char previous = 0;

    if (cleaned.length() == 0) {
      return "";
    }
    if (stripPlatformVersion) {
      cleaned = platformBase(cleaned);
    }
    if (cleaned.length() == 0) {
      return "";
    }

    out = new StringBuffer();
    length = cleaned.length();
    for (i = 0; i < length; ++i) {
      char ch = cleaned.charAt(i);

      if (ch == '_' || ch == '\t') {
        ch = ' ';
      }

      if (out.length() > 0 && needsSeparator(previous, ch) &&
          out.charAt(out.length() - 1) != ' ') {
        out.append(' ');
      }

      if (ch == ' ') {
        if (out.length() > 0 && out.charAt(out.length() - 1) != ' ') {
          out.append(' ');
        }
      } else {
        out.append(ch);
      }

      previous = ch;
    }

    return clean(out.toString());
  }

  private static boolean needsSeparator(char previous, char current) {
    if (previous == 0 || previous == ' ' || current == ' ') {
      return false;
    }
    if (isLowerCaseAscii(previous) && isUpperCaseAscii(current)) {
      return true;
    }
    if (isLowerCaseAscii(previous) && isAsciiDigit(current)) {
      return true;
    }
    if (isAsciiDigit(previous) && isUpperCaseAscii(current)) {
      return true;
    }
    return false;
  }

  private static String platformBase(String rawPlatform) {
    int slash;

    rawPlatform = clean(rawPlatform);
    slash = rawPlatform.indexOf('/');
    if (slash >= 0) {
      return rawPlatform.substring(0, slash);
    }
    return rawPlatform;
  }

  private static String platformVersion(String rawPlatform) {
    int slash;

    rawPlatform = clean(rawPlatform);
    slash = rawPlatform.indexOf('/');
    if (slash >= 0 && slash + 1 < rawPlatform.length()) {
      return rawPlatform.substring(slash + 1);
    }
    return "";
  }

  private static boolean containsIgnoreCase(String text, String needle) {
    String lowerText;
    String lowerNeedle;

    if (text == null || needle == null) {
      return false;
    }
    lowerText = toLowerAscii(text);
    lowerNeedle = toLowerAscii(needle);
    return lowerText.indexOf(lowerNeedle) >= 0;
  }

  private static String toLowerAscii(String value) {
    StringBuffer out = new StringBuffer();
    int i;

    if (value == null) {
      return "";
    }

    for (i = 0; i < value.length(); ++i) {
      out.append(Character.toLowerCase(value.charAt(i)));
    }
    return out.toString();
  }

  private static boolean isAsciiLetter(char ch) {
    return isLowerCaseAscii(ch) || isUpperCaseAscii(ch);
  }

  private static boolean isLowerCaseAscii(char ch) {
    return ch >= 'a' && ch <= 'z';
  }

  private static boolean isUpperCaseAscii(char ch) {
    return ch >= 'A' && ch <= 'Z';
  }

  private static boolean isAsciiDigit(char ch) {
    return ch >= '0' && ch <= '9';
  }

  private static final class SystemPropertySource implements PropertySource {
    public String get(String key) {
      try {
        return System.getProperty(key);
      } catch (RuntimeException ignored) {
        return null;
      }
    }
  }
}
