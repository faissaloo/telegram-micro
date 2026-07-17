package mtproto.handle;

import mtproto.MTProtoConnection;
import mtproto.Response;
import mtproto.RPCResponse;
import mtproto.CombinatorIds;
import mtproto.recieve.service.RecieveFutureSalts;

public class HandleRecieveFutureSalts extends MTProtoCallback {
  public HandleRecieveFutureSalts(MTProtoConnection connection) {
    super(CombinatorIds.future_salts, connection);
  }

  public void execute(Response response) {
    RPCResponse rpc_response = (RPCResponse)response;
    RecieveFutureSalts future_salts = RecieveFutureSalts.from_rpc_response(rpc_response);
    connection.future_salts_request_inflight = false;
    connection.cacheFutureSalts(future_salts);
    System.out.println(
      "FUTURE SALTS READY: count=" + Integer.toString(connection.future_salt_count) +
      " now=" + Integer.toString(future_salts.now)
    );
  }
}
