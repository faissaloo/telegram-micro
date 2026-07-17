package test;

/**
 * Runs expensive, pure-Java cryptographic vectors on the host JVM. The same
 * tests remain available in the target-VM full lane for performance soaks.
 */
public final class SlowCryptoRunner {
  private SlowCryptoRunner() {
  }

  public static void main(String[] args) {
    Test[] tests = new Test[] {
      new HandleRecieveDHParamsOkContext.GenerateAuthKeyTest(),
      new LoginContext.ComputePasswordCheckVectorTest(),
      new PrimeDecomposerContext.DecomposeTest()
    };
    int failures = 0;

    System.out.println();
    System.out.println("[SlowCryptoRunner]");
    System.out.println("Loaded 3 expensive tests (host JVM proof lane)");
    for (int i = 0; i < tests.length; ++i) {
      Test current = tests[i];
      try {
        current.run_tests();
        System.out.println(" ✔ " + current.label());
      } catch (Throwable failure) {
        ++failures;
        System.out.println(" ✗ " + current.label());
        failure.printStackTrace();
      }
    }

    System.out.println();
    if (failures == 0) {
      System.out.println("(3/3) Expensive cryptographic vectors passed!");
    } else {
      System.out.println(
        "(" + Integer.toString(3 - failures) + "/3) " +
        "Expensive cryptographic vectors failed."
      );
    }
    System.out.println();
    System.exit(failures == 0 ? 0 : 1);
  }
}
