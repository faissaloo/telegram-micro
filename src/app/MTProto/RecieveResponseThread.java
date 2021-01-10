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
  Queue responses = new Queue();
  Queue waiting = new Queue();
  SocketConnection connection;
  InputStream response_stream;

  public TCPResponse dequeue_response() {
    return (TCPResponse) responses.dequeue();
  }
  
  public synchronized void wait_for_response() {
    waiting.enqueue(this);
    if (!has_responses()) {
      try {
          wait();
      } catch (InterruptedException e) {
        return;
      }
    }
  }

  public boolean has_responses() {
    return responses.length() > 0;
  }

  public RecieveResponseThread(SocketConnection connection) {
    this.connection = connection;
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
        responses.enqueue(response);
        
        while (waiting.length() > 0) {
          Object waiter = (Object)waiting.dequeue();
          synchronized(waiter) {
            waiter.notify();
          }
        }
      }

      close();
    } catch (IOException exception) {
    }
  }
  
  public void close() throws IOException {
    response_stream.close();
  }
}
