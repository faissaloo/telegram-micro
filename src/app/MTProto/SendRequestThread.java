package mtproto;

import java.lang.InterruptedException;
import java.lang.Thread;
import java.io.IOException;
import java.io.OutputStream;
import javax.microedition.io.SocketConnection;

import support.Queue;

public class SendRequestThread extends Thread {
  private final Queue requests = new Queue(ProtocolLimits.MAX_PENDING_REQUESTS);
  private final SocketConnection connection;
  private OutputStream request_stream;
  private boolean closed = false;

  public SendRequestThread(SocketConnection connection) {
    if (connection == null) {
      throw new NullPointerException("Socket connection is required.");
    }
    this.connection = connection;
  }

  /**
   * Applies bounded backpressure rather than retaining arbitrary request
   * payloads when the network writer stalls.
   */
  public synchronized void enqueue_request(TCPRequest request) {
    if (request == null) {
      throw new NullPointerException("TCP request is required.");
    }
    while (!closed && requests.length() >= ProtocolLimits.MAX_PENDING_REQUESTS) {
      try {
        wait();
      } catch (InterruptedException interrupted) {
        throw new IllegalStateException(
          "Interrupted while applying request backpressure."
        );
      }
    }
    if (closed) {
      throw new IllegalStateException("TCP request stream is closed.");
    }
    requests.enqueue(request);
    notifyAll();
  }

  public void run() {
    try {
      OutputStream stream = connection.openOutputStream();
      synchronized (this) {
        request_stream = stream;
      }
      connection.setSocketOption(SocketConnection.LINGER, 5);
      set_transport();

      while (true) {
        TCPRequest message;
        synchronized (this) {
          while (!closed && requests.length() == 0) {
            try {
              wait();
            } catch (InterruptedException interrupted) {
              closed = true;
            }
          }
          if (closed) {
            break;
          }
          message = (TCPRequest)requests.dequeue();
          notifyAll();
        }

        // https://core.telegram.org/mtproto/transports#tcp
        stream.write(message.request_data());
      }
    } catch (IOException exception) {
      System.out.println("REQUEST STREAM");
      System.out.println(exception);
    } finally {
      try {
        close();
      } catch (IOException ignored) {
      }
    }
  }

  public void close() throws IOException {
    OutputStream stream;
    synchronized (this) {
      closed = true;
      requests.clear();
      stream = request_stream;
      request_stream = null;
      notifyAll();
    }
    if (stream != null) {
      stream.close();
    }
  }

  // https://core.telegram.org/mtproto/mtproto-transports
  public void set_transport() throws IOException {
    OutputStream stream;
    synchronized (this) {
      stream = request_stream;
    }
    if (stream == null) {
      throw new IOException("TCP request stream is unavailable.");
    }
    stream.write(new byte[] {
      (byte)0xee, (byte)0xee, (byte)0xee, (byte)0xee
    });
  }
}
