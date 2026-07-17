package test;

import ui.ScreenRouter;

public class ScreenRouterContext {
  public static class NavigationTracksPreviousScreenTest extends Test {
    public String label() {
      return "Screen router tracks current, previous, and revision";
    }

    public void test() throws TestFailureException {
      ScreenRouter router = new ScreenRouter(1);
      router.navigate(2);

      expect(router.currentScreen, 2);
      expect(router.previousScreen, 1);
      expect(router.revision, 2);
    }
  }
}
