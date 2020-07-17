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

import mtproto.TelegramPublicKeys;
import mtproto.SendReqPqMulti;
import mtproto.SendReqDhParams;
import mtproto.RecieveResponseThread;
import mtproto.SendRequestThread;
import mtproto.UnencryptedResponse;
import mtproto.RecieveResPQ;
import mtproto.PrimeDecomposer;
import mtproto.SendPing;
import mtproto.CombinatorIds;
import mtproto.RecieveServerDHParamsOk;

import crypto.RSAPublicKey;
import crypto.SecureRandomPlus;

import support.Integer128;
import support.Integer256;
import support.RandomPlus;

public class TelegramMicro extends MIDlet {
  private Form form;
  private Display display;

  public TelegramMicro() {
    super();
  }

  public void startApp() {
    Display.getDisplay(this).setCurrent(null);

    try {
      SocketConnection api_connection = (SocketConnection) Connector.open("socket://149.154.167.40:443");
      SendRequestThread message_send_thread = new SendRequestThread(api_connection);
      RecieveResponseThread message_recieve_thread = new RecieveResponseThread(api_connection);

      message_send_thread.start(); //Should we have these start when they're instantiated?
      message_recieve_thread.start();

      SecureRandomPlus random_number_generator = new SecureRandomPlus();
      Integer128 nonce = random_number_generator.nextInteger128();
      Integer256 second_nonce = null;

      SendReqPqMulti key_exchange = new SendReqPqMulti(nonce);
      key_exchange.send();
      System.out.println("REQUESTING PROOF OF WORK PARAMETERS");

      while (true) {
        SendRequestThread.sleep(1); //Don't peg the CPU
        if (RecieveResponseThread.has_responses()) {
          UnencryptedResponse unencrypted_response = UnencryptedResponse.from_tcp_response(RecieveResponseThread.dequeue_response());
          if (unencrypted_response.type() == CombinatorIds.resPQ) {
            RecieveResPQ pq_data = RecieveResPQ.from_unencrypted_message(unencrypted_response);
            PrimeDecomposer.Coprimes decomposed_pq = PrimeDecomposer.decompose(pq_data.pq);
            TelegramPublicKeys public_keys = new TelegramPublicKeys();
            RSAPublicKey public_key = public_keys.find_public_key(pq_data.server_public_key_fingerprints);
            if (public_key == null) {
              System.out.println("NO MATCHING PUBLIC KEY FOUND");
            }
            second_nonce = random_number_generator.nextInteger256();
            
            SendReqDhParams diffie_hellman_params_request = new SendReqDhParams(
              nonce,
              pq_data.server_nonce,
              pq_data.pq,
              decomposed_pq.lesser_prime,
              decomposed_pq.greater_prime,
              public_key,
              second_nonce
            );
            diffie_hellman_params_request.send();
            System.out.println("SENDING PROOF OF WORK");
          } else if (unencrypted_response.type() == CombinatorIds.server_DH_params_ok) {
            System.out.println("SERVER SAID PROOF OF WORK PARAMETERS ARE OK!");
            RecieveServerDHParamsOk.from_unencrypted_message(unencrypted_response, second_nonce);
          }
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
