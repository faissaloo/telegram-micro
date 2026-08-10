package runtime;

import javax.microedition.midlet.MIDlet;

/** Target-VM probe for normalized emulator runtime profile propagation. */
public final class RuntimeProfileProbeMidlet extends MIDlet {
  public void startApp() {
    String expected = System.getProperty(
      "telegrammicro.expected.runtime.profile"
    );
    String actual = System.getProperty("cc.squirreljme.runtime.profile");

    if (expected == null || !expected.equals(actual)) {
      System.out.println(
        "RUNTIME PROFILE PROBE FAILED: expected=" + expected +
        " actual=" + actual
      );
      System.exit(1);
    }

    assertExpectedProperty("microedition.configuration");
    assertExpectedProperty("microedition.profiles");
    assertExpectedProperty("microedition.encoding");
    assertExpectedProperty("microedition.jtwi.version");
    assertExpectedProperty("microedition.msa.version");

    System.out.println("RUNTIME PROFILE PROBE PASSED: " + actual);
    System.exit(0);
  }

  private static void assertExpectedProperty(String property) {
    String expected = System.getProperty(
      "telegrammicro.expected.property." + property
    );

    if (expected == null) {
      return;
    }

    String actual = System.getProperty(property);
    if (!expected.equals(actual)) {
      System.out.println(
        "RUNTIME PROPERTY PROBE FAILED: property=" + property +
        " expected=" + expected + " actual=" + actual
      );
      System.exit(1);
    }
  }

  public void pauseApp() {
  }

  public void destroyApp(boolean unconditional) {
  }
}
