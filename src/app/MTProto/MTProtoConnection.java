package mtproto;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.SecurityException;
import java.util.Hashtable;
import java.util.Enumeration;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import config.AppCredentials;
import config.RuntimeDeviceProfile;
import mtproto.handle.MTProtoCallback;
import mtproto.handle.HandleRecieveResPQ;
import mtproto.handle.HandleRecieveDHParamsOk;
import mtproto.handle.HandleRecieveMsgContainer;
import mtproto.handle.HandleRecieveServerDHGenOk;
import mtproto.handle.HandleRecieveNewSessionCreated;
import mtproto.handle.HandleRecieveUnknown;
import mtproto.handle.HandleRecieveRPCResult;
import mtproto.handle.HandleRecieveRPCError;
import mtproto.handle.HandleRecieveBadMsgNotification;
import mtproto.handle.HandleRecieveAuxiliaryUpdates;
import mtproto.handle.HandleRecieveGzipPacked;
import mtproto.handle.HandleRecieveConfig;
import mtproto.handle.HandleRecieveFutureSalts;
import mtproto.handle.HandleRecieveMsgsAck;
import mtproto.handle.HandleRecievePong;
import mtproto.handle.HandleRecieveUpdateShort;
import mtproto.send.SendReqPqMulti;
import mtproto.send.help.SendGetConfig;
import mtproto.Serializer;
import crypto.SecureRandomPlus;
import crypto.SHA1;
import mtproto.recieve.auth.RecievePasswordState;
import mtproto.recieve.help.RecieveConfig;
import mtproto.recieve.service.RecieveFutureSalts;
import mtproto.recieve.service.RecievePong;
import session.SessionSnapshot;

import support.Integer128;
import support.Integer256;
import support.Decode;

public class MTProtoConnection {
  public static final int FUTURE_SALT_CAPACITY = 4;
  public static final int DEFAULT_API_LAYER = 214;
  public static final String DEFAULT_DEVICE_MODEL = RuntimeDeviceProfile.DEFAULT_DEVICE_MODEL;
  public static final String DEFAULT_SYSTEM_VERSION = RuntimeDeviceProfile.DEFAULT_SYSTEM_VERSION;
  public static final String DEFAULT_APP_VERSION = "TelegramMicro 0.1";

  public static class PendingRetryRequest {
    public final byte[] payload;
    public final boolean contentRelated;
    public final boolean includeHeader;

    public PendingRetryRequest(byte[] payload, boolean contentRelated, boolean includeHeader) {
      this.payload = payload;
      this.contentRelated = contentRelated;
      this.includeHeader = includeHeader;
    }
  }

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
  public int time_offset_seconds = 0;
  public long last_message_id = 0;
  public long retry_id = 0;
  public Integer128 nonce;
  public Integer128 server_nonce;
  public Integer256 new_nonce;
  public RecieveConfig current_config = null;
  public RecievePong last_pong = null;
  public RecievePasswordState current_password_state = null;
  public final long[] future_salts = new long[FUTURE_SALT_CAPACITY];
  public final int[] future_salt_valid_since = new int[FUTURE_SALT_CAPACITY];
  public final int[] future_salt_valid_until = new int[FUTURE_SALT_CAPACITY];
  public int future_salt_count = 0;
  public boolean future_salts_request_inflight = false;
  public boolean connection_open = false;
  public boolean bootstrap_request_inflight = false;
  public boolean direct_authorized_restore = false;
  public boolean restored_session_prepared = false;
  private byte[] pending_retry_payload = null;
  private boolean pending_retry_content_related = false;
  private boolean pending_retry_include_header = true;
  
  public Hashtable callbacks; //hash by combinator_id of hash by callback id
  
  //Constructor for the default port
  public MTProtoConnection(String ip, String authHelpUrl, SecureRandomPlus randomNumberGenerator) throws IOException {
    this(ip, "5222", authHelpUrl, randomNumberGenerator);
  }
  
