package test;

import javax.microedition.midlet.MIDlet;
import java.util.Vector;

public class TestRunner extends MIDlet {
  private static Vector tests = new Vector();
  private static Vector failures = new Vector();
  private static int omitted = 0;
  private static boolean focus_mode = false;
  private static boolean full_mode = false;

  public TestRunner() {
    super();
    full_mode = "full".equals(getAppProperty("TelegramMicro-Test-Mode"));
    TestSuite.register_all_tests();
  }

  public void startApp() {
    run_tests();
  }

  private void run_tests() {
    System.out.println();
    System.out.println("[TestRunner]");
    System.out.println(
      "Loaded " + Integer.toString(tests.size()) + " tests (" +
      (full_mode ? "full" : "quick") + " lane)"
    );

    for (int i = 0; i < tests.size(); i++) {
      Test current_test = (Test)tests.elementAt(i);
      boolean omitted_for_focus = focus_mode && !current_test.focused();
      boolean omitted_for_lane = current_test.slow() && !full_mode;

      if (current_test.skipped()) {
        failures.addElement(new TestFailure(
          current_test,
          new TestFailureException(
            "Unclassified skipped test. Use SlowTest or make the gate explicit."
          )
        ));
        System.out.println(" ✗ " + current_test.label() + " (unclassified skip)");
        continue;
      }

      if (!omitted_for_focus && !omitted_for_lane) {
        try {
          current_test.run_tests();
          System.out.println(" ✔ " +current_test.label());
        } catch (Exception exception) {
          failures.addElement(new TestFailure(current_test, exception));
          System.out.println(" ✗ " +current_test.label());
        }
      }

      if (omitted_for_focus || omitted_for_lane) {
        omitted++;
        System.out.println(
          " - " + current_test.label() +
          (omitted_for_lane ? " (slow lane)" : " (focus filter)")
        );
      }
    }

    print_failures();
    print_conclusion();
    System.exit(failures.size() == 0 ? 0 : 1);
  }

  private String pass_fail_string() {
    return "(" + Integer.toString(tests.size()-failures.size()-omitted) + "/" + Integer.toString(tests.size()-omitted) + ")";
  }

  private void print_conclusion() {
    System.out.println();
    print_omitted();
    if (failures.size() == 0) {
      System.out.println(pass_fail_string() + " Test suite passed!");
    } else {
      System.out.println(pass_fail_string() + " Test suite failed :(");
    }
    System.out.println();
  }

  private void print_failures() {
    if (failures.size() > 0) {
      System.out.println();
    }
    for (int i = 0; i < failures.size(); i++) {
      TestFailure current_failure = (TestFailure)failures.elementAt(i);
      current_failure.print();
    }
  }

  private void print_omitted() {
    if (omitted > 0) {
      System.out.println(
        Integer.toString(omitted) +
        " tests omitted by the explicit " +
        (focus_mode ? "focus" : "quick") + " lane"
      );
    }
  }

  public void pauseApp() {}

  public void destroyApp(boolean unconditional) {
    notifyDestroyed();
  }

  public static void register(Test test) {
    focus_mode |= test.focused();
    tests.addElement(test);
  }
}
