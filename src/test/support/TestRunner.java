package test;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import java.util.Vector;

public class TestRunner extends MIDlet {
  private Form form;
  private Display display;
  private static Vector tests = new Vector();
  private static Vector failures = new Vector();
  private static int skipped = 0;
  private static boolean focus_mode = false;

  public TestRunner() {
    super();
    TestSuite.register_all_tests();
  }

  public void startApp() {
    form = new Form("TestRunner");
    form.append("See console for test suite status");
    display = Display.getDisplay(this);
    display.setCurrent(form);
    run_tests();
  }

  private void run_tests() {
    System.out.println();
    System.out.println("[TestRunner]");
    System.out.println("Loaded "+Integer.toString(tests.size())+" tests");

    for (int i = 0; i < tests.size(); i++) {
      Test current_test = (Test)tests.elementAt(i);
      if ((!focus_mode) || current_test.focused()) {
        try {
          current_test.run_tests();
          System.out.println(" ✔ " +current_test.label());
        } catch (Exception exception) {
          failures.addElement(new TestFailure(current_test, exception));
          System.out.println(" ✗ " +current_test.label());
        }
      }

      if (current_test.skipped() || (focus_mode && !current_test.focused())) {
        skipped++;
        System.out.println(" - " +current_test.label());
      }
    }

    print_failures();
    print_conclusion();
  }

  private String pass_fail_string() {
    return "(" + Integer.toString(tests.size()-failures.size()-skipped) + "/" + Integer.toString(tests.size()-skipped) + ")";
  }

  private void print_conclusion() {
    System.out.println();
    print_skipped();
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

  private void print_skipped() {
    if (skipped > 0) {
      System.out.println(Integer.toString(skipped)+" tests skipped");
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
