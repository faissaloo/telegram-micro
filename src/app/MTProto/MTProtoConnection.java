package mtproto;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import bouncycastle.BigInteger;
/*
import mtproto.TelegramPublicKeys;
import mtproto.RecieveResponseThread;
import mtproto.SendRequestThread;
import mtproto.UnencryptedResponse;
import mtproto.PrimeDecomposer;
import mtproto.CombinatorIds;
*/
import mtproto.recieve.RecieveResPQ;
import mtproto.recieve.RecieveServerDHParamsOk;
import mtproto.send.SendSetClientDHParams;
import mtproto.send.SendReqPqMulti;
import mtproto.send.SendReqDhParams;
import mtproto.send.SendPing;

import crypto.RSAPublicKey;
import crypto.SecureRandomPlus;
import crypto.SHA1;

import support.Integer128;
import support.Integer256;
import support.RandomPlus;
import support.Debug;
import support.Decode;

public class MTProtoConnection {
  SocketConnection api_connection = null;
  SendRequestThread message_send_thread = null;
  RecieveResponseThread message_recieve_thread = null;
  SecureRandomPlus random_number_generator = null;
  public byte[] auth_key = null;
  public byte[] auth_key_hash = null;
  public byte[] auth_key_full_hash = null;
  public long auth_key_id = 0;
  
  
  public MTProtoConnection(String ip, String port) throws IOException {
    random_number_generator = new SecureRandomPlus();
    api_connection = (SocketConnection) Connector.open("socket://"+ip+":"+port);
    message_send_thread = new SendRequestThread(api_connection);
    message_recieve_thread = new RecieveResponseThread(api_connection);
    
    message_send_thread.start(); //Should we have these start when they're instantiated?
    message_recieve_thread.start();
  }
  
  public void send(TCPRequest request) {
    message_send_thread.enqueue_request(request);
  }
  
  //https://github.com/Fnux/telegram-mt-elixir/issues/1
  public void get_auth_key() throws IOException {
    Integer128 nonce = random_number_generator.nextInteger128();
    Integer256 new_nonce = null;
    long retry_id = 0;

    SendReqPqMulti key_exchange = new SendReqPqMulti(nonce);
    key_exchange.send(this);
    System.out.println("SENDING REQ PQ MULTI");
    
    message_recieve_thread.wait_for_response();

    UnencryptedResponse unencrypted_response = UnencryptedResponse.from_tcp_response(message_recieve_thread.dequeue_response());
    System.out.println("RECIEVING RES PQ");
    RecieveResPQ pq_data = RecieveResPQ.from_unencrypted_message(unencrypted_response);
    PrimeDecomposer.Coprimes decomposed_pq = PrimeDecomposer.decompose(pq_data.pq);
    TelegramPublicKeys public_keys = new TelegramPublicKeys();
    RSAPublicKey public_key = public_keys.find_public_key(pq_data.server_public_key_fingerprints);
    if (public_key == null) {
      //Couldn't find the public key
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
    diffie_hellman_params_request.send(this);
    System.out.println("SENDING DH PARAMS");

    message_recieve_thread.wait_for_response();
    
    unencrypted_response = UnencryptedResponse.from_tcp_response(message_recieve_thread.dequeue_response());
    
    RecieveServerDHParamsOk dh_params_ok = RecieveServerDHParamsOk.from_unencrypted_message(unencrypted_response, new_nonce);
    
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
    
    set_client_dh_params.send(this);
    System.out.println("SENDING SET DH PARAMS");
    

    auth_key = dh_params_ok.group_generator_power_a
      .xor(b)
      .mod(dh_params_ok.diffie_hellman_prime)
      .magnitudeToBytes();
    
    auth_key_full_hash = (new SHA1()).digest(auth_key);
    auth_key_hash = (new SHA1()).digest(auth_key, 8); //Optimize me pls
    auth_key_id = Decode.Little.long_decode(auth_key_full_hash, SHA1.HASH_SIZE-8);
    
    message_recieve_thread.wait_for_response();
    
    unencrypted_response = UnencryptedResponse.from_tcp_response(message_recieve_thread.dequeue_response());

    if (unencrypted_response.type() == CombinatorIds.dh_gen_ok) {
      System.out.println("DH GEN OK");
    }
    System.out.println("SENDING DONE");
  }
}