  public MTProtoConnection(String ip, String port, String authHelpUrl, SecureRandomPlus randomNumberGenerator) throws IOException {
    if (randomNumberGenerator == null) {
      throw new IllegalArgumentException("A seeded secure random generator is required.");
    }
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
    
    random_number_generator = randomNumberGenerator;
    api_connection = (SocketConnection) Connector.open("socket://"+ip+":"+port);
    message_send_thread = new SendRequestThread(api_connection);
    message_recieve_thread = new RecieveResponseThread(api_connection);
    setBlankMTProtoHeader();
    
    message_send_thread.start(); //Should we have these start when they're instantiated?
    message_recieve_thread.start();
    connection_open = true;
    
    callbacks = new Hashtable();
    bind_callback(new HandleRecieveResPQ(this, authHelpUrl));
    bind_callback(new HandleRecieveDHParamsOk(this));
    bind_callback(new HandleRecieveServerDHGenOk(this));
    bind_callback(new HandleRecieveMsgContainer(this));
    bind_callback(new HandleRecieveNewSessionCreated(this));
    bind_callback(new HandleRecieveRPCResult(this));
    bind_callback(new HandleRecieveUnknown(this));
    bind_callback(new HandleRecieveRPCError(this));
    bind_callback(new HandleRecieveBadMsgNotification(this));
    bind_callback(new HandleRecieveBadMsgNotification(CombinatorIds.bad_server_salt, this));
    bind_callback(new HandleRecieveGzipPacked(this));
    bind_callback(new HandleRecieveMsgsAck(this));
    bind_callback(new HandleRecieveUpdateShort(this));
    bind_callback(new HandleRecieveAuxiliaryUpdates(CombinatorIds.updates, this));
    bind_callback(new HandleRecieveAuxiliaryUpdates(CombinatorIds.updates_combined, this));
    bind_callback(new HandleRecieveAuxiliaryUpdates(CombinatorIds.updates_too_long, this));
    bind_callback(new HandleRecieveAuxiliaryUpdates(CombinatorIds.update_short_message, this));
    bind_callback(new HandleRecieveAuxiliaryUpdates(CombinatorIds.update_short_chat_message, this));
    bind_callback(new HandleRecieveAuxiliaryUpdates(CombinatorIds.update_short_sent_message, this));
    bind_callback(new HandleRecievePong(this));
    bind_callback(new HandleRecieveConfig(CombinatorIds.config, this));
    bind_callback(new HandleRecieveConfig(CombinatorIds.config_modern, this));
    bind_callback(new HandleRecieveFutureSalts(this));
  }
  
  public byte[] mtprotoHeader;
  public void setBlankMTProtoHeader() {
    mtprotoHeader = new byte[] {};
  }
  public void setDefaultMTProtoHeader() {
    AppCredentials credentials = AppCredentials.bundled();

    mtprotoHeader = buildDefaultMTProtoHeader(credentials);
  }

  public static byte[] buildDefaultMTProtoHeader(AppCredentials credentials) {
    AppCredentials headerCredentials = credentials != null ? credentials : AppCredentials.bundled();
    RuntimeDeviceProfile profile = RuntimeDeviceProfile.detect();

    return buildMTProtoHeader(
      DEFAULT_API_LAYER,
      headerCredentials.headerApiId(),
      profile.deviceModel,
      profile.systemVersion,
      DEFAULT_APP_VERSION,
      profile.systemLangCode,
      "",
      profile.langCode
    );
  }

  public void setMTProtoHeader(
    int layer,
    int api_id,
    String device_model,
    String system_version,
    String app_version,
    String system_lang_code,
    String lang_pack,
    String lang_code
  ) {
    mtprotoHeader = buildMTProtoHeader(
      layer,
      api_id,
      device_model,
      system_version,
      app_version,
      system_lang_code,
      lang_pack,
      lang_code
    );
  }

