import java.util.Date;
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
import mtproto.SendSetClientDHParams;

import crypto.RSAPublicKey;
import crypto.SecureRandomPlus;

import support.Integer128;
import support.Integer256;
import support.RandomPlus;
import support.Debug;

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
    log.append("Starting Telegram Micro");
    display.setCurrent(log);

    try {
      SocketConnection api_connection = (SocketConnection) Connector.open("socket://149.154.167.40:443");
      SendRequestThread message_send_thread = new SendRequestThread(api_connection);
      RecieveResponseThread message_recieve_thread = new RecieveResponseThread(api_connection);

      message_send_thread.start(); //Should we have these start when they're instantiated?
      message_recieve_thread.start();
      log.append((new Date()).toString());
      log.append("Connected to Telegram servers");
      display.setCurrent(log);

      SecureRandomPlus random_number_generator = new SecureRandomPlus();
      Integer128 nonce = random_number_generator.nextInteger128();
      Integer256 new_nonce = null;
      long retry_id = 0;

      SendReqPqMulti key_exchange = new SendReqPqMulti(nonce);
      key_exchange.send();
      log.append("Requesting proof of work parameters");
      display.setCurrent(log);

      while (true) {
        SendRequestThread.sleep(1); //Don't peg the CPU
        if (RecieveResponseThread.has_responses()) {
          UnencryptedResponse unencrypted_response = UnencryptedResponse.from_tcp_response(RecieveResponseThread.dequeue_response());
          if (unencrypted_response.type() == CombinatorIds.resPQ) {
            RecieveResPQ pq_data = RecieveResPQ.from_unencrypted_message(unencrypted_response);
            log.append((new Date()).toString());
            log.append("Processing proof of work");
            display.setCurrent(log);
            PrimeDecomposer.Coprimes decomposed_pq = PrimeDecomposer.decompose(pq_data.pq);
            TelegramPublicKeys public_keys = new TelegramPublicKeys();
            RSAPublicKey public_key = public_keys.find_public_key(pq_data.server_public_key_fingerprints);
            if (public_key == null) {
              log.append("No matching public key found");
              display.setCurrent(log);
            }
            new_nonce = random_number_generator.nextInteger256();
            
            SendReqDhParams diffie_hellman_params_request = new SendReqDhParams(
              nonce,
              pq_data.server_nonce,
              pq_data.pq,
              decomposed_pq.lesser_prime,
              decomposed_pq.greater_prime,
              public_key,
              new_nonce
            );
            diffie_hellman_params_request.send();
            log.append((new Date()).toString());
            log.append("Sending proof of work");
            display.setCurrent(log);
          } else if (unencrypted_response.type() == CombinatorIds.server_DH_params_ok) {
            log.append("Proof of work was ok");
            display.setCurrent(log);
            RecieveServerDHParamsOk dh_params_ok = RecieveServerDHParamsOk.from_unencrypted_message(unencrypted_response, new_nonce);
            log.append((new Date()).toString());
            log.append("Generating client diffie hellman parameters");
            display.setCurrent(log);
            
            BigInteger b = new BigInteger(1, random_number_generator.nextBytes(2048));
            SendSetClientDHParams set_client_dh_params = new SendSetClientDHParams(
              dh_params_ok.nonce,
              dh_params_ok.server_nonce,
              retry_id,
              dh_params_ok.group_generator,
              dh_params_ok.diffie_hellman_prime,
              b,
              dh_params_ok.tmp_aes_key,
              dh_params_ok.tmp_aes_iv
            );
            
            set_client_dh_params.send();
            log.append((new Date()).toString());
            log.append("Sending client diffie hellman parameters");
            display.setCurrent(log);
          } else if (unencrypted_response.type() == CombinatorIds.dh_gen_ok) {
            log.append((new Date()).toString());
            log.append("Diffie hellman generation complete");
            display.setCurrent(log);
          } else {
            log.append("Unknown message recieved "+Integer.toHexString(unencrypted_response.type()));
            display.setCurrent(log);
            System.out.println("UNKNOWN MESSAGE RECIEVED "+Integer.toHexString(unencrypted_response.type()));
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
