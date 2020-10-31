package mtproto;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.SecurityException;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import bouncycastle.BigInteger;
import mtproto.recieve.RecieveResPQ;
import mtproto.recieve.RecieveServerDHParamsOk;
import mtproto.recieve.RecieveServerDHGenOk;
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
import support.Encode;
import support.ArrayPlus;

public class MTProtoConnection {
  SocketConnection api_connection = null;
  SendRequestThread message_send_thread = null;
  RecieveResponseThread message_recieve_thread = null;
  SecureRandomPlus random_number_generator = null;
  
  public byte[] auth_key = null;
  public byte[] auth_key_hash = null;
  public byte[] auth_key_full_hash = null;
  public byte[] server_salt = null;
  public long auth_key_id = 0;
  public long session_id = 0;
  public int seq_no = 0;
  
  //Constructor for the default port
  public MTProtoConnection(String ip) throws IOException {
    this(ip, "5222");
  }
  
  public MTProtoConnection(String ip, String port) throws IOException {
    if (port.equals("80") || port.equals("8080") || port.equals("443") || port.equals("http") || port.equals("https")) {
      throw new SecurityException(
        "Ports 80, 8080 & 443 are reserved under JSR 185," +
        " rendering the ports unusable on most devices. JSR 185 requires" +
        " that applications using these ports be signed but most J2ME devices" +
        " no longer hold valid certificates to be signed against." + 
        " In the interests of writing compatible programs these ports" +
        " have been disabled entirely."
      );
    }
    random_number_generator = new SecureRandomPlus();
    api_connection = (SocketConnection) Connector.open("socket://"+ip+":"+port);
    message_send_thread = new SendRequestThread(api_connection);
    message_recieve_thread = new RecieveResponseThread(api_connection);
    
    message_send_thread.start(); //Should we have these start when they're instantiated?
    message_recieve_thread.start();
    
    perform_handshake();
  }
  
  public void send(TCPRequest request) {
    message_send_thread.enqueue_request(request);
  }
  
  public void wait_for_response() {
    message_recieve_thread.wait_for_response();
  }
  
  public void perform_handshake() throws IOException {
    Integer128 nonce = random_number_generator.nextInteger128();
    Integer256 new_nonce = null;
    long retry_id = 0;

    SendReqPqMulti key_exchange = new SendReqPqMulti(nonce);
    key_exchange.send(this);
    System.out.println("SENDING REQ PQ MULTI");
    
    wait_for_response();

    UnencryptedResponse unencrypted_response = UnencryptedResponse.from_tcp_response(message_recieve_thread.dequeue_response());
    System.out.println("RECIEVING RES PQ");
    RecieveResPQ pq_data = RecieveResPQ.from_unencrypted_message(unencrypted_response);
    PrimeDecomposer.Coprimes decomposed_pq = PrimeDecomposer.decompose(pq_data.pq);
    TelegramPublicKeys public_keys = new TelegramPublicKeys();
    RSAPublicKey public_key = public_keys.find_public_key(pq_data.server_public_key_fingerprints);
    for (int i = 0; i < pq_data.server_public_key_fingerprints.length; i++) {
      System.out.println("Using public key with fingerprint:");
      System.out.println(pq_data.server_public_key_fingerprints[i]);
    }
    
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

    wait_for_response();
    
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
    
    wait_for_response();
    
    unencrypted_response = UnencryptedResponse.from_tcp_response(message_recieve_thread.dequeue_response());
    
    if (unencrypted_response.type() == CombinatorIds.dh_gen_ok) {
      RecieveServerDHGenOk dh_gen_ok = RecieveServerDHGenOk.from_unencrypted_message(unencrypted_response);
      server_salt = ArrayPlus.xor(ArrayPlus.subarray(Encode.Integer256_encode(new_nonce), 8), ArrayPlus.subarray(Encode.Integer128_encode(dh_gen_ok.server_nonce), 8));
      session_id = random_number_generator.nextLong();
      System.out.println("DH GEN OK");
    }
    System.out.println("SENDING DONE");
  }
}
