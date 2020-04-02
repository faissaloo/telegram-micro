import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.Connector;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import mtproto.ReqPqMulti;
import mtproto.RecieveResponseThread;
import mtproto.SendRequestThread;
import mtproto.UnencryptedResponse;
import mtproto.RecieveResPQ;

import support.BigInteger;

public class TelegramLite extends MIDlet {
  private Form form;
  private Display display;

  public TelegramLite() {
    super();
  }

  public void startApp() {
    //Apparently our current display has to be null otherwise it hangs, weird
    //form = new Form("Hello World");
    //String msg = getIP();
    //form.append(msg);
    //display = Display.getDisplay(this);
    //display.setCurrent(form);
    Display.getDisplay(this).setCurrent(null);

    try {
      SocketConnection api_connection = (SocketConnection) Connector.open("socket://149.154.167.40:443");
      SendRequestThread message_send_thread = new SendRequestThread(api_connection);
      RecieveResponseThread message_recieve_thread = new RecieveResponseThread(api_connection);
      message_send_thread.start(); //Should we have these start when they're instantiated?
      message_recieve_thread.start();

      ReqPqMulti key_exchange = new ReqPqMulti();
      key_exchange.send();
      System.out.println("QUEUED FOR SENDING!");

      while (true) {
        SendRequestThread.sleep(1); //Don't peg the CPU
        if (RecieveResponseThread.has_responses()) {
          UnencryptedResponse key_response = UnencryptedResponse.from_tcp_response(RecieveResponseThread.dequeue_response());
          RecieveResPQ a = RecieveResPQ.from_unencrypted_message(key_response);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void pauseApp() {}

  public void destroyApp(boolean unconditional) {
    notifyDestroyed();
  }
}
