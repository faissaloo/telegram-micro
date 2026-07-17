package ui;

public class ScreenRouter {
  public int currentScreen;
  public int previousScreen;
  public int revision;

  public ScreenRouter(int initialScreen) {
    init(initialScreen);
  }

  public void init(int initialScreen) {
    currentScreen = initialScreen;
    previousScreen = initialScreen;
    revision = 1;
  }

  public void navigate(int nextScreen) {
    if (revision == 0) {
      init(nextScreen);
      return;
    }

    if (currentScreen != nextScreen) {
      previousScreen = currentScreen;
      currentScreen = nextScreen;
      revision += 1;
    }
  }
}
