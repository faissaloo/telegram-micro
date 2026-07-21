package runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;
import javax.microedition.midlet.MIDlet;

/** Account-free target-VM probe for hosted GCF socket behavior. */
public final class GcfSocketProbeMidlet extends MIDlet {
  private static final byte[] PING = new byte[] {'P', 'I', 'N', 'G'};
  private static final byte[] PONG = new byte[] {'P', 'O', 'N', 'G'};

  public void startApp() {
    try {
      runProbe();
      System.out.println("GCF SOCKET PROBE PASSED");
      System.exit(0);
    } catch (Throwable failure) {
      System.out.println("GCF SOCKET PROBE FAILED: " + failure.toString());
      failure.printStackTrace();
      System.exit(1);
    }
  }

  private static void runProbe() throws Exception {
    int port = parsePort(System.getProperty("telegrammicro.gcf.port"));
    String uri = "socket://localhost:" + Integer.toString(port);

    expectInvalidMode(uri);
    expectMissingPort();
    verifyReadOnlyMode(uri);
    verifyWriteOnlyMode(uri);
    verifyDuplexLifecycle(uri, port);
    verifyConvenienceStream(uri);
  }

  private static void expectInvalidMode(String uri) throws Exception {
    try {
      Connector.open(uri, 0).close();
      fail("invalid mode was accepted");
    } catch (IllegalArgumentException expected) {
    }
  }

  private static void expectMissingPort() throws Exception {
    try {
      Connector.open("socket://localhost", Connector.READ_WRITE).close();
      fail("missing client port was accepted");
    } catch (IllegalArgumentException expected) {
    }
  }

  private static void verifyReadOnlyMode(String uri) throws Exception {
    SocketConnection socket = (SocketConnection) Connector.open(
      uri,
      Connector.READ
    );
    try {
      try {
        socket.openOutputStream();
        fail("READ socket exposed an output stream");
      } catch (IOException expected) {
      }
    } finally {
      socket.close();
    }
  }

  private static void verifyWriteOnlyMode(String uri) throws Exception {
    SocketConnection socket = (SocketConnection) Connector.open(
      uri,
      Connector.WRITE
    );
    try {
      try {
        socket.openInputStream();
        fail("WRITE socket exposed an input stream");
      } catch (IOException expected) {
      }
    } finally {
      socket.close();
    }
  }

  private static void verifyDuplexLifecycle(String uri, int port)
      throws Exception {
    SocketConnection socket = (SocketConnection) Connector.open(
      uri,
      Connector.READ_WRITE,
      true
    );

    expect(socket.getAddress().length() > 0, "remote address missing");
    expect(socket.getLocalAddress().length() > 0, "local address missing");
    expect(socket.getPort() == port, "remote port changed");
    expect(socket.getLocalPort() > 0, "local port missing");

    socket.setSocketOption(SocketConnection.DELAY, 0);
    expect(socket.getSocketOption(SocketConnection.DELAY) == 0,
      "DELAY option mapping is inverted");
    socket.setSocketOption(SocketConnection.KEEPALIVE, 1);
    expect(socket.getSocketOption(SocketConnection.KEEPALIVE) != 0,
      "KEEPALIVE was not enabled");
    socket.setSocketOption(SocketConnection.LINGER, 1);
    expect(socket.getSocketOption(SocketConnection.LINGER) == 1,
      "LINGER was not applied");
    socket.setSocketOption(SocketConnection.RCVBUF, 1024);
    socket.setSocketOption(SocketConnection.SNDBUF, 1024);
    expect(socket.getSocketOption(SocketConnection.RCVBUF) > 0,
      "receive buffer is invalid");
    expect(socket.getSocketOption(SocketConnection.SNDBUF) > 0,
      "send buffer is invalid");

    InputStream input = socket.openInputStream();
    expectSecondInputRejected(socket);
    OutputStream output = socket.openOutputStream();
    expectSecondOutputRejected(socket);

    // MIDP keeps already-open streams alive after Connection.close().
    socket.close();
    expectClosedAccessorRejected(socket);
    output.write(PING);
    output.flush();
    output.close();

    byte[] response = new byte[PONG.length];
    readFully(input, response);
    for (int i = 0; i < PONG.length; ++i) {
      expect(response[i] == PONG[i], "loopback response changed");
    }
    input.close();
    try {
      input.read();
      fail("closed input stream remained readable");
    } catch (IOException expected) {
    }
    socket.close();
  }

  private static void verifyConvenienceStream(String uri) throws Exception {
    OutputStream output = Connector.openOutputStream(uri);
    output.write(new byte[] {'D', 'O', 'N', 'E'});
    output.close();
  }

  private static void expectSecondInputRejected(SocketConnection socket)
      throws Exception {
    try {
      socket.openInputStream();
      fail("input stream was opened twice");
    } catch (IOException expected) {
    }
  }

  private static void expectSecondOutputRejected(SocketConnection socket)
      throws Exception {
    try {
      socket.openOutputStream();
      fail("output stream was opened twice");
    } catch (IOException expected) {
    }
  }

  private static void expectClosedAccessorRejected(SocketConnection socket)
      throws Exception {
    try {
      socket.getPort();
      fail("closed connection accessor remained usable");
    } catch (IOException expected) {
    }
  }

  private static void readFully(InputStream input, byte[] data)
      throws Exception {
    int offset = 0;
    while (offset < data.length) {
      int count = input.read(data, offset, data.length - offset);
      if (count < 0) {
        fail("loopback response was truncated");
      }
      offset += count;
    }
  }

  private static int parsePort(String value) {
    int port;
    try {
      port = Integer.parseInt(value);
    } catch (Exception invalid) {
      throw new IllegalArgumentException("missing loopback port");
    }
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("invalid loopback port");
    }
    return port;
  }

  private static void expect(boolean condition, String message) {
    if (!condition) {
      fail(message);
    }
  }

  private static void fail(String message) {
    throw new IllegalStateException(message);
  }

  public void pauseApp() {
  }

  public void destroyApp(boolean unconditional) {
  }
}
