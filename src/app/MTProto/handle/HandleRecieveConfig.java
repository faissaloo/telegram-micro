package mtproto.handle;

import mtproto.MTProtoConnection;
import mtproto.Response;
import mtproto.RPCResponse;
import mtproto.recieve.help.RecieveConfig;

public class HandleRecieveConfig extends MTProtoCallback {
  public HandleRecieveConfig(int combinator_id, MTProtoConnection connection) {
    super(combinator_id, connection);
  }

  public void execute(Response response) {
    RPCResponse rpc_response = (RPCResponse)response;
    RecieveConfig config = RecieveConfig.from_rpc_response(rpc_response);
    connection.current_config = config;
    connection.bootstrap_request_inflight = false;

    RecieveConfig.DCOption current_dc = config.find_this_dc_option();
    String endpoint = "";
    if (current_dc != null) {
      endpoint = " endpoint=" + current_dc.ip_address + ":" + Integer.toString(current_dc.port);
    }

    System.out.println(
      "BOOTSTRAP READY: this_dc=" + Integer.toString(config.this_dc) +
      " dc_options=" + Integer.toString(config.dc_options.length) +
      endpoint
    );
  }
}