  public static byte[] buildMTProtoHeader(
    int layer,
    int api_id,
    String device_model,
    String system_version,
    String app_version,
    String system_lang_code,
    String lang_pack,
    String lang_code
  ) {
    return (new Serializer())
      .append_int(CombinatorIds.invoke_with_layer)
      .append_int(layer)
      .append_int(CombinatorIds.init_connection)
      .append_int(0) //initConnection flags
      .append_int(api_id)
      .append_string(device_model)
      .append_string(system_version)
      .append_string(app_version)
      .append_string(system_lang_code)
      .append_string(lang_pack)
      .append_string(lang_code)
      .end();
  }
  public byte[] getMTProtoHeader() {
    return mtprotoHeader;
  }

  public boolean hasUsableAuth() {
    return auth_key != null && auth_key.length > 0 &&
      auth_key.length <= SessionSnapshot.AUTH_KEY_BYTES && auth_key_id != 0;
  }

  public void applySavedSession(SessionSnapshot snapshot) {
    if (snapshot == null || !snapshot.hasUsableAuth()) {
      return;
    }

    auth_key = new byte[SessionSnapshot.AUTH_KEY_BYTES];
    System.arraycopy(snapshot.authKey, 0, auth_key, 0, SessionSnapshot.AUTH_KEY_BYTES);
    auth_key_full_hash = (new SHA1()).digest(auth_key);
    auth_key_id = snapshot.authKeyId != 0
      ? snapshot.authKeyId
      : Decode.Little.long_decode(auth_key_full_hash, SHA1.HASH_SIZE - 8);
    server_salt = snapshot.serverSalt;
    session_id = snapshot.sessionId;
    if (session_id == 0) {
      session_id = random_number_generator.nextLong();
      if (session_id == 0) {
        session_id = 1;
      }
    }
    seq_no = snapshot.seqNo;
    time_offset_seconds = snapshot.timeOffsetSeconds;
    last_message_id = snapshot.lastMessageId;
    retry_id = 0;
    bootstrap_request_inflight = false;
    current_config = null;
    current_password_state = null;
    last_pong = null;
    clearFutureSalts();
    clearPendingRetryRequest();
    direct_authorized_restore = false;
    restored_session_prepared = false;
  }

  public SessionSnapshot captureSessionSnapshot(boolean authorized, int dcId) {
    SessionSnapshot snapshot = new SessionSnapshot();

    if (!hasUsableAuth()) {
      return snapshot;
    }

    snapshot.hasAuthState = true;
    snapshot.authorized = authorized;
    snapshot.dcId = dcId;
    snapshot.authKeyId = auth_key_id;
    snapshot.serverSalt = server_salt;
    snapshot.sessionId = session_id;
    snapshot.seqNo = seq_no;
    snapshot.timeOffsetSeconds = time_offset_seconds;
    snapshot.lastMessageId = last_message_id;
    snapshot.setAuthKey(auth_key);
    return snapshot;
  }

  public void clearSavedAuth() {
    nonce = null;
    server_nonce = null;
    new_nonce = null;
    auth_key = null;
    auth_key_full_hash = null;
    server_salt = 0;
    auth_key_id = 0;
    session_id = 0;
    seq_no = 0;
    time_offset_seconds = 0;
    last_message_id = 0;
    retry_id = 0;
    bootstrap_request_inflight = false;
    current_config = null;
    current_password_state = null;
    last_pong = null;
    clearFutureSalts();
    clearPendingRetryRequest();
    setBlankMTProtoHeader();
    direct_authorized_restore = false;
    restored_session_prepared = false;
  }

  public void clearPendingRetryRequest() {
    pending_retry_payload = null;
    pending_retry_content_related = false;
    pending_retry_include_header = true;
  }

  public void clearFutureSalts() {
    future_salt_count = 0;
    future_salts_request_inflight = false;
    for (int i = 0; i < FUTURE_SALT_CAPACITY; ++i) {
      future_salts[i] = 0;
      future_salt_valid_since[i] = 0;
      future_salt_valid_until[i] = 0;
    }
  }

