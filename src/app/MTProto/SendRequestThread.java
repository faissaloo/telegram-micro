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
  static Queue requests = new Queue();
  static SendRequestThread instance;
  SocketConnection connection;

  public static void enqueue_request(TCPRequest request) {
    requests.enqueue(request);
  }

  public SendRequestThread(SocketConnection connection) {
    instance = this;
    this.connection = connection;
  }

  public void run() {
    try {
      OutputStream request_stream = connection.openOutputStream();
      System.out.println("REQUEST STREAM OPENED");
      connection.setSocketOption(SocketConnection.LINGER, 5);

      while (true) {
        try {
          SendRequestThread.sleep(1); //Don't peg the CPU
        } catch (InterruptedException e) {
          break;
        }

        while (requests.length() > 0) {
          //https://core.telegram.org/mtproto/transports#tcp
          //https://www.oracle.com/technetwork/systems/index-156878.html
          //send message
          TCPRequest message = ((TCPRequest) requests.dequeue());
          byte[] message_data = message.request_data();
          System.out.println("SENDING REQUEST");
          request_stream.write(message_data);
        }
      }

      System.out.println("CLOSING REQUEST STREAM");
      request_stream.close();
      connection.close();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }
}
