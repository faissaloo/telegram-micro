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
  static Queue responses = new Queue();
  static RecieveResponseThread instance;
  SocketConnection connection;

  static public TCPResponse dequeue_response() {
    return (TCPResponse) responses.dequeue();
  }

  static public boolean has_responses() {
    return responses.length() > 0;
  }

  public RecieveResponseThread(SocketConnection connection) {
    instance = this;
    this.connection = connection;
  }

  public void run() {
    try {
      InputStream response_stream = connection.openInputStream();
      connection.setSocketOption(SocketConnection.LINGER, 5);

      // THE NETWORK MONITOR IS A LIE
      TCPResponse response;
      while ((response = TCPResponse.from_stream(response_stream)) != null) {
        // unencrypted messages start with a 8 bytes/long of zero (auth_key_id)
        // encrypted messages start with a 8 bytes/long with a non-zero value (auth_key_id)
        //We should probably have different classes for recieved messages unlike the example
        responses.enqueue(response);
      }

      response_stream.close();
      connection.close();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }
}
