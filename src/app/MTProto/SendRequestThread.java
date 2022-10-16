package mtproto;

import java.lang.Thread;
import java.util.Vector;
import java.lang.InterruptedException;
import java.io.OutputStream;
import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import support.Queue;

public class SendRequestThread extends Thread {
  Queue requests = new Queue();
  SocketConnection connection;
  OutputStream request_stream;

  public synchronized void enqueue_request(TCPRequest request) {
    requests.enqueue(request);
    notify();
  }

  public SendRequestThread(SocketConnection connection) {
    this.connection = connection;
  }
  
  public synchronized void run() {
    try {
      request_stream = connection.openOutputStream();
      connection.setSocketOption(SocketConnection.LINGER, 5);

      set_transport();
      
      while (true) {
        while (requests.length() > 0) {
          //https://core.telegram.org/mtproto/transports#tcp
          //https://www.oracle.com/technetwork/systems/index-156878.html
          //send message
          TCPRequest message = ((TCPRequest) requests.dequeue());
          byte[] message_data = message.request_data();
          System.out.println("WRITING REQUEST");
          request_stream.write(message_data);
        }
        try {
          wait();
        } catch (InterruptedException e) {
          break;
        }
      }

      System.out.println("CLOSING REQUEST STREAM");
      close();
    } catch (IOException exception) {
      System.out.println("REQUEST STREAM");
      System.out.println(exception);
    }
  }
  
  public void close() throws IOException {
    request_stream.close();
  }
  
  //https://core.telegram.org/mtproto/mtproto-transports
  public void set_transport() throws IOException {
    request_stream.write(new byte[] {(byte)0xee, (byte)0xee, (byte)0xee, (byte)0xee});
  }
}