  public static int correctedUnixSeconds(long localTimeMillis, int timeOffsetSeconds) {
    long correctedTimeMillis = localTimeMillis + ((long)timeOffsetSeconds * 1000L);
    if (correctedTimeMillis < 0) {
      correctedTimeMillis = 0;
    }
    return (int)(correctedTimeMillis / 1000L);
  }

  public int correctedUnixSeconds() {
    return correctedUnixSeconds(System.currentTimeMillis(), time_offset_seconds);
  }

  public void cacheFutureSalts(RecieveFutureSalts response) {
    int count = 0;

    clearFutureSalts();
    if (response == null || response.salts == null) {
      return;
    }

    for (int i = 0; i < response.salts.length && count < FUTURE_SALT_CAPACITY; ++i) {
      RecieveFutureSalts.FutureSalt salt = response.salts[i];
      if (salt == null || salt.valid_until <= response.now) {
        continue;
      }
      future_salts[count] = salt.salt;
      future_salt_valid_since[count] = salt.valid_since;
      future_salt_valid_until[count] = salt.valid_until;
      count += 1;
    }
    future_salt_count = count;
    updateServerSaltFromFutureSalts();
  }

  public boolean updateServerSaltFromFutureSalts() {
    int now = correctedUnixSeconds();

    for (int i = 0; i < future_salt_count; ++i) {
      if (future_salt_valid_since[i] <= now && now < future_salt_valid_until[i]) {
        server_salt = future_salts[i];
        return true;
      }
    }
    return false;
  }

  public static int wireSeqNo(int contentSeqNo, boolean contentRelated) {
    int wireSeqNo = contentSeqNo << 1;
    if (contentRelated) {
      wireSeqNo |= 1;
    }
    return wireSeqNo;
  }

  public int nextWireSeqNo(boolean contentRelated) {
    int wireSeqNo = wireSeqNo(seq_no, contentRelated);
    if (contentRelated) {
      seq_no += 1;
    }
    return wireSeqNo;
  }

  public static long clientMessageId(long localTimeMillis, int timeOffsetSeconds, long lastMessageId) {
    long correctedTimeMillis = localTimeMillis + ((long)timeOffsetSeconds * 1000L);
    long unixSeconds;
    long millisRemainder;
    long fractional;
    long candidate;

    if (correctedTimeMillis < 0) {
      correctedTimeMillis = 0;
    }

    unixSeconds = correctedTimeMillis / 1000L;
    millisRemainder = correctedTimeMillis % 1000L;
    fractional = (millisRemainder * 4294967296L) / 1000L;
    fractional &= ~3L;
    if (fractional == 0L) {
      fractional = 4L;
    }

    candidate = (unixSeconds << 32) | (fractional & 0xffffffffL);
    if (lastMessageId != 0 && candidate <= lastMessageId) {
      candidate = (lastMessageId + 4L) & ~3L;
    }
    return candidate;
  }

  public long nextClientMessageId() {
    last_message_id = clientMessageId(System.currentTimeMillis(), time_offset_seconds, last_message_id);
    return last_message_id;
  }

  public void noteRejectedMessageId(long badMessageId) {
    if (badMessageId >= last_message_id) {
      last_message_id = (badMessageId + 4L) & ~3L;
    }
  }

  public void sendEncryptedPayload(byte[] payload, boolean trackRetry, boolean contentRelated) {
    sendEncryptedPayload(payload, trackRetry, contentRelated, true);
  }

  public void sendEncryptedPayload(byte[] payload, boolean trackRetry, boolean contentRelated, boolean includeHeader) {
    byte[] encodedPayload;

    if (payload == null) {
      throw new IllegalArgumentException("Encrypted payload is required.");
    }

    updateServerSaltFromFutureSalts();

    if (trackRetry) {
      pending_retry_payload = new byte[payload.length];
      System.arraycopy(payload, 0, pending_retry_payload, 0, payload.length);
      pending_retry_content_related = contentRelated;
      pending_retry_include_header = includeHeader;
    }

    encodedPayload = includeHeader
      ? (new Serializer())
          .append_raw_bytes(getMTProtoHeader())
          .append_raw_bytes(payload)
          .end()
      : payload;

    (new EncryptedRequest(
      encodedPayload
    )).send(this, contentRelated);
  }

