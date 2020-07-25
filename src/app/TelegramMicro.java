import java.util.Date;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import javax.microedition.io.Connector;

import mtproto.MTProtoConnection;

import bouncycastle.BigInteger;

public class TelegramMicro extends MIDlet {
  private Form form;
  private Display display;

  public TelegramMicro() {
    super();
  }

  public void startApp() {
    Display display = Display.getDisplay(this);
    Form log = new Form("Telegram Micro");
    log.append("Connecting (this may take a while)...");
    display.setCurrent(log);

    try {
      MTProtoConnection connection = new MTProtoConnection("149.154.175.10", "5222");
      connection.get_auth_key();
      System.out.println(connection.auth_key_hash);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void pauseApp() {}

  public void destroyApp(boolean unconditional) {
    notifyDestroyed();
  }
}
