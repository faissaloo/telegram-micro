package mtproto;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import support.Encode;
import support.ByteArrayPlus;
import support.Debug;

public class TCPRequest {
  ByteArrayPlus request_data;
  public TCPRequest(byte[] data) {
    if (data == null) {
      throw new NullPointerException("Request data is required.");
    }
    if (data.length > ProtocolLimits.MAX_WIRE_FRAME_BYTES) {
      throw new IllegalArgumentException("TCP request exceeds the device budget.");
    }
    //https://core.telegram.org/mtproto/mtproto-transports#intermediate
    request_data = new ByteArrayPlus();

    request_data.append_int(data.length);
    request_data.append_raw_bytes(data);
  }

  public byte[] request_data() {
    //Ok so it looks like the network monitor will take anything that's 'negative' and turn it into 3 bytes for some reason, but that's not what's actually being sent
    return request_data.toByteArray();
  }

  public void send(MTProtoConnection sender) {
    sender.send(this);
  }
}
