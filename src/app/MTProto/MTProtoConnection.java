package mtproto;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.SecurityException;
import java.util.Hashtable;
import java.util.Enumeration;

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
  public RecieveResponseThread message_recieve_thread = null;
  SecureRandomPlus random_number_generator = null;
  
  public byte[] auth_key = null;
  public byte[] auth_key_full_hash = null;
  public long server_salt = 0;
  public long auth_key_id = 0;
  public long session_id = 0;
  public int seq_no = 0;
  public long retry_id = 0;
  public Integer128 nonce;
  public Integer256 new_nonce;
  
  public Hashtable callbacks; //hash by combinator_id of hash by callback id
  
  public static abstract class MTProtoCallback {
    public MTProtoConnection connection;
    public int combinator_id;
    
    public MTProtoCallback(int combinator_id, MTProtoConnection connection) {
      this.connection = connection;
      this.combinator_id = combinator_id;
    }
    
    public abstract void execute(Response response);
  }
  
  
  public static class HandleRecieveResPQ extends MTProtoConnection.MTProtoCallback {
    public HandleRecieveResPQ(int combinator_id, MTProtoConnection connection) {
      super(combinator_id, connection);
    }
    
    public void execute(Response response) {
      UnencryptedResponse unencrypted_response = (UnencryptedResponse)response;
      System.out.println("RECIEVING RES PQ");
      RecieveResPQ pq_data = RecieveResPQ.from_unencrypted_message(unencrypted_response);
      PrimeDecomposer.Coprimes decomposed_pq = PrimeDecomposer.decompose(pq_data.pq);
      TelegramPublicKeys public_keys = new TelegramPublicKeys();
      RSAPublicKey public_key = public_keys.find_public_key(pq_data.server_public_key_fingerprints);
      if (public_key == null) {
        throw new SecurityException("Could not find public key for any of the fingerprints recieved");
      }
      connection.new_nonce = connection.random_number_generator.nextInteger256();
      
      SendReqDhParams diffie_hellman_params_request = new SendReqDhParams(
        connection.random_number_generator,
        connection.nonce,
        pq_data.server_nonce,
        pq_data.pq,
        decomposed_pq.lesser_prime,
        decomposed_pq.greater_prime,
        public_key,
        connection.new_nonce
      );
      diffie_hellman_params_request.send(connection);
      System.out.println("SENDING DH PARAMS");
    }
  }
  
  public static class HandleRecieveDHParamsOk extends MTProtoCallback {
    public HandleRecieveDHParamsOk(int combinator_id, MTProtoConnection connection) {
      super(combinator_id, connection);
    }
    
    public void execute(Response response) {
      UnencryptedResponse unencrypted_response = (UnencryptedResponse)response;
      RecieveServerDHParamsOk dh_params_ok = RecieveServerDHParamsOk.from_unencrypted_message(unencrypted_response, connection.new_nonce);
      
      BigInteger b = new BigInteger(1, connection.random_number_generator.nextBytes(2048));
      if (!validate_diffie_hellman_prime(dh_params_ok.group_generator, dh_params_ok.diffie_hellman_prime)) {
        throw new SecurityException("Diffie hellman prime is not known and may be insecure");
      }
      
      SendSetClientDHParams set_client_dh_params = new SendSetClientDHParams(
        connection.random_number_generator,
        dh_params_ok.nonce,
        dh_params_ok.server_nonce,
        connection.retry_id,
        dh_params_ok.group_generator,
        dh_params_ok.diffie_hellman_prime,
        b,
        dh_params_ok.tmp_aes_key,
        dh_params_ok.tmp_aes_iv
      );
      
      set_client_dh_params.send(connection);
      System.out.println("SENDING SET DH PARAMS");
      
      connection.auth_key = connection.generate_auth_key(dh_params_ok.group_generator_power_a, b, dh_params_ok.diffie_hellman_prime);
      connection.auth_key_full_hash = (new SHA1()).digest(connection.auth_key);
      connection.auth_key_id = Decode.Little.long_decode(connection.auth_key_full_hash, SHA1.HASH_SIZE-8); //these should be the 64 lower-order bits right?
    }
  }
  
  public static class HandleRecieveServerDHGenOk extends MTProtoCallback {
    public HandleRecieveServerDHGenOk(int combinator_id, MTProtoConnection connection) {
      super(combinator_id, connection);
    }
    
    public void execute(Response response) {
      UnencryptedResponse unencrypted_response = (UnencryptedResponse)response;
      RecieveServerDHGenOk dh_gen_ok = RecieveServerDHGenOk.from_unencrypted_message(unencrypted_response);
      connection.server_salt = connection.generate_server_salt(connection.new_nonce, dh_gen_ok.server_nonce);
      //https://github.com/badoualy/kotlogram/blob/master/mtproto/src/main/kotlin/com/github/badoualy/telegram/mtproto/auth/AuthKeyCreation.kt#L193
      connection.session_id = connection.random_number_generator.nextLong();
      System.out.println("DH GEN OK");
    }
  }
  
  
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
    
    callbacks = new Hashtable();
    bind_callback(new HandleRecieveResPQ(CombinatorIds.resPQ, this));
    bind_callback(new HandleRecieveDHParamsOk(CombinatorIds.server_DH_params_ok, this));
    bind_callback(new HandleRecieveServerDHGenOk(CombinatorIds.dh_gen_ok, this));
  }
  
  public int bind_callback(MTProtoCallback callback) {
    //returns id for callback
    //check first if there exists a hash
    Integer combinator_id_obj = new Integer(callback.combinator_id);
    if (callbacks.get(combinator_id_obj) == null) {
      callbacks.put(combinator_id_obj, new Hashtable());
    }
    
    Hashtable combinator_callbacks = (Hashtable) callbacks.get(combinator_id_obj);
    int callback_id = combinator_callbacks.size()+1;
    Integer callback_id_obj = new Integer(callback_id);
    combinator_callbacks.put(callback_id_obj, callback);
    return callback_id;
  }
  
  public void remove_callback(int combinator_id, int callback_id) {
    Integer combinator_id_obj = new Integer(combinator_id);
    Integer callback_id_obj = new Integer(callback_id);
    Hashtable combinator_callbacks = (Hashtable)callbacks.get(combinator_id_obj);
    
    combinator_callbacks.remove(callback_id_obj);
    if (combinator_callbacks.size() == 0) {
      callbacks.remove(combinator_id_obj);
    }
  }
  
  public Enumeration get_callbacks(int combinator_id) {
    Integer combinator_id_obj = new Integer(combinator_id);
    Hashtable combinator_callbacks = (Hashtable)callbacks.get(combinator_id_obj);
    return combinator_callbacks.elements();
  }
  
  public void begin_handshake() {
    nonce = random_number_generator.nextInteger128();
    new_nonce = null;
    retry_id = 0;

    SendReqPqMulti key_exchange = new SendReqPqMulti(nonce);
    key_exchange.send(this);
    System.out.println("SENDING REQ PQ MULTI");
  }

  public void main_loop() throws IOException {
    begin_handshake();
    
    while (true) {
      //we'll have a different set of callbacks for RPC responses
      wait_for_response();
      TCPResponse tcp_response = message_recieve_thread.dequeue_response();
      Response response = UnencryptedResponse.from_tcp_response(tcp_response);
      if (response == null) { //we need a way to check which one it is beforehand this is ugly
        response = EncryptedResponse.from_tcp_response(tcp_response, this);
      }
      
      for (Enumeration e = get_callbacks(response.type); e.hasMoreElements();) {
        MTProtoCallback callback = (MTProtoCallback)e.nextElement();
        callback.execute(response);
      }
    }
  }
  
  public void send(TCPRequest request) {
    message_send_thread.enqueue_request(request);
  }
  
  public void wait_for_response() {
    message_recieve_thread.wait_for_response();
  }
  
  public static long generate_server_salt(Integer256 new_nonce, Integer128 server_nonce) {
    return Decode.Little.long_decode(ArrayPlus.subarray(Encode.Integer256_encode(new_nonce), 8), 0) ^
      Decode.Little.long_decode(ArrayPlus.subarray(Encode.Integer128_encode(server_nonce), 8), 0);
  }
  
  public static byte[] generate_auth_key(BigInteger group_generator_power_a, BigInteger b, BigInteger diffie_hellman_prime) {
    return group_generator_power_a
      .modPow(b, diffie_hellman_prime)
      .magnitudeToBytes();
  }
  
  /*
  Client is expected to check whether p = dh_prime is a safe 2048-bit prime (meaning that both p and (p-1)/2 are prime, and that 2^2047 < p < 2^2048), and that g generates a cyclic subgroup of prime order (p-1)/2, i.e. is a quadratic residue mod p. Since g is always equal to 2, 3, 4, 5, 6 or 7, this is easily done using quadratic reciprocity law, yielding a simple condition on p mod 4g -- namely, p mod 8 = 7 for g = 2; p mod 3 = 2 for g = 3; no extra condition for g = 4; p mod 5 = 1 or 4 for g = 5; p mod 24 = 19 or 23 for g = 6; and p mod 7 = 3, 5 or 6 for g = 7. After g and p have been checked by the client, it makes sense to cache the result, so as not to repeat lengthy computations in future.
  */
  public static boolean validate_diffie_hellman_prime(int group_generator, BigInteger prime) {
    // J2ME devices are slow, so we're just going to keep it simple and test against a hardcoded number for now
    // If this changes or if we want to use this with MTProto implementations that aren't the official Telegram one the application will have to be updated unless we add a fallback
    BigInteger valid_prime = new BigInteger(1, new byte[] { (byte)0xC7, (byte)0x1C, (byte)0xAE, (byte)0xB9, (byte)0xC6, (byte)0xB1, (byte)0xC9, (byte)0x04, (byte)0x8E, (byte)0x6C, (byte)0x52, (byte)0x2F, (byte)0x70, (byte)0xF1, (byte)0x3F, (byte)0x73, (byte)0x98, (byte)0x0D, (byte)0x40, (byte)0x23, (byte)0x8E, (byte)0x3E, (byte)0x21, (byte)0xC1, (byte)0x49, (byte)0x34, (byte)0xD0, (byte)0x37, (byte)0x56, (byte)0x3D, (byte)0x93, (byte)0x0F, (byte)0x48, (byte)0x19, (byte)0x8A, (byte)0x0A, (byte)0xA7, (byte)0xC1, (byte)0x40, (byte)0x58, (byte)0x22, (byte)0x94, (byte)0x93, (byte)0xD2, (byte)0x25, (byte)0x30, (byte)0xF4, (byte)0xDB, (byte)0xFA, (byte)0x33, (byte)0x6F, (byte)0x6E, (byte)0x0A, (byte)0xC9, (byte)0x25, (byte)0x13, (byte)0x95, (byte)0x43, (byte)0xAE, (byte)0xD4, (byte)0x4C, (byte)0xCE, (byte)0x7C, (byte)0x37, (byte)0x20, (byte)0xFD, (byte)0x51, (byte)0xF6, (byte)0x94, (byte)0x58, (byte)0x70, (byte)0x5A, (byte)0xC6, (byte)0x8C, (byte)0xD4, (byte)0xFE, (byte)0x6B, (byte)0x6B, (byte)0x13, (byte)0xAB, (byte)0xDC, (byte)0x97, (byte)0x46, (byte)0x51, (byte)0x29, (byte)0x69, (byte)0x32, (byte)0x84, (byte)0x54, (byte)0xF1, (byte)0x8F, (byte)0xAF, (byte)0x8C, (byte)0x59, (byte)0x5F, (byte)0x64, (byte)0x24, (byte)0x77, (byte)0xFE, (byte)0x96, (byte)0xBB, (byte)0x2A, (byte)0x94, (byte)0x1D, (byte)0x5B, (byte)0xCD, (byte)0x1D, (byte)0x4A, (byte)0xC8, (byte)0xCC, (byte)0x49, (byte)0x88, (byte)0x07, (byte)0x08, (byte)0xFA, (byte)0x9B, (byte)0x37, (byte)0x8E, (byte)0x3C, (byte)0x4F, (byte)0x3A, (byte)0x90, (byte)0x60, (byte)0xBE, (byte)0xE6, (byte)0x7C, (byte)0xF9, (byte)0xA4, (byte)0xA4, (byte)0xA6, (byte)0x95, (byte)0x81, (byte)0x10, (byte)0x51, (byte)0x90, (byte)0x7E, (byte)0x16, (byte)0x27, (byte)0x53, (byte)0xB5, (byte)0x6B, (byte)0x0F, (byte)0x6B, (byte)0x41, (byte)0x0D, (byte)0xBA, (byte)0x74, (byte)0xD8, (byte)0xA8, (byte)0x4B, (byte)0x2A, (byte)0x14, (byte)0xB3, (byte)0x14, (byte)0x4E, (byte)0x0E, (byte)0xF1, (byte)0x28, (byte)0x47, (byte)0x54, (byte)0xFD, (byte)0x17, (byte)0xED, (byte)0x95, (byte)0x0D, (byte)0x59, (byte)0x65, (byte)0xB4, (byte)0xB9, (byte)0xDD, (byte)0x46, (byte)0x58, (byte)0x2D, (byte)0xB1, (byte)0x17, (byte)0x8D, (byte)0x16, (byte)0x9C, (byte)0x6B, (byte)0xC4, (byte)0x65, (byte)0xB0, (byte)0xD6, (byte)0xFF, (byte)0x9C, (byte)0xA3, (byte)0x92, (byte)0x8F, (byte)0xEF, (byte)0x5B, (byte)0x9A, (byte)0xE4, (byte)0xE4, (byte)0x18, (byte)0xFC, (byte)0x15, (byte)0xE8, (byte)0x3E, (byte)0xBE, (byte)0xA0, (byte)0xF8, (byte)0x7F, (byte)0xA9, (byte)0xFF, (byte)0x5E, (byte)0xED, (byte)0x70, (byte)0x05, (byte)0x0D, (byte)0xED, (byte)0x28, (byte)0x49, (byte)0xF4, (byte)0x7B, (byte)0xF9, (byte)0x59, (byte)0xD9, (byte)0x56, (byte)0x85, (byte)0x0C, (byte)0xE9, (byte)0x29, (byte)0x85, (byte)0x1F, (byte)0x0D, (byte)0x81, (byte)0x15, (byte)0xF6, (byte)0x35, (byte)0xB1, (byte)0x05, (byte)0xEE, (byte)0x2E, (byte)0x4E, (byte)0x15, (byte)0xD0, (byte)0x4B, (byte)0x24, (byte)0x54, (byte)0xBF, (byte)0x6F, (byte)0x4F, (byte)0xAD, (byte)0xF0, (byte)0x34, (byte)0xB1, (byte)0x04, (byte)0x03, (byte)0x11, (byte)0x9C, (byte)0xD8, (byte)0xE3, (byte)0xB9, (byte)0x2F, (byte)0xCC, (byte)0x5B });
    return prime.equals(valid_prime) && group_generator == 3;
  }
}
