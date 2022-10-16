package mtproto.handle;

import mtproto.MTProtoConnection;
import mtproto.Response;
import mtproto.EncryptedResponse;
import mtproto.recieve.RecieveRpcResult;
import mtproto.send.service.SendMsgsAck;
import mtproto.CombinatorIds;
import mtproto.RPCResponse;

public class HandleRecieveUnknown extends MTProtoCallback {
  public HandleRecieveUnknown(MTProtoConnection connection) {
    super(CombinatorIds.unknown, connection);
  }
  
  public void execute(Response response) {
    System.out.println("WARNING: RECIEVED MESSAGE FOR WHICH THERE IS NO HANDLER WITH TYPE #"+Integer.toHexString(response.type));
  }
}
