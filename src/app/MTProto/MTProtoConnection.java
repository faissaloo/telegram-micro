package mtproto;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.SecurityException;
import java.util.Hashtable;
import java.util.Enumeration;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import mtproto.handle.MTProtoCallback;
import mtproto.handle.HandleRecieveResPQ;
import mtproto.handle.HandleRecieveDHParamsOk;
import mtproto.handle.HandleRecieveMsgContainer;
import mtproto.handle.HandleRecieveServerDHGenOk;
import mtproto.handle.HandleRecieveNewSessionCreated;
import mtproto.handle.HandleRecieveUnknown;
import mtproto.handle.HandleRecieveRPCResult;
import mtproto.handle.HandleRecieveRPCError;
import mtproto.send.SendReqPqMulti;

import crypto.SecureRandomPlus;

import support.Integer128;
import support.Integer256;

public class MTProtoConnection {
  SocketConnection api_connection = null;
  SendRequestThread message_send_thread = null;
  public RecieveResponseThread message_recieve_thread = null;
  public SecureRandomPlus random_number_generator = null;
  
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
    bind_callback(new HandleRecieveResPQ(this));
    bind_callback(new HandleRecieveDHParamsOk(this));
    bind_callback(new HandleRecieveServerDHGenOk(this));
    bind_callback(new HandleRecieveMsgContainer(this));
    bind_callback(new HandleRecieveNewSessionCreated(this));
    bind_callback(new HandleRecieveRPCResult(this));
    bind_callback(new HandleRecieveUnknown(this));
    bind_callback(new HandleRecieveRPCError(this));
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
    
    if (combinator_callbacks == null) {
      return null;
    } else {
      return combinator_callbacks.elements();
    }
  }
  
  public long handshake_start = 0; //benchmarking
  public void begin_handshake() {
    nonce = random_number_generator.nextInteger128();
    new_nonce = null;
    retry_id = 0;

    handshake_start = System.currentTimeMillis();
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
      
      trigger_callbacks(response);
    }
  }
  
  public void trigger_callbacks(Response response) {
    Enumeration e = get_callbacks(response.type);
    if (e == null) {
      e = get_callbacks(CombinatorIds.unknown);
      if (e == null) {
        return;
      }
    }
    for (;e.hasMoreElements();) {
      MTProtoCallback callback = (MTProtoCallback)e.nextElement();
      callback.execute(response);
    }
  }
  
  public void send(TCPRequest request) {
    message_send_thread.enqueue_request(request);
  }
  
  public void wait_for_response() {
    message_recieve_thread.wait_for_response();
  }
}
