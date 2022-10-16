package support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import javax.microedition.io.HttpConnection;
import java.util.Hashtable;
import javax.microedition.io.Connector;

public class ServerConnection {
    String serverUrl = null;

    public ServerConnection(String url) {
        this.serverUrl = url;
    }
    
    public long getModPow(String base, long exponent, String modulus) {
        Hashtable params = new Hashtable(3);
        params.put("base", base);
        params.put("exponent", Long.toString(exponent));
        params.put("modulus", modulus);
        String response = request("/modpow", params, "POST");
        if (response.equals(""))
            return -1;
        return Long.parseLong(response);
    }
    
    public long getPrime(long product) {
        Hashtable params = new Hashtable(1);
        params.put("number", Long.toString(product));
        String response = request("/primes", params, "POST");
        if (response.equals(""))
            return -1;
        return Long.parseLong(response);
    }
    
    private String getParameterString(Hashtable params) {
        String paramString = "";
        if (params.size() == 0)
            return paramString;
        Enumeration e = params.keys();
        boolean firstElem = true;
        while (e.hasMoreElements()){
            paramString += firstElem ? "" : "&";
            String key = e.nextElement().toString();
            String value = params.get(key).toString();
            paramString += key + '=' + value;
            firstElem = false;
        }
        return paramString;
    }
    
    private String request(String route, Hashtable parameters, String method) {
        String url = this.serverUrl + route;
        String paramString = getParameterString(parameters);
        if (method.equals("GET"))
            url += "?" + paramString;
        HttpConnection con = null;
        InputStream is = null;
        String response = "";
        try {
            con = (HttpConnection)Connector.open(url);
            con.setRequestMethod(method);
            con.setRequestProperty("User-Agent", "Profile/MIDP-2.0 Configuration/CLDC-1.0");
            
            if (method.equals("POST")) {
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");  
                con.setRequestProperty("Content-length", "" + paramString.getBytes().length);
                OutputStream os = con.openOutputStream();
                os.write(paramString.getBytes());
                //os.flush();
            }
            
            int responseCode = con.getResponseCode();
            if (responseCode == HttpConnection.HTTP_OK) {
                StringBuffer sb = new StringBuffer();
                is = con.openDataInputStream();
                int chr;
                while ((chr = is.read()) != -1)
                    sb.append((char) chr);
                response = sb.toString();
            } else {
                System.out.println("Could not execute request to " + route + ". HTTP code " + responseCode + ": " + con.getResponseMessage());
                return "";
            }
        } catch (IOException e) {
            System.out.println("Server connection: Catched " + e.getMessage());
        } finally {
            try {
                if (is != null)
                    is.close();
                if (con != null)
                    con.close();
            } catch (IOException e){
                System.out.println("Server connection: Catched " + e.getMessage());
            }
        }
        return response;
    }
}

