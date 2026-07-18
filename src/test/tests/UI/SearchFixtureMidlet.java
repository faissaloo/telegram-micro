package ui;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

import reader.DialogListModel;

/** Account-free visual fixture for bounded in-chat search results. */
public final class SearchFixtureMidlet extends MIDlet {
  private AppShellScreen screen;

  public void startApp() {
    if (screen == null) {
      AppShellModel model = new AppShellModel();
      Display display = Display.getDisplay(this);

      model.onAuthorized();
      model.beginLocalDialogsLoad();
      model.appendLocalDialog(
        DialogListModel.PEER_USER,
        1001L,
        "Bob",
        "Meet by the clock.",
        2,
        false
      );
      model.finishLocalDialogsLoad();
      model.beginHistoryLoadForSelected();
      model.appendHistoryMessage(
        90, "Bob", "Cached history remains here.", false, false, false,
        "", "", null, 1700000000
      );
      model.finishHistoryLoad();
      model.openMessageSearch();
      model.beginMessageSearch("clock");
      model.messageSearch.appendResult(
        94, "Bob", "Meet under the station clock.", false, false,
        "", "", 1700000000
      );
      model.messageSearch.appendResult(
        91, "YOU", "The clock is beside platform three.", true, false,
        "", "", 1700000060
      );
      model.messageSearch.appendResult(
        87, "Bob", "Photo of the old clock", false, true,
        "PHOTO", "640x480", 1700000120
      );
      model.messageSearch.finish(17);
      model.onMessageSearchReady();

      screen = new AppShellScreen(this, display, model, null);
      screen.showMessageSearchResults();
    }

    System.out.println(
      "SEARCH FIXTURE READY: Bob scoped query clock; 3 of 17 bounded results."
    );
  }

  public void pauseApp() {
  }

  public void destroyApp(boolean unconditional) {
  }
}
