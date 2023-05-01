package support;

import bouncycastle.BigInteger;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import javax.microedition.io.SocketConnection;
import java.util.Hashtable;
import javax.microedition.io.Connector;

public class ServerConnection {
    String serverUrl = null;

    public ServerConnection(String url) {
        this.serverUrl = url;
    }
    
    public BigInteger getModPow(String base, long exponent, String modulus) {
        String response = request("m " + base + " " + exponent + " " + modulus);
        if (response.equals(""))
            return new BigInteger("-1");
        return new BigInteger(response);
    }
    
    public long getPrime(long product) {
        String response = request("p " + product);
        if (response.equals(""))
            return -1;
        return Long.parseLong(response);
    }
    
    private String request(String request) {
        if (serverUrl == null)
            return "";
        SocketConnection con = null;
        InputStream is = null;
        OutputStream os = null;
        String response = "";
        try {
            con = (SocketConnection)Connector.open("socket://" + serverUrl);
            con.setSocketOption(SocketConnection.LINGER, 5);
            is = con.openInputStream();
            os = con.openOutputStream();
            StringBuffer sb = new StringBuffer();

            os.write(request.getBytes());
            os.write("\n".getBytes());
            int chr = is.read();
            while (chr != -1 && (char)chr != '.') {
                sb.append((char) chr);
                chr = is.read();
            }
            response = sb.toString();
        } catch (IOException e) {
            System.out.println("Server connection: Catched " + e.getMessage());
        } finally {
            try {
                if (is != null)
                    is.close();
                if (con != null)
                    con.close();
                if (is != null)
                    is.close();
                if (os != null)
                    os.close();
            } catch (IOException e){
                System.out.println("Server connection: Catched " + e.getMessage());
            }
        }
        return response;
    }
}

