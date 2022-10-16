package mtproto.handle;

import mtproto.MTProtoConnection;
import mtproto.Response;
import mtproto.EncryptedResponse;
import mtproto.recieve.RecieveRpcResult;
import mtproto.send.service.SendMsgsAck;
import mtproto.CombinatorIds;
import mtproto.RPCResponse;

public class HandleRecieveRPCResult extends MTProtoCallback {
  public HandleRecieveRPCResult(MTProtoConnection connection) {
    super(CombinatorIds.rpc_result, connection);
  }
  
  public void execute(Response response) {
    EncryptedResponse encrypted_response = (EncryptedResponse)response;
    RecieveRpcResult recieve_rpc_result = RecieveRpcResult.from_encrypted_message(encrypted_response);
    //for now we're assuming nothing on the RPC layer needs message ids or anything like that
    RPCResponse rpc_response = RPCResponse.from_bytes(recieve_rpc_result.body);
    connection.trigger_callbacks(rpc_response);
  }
}
