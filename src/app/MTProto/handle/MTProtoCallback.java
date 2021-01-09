package mtproto.handle;
import mtproto.Response;
import mtproto.MTProtoConnection;

public abstract class MTProtoCallback {
  public MTProtoConnection connection;
  public int combinator_id;
  
  public MTProtoCallback(int combinator_id, MTProtoConnection connection) {
    this.connection = connection;
    this.combinator_id = combinator_id;
  }
  
  public abstract void execute(Response response);
}
