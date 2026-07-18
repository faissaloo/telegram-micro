package ui;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

import reader.DialogListModel;

/** Account-free visual fixture for the production history canvas. */
public final class HistoryFixtureMidlet extends MIDlet {
  private HistoryCanvas canvas;

  public void startApp() {
    if (canvas == null) {
      AppShellModel model = new AppShellModel();

      model.onAuthorized();
      model.beginLocalDialogsLoad();
      model.appendLocalDialog(
        DialogListModel.PEER_USER,
        1001L,
        "Bob",
        "On my way.",
        1,
        false
      );
      model.finishLocalDialogsLoad();
      model.reader.applyLiveOutboxRead(DialogListModel.PEER_USER, 1001L, 91);
      model.beginHistoryLoadForSelected();
      model.appendHistoryMessage(
        90, "Bob", "Meet at platform three.", false, false, false,
        "", "", null, 1700000000
      );
      model.appendHistoryMessage(
        91, "YOU", "On my way.", true, false, false,
        "", "", null, 1700000060
      );
      model.appendHistoryMessage(
        92, "YOU", "See you by the clock.", true, false, false,
        "", "", null, 1700000120
      );
      model.finishHistoryLoad();
      canvas = new HistoryCanvas(model);
    }

    Display.getDisplay(this).setCurrent(canvas);
    System.out.println("HISTORY FIXTURE READY: timestamps 22:13Z through 22:15Z; read then sent.");
  }

  public void pauseApp() {
  }

  public void destroyApp(boolean unconditional) {
  }
}