  public boolean retryPendingRequest() {
    if (pending_retry_payload == null) {
      return false;
    }

    sendEncryptedPayload(
      pending_retry_payload,
      false,
      pending_retry_content_related,
      pending_retry_include_header
    );
    return true;
  }

  public PendingRetryRequest capturePendingRetryRequest() {
    byte[] payloadCopy;

    if (pending_retry_payload == null) {
      return null;
    }

    payloadCopy = new byte[pending_retry_payload.length];
    System.arraycopy(pending_retry_payload, 0, payloadCopy, 0, payloadCopy.length);
    return new PendingRetryRequest(
      payloadCopy,
      pending_retry_content_related,
      pending_retry_include_header
    );
  }

  public void restorePendingRetryRequest(PendingRetryRequest request) {
    clearPendingRetryRequest();
    if (request == null || request.payload == null) {
      return;
    }

    pending_retry_payload = new byte[request.payload.length];
    System.arraycopy(request.payload, 0, pending_retry_payload, 0, request.payload.length);
    pending_retry_content_related = request.contentRelated;
    pending_retry_include_header = request.includeHeader;
  }
  
  public void close() throws IOException {
    connection_open = false;
    message_send_thread.close();
    message_recieve_thread.close();
    api_connection.close();
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
    clearPendingRetryRequest();
    bootstrap_request_inflight = false;
    direct_authorized_restore = false;
    restored_session_prepared = false;
    nonce = random_number_generator.nextInteger128();
    server_nonce = null;
    new_nonce = null;
    session_id = 0;
    seq_no = 0;
    time_offset_seconds = 0;
    last_message_id = 0;
    clearFutureSalts();
    retry_id = 0;

    handshake_start = System.currentTimeMillis();
    SendReqPqMulti key_exchange = new SendReqPqMulti(nonce);
    key_exchange.send(this);
  }

  public void prepareRestoredSession(boolean bootstrapConfig) {
    if (!hasUsableAuth()) {
      begin_handshake();
      return;
    }

    clearPendingRetryRequest();
    if (session_id == 0) {
      session_id = random_number_generator.nextLong();
      if (session_id == 0) {
        session_id = 1;
      }
    }
    handshake_start = System.currentTimeMillis();
    setDefaultMTProtoHeader();
    bootstrap_request_inflight = false;
    current_config = null;
    current_password_state = null;
    last_pong = null;
    clearFutureSalts();
    retry_id = 0;
    direct_authorized_restore = !bootstrapConfig;
    restored_session_prepared = true;

    if (bootstrapConfig) {
      bootstrap_request_inflight = true;
      (new SendGetConfig()).send(this);
    }
  }

  public void begin_restored_session() {
    prepareRestoredSession(true);
  }

  public void main_loop() throws IOException {
    if (hasUsableAuth()) {
      if (!restored_session_prepared) {
        begin_restored_session();
      }
    } else {
      begin_handshake();
    }
    
    while (connection_open) { //if we've disconnected this should stop
      //we'll have a different set of callbacks for RPC responses
      
      //Why is this just not waiting for a response on certain servers?
      wait_for_response();
      TCPResponse tcp_response = message_recieve_thread.dequeue_response();
      if (tcp_response == null) {
        if (connection_open) {
          throw new IOException("Connection terminated by server");
        }
        break;
      }
      recieve_tcp_response(tcp_response);
    }
  }
  
  public void recieve_tcp_response(TCPResponse tcp_response) throws IOException {
    Response response = UnencryptedResponse.from_tcp_response(tcp_response);
    if (response == null) { //we need a way to check which one it is beforehand this is ugly
      response = EncryptedResponse.from_tcp_response(tcp_response, this);
    }
    
    try {
      trigger_callbacks(response);
    } catch (RuntimeException invalidResponse) {
      throw new IOException("Rejected invalid MTProto response: " + invalidResponse.toString());
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
