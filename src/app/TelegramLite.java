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

import crypto.RSAPublicKey;
import crypto.SecureRandomPlus;

import support.Integer128;
import support.Integer256;
import support.RandomPlus;

public class TelegramLite extends MIDlet {
  private Form form;
  private Display display;

  public TelegramLite() {
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

      SendPing ping = new SendPing(69L);
      
      ping.send();
      ping.send();
      
      while (true) {
        SendRequestThread.sleep(1); //Don't peg the CPU
        if (RecieveResponseThread.has_responses()) {
          UnencryptedResponse response = UnencryptedResponse.from_tcp_response(RecieveResponseThread.dequeue_response());
          if (response.type() == CombinatorIds.pong) {
            System.out.println("RECIEVED PONG");
          }
          if (response.type() == CombinatorIds.resPQ) {
            System.out.println("RECIEVED RESPQ");
          }
        }
      }
      /*
      SecureRandomPlus random_number_generator = new SecureRandomPlus();
      Integer128 nonce = random_number_generator.nextInteger128();
      Integer256 second_nonce;

      SendReqPqMulti key_exchange = new SendReqPqMulti(nonce);
      key_exchange.send();
      System.out.println("KEY EXCHANGE SENT");

      while (true) {
        SendRequestThread.sleep(1); //Don't peg the CPU
        if (RecieveResponseThread.has_responses()) {
          UnencryptedResponse key_response = UnencryptedResponse.from_tcp_response(RecieveResponseThread.dequeue_response());
          //check that it's a RecieveResPQ first
          //RecieveResPQ.message_is()
          RecieveResPQ pq_data = RecieveResPQ.from_unencrypted_message(key_response);
          System.out.println("PQ DATA RECIEVED; PQ = "+Long.toString(pq_data.pq, 16));
          PrimeDecomposer.Coprimes decomposed_pq = PrimeDecomposer.decompose(pq_data.pq);
          System.out.println("PQ DECOMPOSED; P = "+Long.toString(decomposed_pq.lesser_prime, 16)+"; Q = "+Long.toString(decomposed_pq.greater_prime, 16));
          TelegramPublicKeys public_keys = new TelegramPublicKeys();
          RSAPublicKey public_key = public_keys.find_public_key(pq_data.server_public_key_fingerprints);
          if (public_key != null) {
            System.out.println("PUBLIC KEY FOUND; FINGERPRINT = "+Long.toString(public_key.fingerprint, 16));
          } else {
            System.out.println("ERROR NO PUBLIC KEY FOUND; POTENTIAL FINGERPRINTS ARE: ");
            for (int i = 0; i < pq_data.server_public_key_fingerprints.length; i++) {
              System.out.println("  "+Long.toString(pq_data.server_public_key_fingerprints[i], 16));
            }
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
          diffie_hellman_params_request.send(); //For some reason this is killing the connection no idea what's up with that
          System.out.println("REQUESTING DIFFIE HELLMAN PARAMETERS");
        }
      }
      */
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
