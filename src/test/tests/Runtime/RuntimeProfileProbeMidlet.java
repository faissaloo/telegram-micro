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

    System.out.println("RUNTIME PROFILE PROBE PASSED: " + actual);
    System.exit(0);
  }

  public void pauseApp() {
  }

  public void destroyApp(boolean unconditional) {
  }
}
