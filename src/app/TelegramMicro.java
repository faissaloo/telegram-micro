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
import mtproto.send.SendPing;
import mtproto.UnencryptedResponse;
import mtproto.EncryptedResponse;
import mtproto.CombinatorIds;
import mtproto.recieve.RecieveMsgContainer;
import mtproto.recieve.RecieveNewSessionCreated;
import mtproto.recieve.RecievePong;

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
      MTProtoConnection connection = new MTProtoConnection("149.154.175.10");
      System.out.println("SENDING PING");
      (new SendPing(25565)).send(connection);
      System.out.println("WAITING FOR RESPONSE");
      connection.wait_for_response();
      System.out.println("GOT RESPONSE");
      EncryptedResponse encrypted_response = EncryptedResponse.from_tcp_response(connection.message_recieve_thread.dequeue_response(), connection);

      RecieveMsgContainer msg_container = RecieveMsgContainer.from_encrypted_message(encrypted_response);
      RecieveNewSessionCreated new_session_created = RecieveNewSessionCreated.from_encrypted_message(msg_container.messages[0]);
      System.out.println("NEW SESSION CREATED");
      System.out.println("First message id");
      System.out.println(new_session_created.first_msg_id);
      System.out.println("Unique id");
      System.out.println(new_session_created.unique_id);
      System.out.println("Server salt");
      System.out.println(new_session_created.server_salt);
      
      RecievePong pong = RecievePong.from_encrypted_message(msg_container.messages[1]);
      System.out.println("PONG");
      System.out.println("message id");
      System.out.println(pong.message_id);
      System.out.println("ping id");
      System.out.println(pong.ping_id);
      //process the pong too
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void pauseApp() {}

  public void destroyApp(boolean unconditional) {
    notifyDestroyed();
  }
}
