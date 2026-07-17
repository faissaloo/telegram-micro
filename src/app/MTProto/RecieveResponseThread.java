package mtproto;

import java.lang.Thread;
import java.util.Vector;
import java.lang.InterruptedException;
import java.io.InputStream;
import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import support.ByteArrayPlus;
import support.Queue;

public class RecieveResponseThread extends Thread {
  Queue responses = new Queue(ProtocolLimits.MAX_PENDING_RESPONSES);
  SocketConnection connection;
  InputStream response_stream;
  boolean closed = false;

  public synchronized TCPResponse dequeue_response() {
    if (responses.length() <= 0) {
      return null;
    }
    TCPResponse response = (TCPResponse)responses.dequeue();
    notifyAll();
    return response;
  }
  
  public synchronized void wait_for_response() {
    while (!closed && !has_responses()) {
      try {
        wait();
      } catch (InterruptedException e) {
        return;
      }
    }
  }

  public synchronized boolean has_responses() {
    return responses.length() > 0;
  }

  public RecieveResponseThread(SocketConnection connection) {
    this.connection = connection;
  }

  private synchronized void notify_waiters() {
    notifyAll();
  }

  private synchronized void mark_closed() {
    if (closed) {
      return;
    }
    closed = true;
    notify_waiters();
  }

  public void run() {
    try {
      response_stream = connection.openInputStream();
      connection.setSocketOption(SocketConnection.LINGER, 5);

      // THE NETWORK MONITOR IS A LIE
      TCPResponse response;
      while ((response = TCPResponse.from_stream(response_stream)) != null) {
        // unencrypted messages start with a 8 bytes/long of zero (auth_key_id)
        // encrypted messages start with a 8 bytes/long with a non-zero value (auth_key_id)
        //We should probably have different classes for recieved messages unlike the example
        synchronized (this) {
          while (!closed && responses.length() >= ProtocolLimits.MAX_PENDING_RESPONSES) {
            try {
              wait();
            } catch (InterruptedException interrupted) {
              mark_closed();
              return;
            }
          }
          if (closed) {
            return;
          }
          responses.enqueue(response);
          notify_waiters();
        }
      }

      close();
      mark_closed();
    } catch (IOException exception) {
      System.out.println(exception);
      mark_closed();
    }
  }
  
  public void close() throws IOException {
    if (response_stream != null) {
      response_stream.close();
    }
    mark_closed();
  }
}
