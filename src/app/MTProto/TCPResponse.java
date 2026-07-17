package mtproto;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

public class TCPResponse {
  public byte[] data;

  public static TCPResponse from_stream(InputStream response_stream) throws IOException {
    if (response_stream == null) {
      throw new NullPointerException("Response stream is required.");
    }

    int length = 0;
    for (int shift = 0; shift < 32; shift += 8) {
      int responseByte = response_stream.read();
      if (responseByte < 0) {
        throw new IOException("Connection terminated inside a frame header.");
      }
      length |= responseByte << shift;
    }
    if (length < 4 || length > ProtocolLimits.MAX_WIRE_FRAME_BYTES) {
      throw new IOException("TCP frame length exceeds the device budget: " + length);
    }

    byte[] responseData = new byte[length];
    int offset = 0;
    while (offset < responseData.length) {
      int read = response_stream.read(responseData, offset, responseData.length - offset);
      if (read < 0) {
        throw new IOException("Connection terminated inside a TCP frame.");
      }
      if (read == 0) {
        int value = response_stream.read();
        if (value < 0) {
          throw new IOException("Connection terminated inside a TCP frame.");
        }
        responseData[offset++] = (byte)value;
      } else {
        offset += read;
      }
    }

    return new TCPResponse(responseData);
  }

  public TCPResponse(byte[] data) throws IOException {
    if (data == null || data.length < 4 || data.length > ProtocolLimits.MAX_WIRE_FRAME_BYTES) {
      throw new IOException("Invalid TCP response length.");
    }
    this.data = data;
  }
}
