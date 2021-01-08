package mtproto;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import support.Encode;
import support.ByteArrayPlus;
import support.Debug;

public class TCPResponse {
  public byte[] data;

  public static TCPResponse from_stream(InputStream response_stream) throws IOException {
    int response_byte = 0;
    int length = 0;
    //read int
    for (int shift = 0; shift < 32 && (response_byte = response_stream.read()) != -1; shift += 8) {
      length |= response_byte << shift;
    }
    if (response_byte == -1) {
      throw new IOException("Connection terminated by server");
    }

    //read the number of bytes specified by TCP response
    ByteArrayPlus response_data = new ByteArrayPlus();
    for (int i = 0; i < length && (response_byte = response_stream.read()) != -1; i++) {
      response_data.append_byte((byte) response_byte);
    }

    if (response_byte == -1) {
      return null;
    } else {
      return new TCPResponse(response_data.toByteArray());
    }
  }

  public TCPResponse(byte[] data) throws IOException {
    this.data = data;
  }
}
